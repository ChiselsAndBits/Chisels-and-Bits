# Terrain renderer rewrite: handoff

**Date:** 2026-11-12, end of day

**Status:** Phase 2 is **not complete**. Both loaders can use the experimental
native terrain path, but split-face lighting parity, vanilla glass/fluid interfaces,
performance acceptance, and broader visual and sharding validation are still open.
Keep the experimental gates off by default. This is a handoff, not an acceptance
report.

## Goal and decisions

Chisels & Bits currently builds CPU-side `BakedQuad` geometry for chiseled blocks.
The goal is to replace that terrain path with a more efficient representation that
can survive Minecraft's upcoming per-section quad/template constraints, while
integrating into Minecraft's normal section rendering rather than drawing a
separate, incompatible pass.

The design evolved toward **Scena-owned native section integration**: C&B publishes
immutable bit/material data and resolved rectangles, and Scena writes them through
the active loader's terrain pipeline. This preserves the existing section lifecycle,
culling, lighting, layer routing, and sorting machinery. The proposed independent
GPU pass and GPU compute mesher were not pursued: the former would compromise
compatibility with native section ownership, shadows, culling and shader packs;
the investigated Minecraft GPU API did not provide the required compute facilities.

User decisions to retain:

- Minecraft updates animated sprites in the atlas while their UVs remain stable.
  Do not invent a separate animation system for the terrain producer.
- Full support for non-cubic source models is not required now. Keep the existing
  naive/legacy fallback instead of silently dropping such models.
- Faces surrounding provably sealed opaque air cavities may be omitted. Open
  tunnels and cavities reachable from outside must remain visible.
- Preserve the existing appearance and fallback behavior unless a difference is
  explicitly accepted. In particular, do not claim lighting or pixel parity based
  only on matching vertex attributes.

The Phase 2 exit criteria in `docs/rendering/07-poc-plan.md` include scene visual
parity or accepted differences, section compile p99 at or below baseline, and no
terrain-path `BakedQuad` allocations apart from V1's throwaway lighter quads.
None of the current measurements establishes all of those criteria.

## Repository and working-tree state

There are **two separate repositories**, both with substantial uncommitted changes,
including untracked source and test files:

| Repository | Location | Branch | HEAD |
|---|---|---|---|
| Chisels & Bits | this checkout | `feature/rendering-rebuild` | `fc83ab76` |
| Scena | `../Scena` | `feature/rendering-rework` | `192a188` |

Do not reset, clean, revert, commit, or selectively discard these changes without
review and user approval. Many important files are untracked, so plain `git diff`
and `git diff --check` do **not** show or validate all of the work. Inspect both
`git status --short` outputs when resuming.

C&B's `gradle.properties` pins the locally published Scena version
`26.2-rendering-dev-1`. The latest Scena changes were published locally after
the terrain adapter optimizations. After changing Scena, republish before testing
C&B:

```sh
cd ../Scena
VERSION=26.2-rendering-dev-1 ./gradlew publishToMavenLocal --offline --no-parallel --max-workers=1
cd ../Chisels-and-Bits
```

The environment is shared, not a task-dedicated sandbox. Use isolated disposable
worlds, do not touch user worlds, do not broadly clear caches, and do not leave
test clients running. Run Fabric and NeoForge client checks **sequentially**:
parallel launches previously caused a primary-monitor initialization failure.
An interrupted Fabric run at the end of the session left a test-created save;
its creation time and log were checked, and that specific disposable save was
removed. No matching test client was running at handoff.

## What is implemented

### C&B terrain producer and snapshots

- `ChiseledRenderSnapshot` normalizes supported bit sizes to a 16-grid immutable
  render snapshot and feeds the greedy rectangle mesher. Intrinsic geometry can be
  cached by policy; neighbour-dependent geometry is not treated as intrinsic.
- `StateEntryStorage.PackedSnapshot` and `copyPackedSnapshot()` capture independent
  palette and packed-long data and allow direct reads, including fields crossing
  a word boundary. `SimpleSnapshot.copyPackedStorage()` exposes that copy.
- Exact `SimpleSnapshot` accessors use the packed fast path when their size matches
  the configured bit size. Custom accessors and subclasses retain source-cell
  center sampling. Exact snapshots normalize all air states as the old accessor
  path did; custom accessors may explicitly return non-default air.
- `NeighbourBoundary.withAccessor` captures only the touching source plane rather
  than constructing a whole 4096-cell neighbour render snapshot. Captured planes
  are immutable. Replacing a plane now shallow-copies the outer plane array and
  shares the other immutable planes rather than cloning all six.
- `ChiseledBlockModelDataExecutor` captures source and neighbour data, performs
  off-thread builds, rejects stale generations, and rechecks captured neighbourhood
  and resource generations before publishing. Neighbour updates and client chunk
  transitions request rebuilds.
- `ChiseledTerrainRenderData` publishes direct metadata/geometry with a coherent,
  memoized baked fallback. Supported native compilation does not need to eagerly
  build that fallback. Unsupported models continue through the legacy path.
- The candidate supports preflighted SOLID, CUTOUT and TRANSLUCENT source faces,
  tint, material flags, source-model light emission, and atlas-animated sprites.
  Static source model keys and compatible face geometry are required. Waterlogged
  and incompatible mixed-material interfaces, dynamic models and other unsupported
  shapes remain on fallback.
- `ExteriorAir` allows the direct opaque path to omit only provably sealed local
  cavity faces. The normal legacy/default mesher semantics remain unchanged.

Important files:

- `api/src/main/java/mod/chiselsandbits/api/block/storage/StateEntryStorage.java`
- `api/src/main/java/mod/chiselsandbits/api/block/storage/StateEntryPalette.java`
- `core/src/main/java/mod/chiselsandbits/multistate/snapshot/SimpleSnapshot.java`
- `core/src/main/java/mod/chiselsandbits/client/model/meshing/ChiseledRenderSnapshot.java`
- `core/src/main/java/mod/chiselsandbits/client/model/meshing/NeighbourBoundary.java`
- `core/src/main/java/mod/chiselsandbits/client/model/meshing/RectangleMesher.java`
- `core/src/main/java/mod/chiselsandbits/client/model/meshing/DirectTerrainGeometryBuilder.java`
- `core/src/main/java/mod/chiselsandbits/client/model/data/ChiseledBlockModelDataExecutor.java`
- `core/src/main/java/mod/chiselsandbits/client/model/block/ChiseledTerrainRenderData.java`

Neighbour-plane coordinates are X `(u,v)=(y,z)`, Y `(x,z)`, and Z `(x,y)`.
The greedy rectangle row/column convention for Z is `(y,x)`. Do not change
generic-accessor sampling to arbitrary normalized 16-grid positions: custom
accessors must be sampled at their original source-cell centers.

### Boundary culling and fallbacks

`BoundarySelfCulling` uses immutable chiseled-neighbour planes to remove matching
fluid cells and mutually self-culling transparent glass cells. Matching fluid
families include source and flowing states when variants match. Partial overlaps
are split into visible rectangles. The supported native and baked fallback paths
apply the same boundary rule; fallback retains sealed-cavity faces.

Both loader live-world checks cover a native water block next to a legacy-fallback
chiseled neighbour, including an interior stair bit and a stair bit **on the
boundary**. In the boundary-stair fixture, matching water faces are removed,
while the stair face and its opposing water face remain. The scene is subsequently
edited back to water. This is one non-cubic case, not proof that all non-cubic
or non-affine fallback seams are correct.

Vanilla glass and fluid neighbours are **not** yet self-culled. Their geometry is
emitted independently, so changing only the C&B side is not a safe solution.
Vanilla fluids can have partial-height or sloped surfaces; a full-plane assumption
would hide legitimate faces.

### Scena native integration and sharding

Scena's terrain contracts, compatibility guards and section compiler hooks live
under `../Scena/core/.../rendering/terrain/` and the loader-specific
`TerrainGeometrySectionCompilerMixin` files. NeoForge uses the active
`ModelBlockRenderer`/lighter with transient lighter quads and a section-owned
writer. Fabric emits through Indigo/FRAPI. Both paths remain gated, and unknown
renderer hooks or incompatible renderers must continue to fall back rather than
silently producing partial geometry.

Scena's experimental CPU/GPU native sharding covers **opaque and cutout**
section meshes, not translucent meshes. The parent section retains ownership;
shards are published and cleaned up with the section lifecycle. A bounded
NeoForge 125-block checkerboard scene previously uploaded approximately
171.7 MB of SOLID geometry, survived an edit, and later rendered after restart
and travel away/return. After alignment fixes, the edited upload used six shards.
The user observed stutter; its cause has not been isolated.

This is not proof of the full worst-case scene, forced-shard Fabric rendering,
translucent sharding, Sodium or Iris compatibility. The original worst-case
baseline crashed on a staging-buffer allocation. See
`docs/rendering/08-scena-26.2-sharding.md` for the precise scope and history.

## Lighting blocker: why the clipping experiment is disabled

The current C&B boundary producer splits partially hidden rectangles and asks
the loader to light each fragment again. In an uneven-light NeoForge live fixture,
the original and split quads differ: one run found maximum interpolated differences
of `1.3125/255` in color and `0.46875/255` in light-coordinate components.
Native and baked **split** buffers agree in that fixture, but that does not make
them equivalent to the original unsplit face.

Scena now contains `FaceTriangleClipper`, optional immutable `TerrainQuad.clipRegions`,
validation of those regions, a NeoForge post-light triangle writer, and a Fabric
Indigo post-transform experiment. Degenerate quads encode triangles in the normal
QUADS topology. Tests show that clipping the original triangles can preserve
unquantized original-vertex **attribute** interpolation.

However, Minecraft 26.2's terrain vertex shader computes
`vertexColor = Color * sample_lightmap(Sampler2, UV2)` at each vertex and then
interpolates that product. Independently interpolating color and light coordinates
at newly introduced vertices does **not** generally preserve the shader's output,
even with a linear lightmap. The lightmap is dynamic, and fog distance is also
computed per vertex. Baking currently sampled lightmap colors into section geometry
is therefore not a general solution.

Consequently, `TerrainGeometryValidation.supportsCurrentTerrainShader` rejects
clip-region geometry, and **both runtime backends reject it before writing**.
C&B does not currently produce `clipRegions`. The guarded experiment is not a
fix for the existing relit split rectangles. Do not enable it or describe the
mathematical reference as pixel parity without solving the actual shader behavior,
quantization and sorting implications.

Useful files:

- `../Scena/core/src/main/java/com/communi/suggestu/scena/core/client/rendering/terrain/FaceTriangleClipper.java`
- `../Scena/core/src/main/java/com/communi/suggestu/scena/core/client/rendering/terrain/TerrainGeometryValidation.java`
- `../Scena/core/src/main/java/com/communi/suggestu/scena/core/client/rendering/terrain/NativeTerrainQuadWriter.java`
- `../Scena/fabric/src/main/java/com/communi/suggestu/scena/fabric/platform/client/rendering/ClippedTerrainOutput.java`
- `neoforge/src/main/java/mod/chiselsandbits/forge/gametest/TerrainWorldClientCheck.java`
- `docs/rendering/05-lighting.md`

Fabric Indigo applies culling, shading, tint and translation in its renderer
transform. Emitter transforms run last-pushed first, so merely pushing a model
transform does not provide post-light clipping. NeoForge's writer receives the
already-lit `QuadInstance`. The normal translucent builder and section sort still
own translucent output; splitting can also change sort behavior.

## Performance work and measurements

Snapshot microbenchmarks, from warmed 1,000-iteration sparse 16-grid runs:

| Operation | Time per operation | Allocated bytes per operation |
|---|---:|---:|
| Legacy temporary-storage snapshot reconstruction | 389.90 us | 3,476,568 |
| Generic direct accessor sampling | 78.81 us | 338,968 |
| Packed render snapshot | 44.97 us | 76,600 |
| Full neighbour snapshot and boundary capture | 48.36 us | 90,840 |
| Touching-plane-only boundary capture | 6.13 us | 9,264 |

These are microbenchmarks, not end-to-end Phase 2 results. A separate before/after
run of **six** touching-plane captures reduced allocation from 55,432 to 17,992
bytes per operation by sharing unchanged immutable planes. Times were 6.26 and
6.30 us respectively; that run does not demonstrate a speedup.

Today a paired **actual section-compilation probe** was added. It alternates native
and baked compilation of the same section, with five warmup pairs and 100 measured
pairs. The scene contains 27 isolated opaque half-block shapes and a translucent
boundary fixture. The baked comparison is the published direct model's memoized
fallback, so the probe measures warm section compilation, not initial model
generation or GPU upload. It checks the sorted translucent fixture bytes and,
in its latest version, verifies that each of the 27 specified positions actually
emitted through the native path in native samples and none did in baked samples.
Allocation is measured on the compiling thread; `-1` means unavailable.

Representative latest successful **position-checked** runs:

| Loader / sharding | Path | p50 ms | p99 ms | allocation p50 | allocation p99 |
|---|---|---:|---:|---:|---:|
| NeoForge / off | native | 0.442 | 0.748 | 92,360 | 94,208 |
| NeoForge / off | baked | 0.368 | 0.580 | 30,248 | 30,384 |
| NeoForge / on | native | 0.356 | 0.744 | 91,016 | 92,864 |
| NeoForge / on | baked | 0.273 | 0.540 | 28,904 | 28,960 |
| Fabric / off | native | 0.534 | 0.886 | 44,656 | 46,824 |
| Fabric / off | baked | 0.396 | 0.677 | 27,616 | 34,088 |

The final Fabric/sharding-on position-checked run was **interrupted by the stop
request**; it has no result. An earlier Fabric/sharding-on probe passed using a
native-emission count, before the final per-position check. Do not treat that
earlier run as verification of the latest position-check change. The small scene
does not force a shard even when sharding is enabled. `docs/rendering/benchmarks.md`
contains earlier successful values; its table has not been updated to these final
position-checked results.

The native path is currently slower and allocates more than its warm baked
comparison for this fixture. It does **not** satisfy the performance exit
criterion. Before today's adapter changes, the same style of NeoForge probe
reported native allocation p50 of 247,944 bytes. Changes since then include:

- Grouping NeoForge resolved quads by cull direction in one pass rather than
  building multiple stream pipelines and lists for each direction.
- Avoiding clip-association maps when no clipped geometry is present.
- Avoiding repeated stream/array allocations in compatibility and shader checks.
- Avoiding repeated debug-only boundary scans once the one-time diagnostic has
  already been reported.
- Reusing the vanilla `VertexConsumer.putBlockBakedQuad` writer for ordinary
  NeoForge quads.

A local JFR recording sampled `Vector3f` and transient `BakedQuad` allocation in
NeoForge's lighter adapter, directional group wrappers, and validation iterators.
The recording is a profiling aid, not a complete allocation accounting or a
ranking of every hotspot. The current native result still has a substantial gap.
Do not cache mutable/shared lighter data or persistent baked quads merely to
improve this microbenchmark without preserving thread safety, fallback semantics
and the Phase 2 allocation requirement.

Benchmark implementation:

- `core/src/main/java/mod/chiselsandbits/debug/bench/TerrainWorldCompileBenchmark.java`
- `core/src/main/java/mod/chiselsandbits/debug/bench/TerrainWorldMeshCapture.java`
- Both loader `SectionCompilerMixin` benchmark hooks.
- The paired probe and its limitations are also described in
  `docs/rendering/benchmarks.md`.

## Verification completed and what is not verified

- NeoForge server GameTests passed all 18 required tests after packed-snapshot
  and boundary-plane work. They cover six directions, accessor semantics,
  mutation independence, packed-word edge cases and cavity behavior.
- Both loader model-parity checks passed after the packed-snapshot and guarded
  shader experiment. They cover source metadata, materials, glass/fluid boundary
  geometry, synthetic emission and non-affine rejection.
- Scena `:core:test`, `:fabric:test` and `:neoforge:test` passed after today's
  adapter optimizations, and the result was published to Maven local.
- Both loader disposable live-world checks passed with the mixed fallback and
  boundary-stair fixture. They exercised sharding on and off, but the fixture did
  not force a shard.
- The final position-checked paired probe passed for NeoForge with sharding off
  and on, and Fabric with sharding off. Fabric with sharding on was interrupted.
- `git diff --check` passed in both repositories at handoff, but it does not
  inspect untracked files.
- The live translucent fixture checks native/baked **split** vertex buffers and
  sort indices, not unsplit pixel equivalence. There is no comprehensive screenshot
  or pixel comparison of the Phase 0 scenes.

Detailed evidence:

- `build/six-planes-before.log` and `build/six-planes-after.log`
- `build/packed-fabric-parity.log` and `build/packed-neo-parity.log`
- `build/section-final-neoforge-false.log`
- `build/section-final-neoforge-true.log`
- `build/section-final-fabric-false.log`
- `build/section-final-fabric-true.log` is **interrupted**, not a pass.
- `build/section-benchmark-count-fabric-sharded.log` is the earlier successful
  count-checked, rather than position-checked, sharding-on run.
- `../Scena/build/terrain-compile-optimization.log`

Build logs are local artifacts, not necessarily durable across a clean checkout.
The design and result summaries under `docs/rendering/` are the longer-lived
references.

## Resume tomorrow: ordered next steps

1. **Establish the exact current state.** Read this document, then inspect
   `git status --short` in both repositories and review untracked files as well as
   diffs. Confirm the local Scena artifact still corresponds to the checkout.
   Do not restart the previously cancelled Plannotator goal setup.

2. **Finish validation of the latest probe change.** Run the Fabric
   position-checked benchmark with sharding enabled:

   ```sh
   ./gradlew --offline :fabric:verifyTerrainWorld \
     -PcandbTerrainWorldCheck=true -PcandbTerrainCompileBenchmark=true \
     -PscenaTerrainSharding=true --no-parallel --max-workers=1
   ```

   Then update `docs/rendering/benchmarks.md` with the latest successful
   position-checked values, clearly distinguishing small non-forced-shard
   measurements from actual shard acceptance.

3. **Resolve split-face lighting and sorting.** Determine whether a
   shader-compatible representation or a conservative alternative can preserve
   the original rendered result while respecting native section ownership,
   fallback, translucent sort and user requirements. Keep the experimental
   clip-region writer guarded until actual shader-output correctness is proven.
   Include dynamic lightmap, fog, tint, emission, vertex quantization, winding,
   transparent ordering, and visual/pixel comparisons in the acceptance plan.

4. **Design coordinated two-sided vanilla interfaces.** A fully covered matching
   vanilla-glass interface might be a bounded first case because whole faces
   could be removed without introducing vertices. This is only an idea, **not an
   implemented or verified design**. Both vanilla and C&B sides need coherent
   immutable compile-time neighbour data, correct rebuilds on edits/chunk
   transitions, and safe behavior when the native path or a renderer hook is
   unavailable. `Block.shouldRenderFace(state, neighbourState, direction)` has
   no world position, so a suitable positioned renderer integration is needed.
   Do not treat vanilla fluid sides as full planes; their partial and sloped
   surfaces need separate handling.

5. **Improve and measure section-compile performance.** Use the paired probe
   to check changes, but also run representative Phase 0 scenes with comparable
   baseline conditions. Investigate the remaining native per-quad lighter and
   adapter allocation. Measure p99 and allocations on both loaders, including
   cold/model-generation costs where appropriate; the warm memoized-fallback
   comparison alone is not the full acceptance benchmark.

6. **Expand lifecycle, fallback and sharding acceptance.** Cover more non-cubic
   and non-affine boundary cases, both loaders' edits and chunk transitions,
   and a genuinely forced opaque/cutout shard on Fabric as well as NeoForge.
   Preserve the documented limitation that translucent sharding is not provided.
   The full worst-case scene remains unverified and can consume substantial
   resources; use only isolated disposable worlds and a bounded progression.

7. **Complete Phase 2 acceptance before declaring it done.** Capture visual or
   pixel comparisons for the relevant scenes, or have the user explicitly accept
   documented differences; establish comparable p99/allocation results; verify
   both loader backends and bounded sharding; and confirm the terrain-path
   allocation condition. Sodium and Iris are later phases, but incompatible
   renderers must continue to select a safe fallback now.

## Useful commands

Run client checks one at a time:

```sh
# Scena tests and local publication
cd ../Scena
VERSION=26.2-rendering-dev-1 ./gradlew :core:test :fabric:test :neoforge:test \
  publishToMavenLocal --offline --no-parallel --max-workers=1
cd ../Chisels-and-Bits

# Snapshot/mesher tests; opt-in microbenchmark is included with this property
JAVA_TOOL_OPTIONS=-Dchiselsandbits.snapshotBenchmark=true \
  ./gradlew --offline :neoforge:runGameTestServer --no-parallel --max-workers=1

# Loader parity checks, sequentially
./gradlew --offline :neoforge:verifyTerrainModelParity \
  -PcandbTerrainModelParity=true --no-parallel --max-workers=1
./gradlew --offline :fabric:verifyTerrainModelParity \
  -PcandbTerrainModelParity=true --no-parallel --max-workers=1

# Disposable client-world checks, sequentially
./gradlew --offline :neoforge:verifyTerrainWorld \
  -PcandbTerrainWorldCheck=true --no-parallel --max-workers=1
./gradlew --offline :fabric:verifyTerrainWorld \
  -PcandbTerrainWorldCheck=true --no-parallel --max-workers=1
```

Add `-PcandbTerrainCompileBenchmark=true` for the paired warm compile probe and
`-PscenaTerrainSharding=true` to enable sharding. A flag being enabled does not
prove that a given fixture actually produced a shard.

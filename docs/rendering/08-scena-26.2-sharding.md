# Scena 26.2 native sharding: first implementation contract

This is the first slice of Phase 2, before C&B switches to the rectangle mesher.
It must also work with the existing BakedQuad path: the immediate problem is an
oversized native mesh, independently of how that mesh was produced.

## Scope and limits

- Implement in the prepared Scena `feature/rendering-rework` branch.
- Initially shard only SOLID/CUTOUT meshes using vanilla's sequential quad index
  buffer. Leave translucent/custom-index uploads unchanged; do not silently drop
  them, reorder them, or claim that this milestone supports oversized translucency.
- Keep vanilla's pipelines, pass scheduling, section uniforms, visibility and
  fade. No custom terrain pass or shader.
- Sodium's own meshes do not use these vanilla upload hooks. This is not a
  Sodium adapter or a claim of verified Iris compatibility. The experimental
  vanilla mixins are skipped when Sodium is present.
- Do not enlarge staging buffers or heaps to hide an unbounded allocation.
- Gate the experimental implementation with an explicit startup property,
  `scena.experimentalTerrainSharding`, until client acceptance is established.

## Ownership contract

The existing `CompiledSectionMesh` remains the parent: it owns visibility,
block entities and publication into its `RenderSection`. An attached shard set
owns opaque layer metadata and child `SectionMesh` allocation keys.

Each child describes a quad-aligned vertex byte range, a local index count and
upload state. It is an allocation key in the dispatcher's existing
`UberGpuBuffer<SectionMesh>`, not a separately visible world section.

An allocation must fit the actual staging capacity and vertex heap. Prefer
reading the capacities rather than relying on nominal constructor constants:
the persistently mapped staging implementation has half the requested capacity.
Align slices to four vertices of the active layer's vertex format. All offset
and index arithmetic must be checked before narrowing to an integer.

## Compile, retry and publication

1. Intercept `RenderSection.addSectionBuffersToUberBuffer` only for eligible
   over-budget vertex uploads. Under the dispatcher's existing copy lock,
   initialize the layer's complete shard plan before staging its first slice.
2. Remember accepted slices across retries. A full staging buffer is backpressure,
   not an error: return false to vanilla's existing retry loop. Do not copy or
   replace already accepted slices on each retry.
3. Keep the CPU source buffer owned by vanilla's `MeshData` until every slice has
   been copied into staging. No raw ByteBuffer view may outlive that ownership.
4. Each upload callback counts a child at most once. Mark the parent layer's
   vertex upload complete only after every child has reached the GPU. Use
   vanilla's existing layer-completion/publication mechanism, including any
   unsharded layers in the same parent.
5. Publication is all-or-nothing. The previous section mesh remains visible until
   every required layer is ready; drawing a partially uploaded replacement is
   forbidden.

## Cancellation, replacement and teardown

- Associate pending shard sets with their owning section and compile generation.
  A callback from a superseded generation must never publish an old mesh.
- Cancellation/reset must release pending child keys, including staged entries,
  even if the worker has already returned after staging everything.
- Do not release the currently visible old mesh merely because a replacement
  starts compiling. Release its children when vanilla releases that parent.
- Release metadata and native allocations together under the same copy lock;
  repeated cleanup must be safe. Avoid static maps retaining worlds/sections.
- Dispatcher disposal closes native buffers; no later callback may publish or
  retain an orphan shard set.

## Native draw integration

`LevelRenderer.prepareChunkRenders` currently emits one `RenderPass.Draw` per
section/layer from `getSectionDraw` and `getRenderSectionSlice`.

For a sharded layer it must emit one draw per uploaded child, with that child's
vertex buffer, base vertex and local sequential index count. Reuse the parent's
`ChunkSection` uniform uploader and native layer/pass. The default sequential
index buffer need only cover the largest child, not the combined parent mesh.

No giant placeholder allocation for the parent is permitted. Preserve the
unsharded extraction path exactly. Use precise mixin descriptors and fail loudly
if expected hooks no longer exist.

## Verification

Automated tests must cover quad-aligned partitioning, tail slices, invalid or
overflowing counts, retry progress, out-of-order/duplicate completion,
cancellation after staging, replacement generations and idempotent cleanup.
Compile both loader modules. Testable state/partition logic belongs in shared
Scena code; keep loader mixins thin.

These tests do not establish GPU rendering correctness. The upload milestone's
client check is an opaque mesh above staging capacity but below CPU builder
limits, including edits/rebuilds and unload/reload without oversized uploads or
stale publication. Record unverified client checks explicitly.

### Additional limit found during the lifecycle investigation

Upload-time slicing cannot by itself satisfy the full `worst_case` acceptance
criterion. In 26.2, `BufferBuilder.beginVertex` enforces 16,777,215 vertices and
`ByteBufferBuilder.build` rejects individual results over 2,147,483,647 bytes.
An isolated 16-grid stone/air checkerboard has 12,288 exposed bit faces.
Repeating it across 4,096 blocks is roughly 50 million quads before inter-block
culling: about 201 million vertices and 6 GiB at 32 bytes per vertex. Boundary
culling changes the exact count, not the conclusion.

The reported 362 MiB upload failure was an earlier, partially built section, not
proof that the final scene's CPU mesh fits vanilla's builder limits.

Consequently, CPU builders must also flush during section compilation, retaining
multiple bounded `MeshData` results per layer and feeding those results into the
same allocation/publication lifecycle. Raising native
vertex counts or merely raising the staging capacity is not a solution. Only
after both slices are integrated can the full `worst_case` scene be an acceptance
test. Its memory and rendering cost remain substantial even after sharding.

## Implementation status

The local Scena implementation includes CPU builder rotation, bounded native GPU
allocations, retry tracking, parent publication, native draw expansion and
generation-aware cancellation/teardown for opaque and cutout layers.

CPU builders rotate at quad boundaries when vanilla reacquires a layer builder
and the next quad would exceed 32 MiB. Vanilla's block quad outputs reacquire it for every quad.
Other producers that retain a builder indefinitely can bypass this boundary;
this is not a universal bound for arbitrary third-party geometry producers.
Each rotated builder has its own backing allocation, avoiding a single aggregate
Java buffer. GPU slices additionally respect the actual staging/heap capacities.

Compile generations are captured before compilation, not at first staging, so
cancellation before the first upload cannot adopt the new generation. Cancelling
GPU state does not free CPU results still owned by a worker: the worker releases
them through vanilla's results cleanup, or after successful staging.

Both Scena loader artifacts are published locally as `26.2-rendering-dev-1`.
Both C&B loader modules compile against them; all 13 server GameTests and
19 focused Scena tests pass.
The new rectangle mesher remains standalone, not wired into terrain rendering.
NeoForge client startup with the gate enabled reaches resource loading; a
temporary, uncommitted JVM audit additionally force-loaded all terrain mixin
targets, including the otherwise lazy compile task, without transformation
errors. This does **not** establish that oversized terrain uploads,
editing/cancellation or unload/reload work. Fabric client transformation and GPU
behavior remain unverified.
The bounded opaque NeoForge in-world check has passed; the full original
`worst_case` remains unverified, so do not treat that crash as resolved yet.
Shard allocation keys use object identity, including when several shards have
the same size; value-equal keys would overwrite one another in the native heap.

## Following slice: terrain producer API

After allocation/draw ownership is proven, implement the model opt-in and
native-format geometry sink from [03](03-scena-api.md). The sink emits into these
bounded native allocations; it must not introduce a second independent GPU
resource lifecycle. The immutable C&B snapshot stays owned by C&B.

Translucent allocation splitting requires its own ordering contract before
implementation; independent shard sorts cannot provide section-wide order.

## Local development build

C&B temporarily pins Scena to `26.2-rendering-dev-1`, published only to Maven
Local from the sibling checkout. The normal repository setup already includes
Maven Local; no developer-specific path is committed.

From the Scena checkout:

```
./gradlew -Plocal.version=26.2-rendering-dev-1 :core:publishToMavenLocal :neoforge:publishToMavenLocal :fabric:publishToMavenLocal
```

Then from C&B:

```
./gradlew :neoforge:runClient -PscenaTerrainSharding=true
```

The Gradle client run resolves NeoGradle's asset placeholders using its
downloaded asset index. Without those assets the title panorama and sounds are
missing even when terrain sharding is disabled.

For an IntelliJ launch, reimport the Gradle project and add
`-Dscena.experimentalTerrainSharding=true` to the client JVM options. Rebuild and
restart after each local publication. The temporary version must be replaced with
a published Scena release before distribution.

Use the same client with the property off for a control run. Do not generate the
oversized scene in an important world: disabling sharding can make that area
unloadable by the vanilla renderer.

For the first in-world check, use a disposable creative world with cheats and no
Sodium, and run `/candb scene sharding 0 64 0` near those coordinates. The
5×5×5 checkerboard is much smaller than `worst_case` but can still consume
hundreds of MiB and cause long pauses; the command clears a one-block margin.
The explicit origin keeps the cube within one render section. Leave the client
running until the scene settles, then use F3+A and edit a block; unload and reload
the area if possible. Capture `neoforge/runs/client/logs/latest.log`, including
the `Experimental terrain sharding` and `Experimental terrain shards uploaded`
lines, and any crashes. A scene that never reports sharding is not an acceptance
test. Do not run `worst_case` until this smaller case succeeds.

On 2026-11-12, a NeoForge run rendered the 125-block scene and correctly
updated it after one block was broken. The initial SOLID upload was 171,673,600
bytes in 11 shards, exceeding both the nominal staging-buffer capacity and one
vertex heap; all upload callbacks completed. The rebuild uploaded 170,297,344
bytes in 11 shards. This established a first GPU-path success, but not unload
and reload, Fabric, or the full `worst_case`. The run revealed tiny extra shards
when the 32 MiB threshold was not divisible by the quad size; CPU rotation now
flushes before the next quad exceeds the threshold. In a subsequent run the
edited scene loaded and rendered after a restart and after travelling 10,000
blocks away and returning. Each logged 170,297,344-byte SOLID upload used six
shards and completed. The user observed stutter on initial load and return.
The logs show these uploads and rebuilds on the render thread; they do not
isolate whether mesh compilation, staging, GPU work or other loading caused
the stutter. F3+A was not exercised on the user's Mac.

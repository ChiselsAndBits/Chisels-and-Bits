# Scena API proposal: terrain geometry

Goal: give mods (C&B first) a **renderer-agnostic way to emit block geometry during section
compilation**. The geometry goes straight into the native mesh of whichever terrain renderer is
active (vanilla 26.2, vanilla 26.4, Sodium), so culling, sorting, shadows and shader packs work
unchanged.

## 1. Opt-in: a model interface, not a block registry

```java
package com.communi.suggestu.scena.core.client.rendering.terrain;

/** Implemented by a BlockStateModel that wants to emit its own terrain geometry. */
public interface TerrainGeometryBlockStateModel {

    /** Called on the section-compile worker thread. Must be thread-safe and must not touch the live level. */
    void emitTerrainGeometry(ITerrainGeometryContext context);

    /** True if Scena has an emitter for the active renderer; otherwise the model's normal collectParts path is used. */
    @ApiStatus.NonExtendable
    static boolean isSupported() { ... }
}
```

Why put it on the model:

- `ChiseledBlockStateModel` already exists and is registered through
  `IRegisterBlockStateModelEvent`. Nothing new to register.
- **Graceful fallback:** if a renderer we don't support is active (Embeddium, a future
  renderer, ...), the hook never fires. The model then falls back to its existing
  `DataAwareBlockStateModel.collectParts` path (BakedQuads), which still renders, just more slowly.
- Hook sites only need one `instanceof` check per block.

## 2. Context and sink

```java
public interface ITerrainGeometryContext {
    BlockAndTintGetter region();          // the renderer's snapshot (RenderSectionRegion / LevelSlice)
    BlockPos pos();                       // absolute
    BlockState state();
    @Nullable Object blockEntitySnapshot(); // see section 5
    ITerrainLighter lighter();            // smooth light + AO that exactly matches the active renderer
    ITerrainQuadSink sink(ChunkSectionLayer layer);
    TerrainBackend backend();             // VANILLA_LEGACY, VANILLA_VERTEX_PULLING, SODIUM (diagnostics/tuning)
}

public interface ITerrainQuadSink {
    /**
     * Fast path for C&B: an axis-aligned rectangle on a block-local plane.
     * Coordinates are in 1/16 block units (0..16). UVs are derived from the rectangle and the sprite
     * (the same as vanilla's uvlock behaviour for full-texture faces).
     */
    void axisAlignedQuad(Direction face, int depth, int minA, int minB, int maxA, int maxB,
                         TextureAtlasSprite sprite, int argbTint, TerrainLight light);

    /** General path: four arbitrary vertices. */
    void quad(TerrainVertex v0, TerrainVertex v1, TerrainVertex v2, TerrainVertex v3,
              Direction cullFace, TextureAtlasSprite sprite, TerrainLight light);
}

public interface ITerrainLighter {
    /** Computes 4x packed light + 4x AO brightness for a quad, using the active renderer's algorithm. */
    TerrainLight compute(Direction face, float[] corners /* 12 floats, block-local */, boolean shade);
}
```

- The lighter uses vanilla's AO calculation under vanilla and Sodium's `LightPipeline` under
  Sodium. So "matches vanilla exactly" really means **matches whatever the player sees on the
  neighbouring vanilla blocks**.
- `TerrainLight` is a small reusable mutable holder (packed light ×4 and brightness ×4), so the
  hot loop doesn't allocate.

## 3. Hook sites (all in Scena)

| Backend | Hook | Sink writes to |
|---|---|---|
| Vanilla 26.2 | `SectionCompiler.compile`: `@WrapOperation` around `ModelBlockRenderer.tesselateBlock` | `BufferBuilder` (`DefaultVertexFormat.BLOCK`) of the layer |
| Vanilla 26.4 | Same call site | `SectionBufferBuilder.addQuad(...)` with C&B templates, **plus sharding** (below) |
| Sodium (Fabric + NeoForge) | `ChunkBuilderMeshingTask.execute`: `@WrapOperation` around `BlockRenderer.renderModel` | `ChunkVertexEncoder.Vertex[4]` → `buffers.get(pass).getVertexBuffer(facing).push(vertices, materialBits)`; translucent quads go through `TranslucentGeometryCollector.appendQuad`; `addSprite` for animated textures |
| Items / BESR / previews | No hook. Adapter `ITerrainQuadSink → VertexConsumer` | `SubmitNodeCollector.submitCustomGeometry` |

Notes:

- Use **`@WrapOperation` (MixinExtras)**, not `@Redirect`. The Fabric renderer API already
  `@Redirect`s `tesselateBlock` in `SectionCompilerMixin`, and redirects don't chain.
- On Sodium, Iris sets its per-block context (`mc_Entity`, `at_midBlock`) around
  `renderModel`. Wrapping the same call should keep that context active.
  **Needs verifying against Iris's mixins.**
- Invalidation needs no new API. `LevelRenderer.setSectionDirty` / `setBlocksDirty` is already
  intercepted by Sodium.

### 26.4 sharding (Scena mixins, also fixes vanilla's silent overflow for everyone)

1. `SectionBufferBuilder`: when the number of `bakedQuads` reaches 65,536, flush the current
   templates after the faces written so far and start a new shard. Record `(faceStart, faceCount,
   templateStart)`.
2. `MeshData` / `CompiledSectionMesh` / `SectionMesh.SectionDraw`: carry the shard table.
3. `LevelRenderer.extractSectionDrawGroups`: one `ChunkSectionInfo` + `IndexedDraw` per shard.
4. `MeshData` centroid extraction and translucent sorting: must be shard-aware (see open question
   1 in [02-design.md](02-design.md)).

Shards stay rare as long as C&B keeps its templates generic: a template is
`(face, depth, rect, sprite)`, while position and light are per face.

## 4. Loader / module layout

```
scena-core      api: TerrainGeometryBlockStateModel, ITerrainGeometryContext, ITerrainQuadSink, ITerrainLighter
scena-common    vanilla emitters (26.2 BufferBuilder / 26.4 SectionBufferBuilder + sharding mixins)
scena-sodium    optional compat module (common source set, loaded only if Sodium is present)
scena-neoforge / scena-fabric   wiring only
```

Version split (26.2 vs 26.4) lives in Scena's version branches. C&B's mesher only sees the
`ITerrainQuadSink` interface, so it is shared across versions.

## 5. Thread safety of chiseled data

Compile workers read block-entity data while the main thread may be changing it. Options:

- **A. Immutable snapshot per change (recommended).** C&B publishes an immutable, versioned
  "render snapshot" of the bit storage (palette + packed bits + per-material bitsets) whenever the
  block entity changes. The worker reads the latest reference. That makes the snapshot cheap and
  lock-free, and it replaces today's `ChiseledBlockModelDataExecutor` model data.
- B. Scena calls `snapshot(BlockEntity)` on the main thread when the section region is copied
  (vanilla `SectionCopy` / Sodium `ClonedChunkSection`). Cleaner contract, but more mixins.

## 6. What C&B builds on top of this

- `ChiseledMesher` (core, loader-agnostic): snapshot → per-material, per-face 2D bit masks →
  greedy rectangles → `axisAlignedQuad`. Internal faces are culled using neighbouring bits, and
  neighbouring blocks are handled through `region()`.
- `ChiseledBlockStateModel implements TerrainGeometryBlockStateModel`.
- Remove the per-block `BakedQuad` cache (`ChiseledBlockModelInformation*`, `FaceManager`, the quad
  builders) from the terrain path. Keep a slim version only for the fallback and for items.

## Decisions (round 4)

- Opt-in: **model interface** (`TerrainGeometryBlockStateModel`).
- Thread safety: **option A**, an immutable versioned snapshot owned by C&B.
- 26.4 sharding: **Scena-only mixins**. Not upstreamed for now.
- Sodium: goes in Scena's **existing** optional Sodium compat module.

## 7. Incremental API implementation history (2026-11-12)

The following paragraphs describe successive experiments; the direct-geometry status
at the end supersedes the earlier rectangle-model descriptions.

Scena contains the opt-in model contract and fallback-first dispatch under
`com.communi.suggestu.scena.core.client.rendering.terrain`. Fabric and NeoForge now wrap the vanilla
`SectionCompiler.compile` call to `ModelBlockRenderer.tesselateBlock` with MixinExtras `@WrapOperation`. The API has a separate startup gate,
`-Dscena.experimentalNativeTerrainGeometry=true`; it is independent of
`-Dscena.experimentalTerrainSharding=true`. Dispatch invokes the ordinary model fallback unless the gate is enabled,
the context reports a supported backend, and the model's side-effect-free preflight accepts the complete case.
An exception during emission is not converted into fallback, because doing so after partial native writes could
duplicate geometry.

The adapter remains BakedQuad-assisted: the compile context re-emits a `BlockStateModel` through the same
`ModelBlockRenderer`, `BlockQuadOutput`, per-layer `BufferBuilder`, tint/light/AO, culling, and translucent-sort path
as the original call. C&B now captures an immutable area snapshot on the client thread and builds its render snapshot
and rectangles on the model-building worker pool, using
`ChiseledRenderSnapshot`/`RectangleMesher` for a narrow candidate subset: non-empty shapes whose visible faces are all
strictly inside the owning block. The mesher uses `ANY_OCCUPIED`, matching the old mesher's internal-face rule.
Rectangles are converted through the existing `QuadGenerationUtils`/`FaceManager` source-face extraction and clipping,
then stored as ordinary `BlockStateModelPart` quads. Vanilla remains responsible for tint, light/AO, layer routing, and
translucent handling. The same captured accessor feeds both old and rectangle model-data builds.

Shapes with any face on the block boundary or empty geometry use the existing
`ChiseledBlockModelInformation` built by the old mesher; absent model data or unsupported context leaves the original
C&B model operation untouched. Both branches use the same vanilla `ModelBlockRenderer` output path. The interior-only
gate avoids needing live neighboring-block boundary data; it does not yet provide a direct native quad sink, eliminate
BakedQuad allocation, or demonstrate a performance win. Geometry coverage is compared against `GreedyMeshBuilder` in
the focused NeoForge game tests, but no visual world parity or translucent sorting parity has been claimed. Sodium
detection independently disables this vanilla hook; no Sodium/Iris compatibility is implemented or claimed. The
sharding hooks remain independent and unchanged.

The C&B rectangle candidate is now conservatively preflighted: every occupied source material must be non-fluid,
occluding, solid-rendering, and free of a block entity; all source model cache keys must be static; and every extracted
source quad must use the SOLID layer, have a non-animated sprite, and emit no light. Empty shapes and any geometry
touching the block boundary are rejected. Rejections retain the existing model-data result and baked-model path;
translucent, cutout, animated, emissive, dynamic, and otherwise unsupported models are not sent through the rectangle
candidate.

The candidate's material indices must also resolve to the same tint sources as the
legacy model. The two meshers can discover materials in different orders, while C&B's
color provider uses the legacy model's material table. Incompatible tinted indices
therefore fall back; reordering untinted materials remains supported.

The old model-data pipeline is unchanged when Scena's geometry gate is off: it does not defer to the client thread or
capture a terrain snapshot. With the gate enabled, C&B captures one immutable area accessor on the client thread and
uses it for both the ordinary model and rectangle candidate. Per-block request generations reject stale asynchronous
results before publication, and model data is published through a volatile field. If snapshot capture fails, the
generation-guarded legacy builder remains the fallback. This prevents a late older edit from replacing newer data,
but does not constitute a client-world visual parity test.

To observe an actual runtime selection, launch with both
`-Dscena.experimentalNativeTerrainGeometry=true` and
`-Dchiselsandbits.debugNativeTerrainGeometry=true`. When the vanilla hook reaches a C&B model with data, the first
rectangle-subset selection and first legacy-baked-model selection each produce one client chat message and one log
entry for the process. This diagnostic proves dispatch selection only, not the appearance or correctness of rendered
pixels.

For the Gradle NeoForge client, use
`./gradlew :neoforge:runClient -PscenaNativeTerrainGeometry=true -PcandbDebugTerrainGeometry=true`;
add `-PscenaTerrainSharding=true` to enable the separate sharding experiment.

Disposable client smoke runs on Fabric (with Fabric API/Indigo loaded) and NeoForge both loaded the transformed
`SectionCompiler` and exited before world selection with the experimental gate enabled. This verifies transformation
and hook composition, not C&B dispatch or rendered output. The temporary smoke mixins are removed from the source
configuration.

In a subsequent NeoForge in-world run, the `terrain_subset` scene selected the rectangle
path for its inset stone cube and the legacy path for its boundary-touching slab. The
user reported that it appeared to work; broad output-level and pixel parity remain unverified.

An isolated, self-terminating NeoForge client parity check now compares actual
client-baked legacy and rectangle quad output for three interior opaque fixtures.
Positions, UVs, culling, tint/material and AO metadata match for those fixtures;
pixel-level lighting, other shapes and Fabric remain unverified. Run it with
`./gradlew :neoforge:verifyTerrainModelParity -PcandbTerrainModelParity=true`.

### Direct 26.2 sink: remaining integration constraints

The current vanilla `BlockQuadOutput` accepts a `BakedQuad` and a `QuadInstance`; it is
not itself a raw vertex sink. Avoiding that allocation requires access to the section
compiler's layer builders. The builder must be obtained for **each quad**, not retained:
Scena's sharding hook can rotate it at a quad boundary. The original output also handles
layer routing, and the translucent layer needs its normal sorting path.

The renderer's active lighter, AO choice, tint cache, block offset and face-culling
decisions are currently private to `ModelBlockRenderer`. A replacement must preserve
those decisions, including NeoForge's enhanced lighting and loader-specific behavior.
Fabric Renderer API redirects this same compiler call to its `AltModelBlockRenderer`
and `QuadEmitter`; a vanilla-only raw writer cannot assume that Fabric uses vanilla's
lighter or quad emission path.
Delegating lighting through a temporary quad still needs output comparisons; it is not
proof that differently partitioned rectangle meshes produce identical lighting.
The proposed rectangle-only sink signature also lacks the source model's UV rotation
and vertex ordering, so it cannot safely derive UVs from a sprite alone. The general
vertex form can carry resolved UVs, but its complete lighting, material and native
buffer behavior still needs implementation and verification.

An intermediate NeoForge adapter now prepares a complete solid/cutout baked model and
writes its lit vertices into section-owned layer builders. It reuses the active
`ModelBlockRenderer`, resolves the builder for each quad, and rejects unsupported models
before emission. C&B attempts this adapter only for its experimental rectangle candidate;
otherwise it keeps the baked-model path. With
`-Dchiselsandbits.debugNativeTerrainGeometry=true`, the first native selection logs
`selected native vertex output`. This is still BakedQuad-assisted, not direct rectangle
emission. In a NeoForge in-world run, the inset stone cube selected native vertex output,
the boundary slab selected the legacy path, and the user reported both looked correct.
This is not broad visual or performance validation. Fabric retains its previous
baked-model adapter.

C&B now resolves clipped, ordered source-face vertices without constructing a
rectangle `BakedQuad`. The client parity harness compares those positions and packed
UVs to baked output for its three fixtures.

Scena now provides `prepareNativeGeometry` for immutable resolved vertices, UVs,
material, culling and part AO information. NeoForge constructs temporary quads for its
active lighter; Fabric's Indigo adapter emits through FRAPI. Controlled transformed-hook
probes compared native output with the respective renderer paths, including with optional
sharding enabled. These are not in-world C&B checks, and unsupported renderers and
translucent geometry still fall back.

The experimental C&B path now supplies this resolved geometry to Scena instead of
building a second rectangle baked model. At that stage it still built the legacy model first, both
for fallback and as a parity reference. Candidates must match its ordered parts and
quad multisets, including winding, positions, packed UVs, culling, material and tint
sources; different tessellations fall back. The `terrain_subset` in-world result above
tested the earlier baked-model adapter. A subsequent NeoForge `terrain_subset` run
confirmed the new direct inset-cube path and the slab fallback; the user reported both
looked correct. A Fabric parity mismatch revealed that Scena shared a mutable random
source between concurrent model reads. Scena now seeds a separate source per operation
on both loaders. A disposable Fabric/Indigo world check confirms direct inset-cube
emission and boundary fallback, including a resource reload and concurrent cold-cache
rebuilds, with sharding both off and on. Run it with
`./gradlew :fabric:verifyTerrainWorld -PcandbTerrainWorldCheck=true` and optionally
`-PscenaTerrainSharding=true`. A separate Fabric in-world run confirmed both paths;
the user reported that the cube and slab looked correct. The automated test does not
check pixels. Broad visual parity, performance improvement and a worst-case overflow
fix have not been established.

The next boundary slice removes the interior-only eligibility gate. Direct rectangles
at plane 0 or 16 now carry the same directional cull group as legacy; interior faces
remain unculled. Legacy deliberately retains outer faces regardless of adjacent
materials, so C&B does not pre-apply neighbour occupancy masks. Scena's existing
`TerrainQuad.cullFace` adapters already delegate external occlusion to NeoForge's
renderer and Fabric/Indigo; no Scena change or republish was needed for this slice.
Neighbour accessor snapshots and states are captured on the client thread in all six
directions, shared with the legacy key, and checked again before publication.
Both bounded loader parity checks and the Fabric world check now cover boundary
geometry, including neighbour edits; the world verifier separately requires observed
native boundary emission. The gate remains default-off with the same material limits
and conservative source/layout preflight. See [benchmarks](benchmarks.md#client-baked-model-parity).

For the currently supported opaque subset, C&B also flood-fills air connected to any
block boundary and removes rectangles facing wholly sealed air pockets. Treating every
boundary as open is conservative; cavities sealed only by neighbouring blocks are not
culled. The complete unculled geometry retains source face layout before any
sealed faces are omitted. Open tunnels retain their faces, and unsupported transparent
materials fall back. The user accepted that a spectator camera clipped inside a sealed
cavity can see through the omitted faces. A Fabric disposable-world check confirms that
a one-bit sealed cavity publishes six fewer quads; broader visual parity is unverified.

### Direct snapshot and lazy fallback slice

The supported producer no longer builds a legacy chiseled model. A source-only traversal
derives the tint registration table, exact ordered part keys/AO, particle material, flags
and geometry cache key independently. Part grouping preserves legacy `HashMap.computeIfAbsent`
insertion semantics even when keys collide. The rectangle layout must match the source traversal:
equal coverage alone is insufficient because different tessellation changes lighting
interpolation. Ordered clipped source vertices preserve UVs and winding; native emission
does not run an exact baked-output comparison. That oracle now exists only in the isolated
client test harnesses.

One `ChiseledTerrainRenderData` owns immutable metadata, visible resolved geometry and all
resolved inputs needed for a thread-safe memoized baked fallback. Supported native compilation
does not construct that fallback. Rejected native preparation, a disabled hook, or an unsupported
renderer can obtain ordinary baked parts lazily without reading the current accessor, model
manager or atlas. The fallback retains sealed cavity faces while supported native output omits
them; both paths may clip safe matching-material boundaries as described below. Unsupported
geometries use the legacy builder, with the same conservative boundary rule where possible.

Model keys, parts, particle/flags and colors read the loader's **captured** data: NeoForge's
`BlockAndTintGetter.getModelData`/region snapshot and Fabric's `getBlockEntityRenderData`.
C&B supplies its immutable model-data reference as Fabric block-entity render data. This
also covers renderers that bypass Scena, provided they implement the loader's render-data
snapshot contract. Scena emission additionally pins the selected material table to the
current thread/view/position with `try/finally`, including native-preparation rejection.
NeoForge views inheriting the interface's always-empty `getModelData` default retain the
original live-entity fallback instead of disappearing. Such non-snapshot views cannot
provide edit/compile coherence until their renderer implements the loader snapshot contract.
Color collection never rebuilds a model when published data exists and uses indexed writes,
not insertions into a pre-sized list.

Resource generations reject builds crossing a cache/resource reload. Actual resource reloads
queue fresh model-data updates for still-loaded experimental entities on the following client
tick. Already-captured compile snapshots and their lazy fallbacks remain self-contained.
No Scena modification or republish was needed. Default-off gating and material restrictions
are unchanged. The source metadata pass still traverses the old greedy face layout; this slice
does not establish a performance improvement.

### Static cutout slice

The default-off direct producer now accepts actual SOLID or CUTOUT source layers, still
requiring static model keys, full directional source faces, zero emission/flags and
non-animated sprites. Dry `LeavesBlock` states join the existing opaque-state candidates;
other non-opaque materials remain unsupported. Fluid-bearing states, including waterlogged
leaves, retain legacy output. Candidate state classification alone never admits a source face.

Mixed shapes retain legacy `ANY_OCCUPIED` internal culling and directional boundary groups;
no extra opaque/cutout interface faces are emitted. Sealed-air omission is disabled for the
entire shape if any occupied material is non-opaque, even buried leaves, or any emitted source
quad uses CUTOUT (including grass overlays on an occluding state). Source tint registration,
UVs/winding, shade and part AO still follow the existing exact-layout/source checks.
Scena's existing Indigo and NeoForge adapters already preserve CUTOUT routing and delegate
lighting/culling to the active renderer; backend/write-safety checks are unchanged. No Scena
change or local publication was needed. See the bounded checks in [benchmarks](benchmarks.md#client-baked-model-parity);
these establish fixture output parity, not general pixel/lighting parity or a performance gain.

Both offline loader client harnesses passed independent metadata/lazy fallback parity,
colliding part-key order, non-snapshot-view fallback, distinct tinted-material order,
nested/exceptional/thread-isolated color scopes, data
replacement between preflight/emission, native rejection and hook-bypass fallback. The
Fabric disposable-world test additionally observed native inset/boundary compilation with
no baked fallback built, then disabled the hook and observed actual lazy fallback compilation,
restored the hook, and passed automatic resource-reload rebuilding, edits and cavity checks.
Sodium/Iris were not installed for these checks; their actual integrations and broad pixel/AO
parity remain unverified. NeoForge was compiled and exercised by the isolated client harness
and server GameTests, not by a new automated world run in this slice.

### Expanded direct material slice

The default-off direct rectangle candidate now classifies from the actual source geometry and render layer,
not a `LeavesBlock` allowlist. It accepts verified full directional faces using SOLID, CUTOUT, or TRANSLUCENT
materials, including atlas-animated sprites. Each direct quad keeps its source quad's layer (for example vanilla
glass and leaves are CUTOUT, water TRANSLUCENT, lava SOLID), tint index, resolved sprite, clipped UVs, per-quad
model light emission, shading, and part AO/material flags; these are part of the preflight and legacy-output
parity checks. Per-quad model emission is only what the source model declares; world block light continues to come
from the chiseled block state's existing light level. Vanilla's sea lantern cube declares no per-quad emission, so
the sea lantern fixtures only prove that no emission is invented; they do not exercise a nonzero producer. No vanilla
full-cube model declares one, so the parity harness uses a synthetic source (below). Minecraft keeps
animating atlas sprites with stable UVs; no parallel animation path is introduced.

Source models must be static: every non-air contained state needs a geometry key whose class is exactly
`SingleVariant` (Fabric's `SingleVariant` key is the variant itself; `WeightedVariants` delegates to its selected
variant). Null, `MultiPart`, subclassed, or other dynamic keys stay on the legacy path, because a non-null key alone
does not prove the model is stable across asynchronous capture and publication. The cache key carries the contained
states index-aligned with their model keys so this is checked per state. The only exemption is a pure
`LiquidBlock` fluid state: C&B builds its faces from the fluid model set, never the block-state model. Waterlogged
non-liquid blocks are not exempt.

Every layer keeps the legacy outer-face cull group, so the host's `Block.shouldRenderFace` culls a boundary face
against opaque full-block neighbours. Adjacent chiseled snapshots additionally hide matching pure-fluid cells
(including source and flowing states of the same family with equal variants) and mutually self-culling transparent
glass cells. Partial overlaps are split. Both native and supported baked fallback paths apply the boundary rule;
the baked fallback still retains sealed-cavity faces. Source faces with non-affine UVs or unsupported source
geometry retain their unsplit baked faces instead. Neighbour payloads and client chunk load/unload events request
adjacent rebuilds, and publication rechecks the captured snapshot.

Vanilla glass and fluid neighbours are not yet self-culled: their own geometry is emitted independently, and
vanilla fluid surfaces can be sloped or shorter than a block. Unsupported non-cubic or non-affine fallback
sources may still leave seams. Translucent self-culling remains incomplete, and the experiment stays default-off.
The bottom fluid source's two reversed V coordinates were also corrected, including on the legacy path, so clipped
and unsplit rectangles use consistent UVs.

Translucent quads route to the ordinary section TRANSLUCENT builder on both loaders, where vanilla section
compilation performs its normal translucent sort. Scena's 26.2 native sharding still excludes translucent
geometry. Sealed-air omission is used only when all occupied source states are opaque solid-rendering and every
resolved face is SOLID. Unlike materials that touch internally retain legacy fallback unless both sides are proven
opaque SOLID, because the legacy mesher otherwise suppresses that interface.

Fluid support is limited to the existing C&B `FaceManager` cuboid-fluid source: it preserves the source model's
still/flow materials, clipped UVs, and fluid tint source, and animated fluid sprites remain atlas-driven. This is
not vanilla's arbitrary sloped fluid surface renderer. Waterlogged blocks (whose legacy C&B path replaces block
faces with fluid-only output), fluid/cutout or fluid/translucent internal interfaces, non-cubic source faces,
dynamic or directionally heterogeneous models, and any source failing complete-face/layout validation remain on
the legacy baked path. No bits or faces are silently discarded.

Scena's resolved-geometry validation and native section writer accept translucent materials; both loader adapters
pass that layer through to the normal section compiler. Verification (offline, isolated run directories):

- `:fabric:verifyTerrainWorld -PcandbTerrainWorldCheck=true` passes in a disposable world that deletes its save:
  hook-bypass fallback, sealed cavity, leaves/glass cutout, animated water, sea lantern, translucent boundary,
  waterlogged and fluid/cutout fallback fixtures, and a boundary-neighbour check (stone culls the WEST group,
  vanilla glass retains it and matching chiseled water hides it). Cross-chunk partial overlap, mixed native/baked
  neighbours, edits, removal, and an actual neighbour chunk unload/reload trigger automatic updates. Matching
  chiseled glass culls while different stained glass remains visible. A live compiled TRANSLUCENT section with
  a split water face has identical lit vertex buffers in native and baked modes, and both produce sort indices.
  This does not establish pixel equivalence with an unsplit face. The Gradle guard requires every fixture marker.
- `:neoforge:verifyTerrainWorld -PcandbTerrainWorldCheck=true` creates and deletes a unique disposable world,
  checks the native boundary emission marker, compares compiled native and baked translucent section buffers and
  their sort indices, and verifies neighbour chunk unload/reload rebuilds. An asymmetric-light diagnostic evaluates
  the original and split quads through `BlockModelLighter` and samples their triangulated interpolation: one run
  differed by up to 1.3125/255 in color and 0.46875/255 in light-coordinate components. This diagnostic is not a
  pixel test and does not establish split/unsplit equivalence; preserving the original lighting remains unresolved.
  Scena's `FaceTriangleClipper` clips the original two triangles and retains original-vertex weights; the same fixture
  verifies its unquantized reference interpolation, and both loader parity checks cover six directions and a
  sloped side. Scena has tested post-light writers for both loaders, but its compatibility preflight rejects their
  clipped faces: Minecraft's terrain vertex shader multiplies color by a dynamically sampled lightmap at each
  vertex, so interpolating color and light coordinates separately cannot preserve that product in general.
  Vertex-attribute parity is not pixel parity. The current C&B producer still emits and relights split rectangles;
  this shader limitation and vanilla glass/fluid interfaces remain unresolved.
- `:fabric:verifyTerrainModelParity` and `:neoforge:verifyTerrainModelParity` with `-PcandbTerrainModelParity=true`
  pass, covering glass, water, lava, sea lantern, and leaves at inset and boundary positions. Both also check
  partial and fragmented fluid masks, full overlap including flowing water, all six directions, clipped UVs and
  winding, differing material retention, baked clipping parity, all six glass directions, and rejection of
  non-affine UV clipping. On both loaders,
  `TERRAIN_MODEL_PARITY_EMISSIVE_SOURCE` (required by the Gradle guard) runs real loader-extracted sea lantern
  source quads, rewritten through the `FaceManager.runWithSourceTransform` verification seam to per-quad emission 11.
  Those quads go through C&B source metadata, direct geometry, the lazy baked fallback and the uncached legacy builder,
  next to a touching stone material that must stay at 0. The check needs exact legacy parity for the geometry and both
  fallbacks, and it fails if the oracle accepts the unmodified (zero-emission) legacy model. A mutation that drops
  emission in `DirectTerrainGeometryBuilder` made it fail. Scena's adapter tests separately cover non-SOLID emission:
  the Fabric direct emitter's vertex buffer equals Indigo's `fromBakedQuad` output at emission 7, and the NeoForge
  resolved model keeps emission 9. The disposable world fixture does not cover a nonzero-emission source.
- NeoForge `runGameTestServer` passes, including the static-key guard tests (dynamic, null, subclassed, misaligned
  keys rejected; liquid exemption; waterlogged and mixed rejection).

A local Scena republish must use `VERSION=26.2-rendering-dev-1`; Fabric Loader reuses extracted nested jars in
`<runDir>/.fabric/processedMods` only when the byte size matches, so old extracted copies are not a correctness
risk for changed jars, but a jar updated without republishing its nested core-bundled jar stays stale. This is
fixture-level geometry parity, not a broad pixel/AO or shader-backend guarantee. The feature remains experimental
and default-off.

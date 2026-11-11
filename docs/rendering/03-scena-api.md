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

# Design direction

## Core principle

> **C&B produces geometry in whatever format the active terrain renderer consumes. It never owns
> the terrain draw.**

Whoever owns terrain (vanilla, Sodium, Iris on top of either) also owns culling, sorting, shadow
passes, OIT and shader-pack programs. If chiseled geometry lives inside their meshes, we get all of
that for free. This is the only approach that is compatible with shader packs.

## Architecture

```
                 ┌──────────────────────────────────────────────┐
  worker thread  │ C&B mesher (shared core, loader-agnostic)    │
  (section       │  bits -> greedy faces -> per-vertex light/AO │
   compile)      │  -> ChiseledFace stream                      │
                 └───────────────┬──────────────────────────────┘
                                 │ ChiseledFaceSink (Scena)
      ┌──────────────────────────┼───────────────────────────┐
      ▼                          ▼                           ▼
 Vanilla 26.4 sink         Vanilla 26.2 sink            Sodium sink
 SectionBufferBuilder      BufferBuilder (BLOCK)        Sodium ChunkMeshBufferBuilder
 + template sharding       no cap                       (Iris adds its attributes)
```

1. **Mesher (core).** Runs on the section-compile worker, using the region snapshot the renderer
   already passes in. Bits become greedy-merged faces per material, with occlusion against
   neighbouring bits and blocks. Light and AO are computed with the vanilla algorithm on the same
   region, which gives exact parity without extra synchronisation. There are no `BakedQuad`
   objects and no per-block model caching.
2. **Sink (Scena).** A small interface that accepts faces (position, size, direction, sprite,
   tint, 4× light, 4× AO) and writes them in the native format of the active renderer.
3. **Draw.** Untouched. The geometry is part of the normal section mesh.

## Vanilla 26.4 sink: template sharding

- `SectionBufferBuilder` gets a shard boundary. When the template count would reach 65,536, the
  builder closes the current `[faces][templates]` block and starts a new one in the same buffer.
- `SectionDraw` / `CompiledSectionMesh` record the shard table: `(faceStart, faceCount,
  templateStart)` for each shard.
- `LevelRenderer.extractSectionDrawGroups` emits one `ChunkSectionInfo` + `IndexedDraw` per shard
  instead of one per section/layer.
- `MeshData` sorting and the mid-point extraction (`MeshData` line ~59) must handle shards.
- This fixes the silent vanilla overflow for **every** mod. It is also a good candidate to upstream
  to NeoForge, or to report to Mojang.
- Template pressure for C&B: a face's offset inside the block (1/16) and its size go into the
  template, and the block offset goes into the face record. A template is defined by
  `(direction, u0, v0, w, h, depth, sprite UVs)`. Greedy meshing reduces the face count,
  deduplication handles repeats, and sharding handles the rest.

## Vanilla 26.2 sink

`BufferBuilder` in `DefaultVertexFormat.BLOCK` with no cap. The new mesher still replaces
`BakedQuad` generation, so the performance gain applies here too.

## Sodium sink

- Fabric: Renderer API (`FabricBlockStateModel.emitQuads`), or a direct Scena hook into Sodium's
  `BlockRenderer` / `ChunkBuildBuffers`.
- NeoForge: `AddSectionGeometryEvent` (Sodium supports it), or the same Scena hook.
- A direct Scena hook is preferred on both loaders: one code path, and no per-quad
  `QuadEmitter` overhead.
- Iris on Sodium computes `mc_Entity`, `at_midBlock` and tangents from the block context we set
  (as LittleTiles does).

## Translucency

The geometry is in the vanilla or Sodium translucent layer, so it uses their sorting (vanilla OIT
on 26.4, Sodium's BSP or topological sort). Open point: vanilla sorting across shards (see below).

## Hybrid and LOD (later)

Because we don't own the draw, LOD must also be expressed as mesh content. For example, far
sections could be meshed at 1/8 or 1/4 resolution, using the section's distance at compile time.
A separate GPU-driven path (vertex pulling or GPU baking) stays a possible **vanilla-only,
no-shader-pack** optimisation, but it is not the baseline.

## Rejected approaches

| Approach | Why rejected |
|---|---|
| Own vertex-pulling pass with a custom shader (hooked into `renderLayers` or render-stage events) | Iris replaces the shader. Sodium never calls the vanilla hooks. No shadows. |
| Raymarching 16³ volumes in the fragment shader | No 3D textures. Breaks shader packs (depth/normal G-buffers). Expensive at close range. |
| GPU baking (render-to-buffer) as the baseline | Works only where we own the buffer format. Sodium and Iris have their own formats and meshing path. Kept as a possible future optimisation. |
| `AddSectionGeometryEvent` with `BakedQuad`s on 26.4 | Still subject to the 16-bit template cap. NeoForge only. |
| `SubmitNodeCollector.submitCustomGeometry` | Immediate mode, re-submitted every frame. Not terrain. |

## Open questions

1. Vanilla translucent sort with shards: does `MeshData.sortQuads` reorder across shards? Shards
   may need to be split by distance, or kept as one shard in the translucent layer.
2. How will Iris handle 26.4 vertex-pulling terrain? (Snapshot. Watch Iris development.)
3. Template budget: measure the worst case of unique templates per section after greedy meshing.
4. Item, held and entity rendering (`ChiseledBlockItemModel`, BESRs, previews) uses
   `SubmitNodeCollector`. The same mesher can output to a `VertexConsumer` there.
5. Do sections that contain only chiseled blocks need a "C&B dirty" re-mesh trigger, separate
   from block updates? (The current block-entity model-data refresh does this.)

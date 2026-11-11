# Research

Versions looked at: MC 26.2 (NeoForm 26.2-2), NeoForge 26.2.0.93, Fabric API 0.161.0
(fabric-rendering-v1 27.2.0, fabric-renderer-api-v1 17.0.17), MC **26.4-snapshot-8**
(decompiled with Vineflower), Iris branch `26.1` (`bff1e69c`), Sodium `dev` (`mc26.3-0.9.3`).

## GPU API

### 26.2: `com.mojang.blaze3d.systems.GpuDevice`

- `ShaderType` has only `VERTEX` and `FRAGMENT`. There are **no compute shaders**.
- `GpuBuffer` usage flags: `MAP_READ/WRITE`, `COPY_SRC/DST`, `VERTEX`, `INDEX`, `UNIFORM`,
  `UNIFORM_TEXEL_BUFFER`, `INDIRECT_PARAMETERS`. There are **no storage buffers (SSBO)** and no
  transform feedback.
- Textures are 2D or cubemap only. Array and 3D textures throw `UnsupportedOperationException`.
- `UniformType`: `UNIFORM_BUFFER`, `TEXEL_BUFFER` (typed through `GpuFormat`, including `R32_UINT`
  up to `RGBA32_UINT`).
- `RenderPass`: `drawIndexed`, `multiDrawIndexed`, `drawIndexedIndirect`, `draw`, `multiDraw`,
  `drawIndirect`, `drawMultipleIndexed`.
- `DeviceFeatures`: `shaderDrawParameters`, `multiDraw*`, `multiDrawIndirect`, `drawIndirect`,
  `nonZeroFirstInstance`, `persistentMapping`.
- `CommandEncoder.copyTextureToBuffer` runs on the GPU on both backends (GL uses a
  `PIXEL_PACK_BUFFER`, Vulkan uses `vkCmdCopyImageToBuffer`). In principle you can render vertex
  data into an `RGBA32_UINT` target and copy it into a vertex buffer ("GPU baking").
- Precedent: `CloudRenderer` already does vertex pulling (`isamplerBuffer CloudFaces` plus a
  sequential quad index buffer).

### 26.4-snapshot-8: `com.mojang.renderpearl.api.*`

- The API moved into `renderpearl.api.{device,commands,buffers,pipeline,textures}`. Backends are in
  `renderpearl.backend.{opengl,vulkan}`.
- Still vertex and fragment shaders only. There are no storage buffers, and the buffer usage flags
  are unchanged.
- New: `UniformType.COMBINED_IMAGE_SAMPLER`, `INPUT_ATTACHMENT`, subpasses (`RenderPassLayout`),
  `SpvModule`, OIT stages (`OitStage.DEPTH_BOUNDS/TRANSMITTANCE/ACCUMULATE`),
  `DeviceLimits.maxDrawIndirectDrawCount`.

**Conclusion:** with this API, "the GPU builds the model" can only mean vertex pulling (the vertex
shader expands compact records from a texel buffer) or render-to-buffer baking.

## Vanilla section pipeline

### 26.2

- `ChunkSectionLayer` is `SOLID`, `CUTOUT` or `TRANSLUCENT`. `SectionCompiler.compile` runs on
  worker threads and writes `BufferBuilder`s in `DefaultVertexFormat.BLOCK`.
- `LevelRenderer.prepareChunkRenders` creates one `RenderPass.Draw` per section and layer, plus a
  per-section `ChunkSection` UBO.
- `IndexType.least()` switches automatically from 16-bit to 32-bit indices. There is no quad cap
  (the hard ceiling is 16,777,215 vertices per buffer).

### 26.4-snapshot-8: vertex-pulling terrain

`terrain.vsh` reads `usamplerBuffer TerrainVertexBuffer`. Each section/layer mesh is laid out as:

```
[ face records  (layer.vertexBufferFaceSize()  bytes each) ]
[ baked quad templates (layer.vertexBufferBakedFaceSize() bytes each) ]
```

- **Face record** (written by `com.mojang.blaze3d.vertex.SectionBufferBuilder.addQuad`):
  - block offset in the section: 3×8 bit, quantized as `round(f*15)+7`
  - **16-bit `bakedQuadIndex`**
  - ABGR colour
  - 4×8-bit brightness (AO)
  - 4×16-bit light coordinates
- **Template**: 4 corners × (xyz float, packed UV). Templates are deduplicated through
  `Object2IntMap<BakedQuad>`.
- `memPutShort(pointer + 3, (short) bakedQuadIndex)` has **no overflow check**. With more than
  65,536 unique `BakedQuad`s in one section/layer, the index wraps and the section silently renders
  the wrong quads.
- The block offset grid is 1/15, so C&B's 1/16 offsets cannot go in the face record. They have to
  go in the templates, which makes the number of unique templates explode.
- Draw: `LevelRenderer.extractSectionDrawGroups` emits **one** indirect draw per section and layer.
  Each draw has its own `ChunkSectionInfo(x, y, z, visibility, vertexStartIndex,
  bakedFaceStartIndex)`, passed as per-instance vertex attributes (`ChunkPosition`,
  `ChunkVisibility`, `BufferStartOffsets`). `ChunkSectionsToRender.DrawIndirect.render` uses
  `drawIndexedIndirect`.
- Because offsets are per draw, a section/layer can be split into several
  `[faces][templates]` **shards**, each with at most 65,536 templates and one extra draw.
- Light and AO are still computed on the CPU.

## Loader hooks

### NeoForge

- `AddSectionGeometryEvent`: adds `AdditionalSectionRenderer`s that write into the vanilla section
  `VertexConsumer`s during compile. On 26.4 this would still hit the template cap.
- `RenderLevelStageEvent.{AfterOpaqueBlocks, AfterTranslucentBlocks, ...}` passes
  `getRenderableSections()` (`IRenderableSection`: origin, AABB, isEmpty).
- `SubmitCustomGeometryEvent` and `SubmitNodeCollector.submitCustomGeometry` are immediate-mode
  `VertexConsumer` paths. They are not suited to persistent terrain.

### Fabric

- `LevelRenderEvents`: `START_MAIN`, `AFTER_OPAQUE_TERRAIN`, `BEFORE/AFTER_TRANSLUCENT_TERRAIN`
  (`LevelTerrainRenderContext.sectionsToRender()`), `COLLECT_SUBMITS`, and others.
- There is no equivalent of `AddSectionGeometryEvent`. Custom meshing goes through the Renderer
  API (`FabricBlockStateModel.emitQuads`).

## Sodium

- Has its own compile and draw path. It does **not** call vanilla `SectionCompiler` or
  `ChunkSectionsToRender`.
- `AddSectionGeometryEvent` is supported on **NeoForge only**. The Fabric hook is an empty stub.
- The Fabric Renderer API is supported on **Fabric only** (bundled `frapi` module).
- **No per-section quad cap** (shared `INT` index buffer).
- Does **not** fire `RenderLevelStageEvent` or `LevelRenderEvents.AFTER_OPAQUE_TERRAIN`. The
  platform hooks exist but are empty.
- Public API (`net.caffeinemc.mods.sodium.api`): `vertex.buffer.VertexBufferWriter`, vertex
  serializers, sprite utilities. `ChunkMeshFormats` is internal.

## Iris

- `IrisApi.assignPipeline(RenderPipeline, IrisProgram)`, `assignPipelineShadow`,
  `registerShadowRenderCallback`, `isRenderingShadowPass`.
- When a pack is active, Iris **replaces the pipeline's shaders completely** with the pack's
  gbuffers program. Custom vertex-pulling shaders cannot survive this; Iris documents mod TBO
  rendering as unfixable.
- Terrain format: `IrisVertexFormats.TERRAIN` (Position, Color, UV0, UV2, Normal, `mc_Entity`,
  `mc_midTexCoord`, `at_tangent`, `at_midBlock`).
- Shadow pass: there are no loader events. Terrain gets no shadow rendering automatically unless it
  goes through the normal meshing path.
- Closest analogue: **LittleTiles** feeds real vertices through Sodium's `VertexBufferWriter` and
  Iris's `BlockSensitiveBufferBuilder`. Iris does the rest.
- How Iris will handle 26.4 vertex-pulling terrain is **unknown** (it is a snapshot).

# 06 – Translucency & Shard-Aware Sorting

## 1. How each renderer handles translucent terrain

| Renderer | Mechanism | What C&B has to do |
|---|---|---|
| Vanilla 26.2 | CPU sorting per section: `MeshData.SortState` centroids → sorted index buffer, re-sorted when the camera's point of view changes | Nothing. C&B quads go into the native translucent `BufferBuilder` and get sorted along with everything else. |
| Sodium | Its own translucency system: `TranslucentGeometryCollector` with topological/BSP sorting, fed by `collector.appendQuad` in `BlockRenderer.bufferQuad` | Nothing. The Scena Sodium sink calls `collector.appendQuad` for translucent quads, as Sodium's own `BlockRenderer` does. **No quad cap, so no sharding.** |
| Vanilla 26.4, *Improved Transparency* **on** | **OIT:** `LevelRenderer.executeOit` draws the translucent layer group in 3 subpasses (`DEPTH_BOUNDS`, `TRANSMITTANCE`, `ACCUMULATE`), then `OIT_COMPOSITE`. `scheduleResort` is skipped (`!useImprovedTransparency()`). | Nothing. Draw order doesn't matter, so **translucent shards are fully correct**. |
| Vanilla 26.4, *Improved Transparency* **off** | CPU sorting per section mesh (`ResortTransparencyTask` → `SortState.buildSortedIndexBuffer`), with centroids decoded from the per-face data | **This is the only problem case.** A shard is a separate mesh and a separate draw, so its faces cannot be interleaved with the main mesh's translucent faces. |

## 2. Template budget is per layer

In 26.4, `SectionCompiler` creates **one `SectionBufferBuilder` per `ChunkSectionLayer`**, so the template map and the 16-bit `bakedQuadIndex` are scoped per layer. That gives 64k templates each for SOLID, CUTOUT and TRANSLUCENT in every section. For a translucent shard to be needed, C&B *translucent* bits in one section would have to produce more than 64k unique translucent templates. That is about 5 worst-case glass checkerboards in one 16³ section, which is very rare in real builds.

## 3. Proposed policy (Scena, 26.4 only)

1. **Opaque/cutout:** shard freely (the order doesn't matter, and depth testing handles it).
2. **Translucent, within budget:** always stay in the main translucent mesh, so sorting is exact.
3. **Translucent, over budget:**
   - **OIT on:** shard. The result is correct.
   - **OIT off:** shard, emitting the shards *after* the main translucent draw, with each shard sorted on its own. Ordering *between* meshes is approximate, the same artefact class vanilla already has between sections. A one-time debug log is written for each section.
4. **Value-keyed template dedupe** (see [04](04-mesher.md)) is applied first, so step 3 almost never runs.

The shard re-sort hook: Scena attaches a `SortState` to each shard and piggybacks on `ResortTransparencyTask`, so a shard re-sorts in the same task as its parent section.

## 4. C&B-specific translucency correctness

- **Internal faces between translucent materials:** for example a glass bit next to stained glass. Both faces are emitted, because vanilla's `skipRendering` only culls the same material, and both are sorted by centroid like vanilla.
- **Greedy rectangles and sorting:** a big greedy rectangle has one centroid. That is the same artefact vanilla has with big quads, and per-block greedy (round 5) caps a rectangle at 1×1 block, the same size as a vanilla face. So sorting quality matches vanilla.
- **Translucent next to opaque inside a block:** the opaque face is emitted (it isn't culled by the translucent neighbour) and the translucent face is emitted. This matches vanilla glass next to stone.

## 5. Open questions (round 7)

1. Accept the policy in section 3, including approximate ordering for over-budget translucent shards when OIT is off?
2. Should Scena expose a hook so C&B can *split translucent bits per block* when a block is huge, e.g. by forcing smaller greedy rectangles? (Probably not needed.)

## 6. Decisions (round 7)

- Policy in section 3 **accepted**, including approximate cross-shard ordering for over-budget translucent geometry when OIT is off.

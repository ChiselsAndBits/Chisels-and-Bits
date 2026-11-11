# 04 – C&B Mesher

The mesher turns the bits in one chiseled block into axis-aligned rectangles, and then into quads, which it writes straight to the `ITerrainQuadSink` described in [03](03-scena-api.md). It does not create `BakedQuad`s, does not allocate memory for each quad, and does not use a thread pool of its own. It runs inside whatever section-compile thread the active renderer (vanilla or Sodium) is already using.

## 1. Current pipeline and its costs

| Step | Code | Cost |
|---|---|---|
| Model data built async on a C&B thread pool | `ChiseledBlockModelDataExecutor` | An extra thread pool and an extra `sendBlockUpdated` round trip, so the block re-renders a frame late |
| Greedy meshing | `GreedyMeshBuilder.buildMesh` (`int[]` mask, voxel-by-voxel loop, one material lookup per voxel per direction) | O(n³·3) with a virtual call per voxel |
| Face → quads | `QuadGenerationUtils.generateQuads` → `FaceManager.extractQuads` (cached `DeconstructedModelPartComponent`s for each state and direction) → `VertexDataUtils.adaptVertices` (clip and remap UVs) → `BakedQuadBuilder.build()` | **One `BakedQuad` object for every greedy face.** All of them are unique, so on 26.4 every one becomes a separate template |
| Grouping | `ChiseledBlockModelPart`s keyed by `ChiseledBlockModelPartKey` | Extra objects and map lookups |

Worth keeping:
- `FaceManager`'s approach of breaking the source block model down per direction into layers. Each layer has a sprite, tint, flags, and a sub-rectangle. This is what supports multi-layer models such as grass overlays, tinted faces, and faces that use different textures.
- The neighbourhood culling logic, which considers occluding neighbours and same-material translucency.

## 2. Render snapshot (immutable, versioned)

Decision A from round 4: on every change, C&B publishes an immutable snapshot that section compiles read without taking any locks.

```java
record ChiseledRenderSnapshot(
    int version,
    int size,                    // 1..16 (StateEntrySize, max ONE_SIXTEENTH = 16)
    MaterialEntry[] materials,   // non-air palette entries only
    long[][] occupancy,          // [material][64]: 16 slices(x) × 16 rows(y) × 16 bits(z)
    long[]   occluders,          // union of occupancy for all opaque/occluding materials
    short[][] boundary,          // [direction][16]: occupancy of each outer face, for neighbours to cull against
    short[][] boundaryOccluding  // [direction][16]: the occluding subset of the above
) {}
```

- `long[64]` per material comes to 512 B for a 16³ block. Each row is a `short` packed four to a `long`, so getting a row is `(occ[i >> 2] >>> ((i & 3) << 4)) & 0xFFFF`.
- The snapshot is built from `StateEntryStorage` on the thread that applies the change. That is one pass over 4096 entries, about 10 µs. Swapping the reference is atomic.
- Smaller bit sizes (8/4/2/1) are expanded into the 16-grid when the snapshot is built. This gives the mesher and the neighbour culling a single code path, and the greedy pass merges the expanded rectangles back together anyway.

## 3. Bitwise face masks

Take material `m`, direction `d` and slice `s`. The visible face mask is a 16×16 bit plane, stored as 16 `short` rows:

```
visible_m(d, s) = occ_m(s) & ~( occluders(s+d) | (selfCull_m ? occ_m(s+d) : 0) )
```

- **±X:** compare slice `s` with slice `s±1`, row by row. No shifting needed.
- **±Y:** compare row `y` with row `y±1` inside the same slice.
- **±Z:** `row & ~(row >>> 1)` / `row & ~(row << 1)`. One shift per row covers the whole axis at once.
- `selfCull_m` is vanilla's `skipRendering` behaviour, for example glass next to glass. Matching how vanilla decides culling between different materials is the main correctness work here. Opaque neighbours always cull. Translucent neighbours cull only the same material.
- **Outer slices** (s+d leaves the block) compare against the neighbour's face mask, read through the section-compile region and not through the BE neighbourhood:
  - Full occluding block (`canOcclude` and full face shape): `0xFFFF` for every row.
  - Chiseled neighbour: its snapshot's `boundaryOccluding[opposite(d)]`, plus `boundary` filtered to the same material for self-cull.
  - Anything else: use `Block.shouldRenderFace`, evaluated per direction once per block. Partial shapes such as slabs and stairs fall back to "don't cull".

The whole mask pass costs 6 × 16 slices × 16 rows × M materials of `short` operations. That is about 1.5k operations per material, versus about 25k virtual calls today.

## 4. Greedy rectangles on bit planes

```
for row r in 0..15:
  while plane[r] != 0:
    x0 = ctz(plane[r]); w = ctz(~(plane[r] >>> x0)); run = ((1<<w)-1) << x0
    h = 1; while r+h < 16 && (plane[r+h] & run) == run: plane[r+h] &= ~run; h++
    plane[r] &= ~run
    emit(material, d, s, x0, r, w, h)
```

The cost is O(rectangles), with no mask clearing, no `int[]` buffers and no allocation. The output is a compact `int` per rectangle (4+4+4+4 bits for x0, y0, w-1, h-1) plus the direction, slice and material.

**Rectangle cache.** Each snapshot keeps its own `int[]` of rectangles, grouped by (material, direction). It is computed once per version and shared by every section rebuild (light changes, neighbour updates and the like). When the outer culling changes, only the boundary-slice rectangles are recomputed, and they are stored separately for that reason.

## 5. Rectangle → quads

For each material and direction, a cached **face layer list** is derived from `FaceManager`'s breakdown of the source model. Each entry holds:
- sprite
- UV sub-rectangle in block space
- tint index
- material flags
- shade
- emission
- the layer's own 2D rectangle, when it is a partial-face layer

For each rectangle and each layer:
1. Intersect the rectangle with the layer's 2D rectangle. Skip it if they don't overlap. This replaces `VertexDataUtils.adaptVertices`.
2. Map the block-space rectangle to sprite UVs the same way a vanilla full-cube face does, so textures line up with neighbouring vanilla blocks.
3. Call `sink.axisAlignedQuad(d, depth, x0, y0, x1, y1, layer)`. The sink does lighting and AO through `ITerrainLighter` (see the open question below) and writes vertices in the renderer's native format.

Source models whose faces are not cuboid, or that are rotated, keep using the existing `adaptVertices` path as a fallback, flagged per material.

## 6. 26.4 template budget

In 26.4 a template is the 4 corner positions plus packed UVs of one quad, block-local, deduplicated by `BakedQuad` identity. If the Scena sink keys templates by value, i.e. `(d, depth, rect, sprite-UV rect, tint, flags)`, then:
- identical chiseled designs repeated across a section share all their templates, and
- matching faces inside one block, such as the many 1×1 faces of a checkerboard, collapse into one template per (position, sprite).

Worst cases for a single 16³ block:

| Pattern | Rectangles (faces) | Unique templates |
|---|---|---|
| Solid / mostly solid | 6 – ~100 | about the same number |
| Typical builder detail | 200 – 2k | about 0.6× that |
| 3D checkerboard (single material) | 12 288 | 6 × 16 × 256/2 ≈ 12k (every one is unique by position) |

**Conclusion:** dedupe alone is not enough. Five worst-case blocks already exceed 64k, so **sharding in Scena is required** ([03](03-scena-api.md)). With value-keyed dedupe, sharding is rarely triggered in real builds.

## 7. Threading and lifecycle

- The `ChiseledBlockModelDataExecutor` pool and the async model-data round trip go away. A block change publishes a new snapshot and marks the section dirty, and the renderer remeshes on its own threads.
- The terrain path does not use C&B's `ChiseledBlockStateModelManager` cache. Its replacement is the rectangle cache on each snapshot.
- Items, held blocks and BESRs run the same mesher with a `VertexConsumer`-backed sink (open question in 02).

## 8. Open questions (round 5)

1. **Greedy scope:** per block (keeps the rectangle cache and lighting simple) or across a whole section (fewer quads, but harder AO and caching)?
2. **Fallback scope:** how many source models are not cuboid? Can we require cuboid face layers and fall back for everything else?
3. **Mismatched bit sizes:** expanding to 16 when the snapshot is built is assumed to be acceptable.
4. **AO on inset faces:** vanilla uses shape-aware AO for faces that don't sit on the block boundary. Exact parity is round 6 (`ITerrainLighter`).

## 9. Decisions (round 5)

| Topic | Decision |
|---|---|
| Greedy scope | **Per block**, same as today. Section-wide merging can be revisited later. |
| Non-cuboid source models | **Restrict:** only models whose face layers are cuboid are allowed as bit materials. The `adaptVertices` fallback is dropped from the terrain path. |
| Smaller bit sizes | Expanded into the 16-grid when the snapshot is built. |
| Async executor | **Kept** (already proven fast and responsive). It now builds the snapshot plus the rectangle cache off-thread and publishes them atomically. Section compile then only reads cached rectangles and emits quads. If no snapshot is ready yet, compile builds one inline. |

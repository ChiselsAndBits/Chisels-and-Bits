# 05 – Lighting Parity

The requirement from round 1 is that chiseled blocks light **exactly** like vanilla: smooth light, AO, directional shade and emission. That has to hold under whichever renderer is active:
- vanilla,
- NeoForge's enhanced lighter, or
- Sodium's smooth lighting pipeline.

## 1. What each lighter actually depends on

| Lighter | Entry point | Inputs that come from the quad |
|---|---|---|
| Vanilla `BlockModelLighter` (26.2) | `prepareQuadAmbientOcclusion(level, state, pos, BakedQuad, QuadInstance)` / `prepareQuadFlat` | Only `quad.direction()`, the **bounding box** of the 4 positions (`prepareQuadShape` → `faceShape[]`, `facePartial`, `faceCubic`), and `materialInfo().shade()` |
| NeoForge `EnhancedBlockModelLighter` (replaces the vanilla one when `enhancedLighting=true`, which is the default) | Same signature, `calculateAxisAligned` / `calculateIrregular` | Direction, vertex positions, shade |
| Sodium `SmoothLightPipeline` / `FlatLightPipeline` | `calculate(ModelQuadView, pos, QuadLightData, cullFace, lightFace, shadeOverride, enhanced)` | `ModelQuadView` is an **interface**: x/y/z per vertex, flags (`IS_PARALLEL`, `IS_ALIGNED`, `IS_PARTIAL`), light face |

**Key finding:** each of these lighters only reads the quad's direction, its 4 corner positions and its shade flag. A C&B rectangle `(d, depth, x0, y0, x1, y1)` fully determines all of them, so exact parity is possible without keeping any `BakedQuad`s around.

Vanilla's behaviour also tells us what "parity" means for inset faces:
- Faces that are not on the block boundary (`faceCubic == false`) are lit from the light of the **block's own position**.
- AO for those faces samples the **neighbouring blocks** around that position, weighted by the face's bounding box (`doNonCubicWeight`).
- A block's own geometry never occludes itself, so a stair's inner step gets no self-AO.

C&B bits therefore get exactly the AO a vanilla stair or slab would get. There is no AO from the block's own bits (see question 2).

## 2. Proposed `ITerrainLighter` (Scena)

Scena wraps whichever lighter is active, so C&B never needs to know about it:

```java
interface ITerrainLighter {
    /** Lights an axis-aligned rect in block space [0,1]; writes 4 packed lightmap + 4 ARGB shade values. */
    void lightAxisAligned(Direction face, float depth, float u0, float v0, float u1, float v1,
                          boolean shade, int emission, int[] outLight, int[] outColor);
}
```

| Active renderer | Scena implementation |
|---|---|
| Vanilla 26.2 / 26.4 | **Option V1:** delegate to the live `BlockModelLighter` using a throwaway `BakedQuad` built from the rectangle. This is guaranteed parity, follows NeoForge's enhanced lighter automatically, and costs one short-lived allocation per quad.<br>**Option V2:** re-implement the vanilla math. Per block and direction, precompute the corner samples (4 sides, 4 corners, centre) for both the cubic and non-cubic cases, then do bilinear weights per quad. Fastest and allocation-free, but it has to be ported **twice** (vanilla and NeoForge enhanced) and kept in sync with Mojang changes. |
| Sodium | Use a mutable `ModelQuadView` that Scena owns. Its flags are computed directly from the rectangle (`IS_PARALLEL` always, `IS_ALIGNED` when depth is 0 or 1, `IS_PARTIAL` when the rectangle is smaller than the full face). It is passed to Sodium's own `LightPipeline`, which caches per-face AO data for each block, so the cost is low. **Parity with Sodium comes for free.** |

## 3. Emission and shade

- **AO vs flat per material:** vanilla skips AO for the whole block when `blockState.getLightEmission() != 0`. For C&B the decision is made per quad, using the bit material's emission. Glowstone bits get flat full light, and stone bits next to them still get AO.
- **Emissive quads:** the material's `lightEmission` from `MaterialInfo` is applied as `max(lightmap, emission)`, the same as vanilla's per-quad emission.
- **Directional shade:** `level.cardinalLighting().byFace(d)` if `shade`, otherwise `up()`. Nether and End cardinal lighting comes along automatically because this is read from the level.

## 4. Iris and shaders

Under Iris, lighting is still baked into vertex colour and lightmap, so parity holds. Shader packs also need **block identity** for each vertex (`mc_Entity`, from the block-id mapping in `block.properties`). If every quad reports its **bit material's** block state rather than the chiseled block, then:
- glass bits refract and are treated as translucent glass,
- leaf bits wave, and
- emissive bits glow under the shader pack.

That would be a visible improvement over today, where everything is tagged as `chiseled_block`. Scena's sink therefore needs a `setBlockContext(BlockState material)` call that the Iris integration forwards to Iris's block-id context.

## 5. Cost estimate

Per block and direction, the AO neighbour sampling is shared by all its quads (Sodium already caches it, and V2 would too). Per quad, it is 4 bilinear weight evaluations. For a typical detailed block (around 500 quads), lighting should cost roughly the same as or less than meshing.

## 6. Open questions (round 6)

1. Vanilla path: **V1** (delegate, guaranteed parity, small allocation) or **V2** (re-implement, fastest)?
2. Should "exact vanilla" stay the only mode, or should we add an optional **bit-level self-AO** (AO computed from the block's own bits at 1/16 resolution) that breaks strict parity but looks better?
3. Tag quads with the bit material's block state for Iris `mc_Entity`?
4. Should AO vs flat be decided per material emission, as proposed in section 3?

## 7. Decisions (round 6)

| Topic | Decision |
|---|---|
| Vanilla/NeoForge implementation | **V1 first:** delegate to the live `BlockModelLighter` (vanilla or NeoForge enhanced) through a throwaway `BakedQuad`. **V2 later:** an allocation-free re-implementation, shipped only once golden tests (comparing V2 output with V1 across randomized neighbourhoods) pass. |
| Bit-level self-AO | Optional client setting, **off by default**. Strict vanilla parity is the default. |
| Iris block identity | Every quad is tagged with its **bit material's** `BlockState` (for `mc_Entity`). |
| AO vs flat | Decided **per bit material** emission. |

# Chiseled block rendering rewrite: design notes

Status: **brainstorm / design**. Nothing in here is implemented yet.

| Document | Content |
|---|---|
| [01-research.md](01-research.md) | What the platforms offer: GPU API, vanilla section pipeline (26.2 and 26.4 snapshot), NeoForge, Fabric, Sodium, Iris |
| [02-design.md](02-design.md) | Design direction, rejected approaches and reasons, open questions |
| [03-scena-api.md](03-scena-api.md) | Proposed Scena API surface (hooks, events, sinks) |
| [04-mesher.md](04-mesher.md) | C&B mesher: snapshot, bitwise culling, greedy rects, template budget |
| [05-lighting.md](05-lighting.md) | Lighting parity: vanilla/NeoForge/Sodium lighters, emission, Iris block ids |
| [06-translucency.md](06-translucency.md) | Translucency per renderer, 26.4 OIT, shard sorting policy |
| [07-poc-plan.md](07-poc-plan.md) | Phased proof-of-concept and rollout plan with exit criteria |

## Problem statement

Chiseled blocks are rendered through the vanilla block model system. `ChiseledBlockStateModel`
(`core/.../client/model/block`) is a `DataAwareBlockStateModel` that returns `BlockStateModelPart`s
containing CPU-built `BakedQuad`s. The quads are built asynchronously
(`ChiseledBlockModelDataExecutor`) and are then meshed by the game like any other block.

Two problems:

1. **Performance.** Quad generation is expensive and only works because it runs on worker threads.
2. **Hard limit in 26.4.** From 26.4 (snapshot 8) vanilla renders terrain with vertex pulling. Each
   face references a section-local `BakedQuad` template table via a **16-bit index**, so each
   section and layer can hold at most 65,536 unique quads. Chiseled blocks go past that easily
   (see [01-research.md](01-research.md#264-snapshot-8-vertex-pulling-terrain)).

## Requirements gathered so far

- Supports NeoForge and Fabric.
- Supports both **26.2** and **26.4**, with a shared core.
- Smooth lighting and AO must **match vanilla exactly**.
- Translucent bits (glass, water) must sort correctly from v1.
- **Compatible with Sodium and Iris/shader packs.** About half of players use them, and shader
  support is the most requested feature.
- Mixins are fine. They live in **Scena**, which exposes events and infrastructure to C&B.
- Performance matters most. Spreading the work out is welcome.

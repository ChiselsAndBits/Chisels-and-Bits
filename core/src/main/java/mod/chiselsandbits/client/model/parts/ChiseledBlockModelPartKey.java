package mod.chiselsandbits.client.model.parts;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.util.TriState;
import net.minecraft.world.level.block.state.BlockState;

public record ChiseledBlockModelPartKey(
    BlockState appearance,
    ChunkSectionLayer chunkSectionLayer,
    TriState ambientOcclusion,
    TextureAtlasSprite particleIcon
)
{
    public ChiseledBlockModelPart toPart(QuadCollection quads) {
        return new ChiseledBlockModelPart(
            appearance(),
            chunkSectionLayer(),
            quads,
            ambientOcclusion(),
            particleIcon()
        );
    }
}

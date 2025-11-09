package mod.chiselsandbits.client.model.parts;

import mod.chiselsandbits.api.blockinformation.BlockInformation;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.util.TriState;
import net.minecraft.world.level.block.state.BlockState;

public record ChiseledBlockModelPartKey(
    BlockInformation source,
    BlockState appearance,
    ChunkSectionLayer chunkSectionLayer,
    TriState ambientOcclusion,
    TextureAtlasSprite particleIcon
)
{
    public ChiseledBlockModelPart toPart(QuadCollection quads) {
        return new ChiseledBlockModelPart(
            source(),
            appearance(),
            chunkSectionLayer(),
            quads,
            ambientOcclusion(),
            particleIcon()
        );
    }
}

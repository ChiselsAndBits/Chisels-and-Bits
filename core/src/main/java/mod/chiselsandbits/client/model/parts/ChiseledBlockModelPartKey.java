package mod.chiselsandbits.client.model.parts;

import mod.chiselsandbits.api.blockinformation.BlockInformation;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.util.TriState;
import net.minecraft.world.level.block.state.BlockState;

public record ChiseledBlockModelPartKey(
    BlockInformation source,
    BlockState appearance,
    TriState ambientOcclusion,
    Material.Baked particleMaterial,
    int materialFlags
)
{
    public ChiseledBlockModelPart toPart(QuadCollection quads) {
        return new ChiseledBlockModelPart(
            source(),
            appearance(),
            quads,
            ambientOcclusion(),
            particleMaterial(),
            materialFlags()
        );
    }
}

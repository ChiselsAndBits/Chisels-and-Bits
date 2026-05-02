package mod.chiselsandbits.client.model.information;

import mod.chiselsandbits.client.model.builder.ChiseledBlockModelMaterial;
import mod.chiselsandbits.client.model.parts.ChiseledBlockModelPart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.NonNull;

import java.util.List;

public record ChiseledBlockModelInformation(
    Material.Baked particleMaterial,
    @BakedQuad.MaterialFlags int materialFlags,
    List<ChiseledBlockModelPart> parts,
    List<ChiseledBlockModelMaterial> materials,
    ChiseledBlockModelCacheKey key
) implements BlockStateModel
{
    public static final ChiseledBlockModelInformation EMPTY = new ChiseledBlockModelInformation(
        new Material.Baked(
            Minecraft.getInstance().getAtlasManager().get(new SpriteId(TextureAtlas.LOCATION_BLOCKS, MissingTextureAtlasSprite.getLocation()))
            , true),
        BakedQuad.FLAG_TRANSLUCENT,
        List.of(),
        List.of(),
        ChiseledBlockModelCacheKey.EMPTY
    );

    public ChiseledBlockModelInformation(
        final Material.Baked particleMaterial,
        final List<ChiseledBlockModelPart> parts,
        final List<ChiseledBlockModelMaterial> materials,
        final ChiseledBlockModelCacheKey key)
    {
        this(particleMaterial, computeMaterialFlags(parts), parts, materials, key);
    }

    private static int computeMaterialFlags(final List<ChiseledBlockModelPart> parts) {
        int result = 0;

        for (final ChiseledBlockModelPart part : parts)
        {
            result |= part.materialFlags();
        }

        return result;
    }

    @Override
    public void collectParts(final @NonNull RandomSource random, final List<BlockStateModelPart> output)
    {
        output.addAll(parts());
    }
}

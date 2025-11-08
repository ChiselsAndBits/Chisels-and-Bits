package mod.chiselsandbits.client.model.information;

import mod.chiselsandbits.client.model.builder.ChiseledBlockModelInformationBuilder;
import mod.chiselsandbits.client.model.parts.ChiseledBlockModelPart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import org.joml.Vector3f;

import java.util.List;

public record ChiseledBlockModelInformation(
    TextureAtlasSprite particleTexture,
    List<ChiseledBlockModelPart> parts,
    ChiseledBlockModelCacheKey key
)
{
    public static final ChiseledBlockModelInformation EMPTY = new ChiseledBlockModelInformation(
        Minecraft.getInstance().getAtlasManager().get(new Material(TextureAtlas.LOCATION_BLOCKS, MissingTextureAtlasSprite.getLocation())),
        List.of(),
        ChiseledBlockModelCacheKey.EMPTY
    );
}

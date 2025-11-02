package mod.chiselsandbits.api.client.icon;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

public interface IIconManager
{
    TextureAtlasSprite getIcon(ResourceLocation name);

    TextureAtlasSprite getSwapIcon();

    TextureAtlasSprite getPlaceIcon();

    TextureAtlasSprite getUndoIcon();

    TextureAtlasSprite getRedoIcon();

    TextureAtlasSprite getTrashIcon();

    TextureAtlasSprite getSortIcon();

    TextureAtlasSprite getRollXIcon();

    TextureAtlasSprite getRollZIcon();

    TextureAtlasSprite getWhiteIcon();


}

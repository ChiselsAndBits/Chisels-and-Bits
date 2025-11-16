package mod.chiselsandbits.api.client.icon;

import mod.chiselsandbits.api.IChiselsAndBitsAPI;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

public interface IIconManager
{

    public static IIconManager getInstance() {
        return IChiselsAndBitsAPI.getInstance().getIconManager();
    }

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

    TextureAtlasSprite getFilterBagModeIcon();

    TextureAtlasSprite getNormalBagModeIcon();

    TextureAtlasSprite getBagPicksUpFirstIcon();

    TextureAtlasSprite getBagPicksUpSecondIcon();

}

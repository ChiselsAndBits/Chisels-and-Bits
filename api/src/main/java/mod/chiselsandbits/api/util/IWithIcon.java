package mod.chiselsandbits.api.util;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * An object with an icon to render.
 */
public interface IWithIcon
{
    /**
     * The icon to render.
     *
     * @return The icon.
     */
    TextureAtlasSprite getIcon();
}

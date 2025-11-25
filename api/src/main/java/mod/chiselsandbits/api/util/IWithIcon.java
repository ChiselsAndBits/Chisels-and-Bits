package mod.chiselsandbits.api.util;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

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
    ResourceLocation getIcon();
}

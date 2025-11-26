package mod.chiselsandbits.api.util;

import net.minecraft.resources.Identifier;

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
    Identifier getIcon();
}

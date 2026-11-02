package mod.chiselsandbits.api.util;

import net.minecraft.resources.Identifier;

/**
 * An object with an icon to render.
 */
public interface IWithIcon {
	/**
	 * The icon to render.
	 *
	 * @return The icon.
	 */
	Identifier getIcon();

	/**
	 * Indicates whether this group should render as a mode icon on the player hotbar.
	 *
	 * <p>
	 * By default, this is disabled.
	 * Only specific gameplay aspects which have icons turn this on to aid the player.
	 * </p>
	 * @return {@code true} when we should render it on the hotbar.
	 */
	default boolean shouldRenderOnHotBar() {
		return false;
	}
}

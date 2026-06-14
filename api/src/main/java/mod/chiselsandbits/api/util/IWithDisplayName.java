package mod.chiselsandbits.api.util;

import net.minecraft.network.chat.Component;

/**
 * Represents an object that is translatable.
 */
public interface IWithDisplayName
{
	/**
     * Returns the display name of the object in a text component.
     *
     * @return The display name.
     */
	Component getDisplayName();

	/**
     * Returns the display name of the object in a text component.
     * This display name may contain several newline statements which can be wrapped.
     *
     * @return The multiline display name.
     */
	default Component getMultiLineDisplayName() {
		return getDisplayName();
	}
}

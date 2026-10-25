package mod.chiselsandbits.api.util;

import net.minecraft.network.chat.Component;

/**
 * Represents an object with a text associated with it.
 */
public interface IWithText {
	/**
	 * The text associated with this object.
	 *
	 * @return The text.
	 */
	Component getText();
}

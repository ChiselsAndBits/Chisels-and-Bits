package mod.chiselsandbits.api.measuring;

import mod.chiselsandbits.api.item.withmode.IToolMode;

import net.minecraft.world.item.DyeColor;

/**
 * Describes a mode for selecting and displaying measurements in the world.
 */

public interface IMeasuringMode extends IToolMode<IMeasuringType> {
	/**
	 * Returns the color associated with this measuring mode.
	 *
	 * @return this mode's measurement color
	 */
	DyeColor getColor();

	/**
	 * Returns the measuring type associated with this mode.
	 *
	 * @return the measurement type configured for this mode
	 */

	IMeasuringType getType();
}

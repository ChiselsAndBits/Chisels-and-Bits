package mod.chiselsandbits.api.placement;

import net.minecraft.world.item.ItemStack;

/**
 * Provides placement behavior options for an item.
 */

public interface IPlacementProperties {
	/**
	 * Invoked to determine whether this replaces non-air bits of blocks, or only replaces air bits.
	 *
	 * @param heldStack item stack whose placement behavior is being checked
	 * @return {@code true} when placement may replace occupied bits
	 */
	default boolean overridesOccupiedBits(ItemStack heldStack) {
		return true;
	}
}

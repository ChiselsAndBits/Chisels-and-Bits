package mod.chiselsandbits.api.placement;

import net.minecraft.world.item.ItemStack;

public interface IPlacementProperties
{
	/**
     * Invoked to determine whether this replaces non-air bits of blocks, or only replaces air bits.
     */
	default boolean overridesOccupiedBits(ItemStack heldStack)
	{
		return true;
	}
}

package mod.chiselsandbits.api.sealing;

import mod.chiselsandbits.api.exceptions.SealingNotSupportedException;

import net.minecraft.world.item.ItemStack;

/**
 * Represents something which supports the unsealing of itemstacks.
 */
public interface ISupportsUnsealing {
	/**
	 * Performs the unsealing operation on the given itemstack.
	 *
	 * @param source The source itemstack to unseal.
	 * @return The unsealed variant of the itemstack.
	 * @throws SealingNotSupportedException Thrown when the given stack could not be unsealed.
	 */
	ItemStack unseal(ItemStack source) throws SealingNotSupportedException;
}

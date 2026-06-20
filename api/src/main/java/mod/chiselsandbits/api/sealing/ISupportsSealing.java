package mod.chiselsandbits.api.sealing;

import mod.chiselsandbits.api.exceptions.SealingNotSupportedException;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Represents something which supports the sealing of itemstacks.
 */
public interface ISupportsSealing {
	/**
	 * Performs the sealing operation on the given itemstack.
	 *
	 * @param source The source itemstack to seal.
	 * @return The sealed variant of the itemstack.
	 * @throws SealingNotSupportedException Thrown when the given stack could not be sealed.
	 */
	@NotNull
	ItemStack seal(@NotNull ItemStack source) throws SealingNotSupportedException;
}

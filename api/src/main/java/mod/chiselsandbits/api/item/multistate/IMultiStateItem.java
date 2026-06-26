package mod.chiselsandbits.api.item.multistate;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * An item whose itemstacks contain multistate information.
 */
public interface IMultiStateItem {
	/**
	 * Creates an itemstack aware context wrapper that gives access to the
	 * multistate information contained within the given itemstack.
	 *
	 * @param stack The stack to get an {@link IMultiStateItemStack} for.
	 * @return The {@link IMultiStateItemStack} that represents the data in the given itemstack.
	 */
	IMultiStateItemStack createItemStack(final ItemStack stack);
}

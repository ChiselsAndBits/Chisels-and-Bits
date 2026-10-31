package mod.chiselsandbits.api.item.named;

import net.minecraft.world.item.ItemStack;

/**
 * Contract for highlighted-name items that adapt their display stack dynamically.
 */
public interface IDynamicallyHighlightedNameItem extends IPermanentlyHighlightedNameItem {
	/**
	 * Adapts the current tool stack for the dynamic item name.
	 *
	 * @param currentToolStack item stack whose name is being displayed
	 * @return a stack adapted for generating the dynamic name
	 */
	ItemStack adaptItemStack(ItemStack currentToolStack);
}

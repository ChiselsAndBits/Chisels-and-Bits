package mod.chiselsandbits.api.item.named;

import net.minecraft.world.item.ItemStack;

public interface IDynamicallyHighlightedNameItem extends IPermanentlyHighlightedNameItem {
	ItemStack adaptItemStack(final ItemStack currentToolStack);
}

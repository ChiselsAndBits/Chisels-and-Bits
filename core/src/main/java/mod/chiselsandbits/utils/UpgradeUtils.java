package mod.chiselsandbits.utils;

import mod.chiselsandbits.registrars.ModItems;
import net.minecraft.world.item.ItemStack;

public class UpgradeUtils
{

    private UpgradeUtils()
    {
    }

    public static ItemStack consolidateBitBag(ItemStack stack) {
        return new ItemStack(ModItems.ITEM_BIT_BAG.get().builtInRegistryHolder(), stack.getCount(), stack.getComponentsPatch());
    }
}

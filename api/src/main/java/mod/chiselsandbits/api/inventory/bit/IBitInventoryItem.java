package mod.chiselsandbits.api.inventory.bit;

import net.minecraft.world.item.ItemStack;

/**
 * Represents an item which is a bit inventory.
 */
public interface IBitInventoryItem
{

    /**
     * Creates a bit inventory which is represented by the
     * given itemstack which contains this item.
     *
     * @param stack The stack to create an inventory of.
     *
     * @return The bit inventory.
     */
    IBitInventoryItemStack create(final ItemStack stack);

    /**
     * Indicates if this items inventory is an inventory which should be preferred when bits are being picked up by the player.
     *
     * @param stack The stack to check for.
     * @return True when preferred, false when not.
     */
    boolean isPreferredPickupInventory(final ItemStack stack);

    /**
     * Indicates if this items inventory is an inventory which is filtered, so that only bits which are already in the inventory are added when the player picks them up.
     *
     * @param stack The stack to check for.
     * @return True when filtered, false when not.
     */
    boolean isFilteredPickupInventory(final ItemStack stack);
}

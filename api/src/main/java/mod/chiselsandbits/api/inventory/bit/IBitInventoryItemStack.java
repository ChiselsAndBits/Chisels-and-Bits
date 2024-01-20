package mod.chiselsandbits.api.inventory.bit;

import mod.chiselsandbits.api.blockinformation.IBlockInformation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * A bit inventory which can be converted to an itemstack.
 */
public interface IBitInventoryItemStack extends IBitInventory, Container
{

    /**
     * Converts this bit inventory into an itemstack.
     *
     * @return The itemstack which represents this inventory.
     */
    ItemStack toItemStack();

    /**
     * This lists the contents of the itemstacks bit inventory.
     *
     * @return The contents.
     */
    List<Component> listContents();

    /**
     * Calculates the fullness ratio of the itemstack inventory.
     * Useful to render the fullness ration as durability bar on the item.
     *
     * @return The filled ratio.
     */
    double getFilledRatio();

    /**
     * Clears the bit inventory of the current state.
     *
     * @param state The blockstate to remove from the bit inventory.
     */
    void clear(IBlockInformation state);

    /**
     * Sorts the bit inventory.
     */
    void sort();

    /**
     * Converts the inventory into blocks
     */
    void convert(Player player);
}

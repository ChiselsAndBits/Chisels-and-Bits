package mod.chiselsandbits.api.inventory.management;

import mod.chiselsandbits.api.IChiselsAndBitsAPI;
import mod.chiselsandbits.api.inventory.bit.IBitInventory;
import mod.chiselsandbits.api.inventory.bit.IBitInventoryItemStack;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Manager which deals with bit inventories.
 */
public interface IBitInventoryManager {
	/**
	 * Gives access to the bit inventory manager.
	 *
	 * @return The bit inventory manager.
	 */
	static IBitInventoryManager getInstance() {
		return IChiselsAndBitsAPI.getInstance().getBitInventoryManager();
	}

	/**
	 * Creates a new bit inventory wrapping the inventory of the player.
	 *
	 * <p>
	 * This player inventory is aware of items which themselves can act
	 * as a bit inventory.
	 *
	 * @param playerEntity The player inventory.
	 * @return The bit inventory which represents the players inventory.
	 */
	IBitInventory create(Player playerEntity);

	/**
	 * Creates a new bit inventory wrapping the given {@link Object}.
	 *
	 * <p>
	 * This inventory is aware of items which themselves can act
	 * as a bit inventory.
	 *
	 * <p>
	 * This endpoint is platform specific and might or might not be able to convert the object given.
	 * Importantly on forge this endpoint is able to accept IItemHandlers, while on Fabric it will only support
	 * IInventory.
	 *
	 * @param target The {@link Object}.
	 * @return The bit inventory which represents the inventory.
	 */
	IBitInventory create(Object target);

	/**
	 * Creates a new bit inventory wrapping the given inventory.
	 *
	 * <p>
	 * This inventory is aware of items which themselves can act
	 * as a bit inventory.
	 *
	 * @param inventory The inventory.
	 * @return The bit inventory which represents the inventory.
	 */
	IBitInventory create(Container inventory);

	/**
	 * Creates a new bit inventory wrapping the given itemstack.
	 *
	 * @param stack The itemstack to wrap.
	 *
	 * @return The bit inventory which represents the inventory.
	 */
	IBitInventoryItemStack create(ItemStack stack);
}

package mod.chiselsandbits.api.util;

import mod.chiselsandbits.api.item.chiseled.IChiseledBlockItem;
import mod.chiselsandbits.api.item.multistate.IMultiStateItemStack;
import mod.chiselsandbits.api.multistate.accessor.identifier.IAreaShapeIdentifier;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshot;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Helpers for finding and extracting chiseled items from player inventories.
 */

public class InventoryUtils {
	private InventoryUtils() {
		throw new IllegalStateException("Can not instantiate an instance of: InventoryUtils.This is a utility class");
	}

	/**
	 * Finds a chiseled item whose shape identifier matches the supplied snapshot.
	 *
	 * @param player player whose inventory is searched
	 * @param snapshot snapshot providing the shape identifier to match
	 * @return the matching item stack, or {@link ItemStack#EMPTY} if none is present
	 */

	public static ItemStack getChiseledStackMatchingSnapshot(final Player player, final IMultiStateSnapshot snapshot) {
		final IAreaShapeIdentifier identifier = snapshot.createNewShapeIdentifier();

		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			final ItemStack stack = player.getInventory().getItem(i);

			if (stack.getItem() instanceof IChiseledBlockItem) {
				final IChiseledBlockItem chiseledBlockItem = (IChiseledBlockItem) stack.getItem();
				final IMultiStateItemStack stateItemStack = chiseledBlockItem.createItemStack(stack);

				if (stateItemStack.createNewShapeIdentifier().equals(identifier)) {
					return stack;
				}
			}
		}

		return ItemStack.EMPTY;
	}

	/**
	 * Removes one item from the first chiseled stack matching the supplied snapshot.
	 *
	 * @param player player whose inventory is searched
	 * @param snapshot snapshot providing the shape identifier to match
	 */

	public static void extractChiseledStackMatchingSnapshot(final Player player, final IMultiStateSnapshot snapshot) {
		final IAreaShapeIdentifier identifier = snapshot.createNewShapeIdentifier();

		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			final ItemStack stack = player.getInventory().getItem(i);

			if (stack.getItem() instanceof IChiseledBlockItem) {
				final IChiseledBlockItem chiseledBlockItem = (IChiseledBlockItem) stack.getItem();
				final IMultiStateItemStack stateItemStack = chiseledBlockItem.createItemStack(stack);

				if (stateItemStack.createNewShapeIdentifier().equals(identifier)) {
					stack.shrink(1);
					return;
				}
			}
		}
	}
}

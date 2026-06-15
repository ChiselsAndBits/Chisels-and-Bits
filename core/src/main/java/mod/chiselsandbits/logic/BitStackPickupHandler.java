package mod.chiselsandbits.logic;

import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.inventory.bit.IBitInventory;
import mod.chiselsandbits.api.inventory.management.IBitInventoryManager;
import mod.chiselsandbits.api.item.bit.IBitItem;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class BitStackPickupHandler
{
	public static boolean pickupItems(final ItemEntity entityItem, final Player player)

	{
		if (entityItem != null)

		{
			final ItemStack itemStack = entityItem.getItem();
			if (!itemStack.isEmpty()
					&& itemStack.getItem() instanceof final IBitItem bitItem
					&& (!entityItem.hasPickUpDelay() && (entityItem.target == null || entityItem.target.equals(player.getUUID())))
			) {
				final IBitInventory playerInventory = IBitInventoryManager.getInstance().create(player);
				final BlockInformation containedInformation = bitItem.getBlockInformation(itemStack);
				final int insertionCount = Math.min(itemStack.getCount(), playerInventory.getMaxInsertAmount(containedInformation));

				playerInventory.insert(containedInformation, insertionCount);

				itemStack.setCount(itemStack.getCount() - insertionCount);

				if (itemStack.isEmpty())

				{
					entityItem.remove(Entity.RemovalReason.DISCARDED);
				}

				player.level().playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
				return true;
			}
		}

		return false;
	}
}

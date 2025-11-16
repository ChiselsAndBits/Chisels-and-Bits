package mod.chiselsandbits.inventory.bit;

import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.inventory.bit.IBitInventoryItem;
import mod.chiselsandbits.api.inventory.bit.IBitInventoryItemStack;
import mod.chiselsandbits.api.item.bit.IBitItem;
import mod.chiselsandbits.api.item.bit.IBitItemManager;
import mod.chiselsandbits.inventory.player.PlayerMainAndOffhandInventoryWrapper;
import net.minecraft.world.item.ItemStack;

import java.util.stream.IntStream;

public class PlayerBitInventory extends ContainerBitInventory
{
    public PlayerBitInventory(final PlayerMainAndOffhandInventoryWrapper inventory)
    {
        super(inventory);
    }

    @Override
    public int getMaxExtractAmount(final BlockInformation blockState)
    {
        return IntStream.range(0, getInventorySize())
            .mapToObj(this::getItem)
            .mapToInt(stack -> {
                if (stack.getItem() instanceof IBitItem bitItem && bitItem.getBlockInformation(stack).equals(blockState))
                {
                    return stack.getCount();
                }

                if (stack.getItem() instanceof IBitInventoryItem bitInventoryItem)
                {
                    return bitInventoryItem.create(stack).getMaxExtractAmount(blockState);
                }

                return 0;
            })
            .sum();
    }

    @Override
    public int getMaxInsertAmount(final BlockInformation blockState)
    {
        return IntStream.range(0, getInventorySize())
            .mapToObj(this::getItem)
            .mapToInt(stack -> {
                if (stack.isEmpty())
                {
                    return getMaxBitsFor(stack);
                }

                if (stack.getItem() instanceof IBitItem bitItem && bitItem.getBlockInformation(stack).equals(blockState))
                {
                    return getMaxBitsFor(stack) - stack.getCount();
                }

                if (stack.getItem() instanceof IBitInventoryItem bitInventoryItem)
                {
                    return bitInventoryItem.create(stack).getMaxInsertAmount(blockState);
                }

                return 0;
            })
            .sum();
    }

    @Override
    public void extract(final BlockInformation blockInformation, int count) throws IllegalArgumentException
    {
        for (int i = 0; i < getInventorySize(); i++)
        {
            if (count <= 0)
            {
                return;
            }

            final ItemStack stack = getItem(i);
            if (stack.isEmpty())
            {
                continue;
            }

            if (stack.getItem() instanceof IBitItem bitItem && bitItem.getBlockInformation(stack).equals(blockInformation))
            {
                final int toExtract = Math.min(count, stack.getCount());
                stack.setCount(
                    stack.getCount() - toExtract
                );
                setItem(i, stack);
                count -= toExtract;
                continue;
            }

            if (stack.getItem() instanceof IBitInventoryItem bitInventoryItem)
            {
                final IBitInventoryItemStack inventory = bitInventoryItem.create(stack);
                final int maxExtractableCount = inventory.getMaxExtractAmount(blockInformation);
                final int toExtract = Math.min(count, maxExtractableCount);
                inventory.extract(blockInformation, toExtract);
                setItem(i, inventory.toItemStack());
                count -= toExtract;
            }
        }
    }

    @Override
    public void insert(final BlockInformation blockInformation, int count) throws IllegalArgumentException
    {
        //Check for preferred bit bag pickup / insertion handling.
        for (int i = 0; i < getInventorySize(); i++)
        {
            if (count <= 0)
            {
                return;
            }

            final ItemStack stack = getItem(i);
            if (stack.getItem() instanceof IBitInventoryItem bitInventoryItem && bitInventoryItem.isPreferredPickupInventory(stack))
            {
                final IBitInventoryItemStack inventory = bitInventoryItem.create(stack);

                if (!bitInventoryItem.isFilteredPickupInventory(stack) || inventory.contains(blockInformation)) {
                    final int maxInsertableCount = inventory.getMaxInsertAmount(blockInformation);
                    final int toInsert = Math.min(count, maxInsertableCount);
                    inventory.insert(blockInformation, toInsert);
                    setItem(i, inventory.toItemStack());
                    count -= toInsert;
                }
            }
        }

        //Now check if we can fill up the player inventory.
        for (int i = 0; i < getInventorySize(); i++)
        {
            if (count <= 0)
            {
                return;
            }

            final ItemStack stack = getItem(i);
            //Currently empty slot, so simply insert the max amount (either max stack size, or the current count, what ever is smaller)
            if (stack.isEmpty())
            {
                final int toInsert = Math.min(getMaxBitsFor(stack), count);
                final ItemStack bitStack = IBitItemManager.getInstance().create(blockInformation, toInsert);
                setItem(i, bitStack);
                count -= toInsert;
                continue;
            }

            //Already is a bit, lets check if it is the same bit, and increment.
            if (stack.getItem() instanceof IBitItem bitItem && bitItem.getBlockInformation(stack).equals(blockInformation))
            {
                final int toInsert = Math.min(count, getMaxBitsFor(stack) - stack.getCount());
                stack.setCount(
                    stack.getCount() + toInsert
                );
                setItem(i, stack);
                count -= toInsert;
            }
        }

        //No more further space in the player inventory, so inject into all the none preferred bags.
        for (int i = 0; i < getInventorySize(); i++)
        {
            if (count <= 0)
            {
                return;
            }

            final ItemStack stack = getItem(i);
            if (stack.getItem() instanceof IBitInventoryItem bitInventoryItem && !bitInventoryItem.isPreferredPickupInventory(stack))
            {
                final IBitInventoryItemStack inventory = bitInventoryItem.create(stack);

                if (!bitInventoryItem.isFilteredPickupInventory(stack) || inventory.contains(blockInformation)) {
                    final int maxInsertableCount = inventory.getMaxInsertAmount(blockInformation);
                    final int toInsert = Math.min(count, maxInsertableCount);
                    inventory.insert(blockInformation, toInsert);
                    setItem(i, inventory.toItemStack());
                    count -= toInsert;
                }
            }
        }
    }
}

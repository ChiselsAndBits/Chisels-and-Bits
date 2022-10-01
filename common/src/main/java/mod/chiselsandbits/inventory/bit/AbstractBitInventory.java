package mod.chiselsandbits.inventory.bit;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.inventory.bit.IBitInventory;
import mod.chiselsandbits.api.inventory.bit.IBitInventoryItem;
import mod.chiselsandbits.api.inventory.bit.IBitInventoryItemStack;
import mod.chiselsandbits.api.item.bit.IBitItem;
import mod.chiselsandbits.api.item.bit.IBitItemManager;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

public abstract class AbstractBitInventory implements IBitInventory
{

    protected AbstractBitInventory() {}

    /**
     * Checks if it is possible to extract a given amount of bits with the given blockstate from the the current inventory.
     *
     * @param blockState The blockstate.
     * @param count      The amount of bits to extract.
     * @return {@code true} when extraction is possible.
     */
    @Override
    public boolean canExtract(
      final BlockInformation blockState,
      final int count)
    {
        final int contained = getMaxExtractAmount(blockState);
        return count <= contained;
    }

    /**
     * Returns the maximal amount of bits with a given blockstate which can be extracted of a given blockstate.
     *
     * @param blockState The blockstate in question.
     * @return The amount of bits that can be extracted with a given blockstate.
     */
    @Override
    public int getMaxExtractAmount(final BlockInformation blockState)
    {
        return IntStream.range(0, getInventorySize())
                 .mapToObj(this::getItem)
                 .filter(stack -> stack.getItem() instanceof IBitItem || stack.getItem() instanceof IBitInventoryItem)
                 .mapToInt(stack -> {
                     if (stack.getItem() instanceof final IBitItem bitItem) {
                         if (bitItem.getBlockInformation(stack).equals(blockState))
                             return stack.getCount();

                         return 0;
                     }

                     if (stack.getItem() instanceof final IBitInventoryItem bitInventoryItem) {
                         final IBitInventory bitInventory = bitInventoryItem.create(stack);
                         return bitInventory.getMaxExtractAmount(blockState);
                     }

                     return 0;
                 })
                 .sum();
    }

    /**
     * Gets a copy of the stack that is in the given slot.
     * 
     * @param index The index of the slot to read.
     * @return A copy of the stack in the slot.
     */
    protected abstract ItemStack getItem(final int index);

    /**
     * The size of the inventory.
     * 
     * @return The size of the inventory.
     */
    protected abstract int getInventorySize();

    /**
     * Extracts a given amount of bits with the given blockstate from the the current inventory.
     *
     * @param blockInformation The blockstate.
     * @param count      The amount of bits to extract.
     * @throws IllegalArgumentException when extraction is not possible.
     */
    @Override
    public void extract(
      final BlockInformation blockInformation,
      final int count) throws IllegalArgumentException
    {
        if (!canExtract(blockInformation, count))
            throw new IllegalArgumentException("Can not extract: " + blockInformation);

        int toExtract = count;

        for (int i = getInventorySize() - 1; i >= 0; i--)
        {
            final ItemStack stack = getItem(i);
            if (stack.getItem() instanceof final IBitInventoryItem bitInventoryItem) {
                final IBitInventoryItemStack bitInventory = bitInventoryItem.create(stack);

                final int inventoryExtractCount = Math.min(toExtract, bitInventory.getMaxExtractAmount(blockInformation));
                toExtract -= inventoryExtractCount;

                bitInventory.extract(blockInformation, inventoryExtractCount);

                final ItemStack newStack = bitInventory.toItemStack();

                setSlotContents(i, newStack);
            }
        }

        if (toExtract <= 0)
            return;

        for (int i = getInventorySize() - 1; i >= 0; i--)
        {
            final ItemStack stack = getItem(i);
            if (stack.getItem() instanceof final IBitItem bitItem) {
                if (bitItem.getBlockInformation(stack).equals(blockInformation)) {
                    final int stackExtractCount = Math.min(toExtract, stack.getCount());
                    toExtract -= stackExtractCount;

                    stack.setCount(stack.getCount() - stackExtractCount);

                    setSlotContents(i, stack);
                }
            }
        }
    }

    /**
     * Sets the slot with the given index with the given stack.
     *
     * @param index The index of the slot.
     * @param stack The stack to insert.
     */
    protected abstract void setSlotContents(final int index, final ItemStack stack);

    /**
     * Checks if it is possible to insert a given amount of bits with the given blockstate from the the current inventory.
     *
     * @param blockState The blockstate.
     * @param count      The amount of bits to insert.
     * @return {@code true} when insertion is possible.
     */
    @Override
    public boolean canInsert(final BlockInformation blockState, final int count)
    {
        final int insertionCount = getMaxInsertAmount(blockState);
        return count <= insertionCount;
    }

    protected int getMaxBitsForSlot() {
        return IBitItemManager.getInstance().getMaxStackSize();
    }

    /**
     * Returns the maximal amount of bits with a given blockstate which can be inserted of a given blockstate.
     *
     * @param blockInformation The blockstate in question.
     * @return The amount of bits that can be inserted with a given blockstate.
     */
    @Override
    public int getMaxInsertAmount(final BlockInformation blockInformation)
    {
        final long maxInsertAmount = IntStream.range(0, getInventorySize())
                 .mapToObj(this::getItem)
                 .filter(stack -> stack.getItem() instanceof IBitItem || stack.getItem() instanceof IBitInventoryItem || stack.isEmpty())
                 .mapToLong(stack -> {
                     if (stack.isEmpty())
                         return getMaxBitsForSlot();

                     if (stack.getItem() instanceof final IBitItem bitItem) {
                         if (bitItem.getBlockInformation(stack).equals(blockInformation))
                             return getMaxBitsForSlot() - stack.getCount();

                         return 0;
                     }

                     if (stack.getItem() instanceof final IBitInventoryItem bitInventoryItem) {
                         final IBitInventory bitInventory = bitInventoryItem.create(stack);
                         return bitInventory.getMaxInsertAmount(blockInformation);
                     }

                     return 0;
                 })
                 .sum();

        return (int)Math.min(maxInsertAmount, Integer.MAX_VALUE);
    }

    /**
     * Inserts a given amount of bits with the given blockstate from the the current inventory.
     *
     * @param blockInformation The blockstate.
     * @param count      The amount of bits to insert.
     * @throws IllegalArgumentException when insertion is not possible.
     */
    @Override
    public void insert(final BlockInformation blockInformation, final int count) throws IllegalArgumentException
    {
        if (!canInsert(blockInformation, count))
            throw new IllegalArgumentException("Can not insert: " + blockInformation);

        int currentRawCount = 0;
        for (int i = 0; i < getInventorySize(); i++)
        {
            final ItemStack stack = getItem(i);
            if (stack.getItem() instanceof final IBitItem bitItem) {
                if (bitItem.getBlockInformation(stack).equals(blockInformation)) {
                    currentRawCount += stack.getCount();
                }
            }
        }

        int toInsert = count;

        if (currentRawCount == 0) {
            for (int i = 0; i < getInventorySize(); i++)
            {
                final ItemStack stack = getItem(i);
                if (stack.isEmpty()) {
                    final int stackInsertCount = Math.min(toInsert, getMaxBitsForSlot());

                    if (stackInsertCount > 0) {
                        toInsert -= stackInsertCount;

                        final ItemStack newStack = IBitItemManager.getInstance().create(blockInformation, stackInsertCount);

                        setSlotContents(i, newStack);
                        break;
                    }
                }

                if (toInsert <= 0)
                    return;
            }
        }

        if (currentRawCount < getMaxBitsForSlot()) {
            for (int i = 0; i < getInventorySize(); i++)
            {
                final ItemStack stack = getItem(i);
                if (stack.getItem() instanceof final IBitItem bitItem) {
                    if (bitItem.getBlockInformation(stack).equals(blockInformation)) {
                        final int stackInsertCount = Math.min(toInsert, getMaxBitsForSlot() - stack.getCount());

                        if (stackInsertCount > 0) {
                            toInsert -= stackInsertCount;

                            stack.setCount(stack.getCount() + stackInsertCount);

                            setSlotContents(i, stack);
                        }
                    }
                }

                if (toInsert <= 0)
                    return;
            }
        }

        for (int i = getInventorySize() - 1; i >= 0; i--)
        {
            final ItemStack stack = getItem(i);
            if (stack.getItem() instanceof final IBitInventoryItem bitInventoryItem) {
                final IBitInventoryItemStack bitInventory = bitInventoryItem.create(stack);

                final int inventoryInsertCount = Math.min(toInsert, bitInventory.getMaxInsertAmount(blockInformation));

                if (inventoryInsertCount > 0) {
                    toInsert -= inventoryInsertCount;

                    bitInventory.insert(blockInformation, inventoryInsertCount);

                    final ItemStack newStack = bitInventory.toItemStack();
                    setSlotContents(i, newStack);
                }
            }

            if (toInsert <= 0)
                return;
        }


        for (int i = 0; i < getInventorySize(); i++)
        {
            final ItemStack stack = getItem(i);
            if (stack.getItem() instanceof final IBitItem bitItem) {
                if (bitItem.getBlockInformation(stack).equals(blockInformation)) {
                    final int stackInsertCount = Math.min(toInsert, getMaxBitsForSlot() - stack.getCount());

                    if (stackInsertCount > 0) {
                        toInsert -= stackInsertCount;

                        stack.setCount(stack.getCount() + stackInsertCount);

                        setSlotContents(i, stack);
                    }
                }
            }

            if (toInsert <= 0)
                return;
        }

        for (int i = 0; i < getInventorySize(); i++)
        {
            final ItemStack stack = getItem(i);
            if (stack.isEmpty()) {
                final int stackInsertCount = Math.min(toInsert, getMaxBitsForSlot());

                if (stackInsertCount > 0) {
                    toInsert -= stackInsertCount;

                    final ItemStack newStack = IBitItemManager.getInstance().create(blockInformation, stackInsertCount);

                    setSlotContents(i, newStack);
                }
            }

            if (toInsert <= 0)
                return;
        }
    }

    @Override
    public Map<BlockInformation, Integer> getContainedStates()
    {
        return IntStream.range(0, getInventorySize())
          .mapToObj(this::getItem)
          .filter(stack -> stack.getItem() instanceof IBitItem || stack.getItem() instanceof IBitInventoryItem)
          .map((Function<ItemStack, HashMap<BlockInformation, Integer>>) stack -> {
              if (stack.getItem() instanceof final IBitItem bitItem) {
                  return Maps.newHashMap(ImmutableMap.of(bitItem.getBlockInformation(stack), stack.getCount()));
              }

              if (stack.getItem() instanceof final IBitInventoryItem bitInventoryItem) {
                  final IBitInventory bitInventory = bitInventoryItem.create(stack);
                  return Maps.newHashMap(bitInventory.getContainedStates());
              }

              return Maps.newHashMap(ImmutableMap.of());
          })
          .reduce(
            Maps.newHashMap(),
            (blockStateIntegerHashMap, blockStateIntegerHashMap2) -> {
                final HashMap<BlockInformation, Integer> result = Maps.newHashMap(blockStateIntegerHashMap);
                blockStateIntegerHashMap2.forEach((state, count) -> {
                    if (!result.containsKey(state))
                        result.put(state, count);
                    else
                        result.put(state, result.get(state) + count);
                });

                return result;
            }
          );
    }
}

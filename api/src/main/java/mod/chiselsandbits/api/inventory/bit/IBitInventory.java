package mod.chiselsandbits.api.inventory.bit;

import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.item.bit.IBitItem;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * Represents an inventory in which bits are contained.
 */
public interface IBitInventory
{
    /**
     * Checks if it is possible to extract exactly one bit with the given block information from
     * the current inventory.
     *
     * @param blockInformation The block information.
     * @return {@code true} when extraction is possible.
     */
    default boolean canExtractOne(final BlockInformation blockInformation) {
        return this.canExtract(blockInformation, 1);
    }

    /**
     * Checks if it is possible to extract a given amount of bits with the given block information from
     * the current inventory.
     *
     * @param blockInformation The block information.
     * @param count The amount of bits to extract.
     * @return {@code true} when extraction is possible.
     */
    boolean canExtract(final BlockInformation blockInformation, final int count);

    /**
     * Returns the maximal amount of bits with a given block information which can be extracted
     * of a given blockstate.
     * 
     * @param blockInformation The block information in question.
     * @return The amount of bits that can be extracted with a given blockstate.
     */
    int getMaxExtractAmount(final BlockInformation blockInformation);
    
    /**
     * Extracts exactly one bit with the given block information from
     * the current inventory.
     *
     * @param blockInformation The block information.
     * @throws IllegalArgumentException when extraction is not possible.
     */
    default void extractOne(final BlockInformation blockInformation) throws IllegalArgumentException {
        this.extract(blockInformation, 1);
    }

    /**
     * Extracts a given amount of bits with the given block information from
     * the current inventory.
     *
     * @param blockInformation The block information.
     * @param count The amount of bits to extract.
     * @throws IllegalArgumentException when extraction is not possible.
     */
    void extract(final BlockInformation blockInformation, final int count) throws IllegalArgumentException;

    /**
     * Checks if it is possible to insert exactly one bit with the given block information from
     * the current inventory.
     *
     * @param blockInformation The block information.
     * @return {@code true} when insertion is possible.
     */
    default boolean canInsertOne(final BlockInformation blockInformation) {
        return this.canInsert(blockInformation, 1);
    }

    /**
     * Checks if it is possible to insert a given amount of bits with the given block information from
     * the current inventory.
     *
     * @param blockInformation The block information.
     * @param count The amount of bits to insert.
     * @return {@code true} when insertion is possible.
     */
    boolean canInsert(final BlockInformation blockInformation, final int count);

    /**
     * Returns the maximal amount of bits with a given block information which can be inserted
     * of a given blockstate.
     *
     * @param blockInformation The blockstate in question.
     * @return The amount of bits that can be inserted with a given blockstate.
     */
    int getMaxInsertAmount(final BlockInformation blockInformation);

    /**
     * Inserts exactly one bit with the given block information from
     * the current inventory.
     *
     * @param blockInformation The block information.
     * @throws IllegalArgumentException when insertion is not possible.
     */
    default void insertOne(final BlockInformation blockInformation) throws IllegalArgumentException {
        this.insert(blockInformation, 1);
    }

    /**
     * Inserts a given amount of bits with the given block information from
     * the current inventory.
     *
     * @param blockInformation The block information.
     * @param count The amount of bits to insert.
     * @throws IllegalArgumentException when insertion is not possible.
     */
    void insert(final BlockInformation blockInformation, final int count) throws IllegalArgumentException;

    /**
     * Inserts a given amount of bits with the given block information from
     * the current inventory, discards bits that don't fit.
     *
     * @param blockInformation The block information.
     * @param count The amount of bits to insert.
     */
    default void insertOrDiscard(final BlockInformation blockInformation, final int count) {
        insert(blockInformation, Math.min(getMaxInsertAmount(blockInformation), count));
    }

    /**
     * Indicates if this inventory is empty or not.
     * @return {@code true} when empty.
     */
    boolean isEmpty();

    /**
     * Tries to insert a given itemstack with a bit item into the inventory.
     * Draining the itemstack completely if possible.
     *
     * @param stack The stack to insert.
     * @return The remainder, or the original stack if it is not an bit item.
     */
    default ItemStack insert(final ItemStack stack)
    {
        if (!(stack.getItem() instanceof final IBitItem bitItem))
            return stack;

        final BlockInformation blockInformation = bitItem.getBlockInformation(stack);

        final int maxToInsertCount = this.getMaxInsertAmount(blockInformation);
        final int maxToInsertFromStack = Math.min(stack.getCount(), maxToInsertCount);
        final int maxRemainingOnStack = stack.getCount() - maxToInsertCount;

        this.insert(blockInformation, maxToInsertFromStack);

        final ItemStack resultStack = stack.copy();
        resultStack.setCount(maxRemainingOnStack);
        return resultStack;
    }

    /**
     * Returns the summed contained states of all bits in the inventory.
     * @return The contained state count of all bits in the inventory.
     */
    Map<BlockInformation, Integer> getContainedStates();

    /**
     * Indicates whether the inventory contains a bit with this block.
     *
     * @param blockInformation The block to check for.
     * @return True when contained.
     */
    boolean contains(BlockInformation blockInformation);
}

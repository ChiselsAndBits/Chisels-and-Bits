package mod.chiselsandbits.forge.inventory.bit;

import mod.chiselsandbits.inventory.bit.AbstractBitInventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public class IItemHandlerBitInventory extends AbstractBitInventory
{

    private final ResourceHandler<ItemResource> itemHandler;

    public IItemHandlerBitInventory(final ResourceHandler<ItemResource> itemHandler) {this.itemHandler = itemHandler;}

    /**
     * Gets a copy of the stack that is in the given slot.
     *
     * @param index The index of the slot to read.
     * @return A copy of the stack in the slot.
     */
    @Override
    protected ItemStack getItem(final int index)
    {
        return itemHandler.getResource(index).toStack();
    }

    /**
     * The size of the inventory.
     *
     * @return The size of the inventory.
     */
    @Override
    protected int getInventorySize()
    {
        return itemHandler.size();
    }

    /**
     * Sets the slot with the given index with the given stack.
     *
     * @param index The index of the slot.
     * @param stack The stack to insert.
     */
    @Override
    protected void setSlotContents(final int index, final ItemStack stack)
    {
        try(Transaction tx = Transaction.openRoot())
        {
            itemHandler.extract(index, ItemResource.of(stack), stack.getCount(), tx);
            if (itemHandler.insert(index, ItemResource.of(stack), stack.getCount(), tx) != stack.getCount()) {
                throw new IllegalStateException("Failed to insert stack.");
            }

            tx.commit();
        }
    }

    @Override
    public boolean isEmpty()
    {
        return ResourceHandlerUtil.isEmpty(itemHandler);
    }
}

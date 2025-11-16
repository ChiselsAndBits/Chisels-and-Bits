package mod.chiselsandbits.slots;

import mod.chiselsandbits.api.config.IServerConfiguration;
import mod.chiselsandbits.item.bit.BitItem;
import mod.chiselsandbits.registrars.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class BitSlot extends Slot
{
    public BitSlot(
      final Container inventoryIn,
      final int index,
      final int xPosition,
      final int yPosition)
    {
        super(inventoryIn, index, xPosition, yPosition);
    }

    @Override
    public boolean mayPlace(
      final @NotNull ItemStack stack)
    {
        return !stack.isEmpty() && stack.getItem() instanceof BitItem;
    }

    @Override
    public int getMaxStackSize() {
        return IServerConfiguration.getInstance().getBagStackSize().get();
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return stack.getItem() instanceof BitItem ? IServerConfiguration.getInstance().getBagStackSize().get() : 0;
    }

    @Override
    public @NotNull Optional<ItemStack> tryRemove(int count, int decrement, @NotNull Player player) {
        count = Math.min(count, ModItems.ITEM_BLOCK_BIT.get().getDefaultMaxStackSize());
        return super.tryRemove(count, decrement, player);
    }

    public @NotNull ItemStack safeInsert(@NotNull ItemStack stack) {
        return this.safeInsert(stack, stack.getCount());
    }

    public @NotNull ItemStack safeInsert(ItemStack toInsert, int increment) {
        if (!toInsert.isEmpty() && this.mayPlace(toInsert)) {
            ItemStack currentStack = this.getItem();

            int remainingFree = getMaxStackSize() - currentStack.getCount();
            int i = Math.min(remainingFree, increment);

            if (i == 0)
                return toInsert;

            if (currentStack.isEmpty()) {
                this.setByPlayer(toInsert.split(i));
            } else if (ItemStack.isSameItemSameComponents(currentStack, toInsert)) {
                toInsert.shrink(i);
                currentStack.grow(i);
                this.setByPlayer(currentStack);
            }

        }

        return toInsert;
    }
}

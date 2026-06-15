package mod.chiselsandbits.container.helper;

import org.jetbrains.annotations.NotNull;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MergeSupportingHelperContainer extends AbstractContainerMenu
{
	private final int slotCount;

	public MergeSupportingHelperContainer(int slotCount)

	{
		super(null, 0);
		this.slotCount = slotCount;
	}

	@Override
	public @NotNull ItemStack quickMoveStack(@NotNull Player pPlayer, int pIndex) {
		ItemStack itemstack = ItemStack.EMPTY;
		Slot slot = this.slots.get(pIndex);

		if (slot.hasItem()) {
			ItemStack itemstack1 = slot.getItem();
			itemstack = itemstack1.copy();

			if (pIndex < slotCount) {
				if (!this.moveItemStackTo(itemstack1, slotCount, this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.moveItemStackTo(itemstack1, 0, slotCount, false)) {
				return ItemStack.EMPTY;
			}

			if (itemstack1.isEmpty()) {
				slot.set(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}
		}

		return itemstack;
	}

	@Override
	public boolean stillValid(final @NotNull Player playerIn)

	{
		return false;
	}

	public boolean doMergeItemStack(final ItemStack stack, final int startIndex, final int endIndex, final boolean reverseDirection)

	{
		return super.moveItemStackTo(stack, startIndex, endIndex, reverseDirection);
	}
}

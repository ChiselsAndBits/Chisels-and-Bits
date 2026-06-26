package mod.chiselsandbits.inventory.wrapping;

import static mod.chiselsandbits.utils.NullUtils.whenNotNull;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.Clearable;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class WrappingInventory implements Container {
	@Nullable
	private Container wrapped = null;

	@Override
	public int getContainerSize() {
		return whenNotNull(
				getWrapped(),
				0,
				Container::getContainerSize
		);
	}

	@Override
	public boolean isEmpty() {
		return whenNotNull(
				getWrapped(),
				true,
				Container::isEmpty
		);
	}

	@Override
	public ItemStack getItem(final int index) {
		return whenNotNull(
				getWrapped(),
				ItemStack.EMPTY,
				inventory -> inventory.getItem(index)
		);
	}

	@Override
	public ItemStack removeItem(final int index, final int count) {
		return whenNotNull(
				getWrapped(),
				ItemStack.EMPTY,
				inventory -> inventory.removeItem(index, count)
		);
	}

	@Override
	public ItemStack removeItemNoUpdate(final int index) {
		return whenNotNull(
				getWrapped(),
				ItemStack.EMPTY,
				inventory -> inventory.removeItemNoUpdate(index)
		);
	}

	@Override
	public void setItem(final int index, final ItemStack stack) {
		whenNotNull(
				getWrapped(),
				inventory -> inventory.setItem(index, stack)
		);
	}

	@Override
	public void setChanged() {
		whenNotNull(
				getWrapped(),
				Container::setChanged
		);
	}

	@Override
	public boolean stillValid(final Player player) {
		return whenNotNull(
				getWrapped(),
				false,
				inventory -> inventory.stillValid(player)
		);
	}

	@Override
	public void clearContent() {
		whenNotNull(
				getWrapped(),
				Clearable::clearContent
		);
	}

	@Override
	public int getMaxStackSize() {
		return whenNotNull(
				getWrapped(),
				64,
				Container::getMaxStackSize
		);
	}

	@Nullable
	public Container getWrapped() {
		return wrapped;
	}

	public void setWrapped(@Nullable final Container wrapped) {
		this.wrapped = wrapped;
	}
}

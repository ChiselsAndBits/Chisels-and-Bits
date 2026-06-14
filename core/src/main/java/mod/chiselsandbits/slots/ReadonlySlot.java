package mod.chiselsandbits.slots;

import org.jetbrains.annotations.NotNull;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ReadonlySlot extends Slot
{
	public ReadonlySlot(
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
		return false;
	}

	@Override
	public boolean mayPickup(
	  final @NotNull Player playerIn)
	{
		return false;
	}
}

package mod.chiselsandbits.inventory.scanner;

import com.communi.suggestu.scena.core.inventory.ScenaContainerMenu;
import mod.chiselsandbits.registrars.ModBlocks;
import mod.chiselsandbits.registrars.ModContainerTypes;
import mod.chiselsandbits.slots.PatternSlot;
import mod.chiselsandbits.utils.container.SimpleContainer;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ScannerMenu extends ScenaContainerMenu {
	private final ContainerLevelAccess containerLevelAccess;
	private final SimpleContainer      scannerSlotContainer;

	public ScannerMenu(int screenIndex, Inventory inventory) {
		this(screenIndex, inventory, ContainerLevelAccess.NULL);
	}

	public ScannerMenu(final int screenIndex, final Inventory inventory, final ContainerLevelAccess containerLevelAccess)

	{
		super(ModContainerTypes.PATTERN_SCANNER_CONTAINER.get(), screenIndex);
		this.containerLevelAccess = containerLevelAccess;
		this.scannerSlotContainer = new SimpleContainer(0);
	}

	@Override
	public boolean stillValid(final @NotNull Player player)

	{
		return stillValid(containerLevelAccess, player, ModBlocks.PATTERN_SCANNER.get());
	}

	@Override
	protected void registerQuickMoveRules() {
		//Noop for now.
	}
}

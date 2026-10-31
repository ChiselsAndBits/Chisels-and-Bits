package mod.chiselsandbits.input;

import mod.chiselsandbits.api.item.click.ILeftClickControllingItem;
import mod.chiselsandbits.api.item.click.IRightClickControllingItem;
import mod.chiselsandbits.utils.ItemStackUtils;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ProcessingInputTracker {
	private static final ProcessingInputTracker INSTANCE = new ProcessingInputTracker();

	public static ProcessingInputTracker getInstance() {
		return INSTANCE;
	}

	private ProcessingInputTracker() {
	}

	public void onStartedLeftClicking(final Player player) {
		//Noop
	}

	public void onStartedRightClicking(final Player player) {
		//Noop
	}

	public void onStoppedLeftClicking(final Player player) {
		final ItemStack leftClickHandlingStack = ItemStackUtils.getLeftClickControllingItemStackFromPlayer(player);

		if (leftClickHandlingStack.isEmpty()) {
			return;
		}

		if (leftClickHandlingStack.getItem() instanceof ILeftClickControllingItem leftClickControllingItem) {
			leftClickControllingItem.onLeftClickProcessingEnd(
					player, leftClickHandlingStack
			);
		}
	}

	public void onStoppedRightClicking(final Player player) {
		final ItemStack rightClickHandlingStack = ItemStackUtils.getRightClickControllingItemStackFromPlayer(player);

		if (rightClickHandlingStack.isEmpty()) {
			return;
		}

		if (rightClickHandlingStack.getItem() instanceof IRightClickControllingItem rightClickControllingItem) {
			rightClickControllingItem.onRightClickProcessingEnd(
					player, rightClickHandlingStack
			);
		}
	}
}

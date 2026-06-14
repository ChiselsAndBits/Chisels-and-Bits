package mod.chiselsandbits.logic;

import mod.chiselsandbits.api.item.click.ClickProcessingState;
import mod.chiselsandbits.api.item.click.ILeftClickControllingItem;
import mod.chiselsandbits.api.profiling.IProfilerSection;
import mod.chiselsandbits.profiling.ProfilingManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class LeftClickInteractionHandler
{
	public static ClickProcessingState leftClickOnBlock(
	  final Player player,
	  final InteractionHand hand,
	  final ItemStack itemStack,
	  final BlockPos position,
	  final Direction hitFace,
	  final boolean currentCancellationState,
	  final ClickProcessingState.ProcessingResult currentItemUsageState)
	{
		if (itemStack.getItem() instanceof ILeftClickControllingItem) {
			try(IProfilerSection ignored = ProfilingManager.getInstance().withSection("Left click processing")) {
				final ILeftClickControllingItem leftClickControllingItem = (ILeftClickControllingItem) itemStack.getItem();

				if (!leftClickControllingItem.canUse(player, itemStack)) {
					return ClickProcessingState.DENIED;
				}

				return leftClickControllingItem.handleLeftClickProcessing(
				  player,
				  hand,
				  position,
				  hitFace,
				  new ClickProcessingState(
					currentCancellationState,
					currentItemUsageState
				  )
				);
			}
		}

		return ClickProcessingState.DEFAULT;
	}
}

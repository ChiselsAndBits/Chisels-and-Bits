package mod.chiselsandbits.logic;

import mod.chiselsandbits.api.item.click.ClickProcessingState;
import mod.chiselsandbits.api.item.click.IRightClickControllingItem;
import mod.chiselsandbits.api.profiling.IProfilerSection;
import mod.chiselsandbits.api.util.RayTracingUtils;
import mod.chiselsandbits.profiling.ProfilingManager;
import mod.chiselsandbits.registrars.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class RightClickInteractionHandler {
	public static ClickProcessingState rightClickOnBlock(
			final Level level,
			final Player player,
			final InteractionHand hand,
			final ItemStack itemStack,
			final BlockPos position,
			final Direction hitFace,
			final boolean currentCancellationState,
			final ClickProcessingState.ProcessingResult currentItemUsageState
	) {
		if (level.getBlockState(position).getBlock() == ModBlocks.BIT_STORAGE.get()) {
			return ClickProcessingState.ALLOW_NO_CANCEL;
		}

		if (itemStack.getItem() instanceof IRightClickControllingItem) {
			try(IProfilerSection ignored = ProfilingManager.getInstance().withSection("Right click processing")) {
				final IRightClickControllingItem rightClickControllingItem = (IRightClickControllingItem) itemStack.getItem();

				if (!rightClickControllingItem.canUse(player, itemStack)) {
					return ClickProcessingState.DENIED;
				}

				return rightClickControllingItem.handleRightClickProcessing(
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

	public static ClickProcessingState rightClickOnItem(
			final Level level,
			final Player player,
			final InteractionHand interactionHand,
			final ItemStack itemStack,
			final boolean currentCancellationState,
			final ClickProcessingState.ProcessingResult currentItemUsageState) {
		HitResult rayTrace = RayTracingUtils.rayTracePlayer(player);

		if (rayTrace instanceof BlockHitResult blockHitResult && level.getBlockState(blockHitResult.getBlockPos()).getBlock() == ModBlocks.BIT_STORAGE.get()) {
			return ClickProcessingState.ALLOW_NO_CANCEL;
		}

		if (itemStack.getItem() instanceof IRightClickControllingItem) {
			try(IProfilerSection ignored = ProfilingManager.getInstance().withSection("Right click processing")) {
				final IRightClickControllingItem rightClickControllingItem = (IRightClickControllingItem) itemStack.getItem();

				if (!rightClickControllingItem.canUse(player, itemStack)) {
					return ClickProcessingState.DENIED;
				}

				return rightClickControllingItem.handleRightClickProcessing(
						player,
						interactionHand,
						rayTrace instanceof BlockHitResult ? ((BlockHitResult) rayTrace).getBlockPos() : new BlockPos(0, -1000, 0),
						rayTrace instanceof BlockHitResult ? ((BlockHitResult) rayTrace).getDirection() : Direction.UP,
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

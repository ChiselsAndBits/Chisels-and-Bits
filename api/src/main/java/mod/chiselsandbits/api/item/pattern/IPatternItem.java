package mod.chiselsandbits.api.item.pattern;

import mod.chiselsandbits.api.item.change.IChangeTrackingItem;
import mod.chiselsandbits.api.placement.IPlacementPreviewProvidingItem;
import mod.chiselsandbits.api.item.multistate.IMultiStateItem;
import mod.chiselsandbits.api.item.withmode.IWithModeItem;
import mod.chiselsandbits.api.pattern.placement.IPatternPlacementType;
import mod.chiselsandbits.api.placement.PlacementResult;
import mod.chiselsandbits.api.sealing.ISupportsSealing;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector4f;

/**
 * Represents an item that can be a pattern
 */
public interface IPatternItem extends IMultiStateItem, ISupportsSealing, IWithModeItem<IPatternPlacementType>, IPlacementPreviewProvidingItem, IChangeTrackingItem {
	@Override
	default VoxelShape getWireFrame(
			final ItemStack stack, final Player player, final BlockHitResult rayTraceResult) {
		return getMode(stack).buildVoxelShapeForWireframe(
				createItemStack(stack).createSnapshot(),
				player,
				rayTraceResult.getLocation(),
				rayTraceResult.getDirection()
		);
	}

	@Override
	default Vector4f getWireFrameColor(ItemStack heldStack, Player player, BlockHitResult blockHitResult) {
		return getPlacementResult(heldStack, player, blockHitResult).getColor();
	}

	@Override
	default Vec3 getTargetedPosition(ItemStack heldStack, Player player, BlockHitResult blockHitResult) {
		return getMode(heldStack).getTargetedPosition(heldStack, player, blockHitResult);
	}

	@Override
	default PlacementResult getPlacementResult(ItemStack heldStack, Player player, BlockHitResult blockHitResult) {
		return this.getMode(heldStack).performPlacement(
				createItemStack(heldStack).createSnapshot(),
			new BlockPlaceContext(
					player,
					player.getMainHandItem() == heldStack
					? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND,
					heldStack,
					blockHitResult
			),
				true
		);
	}

	@Override
	default boolean overridesOccupiedBits(ItemStack heldStack) {
		return getMode(heldStack).overridesOccupiedBits(heldStack);
	}
}

package mod.chiselsandbits.api.pattern.placement;

import com.communi.suggestu.scena.core.registries.ICustomRegistryEntry;
import mod.chiselsandbits.api.item.withmode.IToolMode;
import mod.chiselsandbits.api.item.withmode.group.IToolModeGroup;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshot;
import mod.chiselsandbits.api.placement.IPlacementProperties;
import mod.chiselsandbits.api.placement.PlacementResult;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Presents a way placing a pattern in the world.
 */
public interface IPatternPlacementType extends ICustomRegistryEntry, IToolMode<IToolModeGroup>, IPlacementProperties {
	/**
	 * Used to build a voxelshape for rendering in the preview.
	 *
	 * @param sourceSnapshot The snapshot stored in the pattern for placement.
	 * @param player The player performing the action.
	 * @param targetedPoint The point targeted by the player.
	 * @param hitFace The face hit by the player.
	 * @return The VoxelShape used to render the wireframe.
	 */
	VoxelShape buildVoxelShapeForWireframe(
			IMultiStateSnapshot sourceSnapshot,
			Player player,
			Vec3 targetedPoint,
			Direction hitFace);

	/**
	 * Invoked to perform the actual placement of the pattern in the world.
	 *
	 * @param source The snapshot stored in the pattern for placement.
	 * @param context The use context of the pattern item on a block.
	 * @param simulate Indicates if the placement operation is a simulation or not.
	 * @return The action result which influences the further processing of the click interaction.
	 */
	PlacementResult performPlacement(
			IMultiStateSnapshot source,
			BlockPlaceContext context,
			boolean simulate);

	/**
	 * Invoked to determine where the targeted position of the placement type is.
	 *
	 * @param heldStack The stack that the player is holding.
	 * @param player The player which is holding the itemstack.
	 * @param blockRayTraceResult The block ray trace result in the current context.
	 * @return The position of the potential placement.
	 */
	Vec3 getTargetedPosition(ItemStack heldStack, Player player, BlockHitResult blockRayTraceResult);
}

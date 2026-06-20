package mod.chiselsandbits.api.placement;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

public interface IPlaceable {
	/**
     * Invoked to determine the result of attempting actual placement in the world.
     *
     * @param heldStack The stack to get the color for.
     * @param playerEntity The entity that is rendering with the color.
     * @param blockRayTraceResult The block ray trace result for the current context.
     * @return The simulated action result of placement
     */
	PlacementResult getPlacementResult(
			ItemStack heldStack,
			Player playerEntity,
			BlockHitResult blockRayTraceResult
	);
}

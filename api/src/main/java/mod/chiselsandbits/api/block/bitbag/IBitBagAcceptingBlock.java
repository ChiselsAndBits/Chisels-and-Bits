package mod.chiselsandbits.api.block.bitbag;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Represents a block which can accept a bit bags interaction.
 */
public interface IBitBagAcceptingBlock {
	/**
	 * Invoked when a bit bag interaction is performed against a given block.
	 * @param bitBagStack The bit bag stack.
	 * @param player The player performing the action.
	 * @param blockRayTraceResult The ray trace result.
	 */
	ItemStack onBitBagInteraction(
			final ItemStack bitBagStack,
			final Player player,
			final BlockHitResult blockRayTraceResult
	);
}

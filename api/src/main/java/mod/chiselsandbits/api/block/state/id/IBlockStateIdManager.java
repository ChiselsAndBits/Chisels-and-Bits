package mod.chiselsandbits.api.block.state.id;

import mod.chiselsandbits.api.IChiselsAndBitsAPI;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The blockstate id manager which manages the blockstate ids for the current session.
 *
 * Generally this manager is comparable to the relevant methods in the game registry.
 */
public interface IBlockStateIdManager {
	static IBlockStateIdManager getInstance() {
		return IChiselsAndBitsAPI.getInstance().getBlockStateIdManager();
	}

	/**
     * Calculates the the integer id representation of the blockstate.
     *
     * @param blockState The blockstate for which the id is requested.
     *
     * @return The id of the given blockstate.
     */
	default int getIdFrom(final BlockState blockState) {
		return Block.getId(blockState);
	}

	/**
     * Calculates the blockstate from the given id.
     *
     * @param id The integer id representation of the requested blockstate.
     *
     * @return The blockstate which is represented by the given id.
     */
	default BlockState getBlockStateFrom(final int id) {
		return Block.stateById(id);
	}
}

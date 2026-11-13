package mod.chiselsandbits.debug.scenes;

import java.util.Optional;

import mod.chiselsandbits.api.block.storage.StateEntryStorage;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.variant.state.IStateVariantManager;
import mod.chiselsandbits.block.entities.ChiseledBlockEntity;
import mod.chiselsandbits.registrars.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Helpers shared by the debug scene generators to build bit storages and place chiseled blocks.
 */
final class SceneBlocks {
	private SceneBlocks() {
		throw new IllegalStateException("Can not instantiate an instance of: SceneBlocks. This is a utility class");
	}

	static BlockInformation info(final BlockState state) {
		return new BlockInformation(state, IStateVariantManager.getInstance().getStateVariant(state, Optional.empty()));
	}

	/**
	 * Builds a storage of the current bit size, where every bit is set by the given function.
	 *
	 * @param function Returns the bit content for a position, or null for air.
	 */
	static StateEntryStorage storage(final BitFunction function) {
		final StateEntryStorage storage = new StateEntryStorage();
		final int size = storage.getSize();

		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				for (int z = 0; z < size; z++) {
					final BlockInformation information = function.at(x, y, z);

					if (information != null) {
						storage.setBlockInformation(x, y, z, information);
					}
				}
			}
		}

		return storage;
	}

	static void fill(final ServerLevel level, final BlockPos from, final BlockPos to, final BlockState state) {
		for (final BlockPos pos : BlockPos.betweenClosed(from, to)) {
			level.setBlock(pos, state, Block.UPDATE_CLIENTS);
		}
	}

	/**
	 * Clears a box of blocks starting at the origin, including a one block margin around it.
	 */
	static void clear(final ServerLevel level, final BlockPos origin, final int sizeX, final int sizeY, final int sizeZ) {
		fill(level, origin.offset(-1, -1, -1), origin.offset(sizeX, sizeY, sizeZ), Blocks.AIR.defaultBlockState());
	}

	/**
	 * Places a stone floor one block below the origin, covering the given footprint.
	 */
	static void floor(final ServerLevel level, final BlockPos origin, final int sizeX, final int sizeZ) {
		fill(level, origin.offset(0, -1, 0), origin.offset(sizeX - 1, -1, sizeZ - 1), Blocks.STONE.defaultBlockState());
	}

	static boolean placeChiseled(final ServerLevel level, final BlockPos pos, final StateEntryStorage storage) {
		level.setBlock(pos, ModBlocks.CHISELED_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);

		if (level.getBlockEntity(pos) instanceof ChiseledBlockEntity chiseledBlockEntity) {
			chiseledBlockEntity.replaceStorage(storage);
			return true;
		}

		return false;
	}

	@FunctionalInterface
	interface BitFunction {
		BlockInformation at(int x, int y, int z);
	}
}

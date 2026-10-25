package mod.chiselsandbits.api.chiseling.conversion;

import java.util.Optional;

import mod.chiselsandbits.api.IChiselsAndBitsAPI;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Manages converting none chiseled objects into chiseled variants and back.
 */
public interface IConversionManager {
	/**
	 * The instance of the manager.
	 *
	 * @return The manager.
	 */
	static IConversionManager getInstance() {
		return IChiselsAndBitsAPI.getInstance().getConversionManager();
	}

	/**
	 * Allows you to get the chiseled block variant of a given blockstate.
	 * In general this is material dependent.
	 *
	 * @param blockState The blockstate to convert.
	 * @return An optional, containing the converted block, if the given blockstate is convertible.
	 */
	default Optional<Block> getChiseledVariantOf(final BlockState blockState) {
		return getChiseledVariantOf(blockState.getBlock());
	}

	/**
	 * Allows you to get the chiseled block variant of a given block.
	 * In general this is material dependent.
	 *
	 * @param block The block to convert.
	 * @return An optional, containing the converted block, if the given block is convertible.
	 */
	Optional<Block> getChiseledVariantOf(final Block block);

	/**
	 * Allows you to get the chiseled block variant of a given item.
	 * In general this is material dependent.
	 * If an item is passed in which is not a {@link BlockItem} then an empty optional is returned.
	 *
	 * @param provider The item provider to convert.
	 * @return An optional, containing the converted block, if the given item in the provider represents a convertible block.
	 */
	default Optional<Block> getChiseledVariantOf(final ItemLike provider) {
		final Item targetItem = provider.asItem();

		if (targetItem instanceof BlockItem) {
			return getChiseledVariantOf(((BlockItem) targetItem).getBlock());
		}

		return Optional.empty();
	}
}

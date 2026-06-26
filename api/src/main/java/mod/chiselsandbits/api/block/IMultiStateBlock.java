package mod.chiselsandbits.api.block;

import com.communi.suggestu.saecularia.caudices.core.block.IBlockWithWorldlyProperties;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.EntityBlock;
import org.jetbrains.annotations.NotNull;

/**
 * Implemented by Chisels and Bits Blocks, can be used to request a material that represents
 * the largest quantity of a Chisels and Bits block.
 */
public interface IMultiStateBlock extends EntityBlock, IBlockWithWorldlyProperties {
	/**
	 * Returns the primary state of the block.
	 * In terms of Chisels and Bits this is the blockstate that is represented the most inside the block.
	 *
	 * @param world The world to read the data from.Required to get access to the tile entity with the backing data.
	 * @param pos The pos in the given world to get the primary blockstate from.Required to get access to the tile entity with the backing data.
	 * @return The primary blockstate, or when not found the default state from the air block.{@link AirBlock#defaultBlockState()}
	 */
		BlockInformation getPrimaryState(
			BlockGetter world,
			BlockPos pos);
}

package mod.chiselsandbits.api.neighborhood;

// ILLEGAL IMPORT: -> import org.jetbrains.annotations.Nullable; -> We use JSpecify nullable annotations in this project.

import java.util.function.Function;

import mod.chiselsandbits.api.IChiselsAndBitsAPI;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.multistate.accessor.IAreaAccessor;
import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;

/**
 * Represents a system which can build unique block neighborhoods.
 */
public interface IBlockNeighborhoodBuilder {
	/**
	 * Gives access to the current builder instance.
	 * Short circuit method for the central API call.
	 *
	 * @return The current instance.
	 */
	static IBlockNeighborhoodBuilder getInstance() {
		return IChiselsAndBitsAPI.getInstance().getBlockNeighborhoodBuilder();
	}

	/**
	 * Builds a block neighborhood for the requested target.
	 *
	 * @param neighborhoodBlockStateProvider The blockstate provider to pull the neighborhood data from.
	 * @param neighborhoodAreaAccessorProvider The area accessor provider to pull the neighborhood data from.
	 * @return A comparable an unique element targeting the requested position and containing the neighborhood data of the target position.
	 */
	IBlockNeighborhood build(
			@Nullable Function<Direction, BlockInformation> neighborhoodBlockStateProvider,
			@Nullable Function<Direction, IAreaAccessor> neighborhoodAreaAccessorProvider
	);
}

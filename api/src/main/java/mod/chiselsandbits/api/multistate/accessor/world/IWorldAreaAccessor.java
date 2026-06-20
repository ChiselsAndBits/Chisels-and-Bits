package mod.chiselsandbits.api.multistate.accessor.world;

import mod.chiselsandbits.api.multistate.accessor.IAreaAccessor;
import mod.chiselsandbits.api.util.BlockPosStreamProvider;
import mod.chiselsandbits.api.util.IWorldObject;
import net.minecraft.core.BlockPos;

import java.util.stream.Stream;

/**
 * Represents an area accessor which actually exists in the world.
 */
public interface IWorldAreaAccessor extends IAreaAccessor, IWorldObject {
	/**
	 * Gives access to a stream of in world state entries.
	 * Filters out all "virtual"-none-in-world state entries that might or might not be included
	 * in {@link #stream()}.
	 *
	 * @return A stream with only state entries which actually exist in world.
	 */
	default Stream<IInWorldStateEntryInfo> inWorldStream() {
		return stream()
			.filter(IInWorldStateEntryInfo.class::isInstance)
			.map(IInWorldStateEntryInfo.class::cast);
	}

	/**
	 * Gives access to a stream which represents all the blocks touched by the accessor.
	 * @return The blocks touched by the accessor.
	 */
	default Stream<BlockPos> coveredAreaStream() {
		final BlockPos startPos = getInWorldStartBlockPoint();
		final BlockPos endPos = getInWorldEndBlockPoint();

		//Short circuit the stream builder if the area covered is exactly one block.
		if (startPos.equals(endPos)) {
			return Stream.of(startPos);
		}

		return BlockPosStreamProvider.getForRange(
				startPos.getX(), startPos.getY(), startPos.getZ(),
				endPos.getX(), endPos.getY(), endPos.getZ()
		);
	}
}

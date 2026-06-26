package mod.chiselsandbits.neighborhood;

import java.util.EnumMap;
import java.util.function.Function;

import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.multistate.accessor.IAreaAccessor;
import mod.chiselsandbits.api.neighborhood.IBlockNeighborhood;
import mod.chiselsandbits.api.neighborhood.IBlockNeighborhoodBuilder;
import mod.chiselsandbits.api.profiling.IProfilerSection;
import mod.chiselsandbits.profiling.ProfilingManager;
import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;

public final class BlockNeighborhoodBuilder implements IBlockNeighborhoodBuilder {
	private static final BlockNeighborhoodBuilder INSTANCE = new BlockNeighborhoodBuilder();

	private BlockNeighborhoodBuilder() {
	}

	public static BlockNeighborhoodBuilder getInstance() {
		return INSTANCE;
	}

	@Override
	public IBlockNeighborhood build(
			@Nullable Function<Direction, BlockInformation> neighborhoodBlockStateProvider,
			@Nullable Function<Direction, IAreaAccessor> neighborhoodAreaAccessorProvider
	) {
		final EnumMap<Direction, BlockNeighborhoodEntry> neighborhoodMap = new EnumMap<>(Direction.class);

		try (IProfilerSection ignored2 = ProfilingManager.getInstance().withSection("Key building")) {
			for (final Direction value : Direction.values()) {
				final BlockInformation state = neighborhoodBlockStateProvider != null ? neighborhoodBlockStateProvider.apply(value) : BlockInformation.AIR;
				final IAreaAccessor accessor = neighborhoodAreaAccessorProvider != null ? neighborhoodAreaAccessorProvider.apply(value) : null;

				if (accessor == null) {
					neighborhoodMap.put(value, new BlockNeighborhoodEntry(state));
				} else {
					neighborhoodMap.put(value, new BlockNeighborhoodEntry(
							state,
							accessor
					)
					);
				}
			}

			return new BlockNeighborhood(neighborhoodMap);
		}
	}
}

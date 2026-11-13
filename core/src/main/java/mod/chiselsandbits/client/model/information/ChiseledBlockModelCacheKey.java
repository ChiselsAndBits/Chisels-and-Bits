package mod.chiselsandbits.client.model.information;

import java.util.Collection;
import java.util.List;

import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.multistate.accessor.identifier.IAreaShapeIdentifier;
import mod.chiselsandbits.api.neighborhood.IBlockNeighborhood;
import org.jspecify.annotations.Nullable;

/**
 * @param sourceStates the contained states, index-aligned with {@code modelCacheKeys}; empty when the producer did not
 *                     record them, in which case no direct terrain source-model proof is possible
 */
public record ChiseledBlockModelCacheKey(
		IAreaShapeIdentifier identifier, BlockInformation primaryState,
		IBlockNeighborhood neighborhood, Collection<@Nullable Object> modelCacheKeys,
		List<BlockInformation> sourceStates) {
	public static final ChiseledBlockModelCacheKey EMPTY = new ChiseledBlockModelCacheKey(
			IAreaShapeIdentifier.DUMMY,
			BlockInformation.AIR,
			IBlockNeighborhood.EMPTY,
			List.of()
	);

	public ChiseledBlockModelCacheKey {
		sourceStates = List.copyOf(sourceStates);
	}

	public ChiseledBlockModelCacheKey(
			final IAreaShapeIdentifier identifier, final BlockInformation primaryState,
			final IBlockNeighborhood neighborhood, final Collection<@Nullable Object> modelCacheKeys
	) {
		this(identifier, primaryState, neighborhood, modelCacheKeys, List.of());
	}
}

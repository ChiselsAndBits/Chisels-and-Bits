package mod.chiselsandbits.client.model.information;

import java.util.Collection;
import java.util.List;

import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.multistate.accessor.identifier.IAreaShapeIdentifier;
import mod.chiselsandbits.api.neighborhood.IBlockNeighborhood;
import org.jspecify.annotations.Nullable;

public record ChiseledBlockModelCacheKey(
		IAreaShapeIdentifier identifier, BlockInformation primaryState,
		IBlockNeighborhood neighborhood, Collection<@Nullable Object> modelCacheKeys) {
	public static final ChiseledBlockModelCacheKey EMPTY = new ChiseledBlockModelCacheKey(
			IAreaShapeIdentifier.DUMMY,
			BlockInformation.AIR,
			IBlockNeighborhood.EMPTY,
			List.of()
	);
}

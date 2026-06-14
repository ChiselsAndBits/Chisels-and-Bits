package mod.chiselsandbits.aabb;

import java.util.List;
import java.util.Objects;

import mod.chiselsandbits.api.axissize.CollisionType;
import mod.chiselsandbits.api.config.IChiselsAndBitsConfiguration;
import mod.chiselsandbits.api.multistate.accessor.IAreaAccessor;
import mod.chiselsandbits.api.multistate.accessor.identifier.IAreaShapeIdentifier;
import mod.chiselsandbits.utils.SimpleMaxSizedCache;

import net.minecraft.world.phys.AABB;

public class AABBManager
{
	private static final AABBManager INSTANCE = new AABBManager();

	public static AABBManager getInstance()
	{
		return INSTANCE;
	}

	private final SimpleMaxSizedCache<Key, List<AABB>> cache = new SimpleMaxSizedCache<>(
	  IChiselsAndBitsConfiguration.getInstance().getCommon().getCollisionBoxCacheSize()::get
	);

	private AABBManager()
	{
	}

	public List<AABB> get(
	  final IAreaAccessor accessor,
	  final CollisionType sizeType
	) {
		final Key cacheKey = new Key(
		  accessor.createNewShapeIdentifier(),
		  sizeType
		);

		return cache.get(
		  cacheKey,
		  () -> AABBCompressor.compressStates(accessor, sizeType)
		);
	}

	public void clearCache()
	{
		this.cache.clear();
	}

	private static final class Key {
		private final IAreaShapeIdentifier identifier;
		private final CollisionType        predicate;

		private Key(final IAreaShapeIdentifier identifier, final CollisionType predicate) {
			this.identifier = identifier;
			this.predicate = predicate;
		}

		@Override
		public boolean equals(final Object o)
		{
			if (this == o)
			{
				return true;
			}
			if (!(o instanceof final Key key))
			{
				return false;
			}
			return Objects.equals(identifier, key.identifier) && Objects.equals(predicate, key.predicate);
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(identifier, predicate);
		}
	}
}

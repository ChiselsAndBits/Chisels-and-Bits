package mod.chiselsandbits.voxelshape;

import java.util.Objects;
import java.util.Optional;

import mod.chiselsandbits.api.axissize.CollisionType;
import mod.chiselsandbits.api.config.ICommonConfiguration;
import mod.chiselsandbits.api.multistate.accessor.IAreaAccessor;
import mod.chiselsandbits.api.multistate.accessor.identifier.IAreaShapeIdentifier;
import mod.chiselsandbits.api.voxelshape.IVoxelShapeManager;
import mod.chiselsandbits.utils.SimpleMaxSizedCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class VoxelShapeManager implements IVoxelShapeManager
{
	private static final Logger LOGGER = LogManager.getLogger();
	private static final VoxelShapeManager INSTANCE = new VoxelShapeManager();

	private final SimpleMaxSizedCache<Key, VoxelShape> cache = new SimpleMaxSizedCache<>(
	  ICommonConfiguration.getInstance().getCollisionBoxCacheSize().get()
	);

	private VoxelShapeManager()

	{
	}

	public static VoxelShapeManager getInstance()

	{
		return INSTANCE;
	}

	@Override
	public VoxelShape get(
	  final IAreaAccessor accessor,
	  final BlockPos offset,
	  final CollisionType sizeType,
	  final boolean simplify)

	{
		final Key cacheKey = new Key(
		  accessor.createNewShapeIdentifier(),
		  offset,
		  sizeType,
		  simplify);

		return cache.get(cacheKey,
		  () -> {
			final VoxelShape calculatedShape = VoxelShapeCalculator.calculate(accessor, offset, sizeType, simplify);
			if (calculatedShape.isEmpty())
				return Shapes.empty();

			return calculatedShape;
		});
	}

	@Override
	public Optional<VoxelShape> getCached(
	  final IAreaShapeIdentifier identifier,
	  final BlockPos offset,
	  final CollisionType sizeType,
	  final boolean simplify)

	{
		final Key key = new Key(
		  identifier,
		  offset, sizeType,
		  simplify);

		return cache.getIfPresent(key);
	}

	public void clearCache()

	{
		this.cache.clear();
	}

	private static final class Key {
		private final IAreaShapeIdentifier identifier;
		private final BlockPos      offset;
		private final CollisionType sizeType;
		private final boolean       simplify;

		private Key(final IAreaShapeIdentifier identifier, final BlockPos offset, final CollisionType sizeType, final boolean simplify) {
			this.identifier = identifier;
			this.offset = offset;
			this.sizeType = sizeType;
			this.simplify = simplify;
		}

		@Override
		public String toString()

		{
			return "Key{" +
					 "identifier=" + identifier.toString() +
					 ", offset=" + offset +
					 ", predicate=" + sizeType +
					 ", simplify=" + simplify +
					 '}';
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

			if (simplify != key.simplify)

			{
				return false;
			}

			if (!Objects.equals(identifier, key.identifier))

			{
				return false;
			}

			if (!Objects.equals(offset, key.offset))

			{
				return false;
			}

			return Objects.equals(sizeType, key.sizeType);
		}

		@Override
		public int hashCode()

		{
			int result = identifier != null ? identifier.hashCode() : 0;
			result = 31 * result + (offset != null ? offset.hashCode() : 0);
			result = 31 * result + (sizeType != null ? sizeType.hashCode() : 0);
			result = 31 * result + (simplify ? 1 : 0);
			return result;
		}
	}
}

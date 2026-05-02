package mod.chiselsandbits.api.neighborhood;

import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.multistate.accessor.IAreaAccessor;
import mod.chiselsandbits.api.variant.state.IStateVariant;
import mod.chiselsandbits.api.variant.state.IStateVariantManager;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Marker interface used to detect block neighborhoods in cache keys.
 */
public interface IBlockNeighborhood
{

    /**
     * Empty neighborhood.
     */
    IBlockNeighborhood EMPTY = new IBlockNeighborhood()
    {
        @Override
        public @NotNull BlockInformation getBlockInformation(final Direction direction)
        {
            return BlockInformation.AIR;
        }

        @Override
        public IAreaAccessor getAreaAccessor(final Direction direction)
        {
            return null;
        }
    };

    /**
     * Creates a block neighborhood around the given block entity.
     *
     * @param blockEntity the entity to get the neighborhood for.
     * @return The neighborhood.
     */
    static IBlockNeighborhood around(BlockEntity blockEntity)
    {
        return IBlockNeighborhoodBuilder.getInstance().build(
            direction -> {
                final Level level = blockEntity.getLevel();
                if (level == null)
                {
                    return BlockInformation.AIR;
                }

                final BlockState state = level.getBlockState(blockEntity.getBlockPos().offset(direction.getUnitVec3i()));
                final Optional<IStateVariant> additionalStateInfo = IStateVariantManager.getInstance().getStateVariant(
                    state,
                    Optional.ofNullable(level.getBlockEntity(blockEntity.getBlockPos().offset(direction.getUnitVec3i())))
                );

                return new BlockInformation(state, additionalStateInfo);
            },
            direction -> {
                final Level level = blockEntity.getLevel();
                if (level == null)
                {
                    return null;
                }

                final BlockEntity otherTileEntity = level.getBlockEntity(blockEntity.getBlockPos().offset(direction.getUnitVec3i()));
                if (otherTileEntity instanceof IAreaAccessor otherAccessor)
                {
                    return otherAccessor;
                }

                return null;
            }
        );
    }

    /**
     * Returns the blocks neighbor in the given direction.
     *
     * @param direction The direction.
     * @return The blockstate
     */
    @NotNull
    BlockInformation getBlockInformation(final Direction direction);

    /**
     * Returns the blocks potential area accessor neighbor in the given direction.
     *
     * @param direction The direction.
     * @return The area accessor.
     */
    @Nullable
    IAreaAccessor getAreaAccessor(final Direction direction);
}

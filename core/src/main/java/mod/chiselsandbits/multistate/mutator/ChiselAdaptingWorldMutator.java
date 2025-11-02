package mod.chiselsandbits.multistate.mutator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import mod.chiselsandbits.api.axissize.CollisionType;
import mod.chiselsandbits.api.block.entity.IMultiStateBlockEntity;
import mod.chiselsandbits.api.block.state.id.IBlockStateIdManager;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.change.IChangeTracker;
import mod.chiselsandbits.api.chiseling.conversion.IConversionManager;
import mod.chiselsandbits.api.chiseling.eligibility.IEligibilityManager;
import mod.chiselsandbits.api.exceptions.SpaceOccupiedException;
import mod.chiselsandbits.api.item.multistate.IStatistics;
import mod.chiselsandbits.api.multistate.StateEntrySize;
import mod.chiselsandbits.api.multistate.accessor.IAreaAccessorWithVoxelShape;
import mod.chiselsandbits.api.multistate.accessor.ISingleBlockAxisAlignedAreaAccessor;
import mod.chiselsandbits.api.multistate.accessor.IStateEntryInfo;
import mod.chiselsandbits.api.multistate.accessor.identifier.IAreaShapeIdentifier;
import mod.chiselsandbits.api.multistate.accessor.identifier.ISingleStateAreaShareIdentifier;
import mod.chiselsandbits.api.multistate.accessor.sortable.IPositionMutator;
import mod.chiselsandbits.api.multistate.mutator.IMutableStateEntryInfo;
import mod.chiselsandbits.api.multistate.mutator.callback.StateClearer;
import mod.chiselsandbits.api.multistate.mutator.callback.StateSetter;
import mod.chiselsandbits.api.multistate.mutator.world.IInWorldMutableStateEntryInfo;
import mod.chiselsandbits.api.multistate.mutator.world.IWorldAreaMutator;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshot;
import mod.chiselsandbits.api.util.BlockPosForEach;
import mod.chiselsandbits.api.util.BlockPosStreamProvider;
import mod.chiselsandbits.api.util.IBatchMutation;
import mod.chiselsandbits.api.util.VectorUtils;
import mod.chiselsandbits.api.variant.state.IStateVariant;
import mod.chiselsandbits.api.variant.state.IStateVariantManager;
import mod.chiselsandbits.multistate.snapshot.EmptySnapshot;
import mod.chiselsandbits.multistate.snapshot.SimpleSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ChiselAdaptingWorldMutator implements IWorldAreaMutator, IAreaAccessorWithVoxelShape, ISingleBlockAxisAlignedAreaAccessor
{
    public static final BlockInformation DEFAULT_STATE = new BlockInformation(Blocks.STONE.defaultBlockState(), Optional.empty());
    private final       LevelAccessor    world;
    private final       BlockPos         pos;

    public ChiselAdaptingWorldMutator(final LevelAccessor world, final BlockPos pos)
    {
        this.world = world;
        this.pos = pos;
    }

    /**
     * Creates a new area shape identifier.
     * <p>
     * Note: This method always returns a new instance.
     *
     * @return The new identifier.
     */
    @Override
    public IAreaShapeIdentifier createNewShapeIdentifier()
    {
        if (getWorld().isOutsideBuildHeight(getPos()))
        {
            return new PreAdaptedShapeIdentifier(getWorld().getBlockState(getPos()));
        }

        final BlockEntity tileEntity = getWorld().getBlockEntity(getPos());
        if (tileEntity instanceof IMultiStateBlockEntity)
        {
            return ((IMultiStateBlockEntity) tileEntity).createNewShapeIdentifier();
        }

        return new PreAdaptedShapeIdentifier(getWorld().getBlockState(getPos()));
    }

    @SuppressWarnings("deprecation")
    @Override
    public Stream<IStateEntryInfo> stream()
    {
        if (getWorld().isOutsideBuildHeight(getPos()))
        {
            return Stream.empty();
        }

        final BlockEntity blockEntity = getWorld().getBlockEntity(getPos());
        if (blockEntity instanceof IMultiStateBlockEntity multiStateBlockEntity)
        {
            return multiStateBlockEntity.stream();
        }

        final BlockState currentState = getWorld().getBlockState(getPos());
        final Optional<IStateVariant> additionalStateInfo = IStateVariantManager
            .getInstance()
            .getStateVariant(currentState, Optional.ofNullable(blockEntity));
        final BlockInformation blockInformation = new BlockInformation(
            currentState,
            additionalStateInfo
        );

        if (IEligibilityManager.getInstance().canBeChiseled(blockInformation) ||
            currentState.isAir())
        {
            return BlockPosStreamProvider.getForRange(StateEntrySize.current().getBitsPerBlockSide())
                .map(blockPos -> new MutablePreAdaptedStateEntry(
                    blockInformation,
                    getWorld(),
                    getPos(),
                    blockPos,
                    this::setInAreaTarget,
                    this::clearInAreaTarget)
                );
        }

        return Stream.empty();
    }

    /**
     * Gets the target state in the current area, using the offset from the area as well as the in area target offset.
     *
     * @param inAreaTarget The in area offset.
     * @return An optional potentially containing the state entry of the requested target.
     */
    @Override
    public Optional<IStateEntryInfo> getInAreaTarget(final Vec3 inAreaTarget)
    {
        if (getWorld().isOutsideBuildHeight(getPos()))
        {
            return Optional.empty();
        }

        if (inAreaTarget.x() < 0 ||
            inAreaTarget.y() < 0 ||
            inAreaTarget.z() < 0)
        {
            return Optional.empty();
        }

        if (inAreaTarget.x() >= 1 ||
            inAreaTarget.y() >= 1 ||
            inAreaTarget.z() >= 1)
        {
            return Optional.empty();
        }

        final BlockEntity blockEntity = getWorld().getBlockEntity(getPos());
        if (blockEntity instanceof IMultiStateBlockEntity multiStateBlockEntity)
        {
            return multiStateBlockEntity.getInAreaTarget(inAreaTarget);
        }

        final BlockState currentState = getWorld().getBlockState(getPos());
        final Optional<IStateVariant> additionalStateInfo = IStateVariantManager
            .getInstance()
            .getStateVariant(currentState, Optional.ofNullable(blockEntity));
        final BlockInformation blockInformation = new BlockInformation(
            currentState,
            additionalStateInfo
        );

        return Optional.of(new MutablePreAdaptedStateEntry(
            blockInformation,
            getWorld(),
            getPos(),
            VectorUtils.toBlockPos(inAreaTarget.multiply(StateEntrySize.current().getBitsPerBlockSide(),
                StateEntrySize.current().getBitsPerBlockSide(),
                StateEntrySize.current().getBitsPerBlockSide())),
            this::setInAreaTarget,
            this::clearInAreaTarget));
    }

    /**
     * Gets the target state in the current area, using the in area block position offset as well as the in block target offset to calculate the in area offset for setting.
     *
     * @param inAreaBlockPosOffset The offset of blocks in the current area.
     * @param inBlockTarget        The offset in the targeted block.
     * @return An optional potentially containing the state entry of the requested target.
     */
    @Override
    public Optional<IStateEntryInfo> getInBlockTarget(final BlockPos inAreaBlockPosOffset, final Vec3 inBlockTarget)
    {
        if (!inAreaBlockPosOffset.equals(BlockPos.ZERO))
        {
            throw new IllegalArgumentException("The chisel adapting world mutator can only mutate the given single block!");
        }

        return getInAreaTarget(inBlockTarget);
    }

    @Override
    public boolean isInside(final Vec3 inAreaTarget)
    {
        if (getWorld().isOutsideBuildHeight(getPos()))
        {
            return false;
        }

        return !(inAreaTarget.x() < 0) &&
            !(inAreaTarget.y() < 0) &&
            !(inAreaTarget.z() < 0) &&
            !(inAreaTarget.x() >= 1) &&
            !(inAreaTarget.y() >= 1) &&
            !(inAreaTarget.z() >= 1);
    }

    @Override
    public boolean isInside(final BlockPos inAreaBlockPosOffset, final Vec3 inBlockTarget)
    {
        if (!inAreaBlockPosOffset.equals(BlockPos.ZERO))
        {
            return false;
        }

        return isInside(inBlockTarget);
    }

    @Override
    public IMultiStateSnapshot createSnapshot()
    {
        if (getWorld().isOutsideBuildHeight(getPos()))
        {
            return EmptySnapshot.INSTANCE;
        }

        final BlockEntity blockEntity = getWorld().getBlockEntity(getPos());
        if (blockEntity instanceof IMultiStateBlockEntity multiStateBlockEntity)
        {
            return multiStateBlockEntity.createSnapshot();
        }

        final BlockState currentState = getWorld().getBlockState(getPos());
        final Optional<IStateVariant> additionalStateInfo = IStateVariantManager
            .getInstance()
            .getStateVariant(currentState, Optional.ofNullable(blockEntity));
        final BlockInformation blockInformation = new BlockInformation(
            currentState,
            additionalStateInfo
        );

        return new SimpleSnapshot(blockInformation);
    }

    @Override
    public Stream<IStateEntryInfo> streamWithPositionMutator(final IPositionMutator positionMutator)
    {
        if (getWorld().isOutsideBuildHeight(getPos()))
        {
            return Stream.empty();
        }

        final BlockEntity blockEntity = getWorld().getBlockEntity(getPos());
        if (blockEntity instanceof IMultiStateBlockEntity multiStateBlockEntity)
        {
            return multiStateBlockEntity.streamWithPositionMutator(positionMutator);
        }

        final BlockState currentState = getWorld().getBlockState(getPos());
        final Optional<IStateVariant> additionalStateInfo = IStateVariantManager
            .getInstance()
            .getStateVariant(currentState, Optional.ofNullable(blockEntity));
        final BlockInformation blockInformation = new BlockInformation(
            currentState,
            additionalStateInfo
        );

        if (IEligibilityManager.getInstance().canBeChiseled(blockInformation) ||
            currentState.isAir())
        {
            return BlockPosStreamProvider.getForRange(StateEntrySize.current().getBitsPerBlockSide())
                .map(positionMutator::mutate)
                .map(blockPos -> new MutablePreAdaptedStateEntry(
                    blockInformation,
                    getWorld(),
                    getPos(),
                    blockPos,
                    this::setInAreaTarget,
                    this::clearInAreaTarget)
                );
        }

        return Stream.empty();
    }

    @Override
    public void forEachWithPositionMutator(
        final IPositionMutator positionMutator, final Consumer<IStateEntryInfo> consumer)
    {
        if (getWorld().isOutsideBuildHeight(getPos()))
        {
            return;
        }

        final BlockEntity blockEntity = getWorld().getBlockEntity(getPos());
        if (blockEntity instanceof IMultiStateBlockEntity multiStateBlockEntity)
        {
            multiStateBlockEntity.forEachWithPositionMutator(positionMutator, consumer);
            return;
        }

        final BlockState currentState = getWorld().getBlockState(getPos());
        final Optional<IStateVariant> additionalStateInfo = IStateVariantManager
            .getInstance()
            .getStateVariant(currentState, Optional.ofNullable(blockEntity));
        final BlockInformation blockInformation = new BlockInformation(
            currentState,
            additionalStateInfo
        );

        if (IEligibilityManager.getInstance().canBeChiseled(blockInformation) ||
            currentState.isAir())
        {
            BlockPosForEach.forEachInRange(StateEntrySize.current().getBitsPerBlockSide(), (BlockPos blockPos) -> {
                final Vec3i target = positionMutator.mutate(blockPos);
                consumer.accept(new MutablePreAdaptedStateEntry(
                    blockInformation,
                    getWorld(),
                    getPos(),
                    target,
                    this::setInAreaTarget,
                    this::clearInAreaTarget));
            });
        }
    }

    @Override
    public IStatistics getStatistics()
    {
        final BlockEntity blockEntity = getWorld().getBlockEntity(getPos());
        if (blockEntity instanceof IMultiStateBlockEntity multiStateBlockEntity)
        {
            return multiStateBlockEntity.getStatistics();
        }

        return new IStatistics()
        {
            @Override
            public BlockInformation getPrimaryState()
            {
                final BlockState blockState = getWorld().getBlockState(getPos());
                return new BlockInformation(blockState, IStateVariantManager.getInstance()
                    .getStateVariant(
                        blockState,
                        Optional.ofNullable(blockEntity)
                    )
                );
            }

            @Override
            public boolean isEmpty()
            {
                return getWorld().isEmptyBlock(getPos());
            }

            @Override
            public Set<BlockInformation> getContainedStates()
            {
                return Set.of(getPrimaryState());
            }

            public Codec<?> codec()
            {
                throw new NotImplementedException("A chisel adapting world mutator statistics are ephemeral.");
            }

            @Override
            public MapCodec<?> mapCodec()
            {
                throw new NotImplementedException("A chisel adapting world mutator statistics are ephemeral.");
            }

            @Override
            public StreamCodec<?, ?> streamCodec()
            {
                throw new NotImplementedException("A chisel adapting world mutator statistics are ephemeral.");
            }
        };
    }

    @Override
    public LevelAccessor getWorld()
    {
        return world;
    }

    public BlockPos getPos()
    {
        return pos;
    }

    @Override
    public Vec3 getInWorldStartPoint()
    {
        return Vec3.atLowerCornerOf(pos);
    }

    @Override
    public Vec3 getInWorldEndPoint()
    {
        return Vec3.atLowerCornerOf(pos).add(
            15 * StateEntrySize.current().getSizePerBit(),
            15 * StateEntrySize.current().getSizePerBit(),
            15 * StateEntrySize.current().getSizePerBit()
        );
    }

    /**
     * Returns all entries in the current area in a mutable fashion. Includes all empty areas as areas containing an air state.
     *
     * @return A stream with a mutable state entry info for each mutable section in the area.
     */
    @Override
    public Stream<IMutableStateEntryInfo> mutableStream()
    {
        if (getWorld().isOutsideBuildHeight(getPos()))
        {
            return Stream.empty();
        }

        final BlockEntity blockEntity = getWorld().getBlockEntity(getPos());
        if (blockEntity instanceof IMultiStateBlockEntity multiStateBlockEntity)
        {
            return multiStateBlockEntity.mutableStream();
        }

        final BlockState currentState = getWorld().getBlockState(getPos());
        final Optional<IStateVariant> additionalStateInfo = IStateVariantManager
            .getInstance()
            .getStateVariant(currentState, Optional.ofNullable(blockEntity));
        final BlockInformation blockInformation = new BlockInformation(
            currentState,
            additionalStateInfo
        );

        if (IEligibilityManager.getInstance().canBeChiseled(blockInformation))
        {
            return BlockPosStreamProvider.getForRange(StateEntrySize.current().getBitsPerBlockSide())
                .map(blockPos -> new MutablePreAdaptedStateEntry(
                    blockInformation,
                    getWorld(),
                    getPos(),
                    blockPos,
                    this::setInAreaTarget,
                    this::clearInAreaTarget)
                );
        }

        return Stream.empty();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setInAreaTarget(
        final BlockInformation blockInformation,
        final Vec3 inAreaTarget) throws SpaceOccupiedException
    {
        if (getWorld().isOutsideBuildHeight(getPos()))
        {
            return;
        }

        if (inAreaTarget.x() < 0 ||
            inAreaTarget.y() < 0 ||
            inAreaTarget.z() < 0)
        {
            throw new IllegalArgumentException(
                "The chisel adapting world mutator can only mutate blocks with an in area offset greater or equal to 0. Requested was: " + inAreaTarget);
        }

        if (inAreaTarget.x() >= 1 ||
            inAreaTarget.y() >= 1 ||
            inAreaTarget.z() >= 1)
        {
            throw new IllegalArgumentException(
                "The chisel adapting world mutator can only mutate blocks with an in area offset smaller then 1. Requested was: " + inAreaTarget);
        }

        final BlockEntity blockEntity = getWorld().getBlockEntity(getPos());
        if (blockEntity instanceof IMultiStateBlockEntity multiStateBlockEntity)
        {
            multiStateBlockEntity.setInAreaTarget(blockInformation, inAreaTarget);
            return;
        }

        final BlockState blockState = getWorld().getBlockState(getPos());
        final Optional<IStateVariant> stateVariant = IStateVariantManager.getInstance()
            .getStateVariant(blockState, Optional.ofNullable(blockEntity));

        final BlockInformation newBlockInformation = new BlockInformation(blockState, stateVariant);
        if (!newBlockInformation.isAir())
        {
            throw new SpaceOccupiedException();
        }

        final Optional<Block> optionalWithConvertedBlock = IConversionManager.getInstance().getChiseledVariantOf(blockInformation.blockState());
        if (optionalWithConvertedBlock.isPresent())
        {
            final Block convertedBlock = optionalWithConvertedBlock.get();
            getWorld().setBlock(
                getPos(),
                convertedBlock.defaultBlockState(),
                Block.UPDATE_ALL
            );

            final BlockEntity convertedTileEntity = getWorld().getBlockEntity(getPos());
            if (convertedTileEntity instanceof IMultiStateBlockEntity multiStateBlockEntity)
            {
                multiStateBlockEntity.initializeWith(newBlockInformation);
                multiStateBlockEntity.setInAreaTarget(blockInformation, inAreaTarget);
                return;
            }

            throw new IllegalStateException("Conversion of the existing block of type: " + newBlockInformation + " into a chiseled variant failed.");
        }
    }

    @Override
    public void setInBlockTarget(final BlockInformation blockInformation, final BlockPos inAreaBlockPosOffset, final Vec3 inBlockTarget) throws SpaceOccupiedException
    {
        if (!inAreaBlockPosOffset.equals(BlockPos.ZERO))
        {
            throw new IllegalArgumentException("The chisel adapting world mutator can only mutate the given single block!");
        }

        this.setInAreaTarget(blockInformation, inBlockTarget);
    }

    /**
     * Clears the current area, using the offset from the area as well as the in area target offset.
     *
     * @param inAreaTarget The in area offset.
     */
    @SuppressWarnings("deprecation")
    @Override
    public void clearInAreaTarget(final Vec3 inAreaTarget)
    {
        if (getWorld().isOutsideBuildHeight(getPos()))
        {
            return;
        }

        if (inAreaTarget.x() < 0 ||
            inAreaTarget.y() < 0 ||
            inAreaTarget.z() < 0)
        {
            throw new IllegalArgumentException(
                "The chisel adapting world mutator can only mutate blocks with an in area offset greater or equal to 0. Requested was: " + inAreaTarget);
        }

        if (inAreaTarget.x() > 1 ||
            inAreaTarget.y() > 1 ||
            inAreaTarget.z() > 1)
        {
            throw new IllegalArgumentException(
                "The chisel adapting world mutator can only mutate blocks with an in area offset smaller then 1. Requested was: " + inAreaTarget);
        }

        final BlockEntity blockEntity = getWorld().getBlockEntity(getPos());
        if (blockEntity instanceof IMultiStateBlockEntity multiStateBlockEntity)
        {
            multiStateBlockEntity.clearInAreaTarget(inAreaTarget);
            return;
        }

        final BlockState blockState = getWorld().getBlockState(getPos());
        final Optional<IStateVariant> stateVariant = IStateVariantManager.getInstance()
            .getStateVariant(blockState, Optional.ofNullable(blockEntity));

        final BlockInformation newBlockInformation = new BlockInformation(blockState, stateVariant);

        if (newBlockInformation.isAir())
        {
            return;
        }

        final Optional<Block> optionalWithConvertedBlock = IConversionManager.getInstance().getChiseledVariantOf(newBlockInformation.blockState());
        if (optionalWithConvertedBlock.isPresent())
        {
            final Block convertedBlock = optionalWithConvertedBlock.get();
            getWorld().setBlock(
                getPos(),
                convertedBlock.defaultBlockState(),
                Block.UPDATE_ALL
            );

            final BlockEntity convertedTileEntity = getWorld().getBlockEntity(getPos());
            if (convertedTileEntity instanceof IMultiStateBlockEntity multiStateBlockEntity)
            {
                multiStateBlockEntity.initializeWith(newBlockInformation);
                multiStateBlockEntity.clearInAreaTarget(inAreaTarget);
                return;
            }

            throw new IllegalStateException("Conversion of the existing block of type: " + newBlockInformation + " into a chiseled variant failed.");
        }
    }

    /**
     * Clears the current area, using the in area block position offset as well as the in block target offset to calculate the in area offset for setting.
     *
     * @param inAreaBlockPosOffset The offset of blocks in the current area.
     * @param inBlockTarget        The offset in the targeted block.
     */
    @Override
    public void clearInBlockTarget(final BlockPos inAreaBlockPosOffset, final Vec3 inBlockTarget)
    {
        if (!inAreaBlockPosOffset.equals(BlockPos.ZERO))
        {
            throw new IllegalArgumentException("The chisel adapting world mutator can only mutate the given single block!");
        }

        this.clearInAreaTarget(inBlockTarget);
    }

    /**
     * Returns all entries in the current area in a mutable fashion. Includes all empty areas as areas containing an air state.
     *
     * @return A stream with a mutable state entry info for each mutable section in the area.
     */
    @SuppressWarnings("deprecation")
    @Override
    public Stream<IInWorldMutableStateEntryInfo> inWorldMutableStream()
    {
        if (getWorld().isOutsideBuildHeight(getPos()))
        {
            return Stream.empty();
        }

        final BlockEntity blockEntity = getWorld().getBlockEntity(getPos());
        if (blockEntity instanceof IMultiStateBlockEntity multiStateBlockEntity)
        {
            return multiStateBlockEntity.inWorldMutableStream();
        }

        final BlockState currentState = getWorld().getBlockState(getPos());
        final Optional<IStateVariant> additionalStateInfo = IStateVariantManager
            .getInstance()
            .getStateVariant(currentState, Optional.ofNullable(blockEntity));
        final BlockInformation blockInformation = new BlockInformation(
            currentState,
            additionalStateInfo
        );

        if (IEligibilityManager.getInstance().canBeChiseled(blockInformation) ||
            currentState.isAir())
        {
            return BlockPosStreamProvider.getForRange(StateEntrySize.current().getBitsPerBlockSide())
                .map(blockPos -> new MutablePreAdaptedStateEntry(
                    blockInformation,
                    getWorld(),
                    getPos(),
                    blockPos,
                    this::setInAreaTarget,
                    this::clearInAreaTarget)
                );
        }

        return Stream.empty();
    }

    /**
     * Trigger a batch mutation start.
     * <p>
     * As long as at least one batch mutation is still running no changes are transmitted to the client.
     *
     * @return The batch mutation lock.
     */
    @SuppressWarnings("deprecation")
    @Override
    public IBatchMutation batch()
    {
        if (getWorld().isOutsideBuildHeight(getPos()))
        {
            return () -> {
                //Noop
            };
        }

        final BlockEntity blockEntity = getWorld().getBlockEntity(getPos());
        if (blockEntity instanceof IMultiStateBlockEntity)
        {
            return ((IMultiStateBlockEntity) blockEntity).batch();
        }

        final BlockState blockState = getWorld().getBlockState(getPos());
        final Optional<IStateVariant> stateVariant = IStateVariantManager.getInstance()
            .getStateVariant(blockState, Optional.ofNullable(blockEntity));

        BlockInformation currentState = new BlockInformation(blockState, stateVariant);
        BlockInformation initializationState = currentState;
        if (currentState.isAir())
        {
            //This happens when placing into an empty blockspace.
            //We will assume a simple rock as the base material. The TE will fix itself after the placement.
            currentState = DEFAULT_STATE;
            initializationState = BlockInformation.AIR;
        }

        if (!IEligibilityManager.getInstance().canBeChiseled(currentState) && !currentState.isAir())
        {
            return () -> {
                //Noop
            };
        }

        final Optional<Block> optionalWithConvertedBlock = IConversionManager.getInstance().getChiseledVariantOf(currentState.blockState());
        if (optionalWithConvertedBlock.isPresent())
        {
            final Block convertedBlock = optionalWithConvertedBlock.get();
            getWorld().setBlock(
                getPos(),
                convertedBlock.defaultBlockState(),
                Block.UPDATE_ALL
            );

            final BlockEntity convertedTileEntity = getWorld().getBlockEntity(getPos());
            if (convertedTileEntity instanceof IMultiStateBlockEntity)
            {
                final IBatchMutation batchMutation = ((IMultiStateBlockEntity) convertedTileEntity).batch();
                ((IMultiStateBlockEntity) convertedTileEntity).initializeWith(initializationState);
                return batchMutation;
            }

            throw new IllegalStateException("Conversion of the existing block of type: " + currentState + " into a chiseled variant failed.");
        }

        return () -> {
            //Noop
        };
    }

    @Override
    public IBatchMutation batch(final IChangeTracker changeTracker)
    {
        if (getWorld().isOutsideBuildHeight(getPos()))
        {
            return () -> {
                //Noop
            };
        }

        final BlockState blockState = getWorld().getBlockState(getPos());
        final BlockEntity blockEntity = getWorld().getBlockEntity(getPos());
        final Optional<IStateVariant> stateVariant = IStateVariantManager.getInstance()
            .getStateVariant(blockState, Optional.ofNullable(blockEntity));

        BlockInformation currentState = new BlockInformation(blockState, stateVariant);
        if (!IEligibilityManager.getInstance().canBeChiseled(currentState) && !currentState.isAir())
        {
            return () -> {
                //Noop
            };
        }

        final IBatchMutation innerMutation = batch();
        if (blockEntity instanceof IMultiStateBlockEntity)
        {
            final IMultiStateSnapshot before = ((IMultiStateBlockEntity) blockEntity).createSnapshot();
            return () -> {
                final IMultiStateSnapshot after = ((IMultiStateBlockEntity) blockEntity).createSnapshot();
                innerMutation.close();
                changeTracker.onBlockUpdated(getPos(), before, after);
            };
        }
        return innerMutation;
    }

    @Override
    public VoxelShape provideShape(final CollisionType type, final BlockPos offset, final boolean simplify)
    {
        if (getWorld().isOutsideBuildHeight(getPos()))
        {
            return Shapes.empty();
        }

        final BlockEntity blockEntity = getWorld().getBlockEntity(getPos());
        if (blockEntity instanceof IMultiStateBlockEntity multiStateBlockEntity)
        {
            return multiStateBlockEntity.provideShape(type, offset, simplify);
        }

        final BlockState blockState = getWorld().getBlockState(getPos());
        final Optional<IStateVariant> stateVariant = IStateVariantManager.getInstance()
            .getStateVariant(blockState, Optional.ofNullable(blockEntity));

        BlockInformation currentState = new BlockInformation(blockState, stateVariant);
        if (!IEligibilityManager.getInstance().canBeChiseled(currentState) && !currentState.isAir())
        {
            return Shapes.empty();
        }

        if (currentState.isAir() && type.isValidFor(currentState.blockState()))
        {
            return Shapes.block().move(offset.getX(), offset.getY(), offset.getZ());
        }

        return currentState.blockState().getShape(getWorld(), getPos()).move(offset.getX(), offset.getY(), offset.getZ());
    }

    @Override
    public @NotNull AABB getBoundingBox()
    {
        return new AABB(
            getPos().getX(),
            getPos().getY(),
            getPos().getZ(),
            getPos().getX() + 1,
            getPos().getY() + 1,
            getPos().getZ() + 1
        );
    }

    private static class MutablePreAdaptedStateEntry implements IInWorldMutableStateEntryInfo
    {
        private final BlockInformation blockInformation;
        private final LevelAccessor    world;
        private final Vec3             startPoint;
        private final Vec3             endPoint;
        private final BlockPos         blockPos;

        private final StateSetter  setCallback;
        private final StateClearer clearCallback;

        public MutablePreAdaptedStateEntry(
            final BlockInformation blockInformation,
            final LevelAccessor world,
            final BlockPos blockPos,
            final Vec3i inBlockOffset,
            final StateSetter setCallback,
            final StateClearer clearCallback)
        {
            this.blockInformation = blockInformation;
            this.world = world;
            this.blockPos = blockPos;
            this.startPoint = Vec3.atLowerCornerOf(inBlockOffset)
                .multiply(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit());
            this.setCallback = setCallback;
            this.clearCallback = clearCallback;
            this.endPoint = this.startPoint.add(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit());
        }

        @Override
        public @NotNull BlockInformation getBlockInformation()
        {
            return blockInformation;
        }

        @Override
        public @NotNull Vec3 getStartPoint()
        {
            return startPoint;
        }

        @Override
        public @NotNull Vec3 getEndPoint()
        {
            return endPoint;
        }

        @Override
        public LevelAccessor getWorld()
        {
            return world;
        }

        @Override
        public BlockPos getBlockPos()
        {
            return blockPos;
        }

        @Override
        public void setBlockInformation(final BlockInformation blockInformation) throws SpaceOccupiedException
        {
            setCallback.set(blockInformation, getStartPoint());
        }

        /**
         * Clears the current state entries blockstate. Effectively setting the current blockstate to air.
         */
        @Override
        public void clear()
        {
            clearCallback.accept(getStartPoint());
        }
    }

    private static class PreAdaptedShapeIdentifier implements ISingleStateAreaShareIdentifier
    {
        private final int blockState;

        private PreAdaptedShapeIdentifier(final BlockState blockState) {this.blockState = IBlockStateIdManager.getInstance().getIdFrom(blockState);}

        @Override
        public boolean equals(final Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof final PreAdaptedShapeIdentifier that))
            {
                return false;
            }
            return blockState == that.blockState;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(blockState);
        }
    }
}

package mod.chiselsandbits.block.entities;

import com.communi.suggestu.scena.core.IScenaPlatform;
import com.communi.suggestu.scena.core.blockstate.ILevelBasedPropertyAccessor;
import com.communi.suggestu.scena.core.client.models.data.IBlockModelData;
import com.communi.suggestu.scena.core.client.models.data.IModelDataBuilder;
import com.communi.suggestu.scena.core.dist.DistExecutor;
import com.communi.suggestu.scena.core.entity.block.IBlockEntityPositionManager;
import com.communi.suggestu.scena.core.entity.block.IBlockEntityWithModelData;
import com.communi.suggestu.scena.core.util.SingleBlockLevelReader;
import com.communi.suggestu.scena.core.world.level.storage.CopyableTagValueInput;
import com.communi.suggestu.scena.core.world.level.storage.WriteableTagValueOutput;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mod.chiselsandbits.ChiselsAndBits;
import mod.chiselsandbits.api.axissize.CollisionType;
import mod.chiselsandbits.api.block.entity.IMultiStateBlockEntity;
import mod.chiselsandbits.api.block.entity.INetworkUpdatableEntity;
import mod.chiselsandbits.api.block.storage.StateEntryStorage;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.change.IChangeTracker;
import mod.chiselsandbits.api.chiseling.conversion.IConversionManager;
import mod.chiselsandbits.api.chiseling.eligibility.IEligibilityManager;
import mod.chiselsandbits.api.exceptions.SpaceOccupiedException;
import mod.chiselsandbits.api.multistate.StateEntrySize;
import mod.chiselsandbits.api.multistate.accessor.ISingleBlockAxisAlignedAreaAccessor;
import mod.chiselsandbits.api.multistate.accessor.IStateEntryInfo;
import mod.chiselsandbits.api.multistate.accessor.identifier.IAreaShapeIdentifier;
import mod.chiselsandbits.api.multistate.accessor.identifier.IArrayBackedAreaShapeIdentifier;
import mod.chiselsandbits.api.multistate.accessor.sortable.IPositionMutator;
import mod.chiselsandbits.api.multistate.mutator.IMutableStateEntryInfo;
import mod.chiselsandbits.api.multistate.mutator.callback.StateClearer;
import mod.chiselsandbits.api.multistate.mutator.callback.StateSetter;
import mod.chiselsandbits.api.multistate.mutator.world.IInWorldMutableStateEntryInfo;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshot;
import mod.chiselsandbits.api.multistate.statistics.IMultiStateObjectStatistics;
import mod.chiselsandbits.api.serialization.CBCodecs;
import mod.chiselsandbits.api.serialization.CBStreamCodecs;
import mod.chiselsandbits.api.serialization.Serializable;
import mod.chiselsandbits.api.util.*;
import mod.chiselsandbits.api.util.constants.NbtConstants;
import mod.chiselsandbits.api.variant.state.IStateVariantManager;
import mod.chiselsandbits.block.ChiseledBlock;
import mod.chiselsandbits.client.model.data.ChiseledBlockModelDataManager;
import mod.chiselsandbits.network.packets.UpdateBlockEntityPacket;
import mod.chiselsandbits.registrars.ModBlockEntityTypes;
import mod.chiselsandbits.serialization.CompressedDataFindingCodec;
import mod.chiselsandbits.storage.IMultiThreadedStorageEngine;
import mod.chiselsandbits.storage.StorageEngineBuilder;
import mod.chiselsandbits.utils.BlockPosUtils;
import mod.chiselsandbits.utils.MultiStateSnapshotUtils;
import mod.chiselsandbits.voxelshape.MultiStateBlockEntityDiscreteVoxelShape;
import mod.chiselsandbits.voxelshape.SingleBlockVoxelShapeCache;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CubeVoxelShape;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ChiseledBlockEntity extends BlockEntity implements
    IMultiStateBlockEntity, INetworkUpdatableEntity<ChiseledBlockEntity.Payload>, IBlockEntityWithModelData
{

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final float ONE_THOUSANDS = 1 / 1000f;

    private       MutableStatistics                    mutableStatistics;
    private final Map<UUID, IBatchMutation>            batchMutations       = Maps.newConcurrentMap();
    private final Object                               tagSyncHandle        = new Object();
    private       StateEntryStorage                    storage;
    private       IMultiThreadedStorageEngine<Payload> storageEngine;
    private       IBlockModelData                      modelData            = IModelDataBuilder.create().build();
    private       CopyableTagValueInput                lastTag              = null;
    private       CompletableFuture<Void>              storageFuture        = null;
    private final List<ValueInput>                     deserializationQueue = Collections.synchronizedList(Lists.newArrayList());
    private final SingleBlockVoxelShapeCache           voxelShapeCache      = new SingleBlockVoxelShapeCache(this);
    private       boolean                              isLoading            = false;
    private final Deque<Runnable>                      afterCurrentLoad     = new ArrayDeque<>();

    public ChiseledBlockEntity(BlockPos position, BlockState state)
    {
        super(ModBlockEntityTypes.CHISELED.get(), position, state);
        storage = new StateEntryStorage();
        mutableStatistics = new MutableStatistics(position);

        createStorageEngine();
    }

    @NotNull
    private static Executor createDefaultExecutor()
    {
        return DistExecutor.unsafeRunForDist(
            () -> Minecraft::getInstance,
            () -> () -> new ServerSchedulingExecutor(IScenaPlatform.getInstance().getCurrentServer())
        );
    }

    private void createStorageEngine()
    {
        storageEngine = StorageEngineBuilder.<Payload>create()
            .fallback(Payload.LEGACY_MAP_CODEC)
            .with(Payload.MAP_CODEC)
            .buildMultiThreaded();
    }

    private Executor getExecutor()
    {
        if (getLevel() != null && getLevel().getServer() != null)
        {
            return new ServerSchedulingExecutor(getLevel().getServer());
        }

        return createDefaultExecutor();
    }

    public void updateModelData()
    {
        ChiseledBlockModelDataManager.getInstance().updateModelData(this);
    }

    private void updateModelDataIfInLoadedChunk()
    {
        if (level != null && level.isClientSide() && level.isLoaded(getBlockPos()))
        {
            updateModelData();
            level.getLightEngine().checkBlock(getBlockPos());
        }
    }

    @Override
    public void setLevel(final @NotNull Level level)
    {
        super.setLevel(level);

        IBlockEntityPositionManager.getInstance().add(this);
        createStorageEngine();

        if (this.deserializationQueue.isEmpty())
        {
            return;
        }

        this.deserializationQueue.forEach(this::deserializeNBT);
    }

    @Override
    public void setRemoved()
    {
        IBlockEntityPositionManager.getInstance().remove(this);
        super.setRemoved();
    }

    @Override
    public IAreaShapeIdentifier createNewShapeIdentifier()
    {
        return new Identifier(this.storage);
    }

    @Override
    public Stream<IStateEntryInfo> stream()
    {
        return BlockPosStreamProvider.getForRange(StateEntrySize.current().getBitsPerBlockSide())
            .map(blockPos -> new StateEntry(
                storage.getBlockInformation(
                    blockPos.getX(),
                    blockPos.getY(),
                    blockPos.getZ()
                ),
                getLevel(),
                getBlockPos(),
                blockPos,
                this::setInAreaTarget,
                this::clearInAreaTarget)
            );
    }

    /**
     * Indicates if the given target is inside the current accessor.
     *
     * @param inAreaTarget The area target to check.
     * @return True when inside, false when not.
     */
    @Override
    public boolean isInside(final Vec3 inAreaTarget)
    {
        return !(inAreaTarget.x() < 0) &&
            !(inAreaTarget.y() < 0) &&
            !(inAreaTarget.z() < 0) &&
            !(inAreaTarget.x() >= 1) &&
            !(inAreaTarget.y() >= 1) &&
            !(inAreaTarget.z() >= 1);
    }

    @Override
    public Optional<IStateEntryInfo> getInAreaTarget(final Vec3 inAreaTarget)
    {
        if (inAreaTarget.x() < 0 ||
            inAreaTarget.y() < 0 ||
            inAreaTarget.z() < 0 ||
            inAreaTarget.x() >= 1 ||
            inAreaTarget.y() >= 1 ||
            inAreaTarget.z() >= 1)
        {
            throw new IllegalArgumentException("Target is not in the current area.");
        }

        final Vec3 exactAreaPos =
            inAreaTarget.multiply(StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide());
        final BlockPos inAreaPos = VectorUtils.toBlockPos(exactAreaPos);

        final BlockInformation blockInformation = this.storage.getBlockInformation(
            inAreaPos.getX(),
            inAreaPos.getY(),
            inAreaPos.getZ()
        );

        return Optional.of(new StateEntry(
            blockInformation,
            getLevel(),
            getBlockPos(),
            inAreaPos,
            this::setInAreaTarget,
            this::clearInAreaTarget)
        );
    }

    /**
     * Indicates if the given target (with the given block position offset) is inside the current accessor.
     *
     * @param inAreaBlockPosOffset The offset of blocks in the current area.
     * @param inBlockTarget        The offset in the targeted block.
     * @return True when inside, false when not.
     */
    @Override
    public boolean isInside(final BlockPos inAreaBlockPosOffset, final Vec3 inBlockTarget)
    {
        if (!inAreaBlockPosOffset.equals(BlockPos.ZERO))
        {
            return false;
        }

        return this.isInside(
            inBlockTarget
        );
    }

    @Override
    public Optional<IStateEntryInfo> getInBlockTarget(final BlockPos inAreaBlockPosOffset, final Vec3 inBlockTarget)
    {
        if (!inAreaBlockPosOffset.equals(BlockPos.ZERO))
        {
            throw new IllegalStateException(String.format("The given in area block pos offset is not inside the current block: %s", inAreaBlockPosOffset));
        }

        return this.getInAreaTarget(
            inBlockTarget
        );
    }

    @Override
    public IMultiStateSnapshot createSnapshot()
    {
        return MultiStateSnapshotUtils.createFromStorage(this.storage);
    }

    @Override
    protected void loadAdditional(final ValueInput input)
    {
        if (this.getLevel() != null)
        {
            this.deserializeNBT(input);
        }

        this.queueDeserializeNbt(input);
    }

    private void queueDeserializeNbt(ValueInput nbt)
    {
        this.deserializationQueue.add(nbt);
    }

    private void deserializeNBT(ValueInput valueInput)
    {
        final Optional<ValueInput> data = valueInput.child(NbtConstants.DATA);
        if (data.isEmpty())
        {
            return;
        }

        var dataInput = data.get();

        readAsync(dataInput)
            .thenAcceptAsync(payload -> {
                this.storage = payload.storage();
                this.mutableStatistics = payload.mutableStatistics();

                if (mutableStatistics.isRequiresRecalculation())
                {
                    mutableStatistics.recalculate(level, this.storage, this.blockPos(), shouldUpdateWorld());
                }

                mutableStatistics.updatePrimaryState(level, shouldUpdateWorld());

                this.isLoading = false;
                if (shouldUpdateWorld())
                {
                    setChanged();
                }

                updateModelDataIfInLoadedChunk();

                while (!this.afterCurrentLoad.isEmpty())
                {
                    this.afterCurrentLoad.pop().run();
                }
            }, getExecutor());

        if (dataInput instanceof CopyableTagValueInput copyableTagValueInput)
        {
            synchronized (this.tagSyncHandle)
            {
                this.lastTag = copyableTagValueInput;
            }
        }
    }

    @Override
    protected void saveAdditional(final @NotNull ValueOutput output)
    {
        super.saveAdditional(output);

        final ValueOutput dataOutput = output.child("data");
        synchronized (this.tagSyncHandle)
        {
            if (this.lastTag instanceof CopyableTagValueInput copyableTagValueInput && dataOutput instanceof WriteableTagValueOutput)
            {
                copyableTagValueInput.copyTo(dataOutput);
                return;
            }
        }

        if (this.storageFuture == null)
        {
            this.storageFuture = writeAsync(output);
        }

        this.storageFuture.join();

        //Now the tag needs to be there!
        Validate.notNull(this.lastTag, "The storage future did not complete.");
        if (this.lastTag instanceof CopyableTagValueInput copyableTagValueInput && dataOutput instanceof WriteableTagValueOutput)
        {
            copyableTagValueInput.copyTo(dataOutput);
        }
        else
        {
            throw new IllegalStateException("The storage future did not result in a TagValueOutput that can be copied to the output!");
        }
    }

    @Override
    public boolean isCanBeFlooded()
    {
        return mutableStatistics.isCanBeFlooded();
    }

    @Override
    public void setCanBeFlooded(boolean canBeFlooded)
    {
        this.mutableStatistics.setCanBeFlooded(canBeFlooded);
        setChanged();
    }

    @Override
    public boolean isEmitsLightBasedOnFullBlock()
    {
        return mutableStatistics.isEmitsLightBasedOnFullBlock();
    }

    @Override
    public void setEmitsLightBasedOnFullBlock(boolean emitsLightBasedOnFullBlock)
    {
        mutableStatistics.setEmitsLightBasedOnFullBlock(emitsLightBasedOnFullBlock);
        setChanged();
    }

    public VoxelShape getShape(final CollisionType type)
    {
        return voxelShapeCache.getShape(type);
    }

    /**
     * For tile entities, ensures the chunk containing the tile entity is saved to disk later - the game won't think it hasn't changed and skip it.
     */
    @Override
    public void setChanged()
    {
        if (!this.batchMutations.isEmpty())
        {
            return;
        }

        if (getLevel() == null)
        {
            return;
        }

        super.setChanged();

        if (this.isLoading)
        {
            return;
        }

        if (getLevel().isLoaded(getBlockPos()))
        {
            ChiseledBlock.updateBlockInformation(this);
            getLevel().updateNeighborsAt(getBlockPos(), getLevel().getBlockState(getBlockPos()).getBlock());
        }

        voxelShapeCache.reset();

        if (!getLevel().isClientSide())
        {
            this.mutableStatistics.updatePrimaryState(level, true);

            synchronized (this.tagSyncHandle)
            {
                if (this.storageFuture != null)
                {
                    this.storageFuture.cancel(false);
                }

                this.lastTag = null;

                this.storageFuture = writeAsync(TagValueOutput.createWithContext(
                    new ProblemReporter.ScopedCollector(LOGGER),
                    registryAccess()
                ));
                ChiselsAndBits.getInstance().getNetworkChannel().sendToTrackingChunk(
                    new UpdateBlockEntityPacket(this),
                    getLevel().getChunkAt(getBlockPos())
                );
            }
        }
    }

    private CompletableFuture<Void> writeAsync(ValueOutput output)
    {
        return this.storageEngine.encodeAsync(payload(), output)
            .thenRun(() -> {
                if (output instanceof WriteableTagValueOutput writeableTagValueOutput)
                {
                    synchronized (this.tagSyncHandle)
                    {
                        this.lastTag = writeableTagValueOutput.asInput();
                    }
                }
            });
    }

    private CompletableFuture<Payload> readAsync(ValueInput valueInput)
    {
        this.isLoading = true;
        return this.storageEngine.decodeAsync(valueInput);
    }

    private boolean shouldUpdateWorld()
    {
        return this.getLevel() != null && this.batchMutations.isEmpty() && this.getLevel() instanceof ServerLevel && getLevel().isLoaded(getBlockPos());
    }

    @Override
    public RegistryAccess registryAccess()
    {
        if (getLevel() == null)
        {
            throw new IllegalStateException("The level is not set.");
        }

        return getLevel().registryAccess();
    }

    @Override
    public BlockPos blockPos()
    {
        return getBlockPos();
    }

    @Override
    public Payload payload()
    {
        return new Payload(
            this.storage,
            this.mutableStatistics
        );
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, Payload> streamCodec()
    {
        return Payload.STREAM_CODEC;
    }

    @Override
    public void receivePayload(Payload payload)
    {
        this.storage = payload.storage();
        this.mutableStatistics = payload.mutableStatistics();
        this.updateModelDataIfInLoadedChunk();
    }

    /**
     * Returns all entries in the current area in a mutable fashion. Includes all empty areas as areas containing an air state.
     *
     * @return A stream with a mutable state entry info for each mutable section in the area.
     */
    @Override
    public Stream<IMutableStateEntryInfo> mutableStream()
    {
        return BlockPosStreamProvider.getForRange(StateEntrySize.current().getBitsPerBlockSide())
            .map(blockPos -> new StateEntry(
                storage.getBlockInformation(blockPos.getX(), blockPos.getY(), blockPos.getZ()),
                getLevel(),
                getBlockPos(),
                blockPos,
                this::setInAreaTarget,
                this::clearInAreaTarget)
            );
    }

    @Override
    public void setInAreaTarget(
        final BlockInformation newInformation,
        final Vec3 inAreaTarget
    ) throws SpaceOccupiedException
    {
        if (inAreaTarget.x() < 0 ||
            inAreaTarget.y() < 0 ||
            inAreaTarget.z() < 0 ||
            inAreaTarget.x() >= 1 ||
            inAreaTarget.y() >= 1 ||
            inAreaTarget.z() >= 1)
        {
            throw new IllegalArgumentException("Target is not in the current area.");
        }

        final Vec3 exactAreaPos =
            inAreaTarget.multiply(StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide());
        final BlockPos inAreaPos = VectorUtils.toBlockPos(exactAreaPos);

        final BlockInformation information = this.storage.getBlockInformation(
            inAreaPos.getX(),
            inAreaPos.getY(),
            inAreaPos.getZ()
        );

        if (!information.isAir())
        {
            throw new SpaceOccupiedException();
        }

        if (getLevel() == null)
        {
            return;
        }

        this.storage.setBlockInformation(
            inAreaPos.getX(),
            inAreaPos.getY(),
            inAreaPos.getZ(),
            newInformation
        );

        if (newInformation.isAir() && !information.isAir())
        {
            mutableStatistics.onBlockStateRemoved(level, information, inAreaPos, shouldUpdateWorld());
        }
        else if (!newInformation.isAir() && information.isAir())
        {
            mutableStatistics.onBlockStateAdded(level, newInformation, inAreaPos, shouldUpdateWorld());
        }
        else if (!newInformation.isAir() && !information.isAir())
        {
            mutableStatistics.onBlockStateReplaced(level, information, newInformation, inAreaPos, shouldUpdateWorld());
        }

        if (getLevel() != null)
        {
            setChanged();
        }
    }

    @Override
    public LevelAccessor getWorld()
    {
        return getLevel();
    }

    @Override
    public Vec3 getInWorldStartPoint()
    {
        return Vec3.atLowerCornerOf(getBlockPos());
    }

    @Override
    public void setInBlockTarget(final BlockInformation blockInformation, final BlockPos inAreaBlockPosOffset, final Vec3 inBlockTarget) throws SpaceOccupiedException
    {
        if (!inAreaBlockPosOffset.equals(BlockPos.ZERO))
        {
            throw new IllegalStateException(String.format("The given in area block pos offset is not inside the current block: %s", inAreaBlockPosOffset));
        }

        this.setInAreaTarget(
            blockInformation,
            inBlockTarget);
    }

    @Override
    public Vec3 getInWorldEndPoint()
    {
        return getInWorldStartPoint().add(1, 1, 1).subtract(ONE_THOUSANDS, ONE_THOUSANDS, ONE_THOUSANDS);
    }

    /**
     * Clears the current area, using the offset from the area as well as the in area target offset.
     *
     * @param inAreaTarget The in area offset.
     */
    @Override
    public void clearInAreaTarget(final Vec3 inAreaTarget)
    {
        if (inAreaTarget.x() < 0 ||
            inAreaTarget.y() < 0 ||
            inAreaTarget.z() < 0 ||
            inAreaTarget.x() >= 1 ||
            inAreaTarget.y() >= 1 ||
            inAreaTarget.z() >= 1)
        {
            throw new IllegalArgumentException("Target is not in the current area.");
        }

        final Vec3 exactAreaPos =
            inAreaTarget.multiply(StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide());
        final BlockPos inAreaPos = VectorUtils.toBlockPos(exactAreaPos);

        if (getLevel() == null)
        {
            return;
        }

        final BlockInformation currentInformation = this.storage.getBlockInformation(
            inAreaPos.getX(),
            inAreaPos.getY(),
            inAreaPos.getZ()
        );

        if (currentInformation.isAir())
        {
            return;
        }

        if (!IEligibilityManager.getInstance().canBeChiseled(currentInformation))
        {
            return;
        }

        final BlockInformation blockState = BlockInformation.AIR;

        this.storage.setBlockInformation(
            inAreaPos.getX(),
            inAreaPos.getY(),
            inAreaPos.getZ(),
            blockState
        );

        if (blockState.isAir() && !currentInformation.isAir())
        {
            mutableStatistics.onBlockStateRemoved(level, currentInformation, inAreaPos, shouldUpdateWorld());
        }
        else if (!blockState.isAir() && currentInformation.isAir())
        {
            mutableStatistics.onBlockStateAdded(level, blockState, inAreaPos, shouldUpdateWorld());
        }
        else if (!blockState.isAir() && !currentInformation.isAir())
        {
            mutableStatistics.onBlockStateReplaced(level, currentInformation, blockState, inAreaPos, shouldUpdateWorld());
        }

        if (getLevel() != null)
        {
            setChanged();
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
            throw new IllegalStateException(String.format("The given in area block pos offset is not inside the current block: %s", inAreaBlockPosOffset));
        }

        this.clearInAreaTarget(
            inBlockTarget
        );
    }

    @Override
    public IMultiStateObjectStatistics getStatistics()
    {
        return mutableStatistics;
    }

    private void executeWhenLoaded(Runnable runnable)
    {
        if (!this.isLoading)
        {
            runnable.run();
            return;
        }

        this.afterCurrentLoad.add(runnable);
    }

    @Override
    public void rotate(final Direction.Axis axis, final int rotationCount)
    {
        if (getLevel() == null)
        {
            return;
        }

        executeWhenLoaded(() -> {
            //Large operation, better batch this together to prevent weird updates.
            try (final IBatchMutation ignored = batch())
            {
                this.storage.rotate(axis, rotationCount);
                this.mutableStatistics.recalculate(this.getLevel(), this.storage, this.getBlockPos());
            }
        });
    }

    @Override
    public void mirror(final Direction.Axis axis)
    {
        if (getLevel() == null)
        {
            return;
        }

        executeWhenLoaded(() -> {
            //Large operation, better batch this together to prevent weird updates.
            try (final IBatchMutation ignored = batch())
            {
                this.storage.mirror(axis);
                this.mutableStatistics.recalculate(this.getLevel(), this.storage, this.getBlockPos());
            }
        });
    }

    @Override
    public void initializeWith(final BlockInformation newInitialInformation)
    {
        if (getLevel() == null)
        {
            return;
        }

        try (IBatchMutation ignored = batch())
        {
            this.storage.initializeWith(newInitialInformation);
            this.mutableStatistics.initializeWith(level, newInitialInformation);
        }
    }

    @Override
    public Stream<IInWorldMutableStateEntryInfo> inWorldMutableStream()
    {
        return BlockPosStreamProvider.getForRange(StateEntrySize.current().getBitsPerBlockSide())
            .map(blockPos -> new StateEntry(
                storage.getBlockInformation(blockPos.getX(), blockPos.getY(), blockPos.getZ()),
                getLevel(),
                getBlockPos(),
                blockPos,
                this::setInAreaTarget,
                this::clearInAreaTarget)
            );
    }

    @Override
    public Stream<IStateEntryInfo> streamWithPositionMutator(final IPositionMutator positionMutator)
    {
        return BlockPosStreamProvider.getForRange(StateEntrySize.current().getBitsPerBlockSide())
            .map(blockPos ->
                {
                    final Vec3i pos = positionMutator.mutate(blockPos);
                    return new StateEntry(
                        storage.getBlockInformation(pos.getX(), pos.getY(), pos.getZ()),
                        getLevel(),
                        getBlockPos(),
                        pos,
                        this::setInAreaTarget,
                        this::clearInAreaTarget);
                }
            );
    }

    @Override
    public void forEachWithPositionMutator(
        final IPositionMutator positionMutator, final Consumer<IStateEntryInfo> consumer)
    {
        BlockPosForEach.forEachInRange(StateEntrySize.current().getBitsPerBlockSide(), (BlockPos blockPos) ->
        {
            final Vec3i pos = positionMutator.mutate(blockPos);
            consumer.accept(new StateEntry(
                storage.getBlockInformation(pos.getX(), pos.getY(), pos.getZ()),
                getLevel(),
                getBlockPos(),
                pos,
                this::setInAreaTarget,
                this::clearInAreaTarget));
        });
    }

    @SuppressWarnings("resource")
    @Override
    public IBatchMutation batch()
    {
        final UUID id = UUID.randomUUID();
        final IBatchMutation storageBatch = storage.batch();

        this.batchMutations.put(id, new BatchMutationLock(() ->
        {
            this.batchMutations.remove(id);
            storageBatch.close();

            if (this.batchMutations.isEmpty())
            {
                setChanged();
            }
        }));
        return this.batchMutations.get(id);
    }

    @SuppressWarnings("resource")
    @Override
    public IBatchMutation batch(final IChangeTracker changeTracker)
    {
        final IBatchMutation innerMutation = batch();
        final IMultiStateSnapshot before = this.createSnapshot();
        return () ->
        {
            final IMultiStateSnapshot after = this.createSnapshot();
            innerMutation.close();
            changeTracker.onBlockUpdated(getBlockPos(), before, after);
        };
    }

    public void setModelData(final IBlockModelData modelData)
    {
        this.modelData = modelData;
    }

    @NotNull
    public IBlockModelData getBlockModelData()
    {
        return this.modelData;
    }

    @Override
    public VoxelShape provideShape(
        final CollisionType type, final BlockPos offset, final boolean simplify)
    {
        VoxelShape shape = new CubeVoxelShape(new MultiStateBlockEntityDiscreteVoxelShape(
            this.mutableStatistics.getCollideableEntries(type, storage)
        ));

        if (offset != BlockPos.ZERO)
        {
            shape = shape.move(offset.getX(), offset.getY(), offset.getZ());
        }

        if (simplify)
        {
            shape = shape.optimize();
        }

        return shape;
    }

    @Override
    public @NotNull AABB getBoundingBox()
    {
        return new AABB(
            this.getBlockPos().getX(),
            this.getBlockPos().getY(),
            this.getBlockPos().getZ(),
            this.getBlockPos().getX() + 1,
            this.getBlockPos().getY() + 1,
            this.getBlockPos().getZ() + 1
        );
    }

    private static final class StateEntry implements IInWorldMutableStateEntryInfo
    {

        private final BlockInformation blockInformation;
        private final LevelAccessor    reader;
        private final BlockPos         blockPos;
        private final Vec3             startPoint;
        private final Vec3             endPoint;

        private final StateSetter  stateSetter;
        private final StateClearer stateClearer;

        public StateEntry(
            final BlockInformation blockInformation,
            final LevelAccessor reader,
            final BlockPos blockPos,
            final Vec3i startPoint,
            final StateSetter stateSetter,
            final StateClearer stateClearer)
        {
            this(
                blockInformation,
                reader,
                blockPos,
                Vec3.atLowerCornerOf(startPoint)
                    .multiply(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit()),
                Vec3.atLowerCornerOf(startPoint)
                    .multiply(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit())
                    .add(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit()),
                stateSetter, stateClearer);
        }

        private StateEntry(
            final BlockInformation blockInformation,
            final LevelAccessor reader,
            final BlockPos blockPos,
            final Vec3 startPoint,
            final Vec3 endPoint,
            final StateSetter stateSetter,
            final StateClearer stateClearer)
        {
            this.blockInformation = blockInformation;
            this.reader = reader;
            this.blockPos = blockPos;
            this.startPoint = startPoint;
            this.endPoint = endPoint;
            this.stateSetter = stateSetter;
            this.stateClearer = stateClearer;
        }

        @Override
        public @NotNull BlockInformation getBlockInformation()
        {
            return blockInformation;
        }

        @Override
        public void setBlockInformation(final BlockInformation blockState) throws SpaceOccupiedException
        {
            stateSetter.set(blockState, getStartPoint());
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
        public void clear()
        {
            stateClearer.accept(getStartPoint());
        }

        @Override
        public LevelAccessor getWorld()
        {
            return reader;
        }

        @Override
        public BlockPos getBlockPos()
        {
            return blockPos;
        }
    }

    private static final class Identifier implements IArrayBackedAreaShapeIdentifier
    {
        private final StateEntryStorage snapshot;

        private Identifier(final StateEntryStorage section)
        {
            this.snapshot = section.createSnapshot();
        }

        @Override
        public boolean equals(final Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof final IArrayBackedAreaShapeIdentifier that))
            {
                return false;
            }

            return Arrays.equals(this.getBackingData(), that.getBackingData()) &&
                this.getPalette().equals(that.getPalette());
        }

        @Override
        public int hashCode()
        {
            return snapshot.hashCode();
        }

        @Override
        public String toString()
        {
            return "Identifier{" +
                "snapshot=" + snapshot +
                '}';
        }

        @Override
        public long[] getBackingData()
        {
            return snapshot.getRawData();
        }

        @Override
        public List<BlockInformation> getPalette()
        {
            return snapshot.states();
        }
    }

    private record BatchMutationLock(Runnable closeCallback) implements IBatchMutation
    {

        @Override
        public void close()
        {
            this.closeCallback.run();
        }
    }

    private static final class MutableStatistics implements IMultiStateObjectStatistics, Serializable.Registry<MutableStatistics>
    {

        private static final BiFunction<LevelReader, BlockPos, Integer> LIGHT_BLOCK_GETTER = (levelReader, pos) -> levelReader.getBlockState(pos).getLightBlock();

        public static final Codec<MutableStatistics> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf(NbtConstants.POSITION).forGetter(MutableStatistics::getInWorldPos),
            CBCodecs.unboundedComplexMap(BlockInformation.CODEC, Codec.INT).fieldOf(NbtConstants.STATE_COUNTS).forGetter(MutableStatistics::getStateCounts),
            CBCodecs.unboundedTable(Codec.INT, Codec.INT, ColumnStatistics.CODEC).fieldOf(NbtConstants.COLUMN_STATISTICS).forGetter(MutableStatistics::getColumnStatisticsTable),
            Codec.unboundedMap(CollisionType.CODEC, CBCodecs.BIT_SET).fieldOf(NbtConstants.COLLISION_DATA).forGetter(MutableStatistics::getCollisionData),
            BlockInformation.CODEC.fieldOf(NbtConstants.PRIMARY_STATE).forGetter(MutableStatistics::getPrimaryState),
            Codec.INT.fieldOf(NbtConstants.TOTAL_USED_BLOCK_COUNT).forGetter(MutableStatistics::getTotalUsedBlockCount),
            Codec.INT.fieldOf(NbtConstants.TOTAL_USED_CHECKS_WEAK_POWER_COUNT).forGetter(MutableStatistics::getTotalUsedChecksWeakPowerCount),
            Codec.INT.fieldOf(NbtConstants.TOTAL_LIGHT_LEVEL).forGetter(MutableStatistics::getTotalLightLevel),
            Codec.INT.fieldOf(NbtConstants.TOTAL_LIGHT_BLOCK_LEVEL).forGetter(MutableStatistics::getTotalLightBlockLevel),
            Codec.BOOL.fieldOf(NbtConstants.CAN_BE_FLOODED).forGetter(MutableStatistics::isCanBeFlooded),
            Codec.BOOL.fieldOf(NbtConstants.EMITS_LIGHT_BASED_ON_FULL_BLOCK).forGetter(MutableStatistics::isEmitsLightBasedOnFullBlock),
            Codec.BOOL.fieldOf(NbtConstants.REQUIRES_RECALCULATION).forGetter(MutableStatistics::isRequiresRecalculation)
        ).apply(instance, MutableStatistics::new));

        public static final Codec<MutableStatistics> LEGACY_CODEC = Codec.unit(MutableStatistics::new);

        public static final MapCodec<MutableStatistics> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockPos.CODEC.fieldOf(NbtConstants.POSITION).forGetter(MutableStatistics::getInWorldPos),
            CBCodecs.unboundedComplexMap(BlockInformation.CODEC, Codec.INT).fieldOf(NbtConstants.STATE_COUNTS).forGetter(MutableStatistics::getStateCounts),
            CBCodecs.unboundedTable(Codec.INT, Codec.INT, ColumnStatistics.CODEC).fieldOf(NbtConstants.COLUMN_STATISTICS).forGetter(MutableStatistics::getColumnStatisticsTable),
            Codec.unboundedMap(CollisionType.CODEC, CBCodecs.BIT_SET).fieldOf(NbtConstants.COLLISION_DATA).forGetter(MutableStatistics::getCollisionData),
            BlockInformation.CODEC.fieldOf(NbtConstants.PRIMARY_STATE).forGetter(MutableStatistics::getPrimaryState),
            Codec.INT.fieldOf(NbtConstants.TOTAL_USED_BLOCK_COUNT).forGetter(MutableStatistics::getTotalUsedBlockCount),
            Codec.INT.fieldOf(NbtConstants.TOTAL_USED_CHECKS_WEAK_POWER_COUNT).forGetter(MutableStatistics::getTotalUsedChecksWeakPowerCount),
            Codec.INT.fieldOf(NbtConstants.TOTAL_LIGHT_LEVEL).forGetter(MutableStatistics::getTotalLightLevel),
            Codec.INT.fieldOf(NbtConstants.TOTAL_LIGHT_BLOCK_LEVEL).forGetter(MutableStatistics::getTotalLightBlockLevel),
            Codec.BOOL.fieldOf(NbtConstants.CAN_BE_FLOODED).forGetter(MutableStatistics::isCanBeFlooded),
            Codec.BOOL.fieldOf(NbtConstants.EMITS_LIGHT_BASED_ON_FULL_BLOCK).forGetter(MutableStatistics::isEmitsLightBasedOnFullBlock),
            Codec.BOOL.fieldOf(NbtConstants.REQUIRES_RECALCULATION).forGetter(MutableStatistics::isRequiresRecalculation)
        ).apply(instance, MutableStatistics::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, MutableStatistics> STREAM_CODEC = CBStreamCodecs.composite(
            BlockPos.STREAM_CODEC,
            MutableStatistics::getInWorldPos,
            ByteBufCodecs.map(HashMap::new, BlockInformation.STREAM_CODEC, ByteBufCodecs.VAR_INT),
            MutableStatistics::getStateCounts,
            CBStreamCodecs.<Integer, Integer, ColumnStatistics, RegistryFriendlyByteBuf>table(HashMap::new,
                HashMap::new,
                HashMap::new,
                ByteBufCodecs.VAR_INT,
                ByteBufCodecs.VAR_INT,
                ColumnStatistics.STREAM_CODEC),
            MutableStatistics::getColumnStatisticsTable,
            ByteBufCodecs.map(HashMap::new, CollisionType.STREAM_CODEC, CBStreamCodecs.BIT_SET),
            MutableStatistics::getCollisionData,
            BlockInformation.STREAM_CODEC,
            MutableStatistics::getPrimaryState,
            ByteBufCodecs.VAR_INT,
            MutableStatistics::getTotalUsedBlockCount,
            ByteBufCodecs.VAR_INT,
            MutableStatistics::getTotalUsedChecksWeakPowerCount,
            ByteBufCodecs.VAR_INT,
            MutableStatistics::getTotalLightLevel,
            ByteBufCodecs.VAR_INT,
            MutableStatistics::getTotalLightBlockLevel,
            ByteBufCodecs.BOOL,
            MutableStatistics::isCanBeFlooded,
            ByteBufCodecs.BOOL,
            MutableStatistics::isEmitsLightBasedOnFullBlock,
            ByteBufCodecs.BOOL,
            MutableStatistics::isRequiresRecalculation,
            MutableStatistics::new
        );

        private final Map<BlockInformation, Integer>            countMap;
        private final Table<Integer, Integer, ColumnStatistics> columnStatisticsTable;
        private final Map<CollisionType, BitSet>                collisionData;
        private       BlockPos                                  inWorldPos;
        private       BlockInformation                          primaryState;
        private       int                                       totalUsedBlockCount           = 0;
        private       int                                       totalUsedChecksWeakPowerCount = 0;
        private       int                                       totalLightLevel               = 0;
        private       int                                       totalLightBlockLevel          = 0;
        private       boolean                                   canBeFlooded                  = true;
        private       boolean                                   emitsLightBasedOnFullBlock    = false;
        private       boolean                                   requiresRecalculation         = false;

        private MutableStatistics(BlockPos inWorldPos)
        {
            this.inWorldPos = inWorldPos;
            this.countMap = Maps.newConcurrentMap();
            this.columnStatisticsTable = HashBasedTable.create();
            this.collisionData = Maps.newConcurrentMap();
            this.primaryState = BlockInformation.AIR;
            this.requiresRecalculation = true;
        }

        public MutableStatistics()
        {
            this(
                BlockPos.ZERO
            );
        }

        public MutableStatistics(
            BlockPos inWorldPos,
            Map<BlockInformation, Integer> countMap,
            Table<Integer, Integer, ColumnStatistics> columnStatisticsTable,
            Map<CollisionType, BitSet> collisionData,
            BlockInformation primaryState,
            int totalUsedBlockCount,
            int totalUsedChecksWeakPowerCount,
            int totalLightLevel,
            int totalLightBlockLevel,
            boolean canBeFlooded,
            boolean emitsLightBasedOnFullBlock,
            boolean requiresRecalculation)
        {
            this.inWorldPos = inWorldPos;
            this.countMap = new ConcurrentHashMap<>(countMap);
            this.columnStatisticsTable = columnStatisticsTable;
            this.collisionData = new ConcurrentHashMap<>(collisionData);
            this.primaryState = primaryState;
            this.totalUsedBlockCount = totalUsedBlockCount;
            this.totalUsedChecksWeakPowerCount = totalUsedChecksWeakPowerCount;
            this.totalLightLevel = totalLightLevel;
            this.totalLightBlockLevel = totalLightBlockLevel;
            this.canBeFlooded = canBeFlooded;
            this.emitsLightBasedOnFullBlock = emitsLightBasedOnFullBlock;
            this.requiresRecalculation = requiresRecalculation;
        }

        private SingleBlockLevelReader createLevelReaderFor(final LevelAccessor levelAccessor, final BlockInformation blockInformation)
        {
            return new SingleBlockLevelReader.Builder()
                .withBlockState(blockInformation.blockState())
                .withBlockEntity(() -> blockInformation.newBlockEntity(inWorldPos))
                .withPos(inWorldPos)
                .withSource(levelAccessor)
                .createSingleBlockLevelReader();
        }

        private <T> T usingLevelReader(
            final LevelAccessor levelAccessor,
            final BlockInformation blockInformation,
            final BiFunction<LevelReader, BlockPos, T> getter
        )
        {
            return getter.apply(createLevelReaderFor(levelAccessor, blockInformation), inWorldPos);
        }

        @SuppressWarnings("SameParameterValue")
        private <T, G> T usingLevelReader(
            final LevelAccessor levelAccessor,
            final BlockInformation blockInformation,
            final @Nullable G value,
            final TriFunction<LevelReader, BlockPos, @Nullable G, T> getter
        )
        {
            return getter.apply(createLevelReaderFor(levelAccessor, blockInformation), inWorldPos, value);
        }

        public BlockPos getInWorldPos()
        {
            return inWorldPos;
        }

        public Table<Integer, Integer, ColumnStatistics> getColumnStatisticsTable()
        {
            return columnStatisticsTable;
        }

        public Map<CollisionType, BitSet> getCollisionData()
        {
            return collisionData;
        }

        public int getTotalUsedBlockCount()
        {
            return totalUsedBlockCount;
        }

        public int getTotalUsedChecksWeakPowerCount()
        {
            return totalUsedChecksWeakPowerCount;
        }

        public int getTotalLightLevel()
        {
            return totalLightLevel;
        }

        public int getTotalLightBlockLevel()
        {
            return totalLightBlockLevel;
        }

        @Override
        public BlockInformation getPrimaryState()
        {
            return primaryState;
        }

        @Override
        public boolean isEmpty()
        {
            return this.countMap.size() == 1 && this.countMap.getOrDefault(BlockInformation.AIR, 0) == 4096;
        }

        @Override
        public Map<BlockInformation, Integer> getStateCounts()
        {
            return Collections.unmodifiableMap(countMap);
        }

        @Override
        public boolean shouldCheckWeakPower()
        {
            return totalUsedChecksWeakPowerCount == totalUsedBlockCount;
        }

        @Override
        public float getFullnessFactor()
        {
            return totalUsedBlockCount / (float) StateEntrySize.current().getBitsPerBlock();
        }

        @Override
        public float getSlipperiness()
        {
            return (float) this.columnStatisticsTable.values()
                .stream()
                .filter(Objects::nonNull)
                .filter(columnStatistics -> columnStatistics.getHighestBit() >= 0)
                .mapToDouble(ColumnStatistics::getHighestBitFriction).average().orElse(0.0);
        }

        @Override
        public float getLightEmissionFactor()
        {
            if (emitsLightBasedOnFullBlock)
            {
                return this.totalLightLevel / (float) StateEntrySize.current().getBitsPerBlock();
            }

            return this.totalLightLevel / (float) this.totalUsedBlockCount;
        }

        @Override
        public float getLightBlockingFactor()
        {
            return this.totalLightBlockLevel / (float) StateEntrySize.current().getBitsPerBlock();
        }

        @Override
        public float getRelativeBlockHardness(final Player player)
        {
            final double totalRelativeHardness = (this.countMap.entrySet().stream()
                .mapToDouble(entry -> (double) entry.getKey().blockState().getDestroyProgress(
                    player,
                    createLevelReaderFor(player.level(), entry.getKey()),
                    inWorldPos
                ) * entry.getValue())
                .filter(Double::isFinite)
                .sum());

            if (totalRelativeHardness == 0 || Double.isNaN(totalRelativeHardness) || Double.isInfinite(totalRelativeHardness))
            {
                return 0;
            }

            return (float) (totalRelativeHardness / totalUsedBlockCount);
        }

        @Override
        public boolean canPropagateSkylight()
        {
            return columnStatisticsTable.values()
                .stream().filter(Objects::nonNull).allMatch(ColumnStatistics::canPropagateSkylightDown);
        }

        @Override
        public boolean canSustainGrassBelow()
        {
            return columnStatisticsTable.values()
                .stream().filter(Objects::nonNull).anyMatch(ColumnStatistics::canLowestBitSustainGrass);
        }

        public BitSet getCollideableEntries(final CollisionType collisionType, StateEntryStorage storage)
        {
            return collisionData.computeIfAbsent(collisionType, type ->
            {
                final BitSet bitSet = new BitSet(StateEntrySize.current().getBitsPerBlock());
                BlockPosForEach.forEachInRange(StateEntrySize.current().getBitsPerBlockSide(), blockPos -> bitSet.set(
                    BlockPosUtils.getCollisionIndex(blockPos),
                    type.isValidFor(storage.getBlockInformation(blockPos.getX(), blockPos.getY(), blockPos.getZ()).blockState())
                ));

                return bitSet;
            });
        }

        private void onBlockStateAdded(final LevelAccessor levelAccessor, final BlockInformation blockInformation, final BlockPos inAreaPos, final boolean updateWorld)
        {
            countMap.putIfAbsent(blockInformation, 0);
            countMap.computeIfPresent(blockInformation, (state, currentCount) -> currentCount + 1);

            updatePrimaryState(levelAccessor, updateWorld);

            this.totalUsedBlockCount++;

            if (usingLevelReader(levelAccessor, blockInformation, Direction.NORTH, ILevelBasedPropertyAccessor.getInstance()::shouldCheckWeakPower))
            {
                this.totalUsedChecksWeakPowerCount++;
            }

            this.totalLightLevel += usingLevelReader(levelAccessor, blockInformation, ILevelBasedPropertyAccessor.getInstance()::getLightEmission);
            this.totalLightBlockLevel += usingLevelReader(levelAccessor, blockInformation, LIGHT_BLOCK_GETTER);

            if (!this.columnStatisticsTable.contains(inAreaPos.getX(), inAreaPos.getZ()))
            {
                this.columnStatisticsTable.put(inAreaPos.getX(), inAreaPos.getZ(), new ColumnStatistics(inWorldPos));
            }

            Objects.requireNonNull(this.columnStatisticsTable.get(inAreaPos.getX(), inAreaPos.getZ())).onBlockStateAdded(levelAccessor, blockInformation, inAreaPos);

            this.collisionData.forEach((collisionType, bitSet) -> bitSet.set(BlockPosUtils.getCollisionIndex(inAreaPos), collisionType.isValidFor(blockInformation.blockState())));
        }

        private void updatePrimaryState(LevelAccessor levelAccessor, final boolean updateWorld)
        {
            final BlockInformation currentPrimary = primaryState;
            primaryState = this.countMap.entrySet()
                .stream()
                .filter(e -> !e.getKey().isAir())
                .min((o1, o2) -> -1 * (o1.getValue() - o2.getValue()))
                .map(Map.Entry::getKey)
                .orElse(BlockInformation.AIR);

            final boolean primaryIsAir = this.primaryState.isAir();

            if ((this.countMap.getOrDefault(primaryState, 0) == StateEntrySize.current().getBitsPerBlock() || primaryIsAir || currentPrimary != primaryState) && updateWorld)
            {
                if (primaryIsAir)
                {
                    levelAccessor.setBlock(
                        inWorldPos,
                        Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_ALL
                    );
                }
                else if (this.countMap.getOrDefault(primaryState, 0) == StateEntrySize.current().getBitsPerBlock())
                {
                    IStateVariantManager.getInstance().setFullBlock(levelAccessor, inWorldPos, primaryState);
                }
                else if (currentPrimary != primaryState)
                {
                    final Optional<Block> optionalWithConvertedBlock = IConversionManager.getInstance().getChiseledVariantOf(this.primaryState.blockState());
                    optionalWithConvertedBlock.ifPresent(convertedBlock -> levelAccessor.setBlock(
                        inWorldPos,
                        convertedBlock.defaultBlockState(),
                        Block.UPDATE_ALL
                    ));
                }
            }
        }

        private void onBlockStateRemoved(LevelAccessor levelAccessor, final BlockInformation blockInformation, final BlockPos inAreaPos, final boolean updateWorld)
        {
            countMap.computeIfPresent(blockInformation, (state, currentCount) -> currentCount - 1);
            countMap.remove(blockInformation, 0);
            updatePrimaryState(levelAccessor, updateWorld);

            this.totalUsedBlockCount--;

            if (usingLevelReader(levelAccessor, blockInformation, Direction.NORTH, ILevelBasedPropertyAccessor.getInstance()::shouldCheckWeakPower))
            {
                this.totalUsedChecksWeakPowerCount--;
            }

            this.totalLightLevel -= usingLevelReader(levelAccessor, blockInformation, ILevelBasedPropertyAccessor.getInstance()::getLightEmission);
            this.totalLightBlockLevel -= usingLevelReader(levelAccessor, blockInformation, LIGHT_BLOCK_GETTER);

            if (!this.columnStatisticsTable.contains(inAreaPos.getX(), inAreaPos.getZ()))
            {
                this.columnStatisticsTable.put(inAreaPos.getX(), inAreaPos.getZ(), new ColumnStatistics(inWorldPos));
            }

            Objects.requireNonNull(this.columnStatisticsTable.get(inAreaPos.getX(), inAreaPos.getZ())).onBlockStateRemoved(levelAccessor, blockInformation, inAreaPos);
            this.collisionData.forEach((collisionType, bitSet) -> bitSet.set(BlockPosUtils.getCollisionIndex(inAreaPos), collisionType.isValidFor(Blocks.AIR.defaultBlockState())));
        }

        private void onBlockStateReplaced(
            LevelAccessor levelAccessor,
            final BlockInformation currentInformation,
            final BlockInformation newInformation,
            final BlockPos pos,
            final boolean updateWorld)
        {
            onBlockStateRemoved(levelAccessor, currentInformation, pos, false);
            onBlockStateAdded(levelAccessor, newInformation, pos, updateWorld);
        }

        private void initializeWith(LevelAccessor levelAccessor, final BlockInformation blockInformation)
        {
            clear();
            final boolean isAir = blockInformation.isAir();

            this.primaryState = blockInformation;
            if (!isAir)
            {
                this.countMap.put(blockInformation, StateEntrySize.current().getBitsPerBlock());
            }
            this.totalUsedBlockCount = isAir ? 0 : StateEntrySize.current().getBitsPerBlock();

            if (usingLevelReader(levelAccessor, blockInformation, Direction.NORTH, ILevelBasedPropertyAccessor.getInstance()::shouldCheckWeakPower))
            {
                this.totalUsedChecksWeakPowerCount = StateEntrySize.current().getBitsPerBlock();
            }

            this.totalLightLevel += usingLevelReader(levelAccessor, blockInformation, ILevelBasedPropertyAccessor.getInstance()::getLightEmission)
                * StateEntrySize.current().getBitsPerBlock();

            this.totalLightBlockLevel += usingLevelReader(levelAccessor, blockInformation, LIGHT_BLOCK_GETTER)
                * StateEntrySize.current().getBitsPerBlock();

            this.columnStatisticsTable.clear();
            IntStream.range(0, StateEntrySize.current().getBitsPerBlockSide())
                .forEach(x -> IntStream.range(0, StateEntrySize.current().getBitsPerBlockSide())
                    .forEach(z ->
                    {
                        final ColumnStatistics columnStatistics = new ColumnStatistics(inWorldPos);
                        columnStatistics.initializeWith(levelAccessor, blockInformation);
                        this.columnStatisticsTable.put(x, z, columnStatistics);
                    }));

            this.collisionData.clear();
            for (final CollisionType collisionType : CollisionType.values())
            {
                final boolean matches = collisionType.isValidFor(blockInformation.blockState());
                final BitSet set = new BitSet(StateEntrySize.current().getBitsPerBlock());
                set.set(0, StateEntrySize.current().getBitsPerBlock(), matches);
                this.collisionData.put(collisionType, set);
            }
        }

        private void clear()
        {
            this.primaryState = BlockInformation.AIR;

            this.countMap.clear();
            this.columnStatisticsTable.clear();
            this.collisionData.clear();

            this.totalUsedBlockCount = 0;
            this.totalUsedChecksWeakPowerCount = 0;
            this.totalLightLevel = 0;
            this.totalLightBlockLevel = 0;
        }

        public boolean isRequiresRecalculation()
        {
            return requiresRecalculation;
        }

        private void recalculate(LevelAccessor levelAccessor, final StateEntryStorage source, BlockPos position)
        {
            recalculate(levelAccessor, source, position, true);
        }

        private void recalculate(LevelAccessor levelAccessor, final StateEntryStorage source, BlockPos position, final boolean mayUpdateWorld)
        {
            if (!mayUpdateWorld)
            {
                this.requiresRecalculation = true;
                return;
            }

            this.inWorldPos = position;

            this.requiresRecalculation = false;
            clear();

            source.count(countMap::put);
            countMap.remove(BlockInformation.AIR);
            updatePrimaryState(levelAccessor, mayUpdateWorld);

            this.totalUsedBlockCount = countMap.values().stream().mapToInt(i -> i).sum();

            countMap.forEach((blockState, count) ->
            {
                if (usingLevelReader(levelAccessor, blockState, Direction.NORTH, ILevelBasedPropertyAccessor.getInstance()::shouldCheckWeakPower))
                {
                    this.totalUsedChecksWeakPowerCount += count;
                }

                this.totalLightLevel += usingLevelReader(levelAccessor, blockState, ILevelBasedPropertyAccessor.getInstance()::getLightEmission)
                    * count;

                this.totalLightBlockLevel += usingLevelReader(levelAccessor, blockState, LIGHT_BLOCK_GETTER)
                    * count;
            });

            BlockPosStreamProvider.getForRange(StateEntrySize.current().getBitsPerBlockSide())
                .forEach(pos ->
                {
                    final BlockInformation blockState = source.getBlockInformation(pos.getX(), pos.getY(), pos.getZ());

                    if (!this.columnStatisticsTable.contains(pos.getX(), pos.getZ()))
                    {
                        this.columnStatisticsTable.put(pos.getX(), pos.getZ(), new ColumnStatistics(inWorldPos));
                    }

                    Objects.requireNonNull(this.columnStatisticsTable.get(pos.getX(), pos.getZ())).onBlockStateAdded(levelAccessor, blockState, pos);
                });

            this.collisionData.clear();
            for (final CollisionType collisionType : CollisionType.values())
            {
                getCollideableEntries(collisionType, source);
            }
        }

        public boolean isCanBeFlooded()
        {
            return canBeFlooded;
        }

        public void setCanBeFlooded(boolean canBeFlooded)
        {
            this.canBeFlooded = canBeFlooded;
        }

        public boolean isEmitsLightBasedOnFullBlock()
        {
            return emitsLightBasedOnFullBlock;
        }

        public void setEmitsLightBasedOnFullBlock(boolean emitsLightBasedOnFullBlock)
        {
            this.emitsLightBasedOnFullBlock = emitsLightBasedOnFullBlock;
        }

        @Override
        public Codec<MutableStatistics> codec()
        {
            return CODEC;
        }

        @Override
        public MapCodec<MutableStatistics> mapCodec()
        {
            return MAP_CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MutableStatistics> streamCodec()
        {
            return STREAM_CODEC;
        }
    }

    private static final class ColumnStatistics implements Serializable.Registry<ColumnStatistics>
    {

        private static final BiFunction<LevelReader, BlockPos, Boolean> BLOCKS_SKYLIGHT_GETTER = (levelReader, pos) -> levelReader.getBlockState(pos).propagatesSkylightDown();

        public static final Codec<ColumnStatistics> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf(NbtConstants.POSITION).forGetter(ColumnStatistics::getInWorldPos),
            CBCodecs.BIT_SET.fieldOf(NbtConstants.SKYLIGHT_BLOCKING_BITS).forGetter(ColumnStatistics::getSkylightBlockingBits),
            CBCodecs.BIT_SET.fieldOf(NbtConstants.NONE_AIR_BITS).forGetter(ColumnStatistics::getNoneAirBits),
            Codec.SHORT.fieldOf(NbtConstants.HIGHEST_BIT).forGetter(ColumnStatistics::getHighestBit),
            Codec.FLOAT.fieldOf(NbtConstants.HIGHEST_BIT_FRICTION).forGetter(ColumnStatistics::getHighestBitFriction),
            Codec.BOOL.fieldOf(NbtConstants.CAN_PROPAGATE_SKYLIGHT_DOWN).forGetter(ColumnStatistics::canPropagateSkylightDown),
            Codec.BOOL.fieldOf(NbtConstants.CAN_LOWEST_BIT_SUSTAIN_GRASS).forGetter(ColumnStatistics::canLowestBitSustainGrass)
        ).apply(instance, ColumnStatistics::new));

        public static final MapCodec<ColumnStatistics> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockPos.CODEC.fieldOf(NbtConstants.POSITION).forGetter(ColumnStatistics::getInWorldPos),
            CBCodecs.BIT_SET.fieldOf(NbtConstants.SKYLIGHT_BLOCKING_BITS).forGetter(ColumnStatistics::getSkylightBlockingBits),
            CBCodecs.BIT_SET.fieldOf(NbtConstants.NONE_AIR_BITS).forGetter(ColumnStatistics::getNoneAirBits),
            Codec.SHORT.fieldOf(NbtConstants.HIGHEST_BIT).forGetter(ColumnStatistics::getHighestBit),
            Codec.FLOAT.fieldOf(NbtConstants.HIGHEST_BIT_FRICTION).forGetter(ColumnStatistics::getHighestBitFriction),
            Codec.BOOL.fieldOf(NbtConstants.CAN_PROPAGATE_SKYLIGHT_DOWN).forGetter(ColumnStatistics::canPropagateSkylightDown),
            Codec.BOOL.fieldOf(NbtConstants.CAN_LOWEST_BIT_SUSTAIN_GRASS).forGetter(ColumnStatistics::canLowestBitSustainGrass)
        ).apply(instance, ColumnStatistics::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ColumnStatistics> STREAM_CODEC = CBStreamCodecs.composite(
            BlockPos.STREAM_CODEC,
            ColumnStatistics::getInWorldPos,
            CBStreamCodecs.BIT_SET,
            ColumnStatistics::getSkylightBlockingBits,
            CBStreamCodecs.BIT_SET,
            ColumnStatistics::getNoneAirBits,
            ByteBufCodecs.SHORT,
            ColumnStatistics::getHighestBit,
            ByteBufCodecs.FLOAT,
            ColumnStatistics::getHighestBitFriction,
            ByteBufCodecs.BOOL,
            ColumnStatistics::canPropagateSkylightDown,
            ByteBufCodecs.BOOL,
            ColumnStatistics::canLowestBitSustainGrass,
            ColumnStatistics::new
        );

        private final BlockPos inWorldPos;
        private final BitSet   skylightBlockingBits;
        private final BitSet   noneAirBits;

        private short   highestBit               = -1;
        private float   highestBitFriction       = 0f;
        private boolean canPropagateSkylightDown = true;
        private boolean canLowestBitSustainGrass = true;

        private ColumnStatistics(BlockPos inWorldPos)
        {
            this.inWorldPos = inWorldPos;
            skylightBlockingBits = new BitSet(StateEntrySize.current().getBitsPerBlockSide());
            noneAirBits = new BitSet(StateEntrySize.current().getBitsPerBlockSide());
        }

        public ColumnStatistics(
            BlockPos inWorldPos,
            BitSet noneAirBits,
            BitSet skylightBlockingBits,
            short highestBit,
            float highestBitFriction,
            boolean canPropagateSkylightDown,
            boolean canLowestBitSustainGrass)
        {
            this.inWorldPos = inWorldPos;
            this.noneAirBits = noneAirBits;
            this.skylightBlockingBits = skylightBlockingBits;
            this.highestBit = highestBit;
            this.highestBitFriction = highestBitFriction;
            this.canPropagateSkylightDown = canPropagateSkylightDown;
            this.canLowestBitSustainGrass = canLowestBitSustainGrass;
        }

        public BlockPos getInWorldPos()
        {
            return inWorldPos;
        }

        public BitSet getSkylightBlockingBits()
        {
            return skylightBlockingBits;
        }

        public BitSet getNoneAirBits()
        {
            return noneAirBits;
        }

        public short getHighestBit()
        {
            return highestBit;
        }

        public float getHighestBitFriction()
        {
            return highestBitFriction;
        }

        public boolean canPropagateSkylightDown()
        {
            return canPropagateSkylightDown;
        }

        public boolean canLowestBitSustainGrass()
        {
            return canLowestBitSustainGrass;
        }

        private SingleBlockLevelReader createLevelReaderFor(final LevelAccessor levelAccessor, final BlockInformation blockInformation)
        {
            return new SingleBlockLevelReader.Builder()
                .withBlockState(blockInformation.blockState())
                .withBlockEntity(() -> blockInformation.newBlockEntity(inWorldPos))
                .withPos(inWorldPos)
                .withSource(levelAccessor)
                .createSingleBlockLevelReader();
        }

        @SuppressWarnings("SameParameterValue")
        private <T> T usingLevelReader(
            final LevelAccessor levelAccessor,
            final BlockInformation blockInformation,
            final BiFunction<LevelReader, BlockPos, T> getter
        )
        {
            return getter.apply(createLevelReaderFor(levelAccessor, blockInformation), inWorldPos);
        }

        private <T, G> T usingLevelReader(
            final LevelAccessor levelAccessor,
            final BlockInformation blockInformation,
            final TriFunction<LevelReader, BlockPos, @Nullable G, T> getter
        )
        {
            return getter.apply(createLevelReaderFor(levelAccessor, blockInformation), inWorldPos, null);
        }

        private Optional<Boolean> canGrowGrassUsingLevelReader(
            final LevelAccessor levelAccessor,
            final BlockInformation blockInformation
        )
        {
            return ILevelBasedPropertyAccessor.getInstance().canBeGrass(createLevelReaderFor(levelAccessor, blockInformation),
                Blocks.GRASS_BLOCK.defaultBlockState(),
                inWorldPos.below(),
                blockInformation.blockState(),
                inWorldPos);
        }

        private Integer getLightBlockInto(
            final BlockInformation blockInformation)
        {
            return LightEngine.getLightBlockInto(
                Blocks.GRASS_BLOCK.defaultBlockState(),
                blockInformation.blockState(),
                Direction.UP,
                blockInformation.blockState().getLightBlock()
            );
        }

        private boolean canLowestBitSustainGrass(LevelAccessor levelAccessor, BlockInformation blockState)
        {
            return canGrowGrassUsingLevelReader(levelAccessor, blockState)
                .orElseGet(() ->
                {
                    if (blockState.blockState().is(Blocks.SNOW) && blockState.blockState().getValue(SnowLayerBlock.LAYERS) == 1)
                    {
                        return true;
                    }
                    else if (blockState.blockState().getFluidState().getAmount() == 8)
                    {
                        return false;
                    }
                    else
                    {
                        int i = getLightBlockInto(blockState);
                        return i < LightEngine.MAX_LEVEL;
                    }
                });
        }

        private void onBlockStateAdded(LevelAccessor levelAccessor, final BlockInformation blockState, final BlockPos inAreaPos)
        {
            skylightBlockingBits.set(inAreaPos.getY(), !usingLevelReader(levelAccessor, blockState, BLOCKS_SKYLIGHT_GETTER));

            if (skylightBlockingBits.get(inAreaPos.getY()))
            {
                canPropagateSkylightDown = false;
            }

            if (!blockState.isAir() && inAreaPos.getY() >= highestBit)
            {
                highestBit = (short) inAreaPos.getY();
                highestBitFriction = usingLevelReader(levelAccessor, blockState, ILevelBasedPropertyAccessor.getInstance()::getFriction);
            }

            if (inAreaPos.getY() == 0)
            {
                canLowestBitSustainGrass = canLowestBitSustainGrass(levelAccessor, blockState);
            }
        }

        private void onBlockStateRemoved(LevelAccessor levelAccessor, final BlockInformation blockInformation, final BlockPos pos)
        {
            skylightBlockingBits.set(pos.getY(), !usingLevelReader(levelAccessor, blockInformation, BLOCKS_SKYLIGHT_GETTER));

            if (!skylightBlockingBits.get(pos.getY()))
            {
                canPropagateSkylightDown = IntStream.range(0, StateEntrySize.current().getBitsPerBlockSide())
                    .noneMatch(skylightBlockingBits::get);
            }

            if (pos.getY() >= highestBit)
            {
                highestBit = -1;
                highestBitFriction = 0f;

                for (int i = StateEntrySize.current().getBitsPerBlockSide() - 1; i >= 0; i--)
                {
                    if (noneAirBits.get(i))
                    {
                        highestBit = (short) i;
                        highestBitFriction = usingLevelReader(levelAccessor, blockInformation, ILevelBasedPropertyAccessor.getInstance()::getFriction);
                        break;
                    }
                }
            }

            if (pos.getY() == 0)
            {
                this.canLowestBitSustainGrass = true;
            }
        }

        private void initializeWith(LevelAccessor levelAccessor, final BlockInformation blockInformation)
        {
            skylightBlockingBits.clear();
            noneAirBits.clear();

            skylightBlockingBits.set(0,
                StateEntrySize.current().getBitsPerBlockSide(),
                !usingLevelReader(levelAccessor, blockInformation, BLOCKS_SKYLIGHT_GETTER));
            noneAirBits.set(0, !blockInformation.isAir());

            if (blockInformation.isAir())
            {
                highestBit = -1;
                highestBitFriction = 0f;
            }
            else
            {
                highestBit = (short) (StateEntrySize.current().getBitsPerBlockSide() - 1);
                highestBitFriction = usingLevelReader(levelAccessor, blockInformation, ILevelBasedPropertyAccessor.getInstance()::getFriction);
            }

            this.canPropagateSkylightDown = usingLevelReader(levelAccessor, blockInformation, BLOCKS_SKYLIGHT_GETTER);
            this.canLowestBitSustainGrass = blockInformation.isAir() || canLowestBitSustainGrass(levelAccessor, blockInformation);
        }

        @Override
        public Codec<ColumnStatistics> codec()
        {
            return CODEC;
        }

        @Override
        public MapCodec<ColumnStatistics> mapCodec()
        {
            return MAP_CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ColumnStatistics> streamCodec()
        {
            return STREAM_CODEC;
        }
    }

    public record Payload(
        StateEntryStorage storage,
        MutableStatistics mutableStatistics)
    {

        public static final Codec<Payload> CODEC = CBCodecs.compressed(
            RecordCodecBuilder.create(instance -> instance.group(
                StateEntryStorage.CODEC.fieldOf(NbtConstants.STORAGE).forGetter(Payload::storage),
                MutableStatistics.CODEC.fieldOf(NbtConstants.STATISTICS).forGetter(Payload::mutableStatistics)
            ).apply(instance, Payload::new))
        );

        private static final Codec<Payload> LEGACY_CODEC = CompressedDataFindingCodec.of(CBCodecs.readLegacyCompressed(
            RecordCodecBuilder.create(instance -> instance.group(
                StateEntryStorage.LEGACY_CODEC.fieldOf(NbtConstants.LEGACY_CHISELED_DATA).forGetter(Payload::storage),
                MutableStatistics.LEGACY_CODEC.fieldOf(NbtConstants.STATISTICS).forGetter(Payload::mutableStatistics)
            ).apply(instance, Payload::new))
        ));

        public static final MapCodec<Payload> MAP_CODEC = CODEC.fieldOf(NbtConstants.PAYLOAD);

        private static final MapCodec<Payload> LEGACY_MAP_CODEC = LEGACY_CODEC.fieldOf(NbtConstants.DATA);

        public static final StreamCodec<RegistryFriendlyByteBuf, Payload> STREAM_CODEC = StreamCodec.composite(
            StateEntryStorage.STREAM_CODEC,
            Payload::storage,
            MutableStatistics.STREAM_CODEC,
            Payload::mutableStatistics,
            Payload::new
        );
    }

    private record ServerSchedulingExecutor(MinecraftServer server) implements Executor
    {

        @Override
        public void execute(@NotNull Runnable command)
        {
            server.schedule(new TickTask(server.getTickCount(), command));
        }
    }
}

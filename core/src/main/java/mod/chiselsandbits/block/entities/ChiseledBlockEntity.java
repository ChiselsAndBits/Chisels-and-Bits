package mod.chiselsandbits.block.entities;

import com.communi.suggestu.scena.core.IScenaPlatform;
import com.communi.suggestu.scena.core.blockstate.ILevelBasedPropertyAccessor;
import com.communi.suggestu.scena.core.client.models.data.IBlockModelData;
import com.communi.suggestu.scena.core.client.models.data.IModelDataBuilder;
import com.communi.suggestu.scena.core.dist.DistExecutor;
import com.communi.suggestu.scena.core.entity.block.IBlockEntityWithModelData;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import mod.chiselsandbits.ChiselsAndBits;
import mod.chiselsandbits.api.axissize.CollisionType;
import mod.chiselsandbits.api.block.entity.IMultiStateBlockEntity;
import mod.chiselsandbits.api.block.entity.INetworkUpdateableEntity;
import mod.chiselsandbits.api.block.storage.IStateEntryStorage;
import mod.chiselsandbits.api.blockinformation.IBlockInformation;
import mod.chiselsandbits.blockinformation.BlockInformation;
import mod.chiselsandbits.api.change.IChangeTracker;
import mod.chiselsandbits.api.chiseling.conversion.IConversionManager;
import mod.chiselsandbits.api.chiseling.eligibility.IEligibilityManager;
import mod.chiselsandbits.api.exceptions.SpaceOccupiedException;
import mod.chiselsandbits.api.multistate.StateEntrySize;
import mod.chiselsandbits.api.multistate.accessor.IStateEntryInfo;
import mod.chiselsandbits.api.multistate.accessor.identifier.IAreaShapeIdentifier;
import mod.chiselsandbits.api.multistate.accessor.identifier.IArrayBackedAreaShapeIdentifier;
import mod.chiselsandbits.api.multistate.accessor.sortable.IPositionMutator;
import mod.chiselsandbits.api.multistate.mutator.IMutableStateEntryInfo;
import mod.chiselsandbits.api.util.IBatchMutation;
import mod.chiselsandbits.api.multistate.mutator.callback.StateClearer;
import mod.chiselsandbits.api.multistate.mutator.callback.StateSetter;
import mod.chiselsandbits.api.multistate.mutator.world.IInWorldMutableStateEntryInfo;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshot;
import mod.chiselsandbits.api.multistate.statistics.IMultiStateObjectStatistics;
import mod.chiselsandbits.api.util.*;
import mod.chiselsandbits.api.util.constants.NbtConstants;
import mod.chiselsandbits.block.entities.storage.SimpleStateEntryStorage;
import mod.chiselsandbits.client.model.data.ChiseledBlockModelDataManager;
import mod.chiselsandbits.network.packets.TileEntityUpdatedPacket;
import mod.chiselsandbits.registrars.ModBlockEntityTypes;
import mod.chiselsandbits.storage.IMultiThreadedStorageEngine;
import mod.chiselsandbits.storage.IStorageHandler;
import mod.chiselsandbits.storage.StorageEngineBuilder;
import mod.chiselsandbits.utils.BlockPosUtils;
import mod.chiselsandbits.utils.LZ4DataCompressionUtils;
import mod.chiselsandbits.utils.MultiStateSnapshotUtils;
import mod.chiselsandbits.voxelshape.MultiStateBlockEntityDiscreteVoxelShape;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CubeVoxelShape;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ChiseledBlockEntity extends BlockEntity implements
        IMultiStateBlockEntity, INetworkUpdateableEntity, IBlockEntityWithModelData {
    public static final float ONE_THOUSANDS = 1 / 1000f;

    private MutableStatistics mutableStatistics;
    private final Map<UUID, IBatchMutation> batchMutations = Maps.newConcurrentMap();
    private final Object tagSyncHandle = new Object();
    private IStateEntryStorage storage;
    private IMultiThreadedStorageEngine storageEngine;
    private boolean isInitialized = false;
    private IBlockModelData modelData = IModelDataBuilder.create().build();
    private CompoundTag lastTag = null;
    private CompletableFuture<Void> storageFuture = null;
    private final List<CompoundTag> deserializationQueue = Collections.synchronizedList(Lists.newArrayList());

    public ChiseledBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntityTypes.CHISELED.get(), position, state);
        storage = new SimpleStateEntryStorage();
        mutableStatistics = new MutableStatistics(this::getLevel, this::getBlockPos);

        createStorageEngine();
    }

    @NotNull
    private static Executor createDefaultExecutor() {
        return DistExecutor.unsafeRunForDist(
                () -> Minecraft::getInstance,
                () -> () -> new ServerSchedulingExecutor(IScenaPlatform.getInstance().getCurrentServer())
        );
    }

    private void createStorageEngine() {
        storageEngine = StorageEngineBuilder.create()
                .with(new LZ4StorageBasedStorageHandler())
                .buildMultiThreaded(getExecutor());
    }

    private Executor getExecutor() {
        if (getLevel() != null && getLevel().getServer() != null)
            return new ServerSchedulingExecutor(getLevel().getServer());

        return createDefaultExecutor();
    }

    public void updateModelData() {
        ChiseledBlockModelDataManager.getInstance().updateModelData(this);
    }

    private void updateModelDataIfInLoadedChunk() {
        if (level != null && level.isClientSide() && level.isLoaded(getBlockPos())) {
            updateModelData();
            level.getLightEngine().checkBlock(getBlockPos());
        }
    }

    @Override
    public void setLevel(@Nullable final Level level) {
        super.setLevel(level);

        createStorageEngine();

        if (this.deserializationQueue.isEmpty())
            return;

        this.deserializationQueue.forEach(this::deserializeNBT);
    }

    @Override
    public IAreaShapeIdentifier createNewShapeIdentifier() {
        return new Identifier(this.storage);
    }

    @Override
    public Stream<IStateEntryInfo> stream() {
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
    public boolean isInside(final Vec3 inAreaTarget) {
        return !(inAreaTarget.x() < 0) &&
                !(inAreaTarget.y() < 0) &&
                !(inAreaTarget.z() < 0) &&
                !(inAreaTarget.x() >= 1) &&
                !(inAreaTarget.y() >= 1) &&
                !(inAreaTarget.z() >= 1);
    }

    @Override
    public Optional<IStateEntryInfo> getInAreaTarget(final Vec3 inAreaTarget) {
        if (inAreaTarget.x() < 0 ||
                inAreaTarget.y() < 0 ||
                inAreaTarget.z() < 0 ||
                inAreaTarget.x() >= 1 ||
                inAreaTarget.y() >= 1 ||
                inAreaTarget.z() >= 1) {
            throw new IllegalArgumentException("Target is not in the current area.");
        }

        final Vec3 exactAreaPos = inAreaTarget.multiply(StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide());
        final BlockPos inAreaPos = VectorUtils.toBlockPos(exactAreaPos);

        final IBlockInformation blockInformation = this.storage.getBlockInformation(
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
    public boolean isInside(final BlockPos inAreaBlockPosOffset, final Vec3 inBlockTarget) {
        if (!inAreaBlockPosOffset.equals(BlockPos.ZERO)) {
            return false;
        }

        return this.isInside(
                inBlockTarget
        );
    }

    @Override
    public Optional<IStateEntryInfo> getInBlockTarget(final BlockPos inAreaBlockPosOffset, final Vec3 inBlockTarget) {
        if (!inAreaBlockPosOffset.equals(BlockPos.ZERO)) {
            throw new IllegalStateException(String.format("The given in area block pos offset is not inside the current block: %s", inAreaBlockPosOffset));
        }

        return this.getInAreaTarget(
                inBlockTarget
        );
    }

    @Override
    public IMultiStateSnapshot createSnapshot() {
        return MultiStateSnapshotUtils.createFromStorage(this.storage);
    }

    @Override
    public void load(@NotNull final CompoundTag nbt) {
        if (this.getLevel() != null)
            this.deserializeNBT(nbt);

        this.queueDeserializeNbt(nbt);
    }

    private void queueDeserializeNbt(CompoundTag nbt) {
        this.deserializationQueue.add(nbt);
    }

    @Override
    public void deserializeNBT(final CompoundTag nbt) {
       this.deserializeNBT(nbt, this::updateModelDataIfInLoadedChunk);
    }

    public void deserializeNBT(final CompoundTag nbt, Runnable onLoaded) {
        this.storageEngine.deserializeOffThread(nbt)
                .thenRun(onLoaded)
                .thenRunAsync(() -> {
                    if (mutableStatistics.isRequiresRecalculation()) {
                        mutableStatistics.recalculate(this.storage, shouldUpdateWorld());
                    }

                    mutableStatistics.updatePrimaryState(shouldUpdateWorld());
                }, getExecutor());
        this.lastTag = nbt;
    }

    @Override
    public CompoundTag serializeNBT() {
        return saveWithFullMetadata();
    }

    @Override
    public void saveAdditional(@NotNull final CompoundTag compound) {
        super.saveAdditional(compound);

        synchronized (this.tagSyncHandle) {
            if (this.lastTag != null) {
                //Off-Thread completed.)
                final CompoundTag nbt = this.lastTag.copy();
                nbt.getAllKeys().forEach(key -> compound.put(key, nbt.get(key)));
                return;
            }
        }

        if (this.storageFuture != null) {
            this.storageFuture.join();

            //Now the tag needs to be there!
            Validate.notNull(this.lastTag, "The storage future did not complete.");
            final CompoundTag nbt = this.lastTag.copy();
            nbt.getAllKeys().forEach(key -> compound.put(key, nbt.get(key)));
            return;
        }

        this.storageEngine.serializeNBTInto(compound);
    }
    
    @Override
    public boolean isCanBeFlooded() {
        return mutableStatistics.isCanBeFlooded();
    }
    
    @Override
    public void setCanBeFlooded(boolean canBeFlooded) {
        this.mutableStatistics.setCanBeFlooded(canBeFlooded);
        setChanged();
    }
    
    @Override
    public boolean isEmitsLightBasedOnFullBlock() {
        return mutableStatistics.isEmitsLightBasedOnFullBlock();
    }
    
    @Override
    public void setEmitsLightBasedOnFullBlock(boolean emitsLightBasedOnFullBlock) {
        mutableStatistics.setEmitsLightBasedOnFullBlock(emitsLightBasedOnFullBlock);
        setChanged();
    }

    /**
     * For tile entities, ensures the chunk containing the tile entity is saved to disk later - the game won't think it hasn't changed and skip it.
     */
    @Override
    public void setChanged() {
        if (getLevel() != null && this.batchMutations.isEmpty() && !getLevel().isClientSide()) {
            this.mutableStatistics.updatePrimaryState(true);

            if (!getLevel().isClientSide()) {
                synchronized (this.tagSyncHandle) {
                    if (this.storageFuture != null) {
                        this.storageFuture.cancel(false);
                    }
                    this.lastTag = null;

                    this.storageFuture = this.storageEngine.serializeOffThread(
                            tag -> CompletableFuture.runAsync(
                                    () -> this.setOffThreadSaveResult(tag), this.storageEngine
                            ));

                    ChiselsAndBits.getInstance().getNetworkChannel().sendToTrackingChunk(
                            new TileEntityUpdatedPacket(this),
                            getLevel().getChunkAt(getBlockPos())
                    );
                }
            }
        }

        if (getLevel() != null && this.batchMutations.isEmpty() && getLevel().isLoaded(getBlockPos())) {
            super.setChanged();

            getLevel().getLightEngine().checkBlock(getBlockPos());
            getLevel().sendBlockUpdated(getBlockPos(), Blocks.AIR.defaultBlockState(), getBlockState(), Block.UPDATE_ALL);
            getLevel().updateNeighborsAt(getBlockPos(), getLevel().getBlockState(getBlockPos()).getBlock());
        }
    }


    private void setOffThreadSaveResult(final CompoundTag tag) {
        synchronized (this.tagSyncHandle) {
            this.lastTag = tag;
        }
    }

    private boolean shouldUpdateWorld() {
        return this.getLevel() != null && this.batchMutations.size() == 0 && this.getLevel() instanceof ServerLevel && getLevel().isLoaded(getBlockPos());
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @NotNull
    @Override
    public CompoundTag getUpdateTag() {
        if (isInitialized || lastTag == null)
            return this.storageEngine.serializeNBT();

        return lastTag;
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag) {
        this.load(tag);
    }

    @Override
    public void serializeInto(@NotNull final FriendlyByteBuf packetBuffer) {
        storage.serializeInto(packetBuffer);
        mutableStatistics.serializeInto(packetBuffer);
    }

    @Override
    public void deserializeFrom(@NotNull final FriendlyByteBuf packetBuffer) {
        storage.deserializeFrom(packetBuffer);
        mutableStatistics.deserializeFrom(packetBuffer);
        updateModelDataIfInLoadedChunk();
    }

    /**
     * Returns all entries in the current area in a mutable fashion. Includes all empty areas as areas containing an air state.
     *
     * @return A stream with a mutable state entry info for each mutable section in the area.
     */
    @Override
    public Stream<IMutableStateEntryInfo> mutableStream() {
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

    @SuppressWarnings("deprecation")
    @Override
    public void setInAreaTarget(
            final IBlockInformation newInformation,
            final Vec3 inAreaTarget
    ) throws SpaceOccupiedException {
        if (inAreaTarget.x() < 0 ||
                inAreaTarget.y() < 0 ||
                inAreaTarget.z() < 0 ||
                inAreaTarget.x() >= 1 ||
                inAreaTarget.y() >= 1 ||
                inAreaTarget.z() >= 1) {
            throw new IllegalArgumentException("Target is not in the current area.");
        }

        final Vec3 exactAreaPos = inAreaTarget.multiply(StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide());
        final BlockPos inAreaPos = VectorUtils.toBlockPos(exactAreaPos);

        final IBlockInformation information = this.storage.getBlockInformation(
                inAreaPos.getX(),
                inAreaPos.getY(),
                inAreaPos.getZ()
        );

        if (!information.isAir()) {
            throw new SpaceOccupiedException();
        }

        if (getLevel() == null) {
            return;
        }

        this.storage.setBlockInformation(
                inAreaPos.getX(),
                inAreaPos.getY(),
                inAreaPos.getZ(),
                newInformation
        );

        if (newInformation.isAir() && !information.isAir()) {
            mutableStatistics.onBlockStateRemoved(information, inAreaPos, shouldUpdateWorld());
        } else if (!newInformation.isAir() && information.isAir()) {
            mutableStatistics.onBlockStateAdded(newInformation, inAreaPos, shouldUpdateWorld());
        } else if (!newInformation.isAir() && !information.isAir()) {
            mutableStatistics.onBlockStateReplaced(information, newInformation, inAreaPos, shouldUpdateWorld());
        }

        if (getLevel() != null) {
            setChanged();
        }
    }

    @Override
    public LevelAccessor getWorld() {
        return getLevel();
    }

    @Override
    public Vec3 getInWorldStartPoint() {
        return Vec3.atLowerCornerOf(getBlockPos());
    }

    @Override
    public void setInBlockTarget(final IBlockInformation blockInformation, final BlockPos inAreaBlockPosOffset, final Vec3 inBlockTarget) throws SpaceOccupiedException {
        if (!inAreaBlockPosOffset.equals(BlockPos.ZERO)) {
            throw new IllegalStateException(String.format("The given in area block pos offset is not inside the current block: %s", inAreaBlockPosOffset));
        }

        this.setInAreaTarget(
                blockInformation,
                inBlockTarget);
    }

    @Override
    public Vec3 getInWorldEndPoint() {
        return getInWorldStartPoint().add(1, 1, 1).subtract(ONE_THOUSANDS, ONE_THOUSANDS, ONE_THOUSANDS);
    }

    /**
     * Clears the current area, using the offset from the area as well as the in area target offset.
     *
     * @param inAreaTarget The in area offset.
     */
    @Override
    public void clearInAreaTarget(final Vec3 inAreaTarget) {
        if (inAreaTarget.x() < 0 ||
                inAreaTarget.y() < 0 ||
                inAreaTarget.z() < 0 ||
                inAreaTarget.x() >= 1 ||
                inAreaTarget.y() >= 1 ||
                inAreaTarget.z() >= 1) {
            throw new IllegalArgumentException("Target is not in the current area.");
        }

        final Vec3 exactAreaPos = inAreaTarget.multiply(StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide());
        final BlockPos inAreaPos = VectorUtils.toBlockPos(exactAreaPos);

        if (getLevel() == null) {
            return;
        }

        final IBlockInformation currentInformation = this.storage.getBlockInformation(
                inAreaPos.getX(),
                inAreaPos.getY(),
                inAreaPos.getZ()
        );

        if (currentInformation.isAir()) {
            return;
        }

        if (!IEligibilityManager.getInstance().canBeChiseled(currentInformation)) {
            return;
        }

        final IBlockInformation blockState = BlockInformation.AIR;

        this.storage.setBlockInformation(
                inAreaPos.getX(),
                inAreaPos.getY(),
                inAreaPos.getZ(),
                blockState
        );

        if (blockState.isAir() && !currentInformation.isAir()) {
            mutableStatistics.onBlockStateRemoved(currentInformation, inAreaPos, shouldUpdateWorld());
        } else if (!blockState.isAir() && currentInformation.isAir()) {
            mutableStatistics.onBlockStateAdded(blockState, inAreaPos, shouldUpdateWorld());
        } else if (!blockState.isAir() && !currentInformation.isAir()) {
            mutableStatistics.onBlockStateReplaced(currentInformation, blockState, inAreaPos, shouldUpdateWorld());
        }

        if (getLevel() != null) {
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
    public void clearInBlockTarget(final BlockPos inAreaBlockPosOffset, final Vec3 inBlockTarget) {
        if (!inAreaBlockPosOffset.equals(BlockPos.ZERO)) {
            throw new IllegalStateException(String.format("The given in area block pos offset is not inside the current block: %s", inAreaBlockPosOffset));
        }

        this.clearInAreaTarget(
                inBlockTarget
        );
    }

    @Override
    public IMultiStateObjectStatistics getStatistics() {
        return mutableStatistics;
    }

    @Override
    public void rotate(final Direction.Axis axis, final int rotationCount) {
        if (getLevel() == null) {
            return;
        }

        //Large operation, better batch this together to prevent weird updates.
        try (final IBatchMutation ignored = batch()) {
            this.storage.rotate(axis, rotationCount);
            this.mutableStatistics.recalculate(this.storage);
        }
    }

    @Override
    public void mirror(final Direction.Axis axis) {
        if (getLevel() == null) {
            return;
        }

        //Large operation, better batch this together to prevent weird updates.
        try (final IBatchMutation ignored = batch()) {
            this.storage.mirror(axis);
            this.mutableStatistics.recalculate(this.storage);
        }
    }

    @Override
    public void initializeWith(final IBlockInformation newInitialInformation) {
        if (getLevel() == null) {
            return;
        }

        try (IBatchMutation batchMutation = batch()) {
            this.storage.initializeWith(newInitialInformation);
            this.mutableStatistics.initializeWith(newInitialInformation);
        }
    }

    @Override
    public Stream<IInWorldMutableStateEntryInfo> inWorldMutableStream() {
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
    public Stream<IStateEntryInfo> streamWithPositionMutator(final IPositionMutator positionMutator) {
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
            final IPositionMutator positionMutator, final Consumer<IStateEntryInfo> consumer) {
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

    @Override
    public IBatchMutation batch() {
        final UUID id = UUID.randomUUID();
        final IBatchMutation storageBatch = storage.batch();

        this.batchMutations.put(id, new BatchMutationLock(() ->
        {
            this.batchMutations.remove(id);
            storageBatch.close();

            if (this.batchMutations.isEmpty()) {
                setChanged();
            }
        }));
        return this.batchMutations.get(id);
    }

    @Override
    public IBatchMutation batch(final IChangeTracker changeTracker) {
        final IBatchMutation innerMutation = batch();
        final IMultiStateSnapshot before = this.createSnapshot();
        return () ->
        {
            final IMultiStateSnapshot after = this.createSnapshot();
            innerMutation.close();
            changeTracker.onBlockUpdated(getBlockPos(), before, after);
        };
    }

    public void setModelData(final IBlockModelData modelData) {
        this.modelData = modelData;
    }

    @NotNull
    public IBlockModelData getBlockModelData() {
        return this.modelData;
    }

    @Override
    public VoxelShape provideShape(
            final CollisionType type, final BlockPos offset, final boolean simplify) {
        VoxelShape shape = new CubeVoxelShape(new MultiStateBlockEntityDiscreteVoxelShape(
                this.getStatistics().getCollideableEntries(type)
        ));

        if (offset != BlockPos.ZERO) {
            shape = shape.move(offset.getX(), offset.getY(), offset.getZ());
        }

        if (simplify) {
            shape = shape.optimize();
        }

        return shape;
    }

    @Override
    public @NotNull AABB getBoundingBox() {
        return new AABB(
                this.getBlockPos().getX(),
                this.getBlockPos().getY(),
                this.getBlockPos().getZ(),
                this.getBlockPos().getX() + 1,
                this.getBlockPos().getY() + 1,
                this.getBlockPos().getZ() + 1
        );
    }

    private static final class StateEntry implements IInWorldMutableStateEntryInfo {

        private final IBlockInformation blockInformation;
        private final LevelAccessor reader;
        private final BlockPos blockPos;
        private final Vec3 startPoint;
        private final Vec3 endPoint;

        private final StateSetter stateSetter;
        private final StateClearer stateClearer;

        public StateEntry(
                final IBlockInformation blockInformation,
                final LevelAccessor reader,
                final BlockPos blockPos,
                final Vec3i startPoint,
                final StateSetter stateSetter,
                final StateClearer stateClearer) {
            this(
                    blockInformation,
                    reader,
                    blockPos,
                    Vec3.atLowerCornerOf(startPoint).multiply(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit()),
                    Vec3.atLowerCornerOf(startPoint).multiply(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit()).add(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit()),
                    stateSetter, stateClearer);
        }

        private StateEntry(
                final IBlockInformation blockInformation,
                final LevelAccessor reader,
                final BlockPos blockPos,
                final Vec3 startPoint,
                final Vec3 endPoint,
                final StateSetter stateSetter,
                final StateClearer stateClearer) {
            this.blockInformation = blockInformation;
            this.reader = reader;
            this.blockPos = blockPos;
            this.startPoint = startPoint;
            this.endPoint = endPoint;
            this.stateSetter = stateSetter;
            this.stateClearer = stateClearer;
        }

        @Override
        public @NotNull IBlockInformation getBlockInformation() {
            return blockInformation;
        }

        @Override
        public void setBlockInformation(final IBlockInformation blockState) throws SpaceOccupiedException {
            stateSetter.set(blockState, getStartPoint());
        }

        @Override
        public @NotNull Vec3 getStartPoint() {
            return startPoint;
        }

        @Override
        public @NotNull Vec3 getEndPoint() {
            return endPoint;
        }

        @Override
        public void clear() {
            stateClearer.accept(getStartPoint());
        }

        @Override
        public LevelAccessor getWorld() {
            return reader;
        }

        @Override
        public BlockPos getBlockPos() {
            return blockPos;
        }
    }

    private static final class Identifier implements IArrayBackedAreaShapeIdentifier {
        private final IStateEntryStorage snapshot;

        private Identifier(final IStateEntryStorage section) {
            this.snapshot = section.createSnapshot();
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof final IArrayBackedAreaShapeIdentifier that)) {
                return false;
            }

            return Arrays.equals(this.getBackingData(), that.getBackingData()) &&
                    this.getPalette().equals(that.getPalette());
        }

        @Override
        public int hashCode() {
            return snapshot.hashCode();
        }

        @Override
        public String toString() {
            return "Identifier{" +
                    "snapshot=" + snapshot +
                    '}';
        }

        @Override
        public byte[] getBackingData() {
            return snapshot.getRawData();
        }

        @Override
        public List<IBlockInformation> getPalette() {
            return snapshot.getContainedPalette();
        }
    }

    private record BatchMutationLock(Runnable closeCallback) implements IBatchMutation {

        @Override
        public void close() {
            this.closeCallback.run();
        }
    }

    private final class MutableStatistics implements IMultiStateObjectStatistics, INBTSerializable<CompoundTag>, IPacketBufferSerializable {

        private final Supplier<LevelAccessor> worldReaderSupplier;
        private final Supplier<BlockPos> positionSupplier;
        private final Map<IBlockInformation, Integer> countMap = Maps.newConcurrentMap();
        private final Table<Integer, Integer, ColumnStatistics> columnStatisticsTable = HashBasedTable.create();
        private final Map<CollisionType, BitSet> collisionData = Maps.newConcurrentMap();
        private IBlockInformation primaryState = BlockInformation.AIR;
        private int totalUsedBlockCount = 0;
        private int totalUsedChecksWeakPowerCount = 0;
        private int totalLightLevel = 0;
        private int totalLightBlockLevel = 0;
        private boolean canBeFlooded = true;
        private boolean emitsLightBasedOnFullBlock = false;

        private boolean requiresRecalculation = false;

        private MutableStatistics(final Supplier<LevelAccessor> worldReaderSupplier, final Supplier<BlockPos> positionSupplier) {
            this.worldReaderSupplier = worldReaderSupplier;
            this.positionSupplier = positionSupplier;
        }

        @Override
        public IBlockInformation getPrimaryState() {
            return primaryState;
        }

        @Override
        public boolean isEmpty() {
            return this.countMap.size() == 1 && this.countMap.getOrDefault(BlockInformation.AIR, 0) == 4096;
        }

        @Override
        public Map<IBlockInformation, Integer> getStateCounts() {
            return Collections.unmodifiableMap(countMap);
        }

        @Override
        public boolean shouldCheckWeakPower() {
            return totalUsedChecksWeakPowerCount == totalUsedBlockCount;
        }

        @Override
        public float getFullnessFactor() {
            return totalUsedBlockCount / (float) StateEntrySize.current().getBitsPerBlock();
        }

        @Override
        public float getSlipperiness() {
            return (float) this.columnStatisticsTable.values()
                    .stream()
                    .filter(columnStatistics -> columnStatistics.getHighestBit() >= 0)
                    .mapToDouble(ColumnStatistics::getHighestBitFriction).average().orElse(0.0);
        }

        @Override
        public float getLightEmissionFactor() {
            if (emitsLightBasedOnFullBlock)
                return this.totalLightLevel / (float) StateEntrySize.current().getBitsPerBlock();
            
            return this.totalLightLevel / (float) this.totalUsedBlockCount;
        }

        @Override
        public float getLightBlockingFactor() {
            return this.totalLightBlockLevel / (float) StateEntrySize.current().getBitsPerBlock();
        }

        @Override
        public float getRelativeBlockHardness(final Player player) {
            final double totalRelativeHardness = (this.countMap.entrySet().stream()
                    .mapToDouble(entry -> (double) entry.getKey().getBlockState().getDestroyProgress(
                            player,
                            new SingleBlockWorldReader(
                                    entry.getKey(),
                                    this.positionSupplier.get(),
                                    this.worldReaderSupplier.get()
                            ),
                            this.positionSupplier.get()
                    ) * entry.getValue())
                    .filter(Double::isFinite)
                    .sum());

            if (totalRelativeHardness == 0 || Double.isNaN(totalRelativeHardness) || Double.isInfinite(totalRelativeHardness)) {
                return 0;
            }

            return (float) (totalRelativeHardness / totalUsedBlockCount);
        }

        @Override
        public boolean canPropagateSkylight() {
            return columnStatisticsTable.values()
                    .stream().allMatch(ColumnStatistics::canPropagateSkylightDown);
        }

        @Override
        public boolean canSustainGrassBelow() {
            return columnStatisticsTable.values()
                    .stream().anyMatch(ColumnStatistics::canLowestBitSustainGrass);
        }

        @Override
        public BitSet getCollideableEntries(final CollisionType collisionType) {
            final BitSet collisionDataSet = collisionData.computeIfAbsent(collisionType, type ->
            {
                if (!shouldUpdateWorld())
                    return null;

                final BitSet bitSet = new BitSet(StateEntrySize.current().getBitsPerBlock());
                BlockPosForEach.forEachInRange(StateEntrySize.current().getBitsPerBlockSide(), blockPos -> bitSet.set(
                        BlockPosUtils.getCollisionIndex(blockPos),
                        type.isValidFor(storage.getBlockInformation(blockPos).getBlockState())
                ));

                return bitSet;
            });

            if (collisionDataSet == null)
                return new BitSet(0);

            return collisionDataSet;
        }

        private void onBlockStateAdded(final IBlockInformation blockInformation, final BlockPos pos, final boolean updateWorld) {
            countMap.putIfAbsent(blockInformation, 0);
            countMap.computeIfPresent(blockInformation, (state, currentCount) -> currentCount + 1);
            updatePrimaryState(updateWorld);

            this.totalUsedBlockCount++;

            if (ILevelBasedPropertyAccessor.getInstance().shouldCheckWeakPower(
                    new SingleBlockWorldReader(
                            blockInformation,
                            this.positionSupplier.get(),
                            this.worldReaderSupplier.get()
                    ),
                    this.positionSupplier.get(),
                    Direction.NORTH
            )) {
                this.totalUsedChecksWeakPowerCount++;
            }

            this.totalLightLevel += ILevelBasedPropertyAccessor.getInstance().getLightEmission(
                    new SingleBlockWorldReader(
                            blockInformation,
                            this.positionSupplier.get(),
                            this.worldReaderSupplier.get()
                    ),
                    this.positionSupplier.get()
            );

            this.totalLightBlockLevel += ILevelBasedPropertyAccessor.getInstance().getLightBlock(
                    new SingleBlockWorldReader(
                            blockInformation,
                            this.positionSupplier.get(),
                            this.worldReaderSupplier.get()
                    ),
                    this.positionSupplier.get()
            );

            if (!this.columnStatisticsTable.contains(pos.getX(), pos.getZ())) {
                this.columnStatisticsTable.put(pos.getX(), pos.getZ(), new ColumnStatistics(this.worldReaderSupplier, this.positionSupplier));
            }

            this.columnStatisticsTable.get(pos.getX(), pos.getZ()).onBlockStateAdded(blockInformation, pos);

            this.collisionData.forEach((collisionType, bitSet) -> bitSet.set(BlockPosUtils.getCollisionIndex(pos), collisionType.isValidFor(blockInformation.getBlockState())));
        }

        private void updatePrimaryState(final boolean updateWorld) {
            final IBlockInformation currentPrimary = primaryState;
            primaryState = this.countMap.entrySet()
                    .stream()
                    .filter(e -> !e.getKey().isAir())
                    .min((o1, o2) -> -1 * (o1.getValue() - o2.getValue()))
                    .map(Map.Entry::getKey)
                    .orElse(BlockInformation.AIR);

            final boolean primaryIsAir = this.primaryState.isAir();

            if ((this.countMap.getOrDefault(primaryState, 0) == StateEntrySize.current().getBitsPerBlock() || primaryIsAir || currentPrimary != primaryState) && updateWorld) {
                if (primaryIsAir) {
                    this.worldReaderSupplier.get().setBlock(
                            this.positionSupplier.get(),
                            Blocks.AIR.defaultBlockState(),
                            Block.UPDATE_ALL
                    );
                } else if (this.countMap.getOrDefault(primaryState, 0) == StateEntrySize.current().getBitsPerBlock()) {
                    this.worldReaderSupplier.get().setBlock(
                            this.positionSupplier.get(),
                            this.primaryState.getBlockState(),
                            Block.UPDATE_ALL
                    );
                } else if (currentPrimary != primaryState) {
                    final Optional<Block> optionalWithConvertedBlock = IConversionManager.getInstance().getChiseledVariantOf(this.primaryState.getBlockState());
                    if (optionalWithConvertedBlock.isPresent()) {
                        final Block convertedBlock = optionalWithConvertedBlock.get();
                        this.worldReaderSupplier.get().setBlock(
                                this.positionSupplier.get(),
                                convertedBlock.defaultBlockState(),
                                Block.UPDATE_ALL
                        );
                    }
                }
            }
        }

        private void onBlockStateRemoved(final IBlockInformation blockInformation, final BlockPos pos, final boolean updateWorld) {
            countMap.computeIfPresent(blockInformation, (state, currentCount) -> currentCount - 1);
            countMap.remove(blockInformation, 0);
            updatePrimaryState(updateWorld);

            this.totalUsedBlockCount--;

            if (ILevelBasedPropertyAccessor.getInstance().shouldCheckWeakPower(
                    new SingleBlockWorldReader(
                            blockInformation,
                            this.positionSupplier.get(),
                            this.worldReaderSupplier.get()
                    ),
                    this.positionSupplier.get(),
                    Direction.NORTH
            )) {
                this.totalUsedChecksWeakPowerCount--;
            }

            this.totalLightLevel -= ILevelBasedPropertyAccessor.getInstance().getLightEmission(
                    new SingleBlockWorldReader(
                            blockInformation,
                            this.positionSupplier.get(),
                            this.worldReaderSupplier.get()
                    ),
                    this.positionSupplier.get()
            );

            this.totalLightBlockLevel -= ILevelBasedPropertyAccessor.getInstance().getLightBlock(
                    new SingleBlockWorldReader(
                            blockInformation,
                            this.positionSupplier.get(),
                            this.worldReaderSupplier.get()
                    ),
                    this.positionSupplier.get()
            );

            if (!this.columnStatisticsTable.contains(pos.getX(), pos.getZ())) {
                this.columnStatisticsTable.put(pos.getX(), pos.getZ(), new ColumnStatistics(this.worldReaderSupplier, this.positionSupplier));
            }

            this.columnStatisticsTable.get(pos.getX(), pos.getZ()).onBlockStateRemoved(blockInformation, pos);
            this.collisionData.forEach((collisionType, bitSet) -> bitSet.set(BlockPosUtils.getCollisionIndex(pos), collisionType.isValidFor(Blocks.AIR.defaultBlockState())));
        }

        private void onBlockStateReplaced(final IBlockInformation currentInformation, final IBlockInformation newInformation, final BlockPos pos, final boolean updateWorld) {
            countMap.computeIfPresent(currentInformation, (state, currentCount) -> currentCount - 1);
            countMap.remove(currentInformation, 0);
            countMap.putIfAbsent(newInformation, 0);
            countMap.computeIfPresent(newInformation, (state, currentCount) -> currentCount + 1);
            updatePrimaryState(updateWorld);

            if (ILevelBasedPropertyAccessor.getInstance().shouldCheckWeakPower(
                    new SingleBlockWorldReader(
                            currentInformation,
                            this.positionSupplier.get(),
                            this.worldReaderSupplier.get()
                    ),
                    this.positionSupplier.get(),
                    Direction.NORTH
            )) {
                this.totalUsedChecksWeakPowerCount--;
            }

            if (ILevelBasedPropertyAccessor.getInstance().shouldCheckWeakPower(
                    new SingleBlockWorldReader(
                            newInformation,
                            this.positionSupplier.get(),
                            this.worldReaderSupplier.get()
                    ),
                    this.positionSupplier.get(),
                    Direction.NORTH
            )) {
                this.totalUsedChecksWeakPowerCount++;
            }

            this.totalLightLevel -= ILevelBasedPropertyAccessor.getInstance().getLightEmission(
                    new SingleBlockWorldReader(
                            currentInformation,
                            this.positionSupplier.get(),
                            this.worldReaderSupplier.get()
                    ),
                    this.positionSupplier.get()
            );

            this.totalLightLevel += ILevelBasedPropertyAccessor.getInstance().getLightEmission(
                    new SingleBlockWorldReader(
                            newInformation,
                            this.positionSupplier.get(),
                            this.worldReaderSupplier.get()
                    ),
                    this.positionSupplier.get()
            );

            this.totalLightBlockLevel -= ILevelBasedPropertyAccessor.getInstance().getLightBlock(
                    new SingleBlockWorldReader(
                            currentInformation,
                            this.positionSupplier.get(),
                            this.worldReaderSupplier.get()
                    ),
                    this.positionSupplier.get()
            );

            this.totalLightBlockLevel += ILevelBasedPropertyAccessor.getInstance().getLightBlock(
                    new SingleBlockWorldReader(
                            newInformation,
                            this.positionSupplier.get(),
                            this.worldReaderSupplier.get()
                    ),
                    this.positionSupplier.get()
            );

            if (!this.columnStatisticsTable.contains(pos.getX(), pos.getZ())) {
                this.columnStatisticsTable.put(pos.getX(), pos.getZ(), new ColumnStatistics(this.worldReaderSupplier, this.positionSupplier));
            }

            this.columnStatisticsTable.get(pos.getX(), pos.getZ()).onBlockStateReplaced(currentInformation, newInformation, pos);

            this.collisionData.forEach((collisionType, bitSet) -> bitSet.set(BlockPosUtils.getCollisionIndex(pos), collisionType.isValidFor(newInformation.getBlockState())));
        }

        @Override
        public void serializeInto(@NotNull final FriendlyByteBuf packetBuffer) {
            this.primaryState.serializeInto(packetBuffer);

            packetBuffer.writeVarInt(this.countMap.size());
            for (final Map.Entry<IBlockInformation, Integer> blockStateIntegerEntry : this.countMap.entrySet()) {
                blockStateIntegerEntry.getKey().serializeInto(packetBuffer);
                packetBuffer.writeVarInt(blockStateIntegerEntry.getValue());
            }

            packetBuffer.writeVarInt(this.columnStatisticsTable.size());
            this.columnStatisticsTable.cellSet()
                    .forEach(cell ->
                    {
                        packetBuffer.writeVarInt(cell.getRowKey());
                        packetBuffer.writeVarInt(cell.getColumnKey());
                        cell.getValue().serializeInto(packetBuffer);
                    });

            packetBuffer.writeVarInt(this.totalUsedBlockCount);
            packetBuffer.writeVarInt(this.totalUsedChecksWeakPowerCount);
            packetBuffer.writeVarInt(this.totalLightLevel);
            packetBuffer.writeVarInt(this.totalLightBlockLevel);

            packetBuffer.writeVarInt(this.collisionData.size());
            this.collisionData
                    .forEach((collisionType, bitSet) ->
                    {
                        packetBuffer.writeVarInt(collisionType.ordinal());
                        packetBuffer.writeLongArray(bitSet.toLongArray());
                    });
            
            packetBuffer.writeBoolean(canBeFlooded);
            packetBuffer.writeBoolean(emitsLightBasedOnFullBlock);
        }

        @Override
        public void deserializeFrom(@NotNull final FriendlyByteBuf packetBuffer) {
            this.countMap.clear();
            this.columnStatisticsTable.clear();
            this.collisionData.clear();

            this.primaryState = new BlockInformation(packetBuffer);

            final int stateCount = packetBuffer.readVarInt();
            for (int i = 0; i < stateCount; i++) {
                this.countMap.put(
                        new BlockInformation(packetBuffer),
                        packetBuffer.readVarInt()
                );
            }

            final int columnBlockCount = packetBuffer.readVarInt();
            for (int i = 0; i < columnBlockCount; i++) {
                final ColumnStatistics statistics = new ColumnStatistics(this.worldReaderSupplier, this.positionSupplier);

                this.columnStatisticsTable.put(
                        packetBuffer.readVarInt(),
                        packetBuffer.readVarInt(),
                        statistics
                );

                statistics.deserializeFrom(packetBuffer);
            }

            this.totalUsedBlockCount = packetBuffer.readVarInt();
            this.totalUsedChecksWeakPowerCount = packetBuffer.readVarInt();
            this.totalLightLevel = packetBuffer.readVarInt();
            this.totalLightBlockLevel = packetBuffer.readVarInt();

            final int axisSizeHandlerCount = packetBuffer.readVarInt();
            for (int i = 0; i < axisSizeHandlerCount; i++) {
                final CollisionType collisionType = CollisionType.values()[packetBuffer.readVarInt()];
                final BitSet set = BitSet.valueOf(packetBuffer.readLongArray());
                this.collisionData.put(collisionType, set);
            }
            
            this.canBeFlooded = packetBuffer.readBoolean();
            this.emitsLightBasedOnFullBlock = packetBuffer.readBoolean();
        }

        @Override
        public CompoundTag serializeNBT() {
            final CompoundTag nbt = new CompoundTag();

            nbt.put(NbtConstants.PRIMARY_BLOCK_INFORMATION, this.primaryState.serializeNBT());

            final ListTag blockStateList = new ListTag();
            for (final Map.Entry<IBlockInformation, Integer> blockStateIntegerEntry : this.countMap.entrySet()) {
                final CompoundTag stateNbt = new CompoundTag();

                stateNbt.put(NbtConstants.BLOCK_INFORMATION, blockStateIntegerEntry.getKey().serializeNBT());
                stateNbt.putInt(NbtConstants.COUNT, blockStateIntegerEntry.getValue());

                blockStateList.add(stateNbt);
            }

            final CompoundTag columnStatisticsTableNbt = new CompoundTag();
            this.columnStatisticsTable.rowMap().forEach((rowKey, columnStatisticsMap) ->
            {
                final CompoundTag rowNbt = new CompoundTag();
                columnStatisticsMap.forEach((columnKey, columnStatistics) -> rowNbt.put(String.valueOf(columnKey), columnStatistics.serializeNBT()));

                columnStatisticsTableNbt.put(String.valueOf(rowKey), rowNbt);
            });

            nbt.put(NbtConstants.BLOCK_STATES, blockStateList);
            nbt.put(NbtConstants.COLUMN_STATISTICS, columnStatisticsTableNbt);

            nbt.putInt(NbtConstants.TOTAL_BLOCK_COUNT, totalUsedBlockCount);
            nbt.putInt(NbtConstants.TOTAL_SHOULD_CHECK_WEAK_POWER_COUNT, totalUsedChecksWeakPowerCount);
            nbt.putInt(NbtConstants.TOTAL_LIGHT_LEVEL, totalLightLevel);
            nbt.putInt(NbtConstants.TOTAL_LIGHT_BLOCK_LEVEL, totalLightBlockLevel);

            final CompoundTag collisionDataNbt = new CompoundTag();
            for (CollisionType collisionType : CollisionType.values()) {
                collisionDataNbt.putLongArray(collisionType.name(), getCollideableEntries(collisionType).toLongArray());
            }
            nbt.put(NbtConstants.COLLISION_DATA, collisionDataNbt);
            
            nbt.putBoolean(NbtConstants.CAN_BE_FLOODED, canBeFlooded);
            nbt.putBoolean(NbtConstants.EMITS_LIGHT_BASED_ON_FULL_BLOCK, emitsLightBasedOnFullBlock);

            return nbt;
        }

        @Override
        public void deserializeNBT(final CompoundTag nbt) {
            this.countMap.clear();

            this.primaryState = new BlockInformation(nbt.getCompound(NbtConstants.PRIMARY_BLOCK_INFORMATION));

            if (nbt.contains(NbtConstants.BLOCK_STATES, Tag.TAG_LIST)) {
                final ListTag blockStateList = nbt.getList(NbtConstants.BLOCK_STATES, Tag.TAG_COMPOUND);
                for (int i = 0; i < blockStateList.size(); i++) {
                    final CompoundTag stateNbt = blockStateList.getCompound(i);

                    BlockInformation blockInformation = new BlockInformation(stateNbt.getCompound(NbtConstants.BLOCK_INFORMATION));

                    this.countMap.put(
                            blockInformation,
                            stateNbt.getInt(NbtConstants.COUNT)
                    );
                }
            }


            this.columnStatisticsTable.clear();
            if (nbt.contains(NbtConstants.COLUMN_STATISTICS, Tag.TAG_COMPOUND)) {
                final CompoundTag columnStatisticsTableNbt = nbt.getCompound(NbtConstants.COLUMN_STATISTICS);
                columnStatisticsTableNbt.getAllKeys().forEach(rowKeyValue ->
                {
                    final Integer rowKey = Integer.valueOf(rowKeyValue);
                    final CompoundTag rowNbt = columnStatisticsTableNbt.getCompound(rowKeyValue);
                    rowNbt.getAllKeys().forEach(columnKeyValue ->
                    {
                        final Integer columnKey = Integer.valueOf(columnKeyValue);
                        final CompoundTag columnStatisticsNbt = rowNbt.getCompound(columnKeyValue);
                        final ColumnStatistics columnStatistics = new ColumnStatistics(
                                this.worldReaderSupplier,
                                this.positionSupplier
                        );

                        columnStatistics.deserializeNBT(columnStatisticsNbt);
                        this.columnStatisticsTable.put(rowKey, columnKey, columnStatistics);
                    });
                });
            } else {
                requiresRecalculation = true;
            }

            this.totalUsedBlockCount = nbt.getInt(NbtConstants.TOTAL_BLOCK_COUNT);
            this.totalUsedChecksWeakPowerCount = nbt.getInt(NbtConstants.TOTAL_SHOULD_CHECK_WEAK_POWER_COUNT);
            this.totalLightLevel = nbt.getInt(NbtConstants.TOTAL_LIGHT_LEVEL);

            //We need to check if this exists or not.
            //This was added in 1.x.60+ to accommodate for the new light level system.
            if (nbt.contains(NbtConstants.TOTAL_LIGHT_BLOCK_LEVEL)) {
                this.totalLightBlockLevel = nbt.getInt(NbtConstants.TOTAL_LIGHT_BLOCK_LEVEL);
            } else {
                this.totalLightBlockLevel = 0;
                this.requiresRecalculation = true;
            }

            this.collisionData.clear();
            if (nbt.contains(NbtConstants.COLLISION_DATA)) {
                final CompoundTag collisionDataNbt = nbt.getCompound(NbtConstants.COLLISION_DATA);
                collisionDataNbt.getAllKeys().forEach(collisionTypeName ->
                {
                    final CollisionType collisionType = CollisionType.valueOf(collisionTypeName);
                    final BitSet set = BitSet.valueOf(collisionDataNbt.getLongArray(collisionTypeName));
                    this.collisionData.put(collisionType, set);
                });
            } else {
                this.requiresRecalculation = true;
            }
            
            this.canBeFlooded = !nbt.contains(NbtConstants.CAN_BE_FLOODED) || nbt.getBoolean(NbtConstants.CAN_BE_FLOODED);
            this.emitsLightBasedOnFullBlock = nbt.contains(NbtConstants.EMITS_LIGHT_BASED_ON_FULL_BLOCK) && nbt.getBoolean(NbtConstants.EMITS_LIGHT_BASED_ON_FULL_BLOCK);
        }

        public void initializeWith(final IBlockInformation blockInformation) {
            clear();
            final boolean isAir = blockInformation.isAir();

            this.primaryState = blockInformation;
            if (!isAir) {
                this.countMap.put(blockInformation, StateEntrySize.current().getBitsPerBlock());
            }
            this.totalUsedBlockCount = isAir ? 0 : StateEntrySize.current().getBitsPerBlock();

            if (ILevelBasedPropertyAccessor.getInstance().shouldCheckWeakPower(
                    new SingleBlockWorldReader(
                            blockInformation,
                            this.positionSupplier.get(),
                            this.worldReaderSupplier.get()
                    ),
                    this.positionSupplier.get(),
                    Direction.NORTH
            )) {
                this.totalUsedChecksWeakPowerCount = StateEntrySize.current().getBitsPerBlock();
            }

            this.totalLightLevel += (ILevelBasedPropertyAccessor.getInstance().getLightEmission(
                    new SingleBlockWorldReader(
                            blockInformation,
                            this.positionSupplier.get(),
                            this.worldReaderSupplier.get()
                    ),
                    this.positionSupplier.get()
            ) * StateEntrySize.current().getBitsPerBlock());

            this.totalLightBlockLevel += (ILevelBasedPropertyAccessor.getInstance().getLightBlock(
                    new SingleBlockWorldReader(
                            blockInformation,
                            this.positionSupplier.get(),
                            this.worldReaderSupplier.get()
                    ),
                    this.positionSupplier.get()
            ) * StateEntrySize.current().getBitsPerBlock());

            this.columnStatisticsTable.clear();
            IntStream.range(0, StateEntrySize.current().getBitsPerBlockSide())
                    .forEach(x -> IntStream.range(0, StateEntrySize.current().getBitsPerBlockSide())
                            .forEach(z ->
                            {
                                final ColumnStatistics columnStatistics = new ColumnStatistics(
                                        this.worldReaderSupplier,
                                        this.positionSupplier
                                );
                                columnStatistics.initializeWith(blockInformation);
                                this.columnStatisticsTable.put(x, z, columnStatistics);
                            }));

            this.collisionData.clear();
            for (final CollisionType collisionType : CollisionType.values()) {
                final boolean matches = collisionType.isValidFor(blockInformation.getBlockState());
                final BitSet set = new BitSet(StateEntrySize.current().getBitsPerBlock());
                set.set(0, StateEntrySize.current().getBitsPerBlock(), matches);
                this.collisionData.put(collisionType, set);
            }
        }

        private void clear() {
            this.primaryState = BlockInformation.AIR;

            this.countMap.clear();
            this.columnStatisticsTable.clear();
            this.collisionData.clear();

            this.totalUsedBlockCount = 0;
            this.totalUsedChecksWeakPowerCount = 0;
            this.totalLightLevel = 0;
            this.totalLightBlockLevel = 0;
        }

        public boolean isRequiresRecalculation() {
            return requiresRecalculation;
        }

        private void recalculate(final IStateEntryStorage source) {
            recalculate(source, true);
        }

        private void recalculate(final IStateEntryStorage source, final boolean mayUpdateWorld) {
            if (!mayUpdateWorld) {
                this.requiresRecalculation = true;
                return;
            }

            this.requiresRecalculation = false;
            clear();

            source.count(countMap::put);
            countMap.remove(BlockInformation.AIR);
            updatePrimaryState(mayUpdateWorld);

            this.totalUsedBlockCount = countMap.values().stream().mapToInt(i -> i).sum();

            countMap.forEach((blockState, count) ->
            {
                if (ILevelBasedPropertyAccessor.getInstance().shouldCheckWeakPower(
                        new SingleBlockWorldReader(
                                blockState,
                                this.positionSupplier.get(),
                                this.worldReaderSupplier.get()
                        ),
                        this.positionSupplier.get(),
                        Direction.NORTH
                )) {
                    this.totalUsedChecksWeakPowerCount += count;
                }

                this.totalLightLevel += (ILevelBasedPropertyAccessor.getInstance().getLightEmission(
                        new SingleBlockWorldReader(
                                blockState,
                                this.positionSupplier.get(),
                                this.worldReaderSupplier.get()
                        ),
                        this.positionSupplier.get()
                ) * count);

                this.totalLightBlockLevel += (ILevelBasedPropertyAccessor.getInstance().getLightBlock(
                        new SingleBlockWorldReader(
                                blockState,
                                this.positionSupplier.get(),
                                this.worldReaderSupplier.get()
                        ),
                        this.positionSupplier.get()
                ) * count);
            });

            BlockPosStreamProvider.getForRange(StateEntrySize.current().getBitsPerBlockSide())
                    .forEach(pos ->
                    {
                        final IBlockInformation blockState = source.getBlockInformation(pos.getX(), pos.getY(), pos.getZ());

                        if (!this.columnStatisticsTable.contains(pos.getX(), pos.getZ())) {
                            this.columnStatisticsTable.put(pos.getX(), pos.getZ(), new ColumnStatistics(this.worldReaderSupplier, this.positionSupplier));
                        }

                        this.columnStatisticsTable.get(pos.getX(), pos.getZ()).onBlockStateAdded(blockState, pos);
                    });

            this.collisionData.clear();
            for (final CollisionType collisionType : CollisionType.values()) {
                getCollideableEntries(collisionType);
            }
        }
        
        public boolean isCanBeFlooded() {
            return canBeFlooded;
        }
        
        public void setCanBeFlooded(boolean canBeFlooded) {
            this.canBeFlooded = canBeFlooded;
            
        }
        
        public boolean isEmitsLightBasedOnFullBlock() {
            return emitsLightBasedOnFullBlock;
        }
        
        public void setEmitsLightBasedOnFullBlock(boolean emitsLightBasedOnFullBlock) {
            this.emitsLightBasedOnFullBlock = emitsLightBasedOnFullBlock;
        }
    }

    private final class ColumnStatistics implements INBTSerializable<CompoundTag>, IPacketBufferSerializable {
        private final BitSet skylightBlockingBits = new BitSet(StateEntrySize.current().getBitsPerBlockSide());
        private final BitSet noneAirBits = new BitSet(StateEntrySize.current().getBitsPerBlockSide());
        private final Supplier<LevelAccessor> worldReaderSupplier;
        private final Supplier<BlockPos> positionSupplier;

        private short highestBit = -1;
        private float highestBitFriction = 0f;
        private boolean canPropagateSkylightDown = true;
        private boolean canLowestBitSustainGrass = true;

        private ColumnStatistics(final Supplier<LevelAccessor> worldReaderSupplier, final Supplier<BlockPos> positionSupplier) {
            this.worldReaderSupplier = worldReaderSupplier;
            this.positionSupplier = positionSupplier;
        }

        @SuppressWarnings("unused")
        public BitSet getSkylightBlockingBits() {
            return skylightBlockingBits;
        }

        @SuppressWarnings("unused")
        public BitSet getNoneAirBits() {
            return noneAirBits;
        }

        public short getHighestBit() {
            return highestBit;
        }

        public float getHighestBitFriction() {
            return highestBitFriction;
        }

        public boolean canPropagateSkylightDown() {
            return canPropagateSkylightDown;
        }

        public boolean canLowestBitSustainGrass() {
            return canLowestBitSustainGrass;
        }

        private void onBlockStateAdded(final IBlockInformation blockState, final BlockPos pos) {
            skylightBlockingBits.set(pos.getY(), !ILevelBasedPropertyAccessor.getInstance().propagatesSkylightDown(
                    new SingleBlockBlockReader(blockState, positionSupplier.get(), this.worldReaderSupplier.get()),
                    positionSupplier.get()
            ));

            if (skylightBlockingBits.get(pos.getY())) {
                canPropagateSkylightDown = false;
            }

            if (!blockState.isAir() && pos.getY() >= highestBit) {
                highestBit = (short) pos.getY();
                highestBitFriction = ILevelBasedPropertyAccessor.getInstance().getFriction(
                        new SingleBlockWorldReader(blockState, positionSupplier.get(), this.worldReaderSupplier.get()),
                        positionSupplier.get(),
                        null
                );
            }

            if (pos.getY() == 0) {
                canLowestBitSustainGrass = ILevelBasedPropertyAccessor.getInstance()
                        .canBeGrass(
                                new SingleBlockWorldReader(blockState, positionSupplier.get(), this.worldReaderSupplier.get()),
                                Blocks.GRASS_BLOCK.defaultBlockState(),
                                positionSupplier.get().below(),
                                blockState.getBlockState(),
                                positionSupplier.get()
                        )
                        .orElseGet(() ->
                        {
                            if (blockState.getBlockState().is(Blocks.SNOW) && blockState.getBlockState().getValue(SnowLayerBlock.LAYERS) == 1) {
                                return true;
                            } else if (blockState.getBlockState().getFluidState().getAmount() == 8) {
                                return false;
                            } else {
                                int i = LightEngine.getLightBlockInto(
                                        new SingleBlockWorldReader(blockState, positionSupplier.get(), this.worldReaderSupplier.get()),
                                        Blocks.GRASS_BLOCK.defaultBlockState(),
                                        this.positionSupplier.get().below(),
                                        blockState.getBlockState(),
                                        this.positionSupplier.get(),
                                        Direction.UP,
                                        blockState.getBlockState().getLightBlock(
                                                new SingleBlockWorldReader(blockState, positionSupplier.get(), this.worldReaderSupplier.get()),
                                                this.positionSupplier.get()));
                                return i < this.worldReaderSupplier.get().getMaxLightLevel();
                            }
                        });
            }
        }

        private void onBlockStateRemoved(final IBlockInformation blockInformation, final BlockPos pos) {
            skylightBlockingBits.set(pos.getY(), !ILevelBasedPropertyAccessor.getInstance().propagatesSkylightDown(
                    new SingleBlockBlockReader(blockInformation, positionSupplier.get(), this.worldReaderSupplier.get()),
                    positionSupplier.get()
            ));

            if (!skylightBlockingBits.get(pos.getY())) {
                canPropagateSkylightDown = IntStream.range(0, StateEntrySize.current().getBitsPerBlockSide())
                        .noneMatch(skylightBlockingBits::get);
            }

            if (pos.getY() >= highestBit) {
                highestBit = -1;
                highestBitFriction = 0f;

                for (int i = StateEntrySize.current().getBitsPerBlockSide() - 1; i >= 0; i--) {
                    if (noneAirBits.get(i)) {
                        highestBit = (short) i;
                        highestBitFriction = ILevelBasedPropertyAccessor.getInstance().getFriction(
                                new SingleBlockWorldReader(ChiseledBlockEntity.this.storage.getBlockInformation(pos.getX(), i, pos.getZ()), positionSupplier.get(), this.worldReaderSupplier.get()),
                                positionSupplier.get(),
                                null
                        );
                        break;
                    }
                }
            }

            if (pos.getY() == 0) {
                this.canLowestBitSustainGrass = true;
            }
        }

        private void onBlockStateReplaced(final IBlockInformation currentInformation, final IBlockInformation newInformation, final BlockPos pos) {
            onBlockStateRemoved(currentInformation, pos);
            onBlockStateAdded(newInformation, pos);
        }

        @Override
        public CompoundTag serializeNBT() {
            final CompoundTag compoundTag = new CompoundTag();

            compoundTag.putByteArray(NbtConstants.SKYLIGHT_BLOCKING_BITS, skylightBlockingBits.toByteArray());
            compoundTag.putByteArray(NbtConstants.NONE_AIR_BITS, noneAirBits.toByteArray());
            compoundTag.putShort(NbtConstants.HIGHEST_BIT, highestBit);
            compoundTag.putFloat(NbtConstants.HIGHEST_BIT_FRICTION, highestBitFriction);
            compoundTag.putBoolean(NbtConstants.CAN_PROPAGATE_SKYLIGHT_DOWN, canPropagateSkylightDown);
            compoundTag.putBoolean(NbtConstants.LOWEST_BIT_CAN_SUSTAIN_GRASS, canLowestBitSustainGrass);

            return compoundTag;
        }

        @Override
        public void deserializeNBT(final CompoundTag nbt) {
            skylightBlockingBits.clear();
            skylightBlockingBits.or(BitSet.valueOf(nbt.getByteArray(NbtConstants.SKYLIGHT_BLOCKING_BITS)));

            noneAirBits.clear();
            noneAirBits.or(BitSet.valueOf(nbt.getByteArray(NbtConstants.NONE_AIR_BITS)));

            highestBit = nbt.getShort(NbtConstants.HIGHEST_BIT);
            highestBitFriction = nbt.getFloat(NbtConstants.HIGHEST_BIT_FRICTION);
            canPropagateSkylightDown = nbt.getBoolean(NbtConstants.CAN_PROPAGATE_SKYLIGHT_DOWN);
            canLowestBitSustainGrass = nbt.getBoolean(NbtConstants.LOWEST_BIT_CAN_SUSTAIN_GRASS);
        }

        @Override
        public void serializeInto(final @NotNull FriendlyByteBuf packetBuffer) {
            packetBuffer.writeBitSet(skylightBlockingBits);
            packetBuffer.writeBitSet(noneAirBits);
            packetBuffer.writeShort(highestBit);
            packetBuffer.writeFloat(highestBitFriction);
            packetBuffer.writeBoolean(canPropagateSkylightDown);
            packetBuffer.writeBoolean(canLowestBitSustainGrass);
        }

        @Override
        public void deserializeFrom(final @NotNull FriendlyByteBuf packetBuffer) {
            skylightBlockingBits.clear();
            skylightBlockingBits.or(packetBuffer.readBitSet());

            noneAirBits.clear();
            noneAirBits.or(packetBuffer.readBitSet());

            highestBit = packetBuffer.readShort();
            highestBitFriction = packetBuffer.readFloat();
            canPropagateSkylightDown = packetBuffer.readBoolean();
            canLowestBitSustainGrass = packetBuffer.readBoolean();
        }

        public void initializeWith(final IBlockInformation blockInformation) {
            skylightBlockingBits.clear();
            noneAirBits.clear();

            skylightBlockingBits.set(0, StateEntrySize.current().getBitsPerBlockSide(),
                    !ILevelBasedPropertyAccessor.getInstance().propagatesSkylightDown(
                            new SingleBlockBlockReader(blockInformation, positionSupplier.get(), this.worldReaderSupplier.get()),
                            positionSupplier.get()
                    ));
            noneAirBits.set(0, !blockInformation.isAir());

            if (blockInformation.isAir()) {
                highestBit = -1;
                highestBitFriction = 0f;
            } else {
                highestBit = (short) (StateEntrySize.current().getBitsPerBlockSide() - 1);
                highestBitFriction = ILevelBasedPropertyAccessor.getInstance().getFriction(
                        new SingleBlockWorldReader(blockInformation, positionSupplier.get(), this.worldReaderSupplier.get()),
                        positionSupplier.get(),
                        null
                );
            }

            this.canPropagateSkylightDown = ILevelBasedPropertyAccessor.getInstance().propagatesSkylightDown(
                    new SingleBlockBlockReader(blockInformation, positionSupplier.get(), this.worldReaderSupplier.get()),
                    positionSupplier.get()
            );

            this.canLowestBitSustainGrass = blockInformation.isAir() || ILevelBasedPropertyAccessor.getInstance()
                    .canBeGrass(
                            new SingleBlockWorldReader(blockInformation, positionSupplier.get(), this.worldReaderSupplier.get()),
                            Blocks.GRASS_BLOCK.defaultBlockState(),
                            positionSupplier.get().below(),
                            blockInformation.getBlockState(),
                            positionSupplier.get()
                    )
                    .orElseGet(() ->
                    {
                        if (blockInformation.getBlockState().is(Blocks.SNOW) && blockInformation.getBlockState().getValue(SnowLayerBlock.LAYERS) == 1) {
                            return true;
                        } else if (blockInformation.getBlockState().getFluidState().getAmount() == 8) {
                            return false;
                        } else {
                            int i = LightEngine.getLightBlockInto(
                                    new SingleBlockWorldReader(blockInformation, positionSupplier.get(), this.worldReaderSupplier.get()),
                                    Blocks.GRASS_BLOCK.defaultBlockState(),
                                    this.positionSupplier.get().below(),
                                    blockInformation.getBlockState(),
                                    this.positionSupplier.get(),
                                    Direction.UP,
                                    blockInformation.getBlockState().getLightBlock(
                                            new SingleBlockWorldReader(blockInformation, positionSupplier.get(), this.worldReaderSupplier.get()),
                                            this.positionSupplier.get()));
                            return i < this.worldReaderSupplier.get().getMaxLightLevel();
                        }
                    });
        }
    }

    private final class LZ4StorageBasedStorageHandler implements IStorageHandler<LZ4StorageBasedStorageHandler.Payload> {

        @Override
        public Payload readPayloadOffThread(CompoundTag nbt) {
            return LZ4DataCompressionUtils.decompress(nbt, compoundTag -> {
                final IStateEntryStorage storage = new SimpleStateEntryStorage();
                final MutableStatistics mutableStatistics = new MutableStatistics(ChiseledBlockEntity.this::getLevel, ChiseledBlockEntity.this::getBlockPos);

                storage.deserializeNBT(compoundTag.getCompound(NbtConstants.CHISELED_DATA));
                mutableStatistics.deserializeNBT(compoundTag.getCompound(NbtConstants.STATISTICS));

                return new Payload(storage, mutableStatistics);
            });
        }

        @Override
        public void syncPayloadOnGameThread(Payload payload) {
            storage = payload.storage;
            mutableStatistics = payload.mutableStatistics;

            if (!isInitialized) {
                setChanged();
            }

            isInitialized = true;
        }

        @Override
        public CompoundTag serializeNBT() {
            return LZ4DataCompressionUtils.compress(compoundTag ->
            {
                compoundTag.put(NbtConstants.CHISELED_DATA, storage.serializeNBT());
                compoundTag.put(NbtConstants.STATISTICS, mutableStatistics.serializeNBT());
            });
        }

        @Override
        public void deserializeNBT(final CompoundTag nbt) {
            LZ4DataCompressionUtils.decompress(nbt, compoundTag ->
            {
                storage.deserializeNBT(compoundTag.getCompound(NbtConstants.CHISELED_DATA));
                mutableStatistics.deserializeNBT(compoundTag.getCompound(NbtConstants.STATISTICS));
            });
        }

        private record Payload(IStateEntryStorage storage, MutableStatistics mutableStatistics) {
        }
    }

    private static final class ServerSchedulingExecutor implements Executor {

        private final MinecraftServer server;

        private ServerSchedulingExecutor(MinecraftServer server) {
            this.server = server;
        }

        @Override
        public void execute(@NotNull Runnable command) {
            server.tell(new TickTask(server.getTickCount(), command));
        }
    }
}

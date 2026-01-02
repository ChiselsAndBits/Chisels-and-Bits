package mod.chiselsandbits.item.multistate;

import com.google.common.collect.Maps;
import mod.chiselsandbits.api.block.storage.IStateEntryStorage;
import mod.chiselsandbits.api.blockinformation.IBlockInformation;
import mod.chiselsandbits.api.exceptions.SpaceOccupiedException;
import mod.chiselsandbits.api.item.multistate.IMultiStateItem;
import mod.chiselsandbits.api.item.multistate.IMultiStateItemStack;
import mod.chiselsandbits.api.item.multistate.IStatistics;
import mod.chiselsandbits.api.item.pattern.IPatternItem;
import mod.chiselsandbits.api.multistate.StateEntrySize;
import mod.chiselsandbits.api.multistate.accessor.IAreaAccessor;
import mod.chiselsandbits.api.multistate.accessor.IStateEntryInfo;
import mod.chiselsandbits.api.multistate.accessor.identifier.IAreaShapeIdentifier;
import mod.chiselsandbits.api.multistate.accessor.identifier.IArrayBackedAreaShapeIdentifier;
import mod.chiselsandbits.api.multistate.accessor.sortable.IPositionMutator;
import mod.chiselsandbits.api.multistate.mutator.IMutableStateEntryInfo;
import mod.chiselsandbits.api.multistate.mutator.callback.StateClearer;
import mod.chiselsandbits.api.multistate.mutator.callback.StateSetter;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshot;
import mod.chiselsandbits.api.util.BlockPosForEach;
import mod.chiselsandbits.api.util.BlockPosStreamProvider;
import mod.chiselsandbits.api.util.VectorUtils;
import mod.chiselsandbits.api.util.constants.NbtConstants;
import mod.chiselsandbits.block.entities.storage.SimpleStateEntryStorage;
import mod.chiselsandbits.blockinformation.BlockInformation;
import mod.chiselsandbits.item.ChiseledBlockItem;
import mod.chiselsandbits.registrars.ModItems;
import mod.chiselsandbits.storage.IStorageEngine;
import mod.chiselsandbits.storage.IStorageHandler;
import mod.chiselsandbits.storage.StorageEngineBuilder;
import mod.chiselsandbits.utils.LZ4DataCompressionUtils;
import mod.chiselsandbits.utils.MultiStateSnapshotUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class SingleBlockMultiStateItemStack implements IMultiStateItemStack
{

    private final ItemStack          sourceStack;
    private IStateEntryStorage compressedSection;
    private Statistics         statistics = new Statistics();
    private final IStorageEngine storageEngine = buildStorageEngine();

    public SingleBlockMultiStateItemStack(final ItemStack sourceStack)
    {
        this.sourceStack = sourceStack;
        this.compressedSection = new SimpleStateEntryStorage();

        this.deserializeNBT(sourceStack.getOrCreateTagElement(NbtConstants.CHISELED_DATA));
        this.sourceStack.getOrCreateTag().put(NbtConstants.CHISELED_DATA, serializeNBT());
    }

    public SingleBlockMultiStateItemStack(final Item item, final IStateEntryStorage compressedSection)
    {
        if (!(item instanceof IMultiStateItem))
            throw new IllegalArgumentException("The given item is not a MultiState Item");

        this.sourceStack = new ItemStack(item);
        this.compressedSection = compressedSection;

        this.statistics.initializeFrom(this.compressedSection);

        this.sourceStack.getOrCreateTag().put(NbtConstants.CHISELED_DATA, serializeNBT());
    }

    public SingleBlockMultiStateItemStack(final Item item, final CompoundTag nbt)
    {
        if (!(item instanceof IMultiStateItem))
            throw new IllegalArgumentException("The given item is not a MultiState Item");

        this.sourceStack = new ItemStack(item);
        this.compressedSection = new SimpleStateEntryStorage();
        this.statistics.initializeFrom(this.compressedSection);

        this.deserializeNBT(nbt);

        this.sourceStack.getOrCreateTag().put(NbtConstants.CHISELED_DATA, serializeNBT());
    }

    private IStorageEngine buildStorageEngine() {
        return StorageEngineBuilder.create()
                 .with(new LZ4StorageBasedStorageHandler())
                 .build();
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
        return new ShapeIdentifier(this.compressedSection);
    }

    /**
     * Gives access to a stream with the entry state info inside the accessors range.
     *
     * @return The stream with the inner states.
     */
    @Override
    public Stream<IStateEntryInfo> stream()
    {
        return BlockPosStreamProvider.getForRange(StateEntrySize.current().getBitsPerBlockSide())
                 .map(blockPos -> new StateEntry(
                     this.compressedSection.getBlockInformation(blockPos.getX(), blockPos.getY(), blockPos.getZ()),
                     blockPos,
                     this::setInAreaTarget,
                     this::clearInAreaTarget
                   )
                 );
    }

    /**
     * Gets the target state in the current area, using the offset from the area as well as the in area target offset.
     *
     * @param inAreaTarget The in area offset.
     * @return An optional potentially containing the state entry of the requested target.
     */
    @SuppressWarnings("deprecation")
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

        final BlockPos inAreaPos = VectorUtils.toBlockPos(inAreaTarget.multiply(StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide()));

        final IBlockInformation currentState = this.compressedSection.getBlockInformation(
          inAreaPos.getX(),
          inAreaPos.getY(),
          inAreaPos.getZ()
        );

        return currentState.isAir() ? Optional.empty() : Optional.of(new StateEntry(
          currentState,
          inAreaPos,
          this::setInAreaTarget,
          this::clearInAreaTarget
        ));
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
            throw new IllegalStateException(String.format("The given in area block pos offset is not inside the current block: %s", inAreaBlockPosOffset));
        }

        return this.getInAreaTarget(
          inBlockTarget
        );
    }

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
    public IMultiStateSnapshot createSnapshot()
    {
        return MultiStateSnapshotUtils.createFromStorage(compressedSection);
    }

    @Override
    public Stream<IMutableStateEntryInfo> mutableStream()
    {
        return BlockPosStreamProvider.getForRange(StateEntrySize.current().getBitsPerBlockSide())
                 .map(blockPos -> new StateEntry(
                     this.compressedSection.getBlockInformation(blockPos.getX(), blockPos.getY(), blockPos.getZ()),
                     blockPos,
                     this::setInAreaTarget,
                     this::clearInAreaTarget
                   )
                 );
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setInAreaTarget(
      final IBlockInformation blockInformation,
      final Vec3 inAreaTarget) throws SpaceOccupiedException
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

        final BlockPos inAreaPos = VectorUtils.toBlockPos(inAreaTarget.multiply(StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide()));

        final IBlockInformation currentState = this.compressedSection.getBlockInformation(
          inAreaPos.getX(),
          inAreaPos.getY(),
          inAreaPos.getZ()
        );

        if (!currentState.isAir())
        {
            throw new SpaceOccupiedException();
        }

        this.compressedSection.setBlockInformation(
          inAreaPos.getX(),
          inAreaPos.getY(),
          inAreaPos.getZ(),
          blockInformation
        );

        if (blockInformation.isAir() && !currentState.isAir()) {
            statistics.onBlockStateRemoved(currentState);
        } else if (!blockInformation.isAir() && currentState.isAir()) {
            statistics.onBlockStateAdded(blockInformation);
        } else if (!blockInformation.isAir() && !currentState.isAir()) {
            statistics.onBlockStateReplaced(currentState, blockInformation);
        }

        this.sourceStack.getOrCreateTag().put(NbtConstants.CHISELED_DATA, serializeNBT());
    }

    @Override
    public void setInBlockTarget(final IBlockInformation blockInformation, final BlockPos inAreaBlockPosOffset, final Vec3 inBlockTarget) throws SpaceOccupiedException
    {
        if (!inAreaBlockPosOffset.equals(BlockPos.ZERO))
        {
            throw new IllegalStateException(String.format("The given in area block pos offset is not inside the current block: %s", inAreaBlockPosOffset));
        }

        this.setInAreaTarget(
          blockInformation,
          inBlockTarget);
    }

    @SuppressWarnings("deprecation")
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

        final BlockPos inAreaPos = VectorUtils.toBlockPos(inAreaTarget.multiply(StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide()));

        final IBlockInformation blockState = BlockInformation.AIR;

        final IBlockInformation currentState = this.compressedSection.getBlockInformation(
          inAreaPos.getX(),
          inAreaPos.getY(),
          inAreaPos.getZ()
        );

        this.compressedSection.setBlockInformation(
          inAreaPos.getX(),
          inAreaPos.getY(),
          inAreaPos.getZ(),
          blockState
        );

        if (blockState.isAir() && !currentState.isAir()) {
            statistics.onBlockStateRemoved(currentState);
        } else if (!blockState.isAir() && currentState.isAir()) {
            statistics.onBlockStateAdded(blockState);
        } else if (!blockState.isAir() && !currentState.isAir()) {
            statistics.onBlockStateReplaced(currentState, blockState);
        }

        this.sourceStack.getOrCreateTag().put(NbtConstants.CHISELED_DATA, serializeNBT());
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
    public void serializeInto(@NotNull final FriendlyByteBuf packetBuffer)
    {
        compressedSection.serializeInto(packetBuffer);
    }

    /**
     * Used to read the data from the packet buffer into the current instance. Potentially overriding the data that currently already exists in the instance.
     *
     * @param packetBuffer The packet buffer to read from.
     */
    @Override
    public void deserializeFrom(@NotNull final FriendlyByteBuf packetBuffer)
    {
        compressedSection.deserializeFrom(packetBuffer);
    }

    @Override
    public CompoundTag serializeNBT()
    {
        return this.storageEngine.serializeNBT();
    }

    @Override
    public void deserializeNBT(final CompoundTag nbt)
    {
        this.storageEngine.deserializeNBT(nbt);
    }

    /**
     * The statistics of the itemstack.
     *
     * @return The statistics.
     */
    @Override
    public IStatistics getStatistics()
    {
        return statistics;
    }

    /**
     * Converts this multistack itemstack data to an actual use able itemstack.
     *
     * @return The itemstack with the data of this multistate itemstack.
     */
    @Override
    public ItemStack toBlockStack()
    {
        if (this.sourceStack.getItem() instanceof IPatternItem)
        {
            //We were created with a pattern item, instead of the block item
            //Create a new item, and copy the nbt.
            final IBlockInformation primaryState = statistics.getPrimaryState();

            final ChiseledBlockItem chiseledBlockItem = ModItems.CHISELED_BLOCK.get();
            final ItemStack blockStack = new ItemStack(chiseledBlockItem);
            blockStack.setTag(sourceStack.getOrCreateTag().copy());

            return blockStack;
        }

        return sourceStack.copy();
    }

    @Override
    public ItemStack toPatternStack()
    {
        if (this.sourceStack.getItem() instanceof IPatternItem) {
            return sourceStack.copy();
        }

        final ItemStack singleUsePatternStack = new ItemStack(ModItems.SINGLE_USE_PATTERN_ITEM.get());
        singleUsePatternStack.setTag(sourceStack.getOrCreateTag().copy());
        return singleUsePatternStack;
    }

    @Override
    public IMultiStateItemStack copy()
    {
        return new SingleBlockMultiStateItemStack(sourceStack);
    }

    @Override
    public Stream<IStateEntryInfo> streamWithPositionMutator(final IPositionMutator positionMutator)
    {
        return BlockPosStreamProvider.getForRange(StateEntrySize.current().getBitsPerBlockSide())
                 .map(positionMutator::mutate)
                 .map(blockPos -> new StateEntry(
                     this.compressedSection.getBlockInformation(blockPos.getX(), blockPos.getY(), blockPos.getZ()),
                     blockPos,
                     this::setInAreaTarget,
                     this::clearInAreaTarget
                   )
                 );
    }

    @Override
    public void forEachWithPositionMutator(
      final IPositionMutator positionMutator, final Consumer<IStateEntryInfo> consumer)
    {
        BlockPosForEach.forEachInRange(StateEntrySize.current().getBitsPerBlockSide(), (BlockPos blockPos) -> {
            final Vec3i pos = positionMutator.mutate(blockPos);
            consumer.accept(new StateEntry(
              this.compressedSection.getBlockInformation(pos.getX(), pos.getY(), pos.getZ()),
              pos,
              this::setInAreaTarget,
              this::clearInAreaTarget
            ));
        });
    }

    @Override
    public void rotate(final Direction.Axis axis, final int rotationCount)
    {
        this.compressedSection.rotate(axis, rotationCount);
        this.statistics.clear();

        BlockPosStreamProvider.getForRange(StateEntrySize.current().getBitsPerBlockSide())
          .forEach(position -> this.statistics.onBlockStateAdded(
            this.compressedSection.getBlockInformation(position.getX(), position.getY(), position.getZ())
          ));

        this.sourceStack.getOrCreateTag().put(NbtConstants.CHISELED_DATA, serializeNBT());
    }

    @Override
    public void mirror(final Direction.Axis axis)
    {
        this.compressedSection.mirror(axis);
        this.statistics.clear();

        BlockPosStreamProvider.getForRange(StateEntrySize.current().getBitsPerBlockSide())
          .forEach(position -> this.statistics.onBlockStateAdded(
            this.compressedSection.getBlockInformation(position.getX(), position.getY(), position.getZ())
          ));

        this.sourceStack.getOrCreateTag().put(NbtConstants.CHISELED_DATA, serializeNBT());
    }

    @Override
    public int hashCode()
    {
        return createNewShapeIdentifier().hashCode();
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (!(obj instanceof IAreaAccessor accessor))
            return false;

        return createNewShapeIdentifier().equals(accessor.createNewShapeIdentifier());
    }

    @Override
    public @NotNull AABB getBoundingBox()
    {
        return new AABB(0,0,0,1,1,1);
    }

    private static final class ShapeIdentifier implements IArrayBackedAreaShapeIdentifier
    {
        private final IStateEntryStorage snapshot;

        private ShapeIdentifier(final IStateEntryStorage chunkSection)
        {
            snapshot = chunkSection.createSnapshot();
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
        public byte[] getBackingData()
        {
            return snapshot.getRawData();
        }

        @Override
        public List<IBlockInformation> getPalette()
        {
            return snapshot.getContainedPalette();
        }
    }

    private static final class StateEntry implements IMutableStateEntryInfo
    {

        private final IBlockInformation blockInformation;
        private final Vec3             startPoint;
        private final Vec3   endPoint;

        private final StateSetter  stateSetter;
        private final StateClearer stateClearer;

        public StateEntry(
          final IBlockInformation blockInformation,
          final Vec3i startPoint,
          final StateSetter stateSetter,
          final StateClearer stateClearer)
        {
            this(
              blockInformation,
              Vec3.atLowerCornerOf(startPoint).multiply(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit()),
              Vec3.atLowerCornerOf(startPoint).multiply(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit()).add(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit()),
              stateSetter, stateClearer);
        }

        private StateEntry(
          final IBlockInformation blockInformation,
          final Vec3 startPoint,
          final Vec3 endPoint,
          final StateSetter stateSetter,
          final StateClearer stateClearer)
        {
            this.blockInformation = blockInformation;
            this.startPoint = startPoint;
            this.endPoint = endPoint;
            this.stateSetter = stateSetter;
            this.stateClearer = stateClearer;
        }

        @Override
        public @NotNull IBlockInformation getBlockInformation()
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
        public void setBlockInformation(final IBlockInformation blockInformation) throws SpaceOccupiedException
        {
            stateSetter.set(blockInformation, getStartPoint());
        }

        @Override
        public void clear()
        {
            stateClearer.accept(getStartPoint());
        }
    }

    private static final class Statistics implements IStatistics
    {

        private       IBlockInformation                     primaryState = BlockInformation.AIR;
        private final Map<IBlockInformation, Integer> countMap     = Maps.newConcurrentMap();

        @Override
        public IBlockInformation getPrimaryState()
        {
            return primaryState;
        }

        @Override
        public boolean isEmpty()
        {
            return countMap.isEmpty() || (countMap.size() == 1 && countMap.containsKey(BlockInformation.AIR));
        }

        @Override
        public Set<IBlockInformation> getContainedStates() {
            return this.countMap.keySet();
        }

        private void clear() {
            this.primaryState = BlockInformation.AIR;

            this.countMap.clear();
        }

        private void onBlockStateAdded(final IBlockInformation blockInformation) {
            countMap.putIfAbsent(blockInformation, 0);
            countMap.computeIfPresent(blockInformation, (state, currentCount) -> currentCount + 1);
            updatePrimaryState();
        }

        private void onBlockStateRemoved(final IBlockInformation blockInformation) {
            countMap.computeIfPresent(blockInformation, (state, currentCount) -> currentCount - 1);
            countMap.remove(blockInformation, 0);
            updatePrimaryState();
        }

        private void onBlockStateReplaced(final IBlockInformation currentInformation, final IBlockInformation newInformation) {
            countMap.computeIfPresent(currentInformation, (state, currentCount) -> currentCount - 1);
            countMap.remove(currentInformation, 0);
            countMap.putIfAbsent(newInformation, 0);
            countMap.computeIfPresent(newInformation, (state, currentCount) -> currentCount + 1);
            updatePrimaryState();
        }

        private void updatePrimaryState() {
            primaryState = this.countMap.entrySet().stream()
                             .filter(entry -> !entry.getKey().isAir())
                             .min((o1, o2) -> -1 * (o1.getValue() - o2.getValue()))
                             .map(Map.Entry::getKey)
                             .orElse(BlockInformation.AIR);
        }

        @Override
        public CompoundTag serializeNBT()
        {
            final CompoundTag nbt = new CompoundTag();

            nbt.put(NbtConstants.PRIMARY_STATE, this.primaryState.serializeNBT());

            final ListTag blockStateList = new ListTag();
            for (final Map.Entry<IBlockInformation, Integer> blockStateIntegerEntry : this.countMap.entrySet())
            {
                final CompoundTag stateNbt = new CompoundTag();

                stateNbt.put(NbtConstants.BLOCK_INFORMATION, blockStateIntegerEntry.getKey().serializeNBT());
                stateNbt.putInt(NbtConstants.COUNT, blockStateIntegerEntry.getValue());

                blockStateList.add(stateNbt);
            }

            nbt.put(NbtConstants.BLOCK_STATES, blockStateList);

            return nbt;
        }

        @Override
        public void deserializeNBT(final CompoundTag nbt)
        {
            this.countMap.clear();

            this.primaryState = new BlockInformation(nbt.getCompound(NbtConstants.PRIMARY_STATE));

            final ListTag blockStateList = nbt.getList(NbtConstants.BLOCK_STATES, Tag.TAG_COMPOUND);
            for (int i = 0; i < blockStateList.size(); i++)
            {
                final CompoundTag stateNbt = blockStateList.getCompound(i);

                final BlockInformation blockInformation = new BlockInformation(stateNbt.getCompound(NbtConstants.BLOCK_INFORMATION));

                this.countMap.put(
                  blockInformation,
                  stateNbt.getInt(NbtConstants.COUNT)
                );
            }
        }

        public void initializeFrom(final IStateEntryStorage compressedSection)
        {
            this.clear();

            compressedSection.count(countMap::putIfAbsent);
            updatePrimaryState();
        }
    }

    private final class LZ4StorageBasedStorageHandler implements IStorageHandler<LZ4StorageBasedStorageHandler.Payload>
    {


        @Override
        public LZ4StorageBasedStorageHandler.Payload readPayloadOffThread(CompoundTag nbt) {
            return LZ4DataCompressionUtils.decompress(nbt, compoundTag -> {
                final IStateEntryStorage storage = new SimpleStateEntryStorage();
                final Statistics mutableStatistics = new Statistics();

                storage.deserializeNBT(compoundTag.getCompound(NbtConstants.CHISELED_DATA));
                mutableStatistics.deserializeNBT(compoundTag.getCompound(NbtConstants.STATISTICS));

                return new LZ4StorageBasedStorageHandler.Payload(storage, mutableStatistics);
            });
        }

        @Override
        public void syncPayloadOnGameThread(LZ4StorageBasedStorageHandler.Payload payload) {
            compressedSection = payload.storage;
            statistics = payload.mutableStatistics;
        }

        private record Payload(IStateEntryStorage storage, Statistics mutableStatistics) {
        }

        @Override
        public CompoundTag serializeNBT()
        {
            return LZ4DataCompressionUtils.compress(compoundTag -> {
                compoundTag.put(NbtConstants.CHISELED_DATA, compressedSection.serializeNBT());
                compoundTag.put(NbtConstants.STATISTICS, statistics.serializeNBT());
            });
        }

        @Override
        public void deserializeNBT(final CompoundTag nbt)
        {
            LZ4DataCompressionUtils.decompress(nbt, compoundTag -> {
                compressedSection.deserializeNBT(compoundTag.getCompound(NbtConstants.CHISELED_DATA));
                statistics.deserializeNBT(compoundTag.getCompound(NbtConstants.STATISTICS));
            });
        }
    }

}

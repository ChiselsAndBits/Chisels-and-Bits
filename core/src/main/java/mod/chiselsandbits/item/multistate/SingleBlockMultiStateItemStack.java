package mod.chiselsandbits.item.multistate;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mod.chiselsandbits.api.block.storage.StateEntryStorage;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
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
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshot;
import mod.chiselsandbits.api.serialization.CBCodecs;
import mod.chiselsandbits.api.serialization.Serializable;
import mod.chiselsandbits.api.util.BlockPosForEach;
import mod.chiselsandbits.api.util.BlockPosStreamProvider;
import mod.chiselsandbits.api.util.LocalStrings;
import mod.chiselsandbits.api.util.VectorUtils;
import mod.chiselsandbits.api.util.constants.NbtConstants;
import mod.chiselsandbits.components.data.MultiStateItemStackData;
import mod.chiselsandbits.item.ChiseledBlockItem;
import mod.chiselsandbits.registrars.ModDataComponentTypes;
import mod.chiselsandbits.registrars.ModItems;
import mod.chiselsandbits.utils.MultiStateSnapshotUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class SingleBlockMultiStateItemStack implements IMultiStateItemStack
{

    private final ItemStack sourceStack;
    private final StateEntryStorage compressedSection;
    private Statistics statistics = new Statistics();

    public SingleBlockMultiStateItemStack(final ItemStack sourceStack) {
        this.sourceStack = sourceStack;

        final MultiStateItemStackData data = sourceStack.getOrDefault(ModDataComponentTypes.MULTI_STATE_ITEM_STACK_DATA.get(), MultiStateItemStackData.empty());

        this.compressedSection = data.storage().createSnapshot();
        this.statistics = new Statistics(
                data.statistics().primaryState(),
                new HashMap<>(data.statistics().counts())
        );
    }

    public SingleBlockMultiStateItemStack(final Item item, final StateEntryStorage compressedSection) {
        if (!(item instanceof IMultiStateItem))
            throw new IllegalArgumentException("The given item is not a MultiState Item");

        this.sourceStack = new ItemStack(item);

        this.compressedSection = compressedSection;
        this.statistics.initializeFrom(this.compressedSection);
    }

    /**
     * Checks if the given stack has data.
     *
     * @param stack The stack to check.
     * @return True if the stack has data, false otherwise.
     */
    public static boolean hasData(final ItemStack stack) {
        return stack.has(ModDataComponentTypes.MULTI_STATE_ITEM_STACK_DATA.get());
    }

    /**
     * Creates a new area shape identifier.
     * <p>
     * Note: This method always returns a new instance.
     *
     * @return The new identifier.
     */
    @Override
    public IAreaShapeIdentifier createNewShapeIdentifier() {
        return new ShapeIdentifier(this.compressedSection);
    }

    /**
     * Gives access to a stream with the entry state info inside the accessors range.
     *
     * @return The stream with the inner states.
     */
    @Override
    public Stream<IStateEntryInfo> stream() {
        return BlockPosStreamProvider.getForRange(StateEntrySize.current().getBitsPerBlockSide())
                .map(blockPos -> new StateEntry(
                                this.compressedSection.getBlockInformation(blockPos.getX(), blockPos.getY(), blockPos.getZ()),
                                blockPos
                        )
                );
    }

    /**
     * Gets the target state in the current area, using the offset from the area as well as the in area target offset.
     *
     * @param inAreaTarget The in area offset.
     * @return An optional potentially containing the state entry of the requested target.
     */
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

        final BlockPos inAreaPos = VectorUtils.toBlockPos(inAreaTarget.multiply(StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide()));

        final BlockInformation currentState = this.compressedSection.getBlockInformation(
                inAreaPos.getX(),
                inAreaPos.getY(),
                inAreaPos.getZ()
        );

        return currentState.isAir() ? Optional.empty() : Optional.of(new StateEntry(
                currentState,
                inAreaPos
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
    public Optional<IStateEntryInfo> getInBlockTarget(final BlockPos inAreaBlockPosOffset, final Vec3 inBlockTarget) {
        if (!inAreaBlockPosOffset.equals(BlockPos.ZERO)) {
            throw new IllegalStateException(String.format("The given in area block pos offset is not inside the current block: %s", inAreaBlockPosOffset));
        }

        return this.getInAreaTarget(
                inBlockTarget
        );
    }

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
    public boolean isInside(final BlockPos inAreaBlockPosOffset, final Vec3 inBlockTarget) {
        if (!inAreaBlockPosOffset.equals(BlockPos.ZERO)) {
            return false;
        }

        return this.isInside(
                inBlockTarget
        );
    }

    @Override
    public IMultiStateSnapshot createSnapshot() {
        return MultiStateSnapshotUtils.createFromStorage(compressedSection);
    }

    /**
     * The statistics of the itemstack.
     *
     * @return The statistics.
     */
    @Override
    public IStatistics getStatistics() {
        return statistics;
    }

    private MultiStateItemStackData toData() {
        return new MultiStateItemStackData(
                compressedSection,
                new MultiStateItemStackData.Statistics(
                        statistics.getPrimaryState(),
                        new HashMap<>(statistics.getCountMap())
                )
        );
    }

    /**
     * Converts this multistack itemstack data to an actual use able itemstack.
     *
     * @return The itemstack with the data of this multistate itemstack.
     */
    @Override
    public ItemStack toBlockStack() {
        if (this.sourceStack.getItem() instanceof IPatternItem) {
            //We were created with a pattern item, instead of the block item
            //Create a new item, and copy the nbt.
            final ChiseledBlockItem chiseledBlockItem = ModItems.CHISELED_BLOCK.get();
            final ItemStack blockStack = new ItemStack(chiseledBlockItem);
            blockStack.set(ModDataComponentTypes.MULTI_STATE_ITEM_STACK_DATA.get(), toData());
            blockStack.set(DataComponents.ITEM_NAME, LocalStrings.DefaultChiseledBlockItemName.getText(getStatistics().getPrimaryState().blockState().getBlock().getName()));

            return blockStack;
        }

        final ItemStack stack = sourceStack.copy();
        stack.set(ModDataComponentTypes.MULTI_STATE_ITEM_STACK_DATA.get(), toData());
        return stack;
    }

    @Override
    public ItemStack toPatternStack() {
        if (this.sourceStack.getItem() instanceof IPatternItem) {
            final ItemStack stack = this.sourceStack.copy();
            stack.set(ModDataComponentTypes.MULTI_STATE_ITEM_STACK_DATA.get(), toData());
            return stack;
        }

        final ItemStack singleUsePatternStack = new ItemStack(ModItems.SINGLE_USE_PATTERN_ITEM.get());
        singleUsePatternStack.set(ModDataComponentTypes.MULTI_STATE_ITEM_STACK_DATA.get(), toData());
        return singleUsePatternStack;
    }

    @Override
    public void writeDataTo(ItemStack stack) {
        stack.set(ModDataComponentTypes.MULTI_STATE_ITEM_STACK_DATA.get(), toData());
    }

    @Override
    public Stream<IStateEntryInfo> streamWithPositionMutator(final IPositionMutator positionMutator) {
        return BlockPosStreamProvider.getForRange(StateEntrySize.current().getBitsPerBlockSide())
                .map(positionMutator::mutate)
                .map(blockPos -> new StateEntry(
                                this.compressedSection.getBlockInformation(blockPos.getX(), blockPos.getY(), blockPos.getZ()),
                                blockPos
                        )
                );
    }

    @Override
    public void forEachWithPositionMutator(
            final IPositionMutator positionMutator, final Consumer<IStateEntryInfo> consumer) {
        BlockPosForEach.forEachInRange(StateEntrySize.current().getBitsPerBlockSide(), (BlockPos blockPos) -> {
            final Vec3i pos = positionMutator.mutate(blockPos);
            consumer.accept(new StateEntry(
                    this.compressedSection.getBlockInformation(pos.getX(), pos.getY(), pos.getZ()),
                    pos
            ));
        });
    }

    @Override
    public void rotate(final Direction.Axis axis, final int rotationCount) {
        this.compressedSection.rotate(axis, rotationCount);
        this.statistics.clear();

        BlockPosStreamProvider.getForRange(StateEntrySize.current().getBitsPerBlockSide())
                .forEach(position -> this.statistics.onBlockStateAdded(
                        this.compressedSection.getBlockInformation(position.getX(), position.getY(), position.getZ())
                ));
    }

    @Override
    public void mirror(final Direction.Axis axis) {
        this.compressedSection.mirror(axis);
        this.statistics.clear();

        BlockPosStreamProvider.getForRange(StateEntrySize.current().getBitsPerBlockSide())
                .forEach(position -> this.statistics.onBlockStateAdded(
                        this.compressedSection.getBlockInformation(position.getX(), position.getY(), position.getZ())
                ));
    }

    @Override
    public int hashCode() {
        return createNewShapeIdentifier().hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof IAreaAccessor accessor))
            return false;

        return createNewShapeIdentifier().equals(accessor.createNewShapeIdentifier());
    }

    @Override
    public @NotNull AABB getBoundingBox() {
        return new AABB(0, 0, 0, 1, 1, 1);
    }

    private static final class ShapeIdentifier implements IArrayBackedAreaShapeIdentifier {
        private final StateEntryStorage snapshot;

        private ShapeIdentifier(final StateEntryStorage chunkSection) {
            snapshot = chunkSection.createSnapshot();
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
        public long[] getBackingData() {
            return snapshot.getRawData();
        }

        @Override
        public List<BlockInformation> getPalette() {
            return snapshot.states();
        }
    }

    private static final class StateEntry implements IStateEntryInfo {

        private final BlockInformation blockInformation;
        private final Vec3 startPoint;
        private final Vec3 endPoint;

        public StateEntry(
                final BlockInformation blockInformation,
                final Vec3i startPoint) {
            this(
                    blockInformation,
                    Vec3.atLowerCornerOf(startPoint).multiply(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit()),
                    Vec3.atLowerCornerOf(startPoint).multiply(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit()).add(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit())
            );
        }

        private StateEntry(
                final BlockInformation blockInformation,
                final Vec3 startPoint,
                final Vec3 endPoint) {
            this.blockInformation = blockInformation;
            this.startPoint = startPoint;
            this.endPoint = endPoint;
        }

        @Override
        public @NotNull BlockInformation getBlockInformation() {
            return blockInformation;
        }

        @Override
        public @NotNull Vec3 getStartPoint() {
            return startPoint;
        }

        @Override
        public @NotNull Vec3 getEndPoint() {
            return endPoint;
        }
    }

    private static final class Statistics implements IStatistics, Serializable.Registry<Statistics> {

        private static final Codec<Statistics> CODEC = RecordCodecBuilder.create(instance ->  instance.group(
                BlockInformation.CODEC.fieldOf(NbtConstants.PRIMARY_STATE).forGetter(Statistics::getPrimaryState),
                CBCodecs.unboundedComplexMap(BlockInformation.CODEC, Codec.INT).fieldOf(NbtConstants.BLOCK_STATES).forGetter(Statistics::getCountMap)
        ).apply(instance, Statistics::new));

        private static final MapCodec<Statistics> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                BlockInformation.CODEC.fieldOf(NbtConstants.PRIMARY_STATE).forGetter(Statistics::getPrimaryState),
                CBCodecs.unboundedComplexMap(BlockInformation.CODEC, Codec.INT).fieldOf(NbtConstants.BLOCK_STATES).forGetter(Statistics::getCountMap)
        ).apply(instance, Statistics::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, Statistics> STREAM_CODEC = StreamCodec.composite(
                BlockInformation.STREAM_CODEC,
                Statistics::getPrimaryState,
                ByteBufCodecs.map(HashMap::new, BlockInformation.STREAM_CODEC, ByteBufCodecs.VAR_INT),
                Statistics::getCountMap,
                Statistics::new
        );

        private BlockInformation primaryState;
        private final Map<BlockInformation, Integer> countMap;

        public Statistics(BlockInformation primaryState, Map<BlockInformation, Integer> countMap) {
            this.countMap = countMap;
            this.primaryState = primaryState;
        }

        public Statistics() {
            this(BlockInformation.AIR, Maps.newHashMap());
        }

        @Override
        public BlockInformation getPrimaryState() {
            return primaryState;
        }

        public Map<BlockInformation, Integer> getCountMap() {
            return countMap;
        }

        @Override
        public boolean isEmpty() {
            return countMap.isEmpty() || (countMap.size() == 1 && countMap.containsKey(BlockInformation.AIR));
        }

        @Override
        public Set<BlockInformation> getContainedStates() {
            return this.countMap.keySet();
        }

        private void clear() {
            this.primaryState = BlockInformation.AIR;

            this.countMap.clear();
        }

        private void onBlockStateAdded(final BlockInformation blockInformation) {
            countMap.putIfAbsent(blockInformation, 0);
            countMap.computeIfPresent(blockInformation, (state, currentCount) -> currentCount + 1);
            updatePrimaryState();
        }

        private void updatePrimaryState() {
            primaryState = this.countMap.entrySet().stream()
                    .filter(entry -> !entry.getKey().isAir())
                    .min((o1, o2) -> -1 * (o1.getValue() - o2.getValue()))
                    .map(Map.Entry::getKey)
                    .orElse(BlockInformation.AIR);
        }

        public void initializeFrom(final StateEntryStorage compressedSection) {
            this.clear();

            compressedSection.count(countMap::putIfAbsent);
            updatePrimaryState();
        }

        @Override
        public Codec<Statistics> codec() {
            return CODEC;
        }

        @Override
        public MapCodec<Statistics> mapCodec() {
            return MAP_CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, Statistics> streamCodec() {
            return STREAM_CODEC;
        }
    }
}

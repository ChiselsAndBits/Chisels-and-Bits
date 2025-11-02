package mod.chiselsandbits.multistate.snapshot;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mod.chiselsandbits.api.block.storage.StateEntryStorage;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.exceptions.SpaceOccupiedException;
import mod.chiselsandbits.api.item.multistate.IMultiStateItemStack;
import mod.chiselsandbits.api.item.multistate.IStatistics;
import mod.chiselsandbits.api.multistate.StateEntrySize;
import mod.chiselsandbits.api.multistate.accessor.ISingleBlockAxisAlignedAreaAccessor;
import mod.chiselsandbits.api.multistate.accessor.IStateEntryInfo;
import mod.chiselsandbits.api.multistate.accessor.identifier.IAreaShapeIdentifier;
import mod.chiselsandbits.api.multistate.accessor.identifier.IArrayBackedAreaShapeIdentifier;
import mod.chiselsandbits.api.multistate.accessor.sortable.IPositionMutator;
import mod.chiselsandbits.api.multistate.mutator.IMutableStateEntryInfo;
import mod.chiselsandbits.api.multistate.mutator.callback.StateClearer;
import mod.chiselsandbits.api.multistate.mutator.callback.StateSetter;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshot;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshotType;
import mod.chiselsandbits.api.multistate.statistics.IMultiStateObjectStatistics;
import mod.chiselsandbits.api.serialization.CBCodecs;
import mod.chiselsandbits.api.serialization.CBStreamCodecs;
import mod.chiselsandbits.api.util.BlockPosForEach;
import mod.chiselsandbits.api.util.BlockPosStreamProvider;
import mod.chiselsandbits.api.util.VectorUtils;
import mod.chiselsandbits.api.util.constants.NbtConstants;
import mod.chiselsandbits.item.ChiseledBlockItem;
import mod.chiselsandbits.item.multistate.SingleBlockMultiStateItemStack;
import mod.chiselsandbits.registrars.ModItems;
import mod.chiselsandbits.registrars.ModMultiStateSnapshotTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class SimpleSnapshot implements IMultiStateSnapshot, ISingleBlockAxisAlignedAreaAccessor {

    public static final Codec<SimpleSnapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StateEntryStorage.CODEC.fieldOf(NbtConstants.STORAGE).forGetter(SimpleSnapshot::getChunkSection),
            SimpleStatistics.CODEC.fieldOf(NbtConstants.STATISTICS).forGetter(SimpleSnapshot::getStateObjectStatistics)
    ).apply(instance, SimpleSnapshot::new));

    public static final MapCodec<SimpleSnapshot> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StateEntryStorage.CODEC.fieldOf(NbtConstants.STORAGE).forGetter(SimpleSnapshot::getChunkSection),
            SimpleStatistics.CODEC.fieldOf(NbtConstants.STATISTICS).forGetter(SimpleSnapshot::getStateObjectStatistics)
    ).apply(instance, SimpleSnapshot::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SimpleSnapshot> STREAM_CODEC = StreamCodec.composite(
            StateEntryStorage.STREAM_CODEC,
            SimpleSnapshot::getChunkSection,
            SimpleStatistics.STREAM_CODEC.apply(CBStreamCodecs.nullable()),
            SimpleSnapshot::getStateObjectStatistics,
            SimpleSnapshot::new
    );

    private final StateEntryStorage chunkSection;
    private SimpleStatistics stateObjectStatistics = null;

    public SimpleSnapshot(final BlockInformation blockInformation) {
        this.chunkSection = new StateEntryStorage();

        this.chunkSection.initializeWith(blockInformation);
    }

    public SimpleSnapshot(final StateEntryStorage chunkSection) {
        this.chunkSection = chunkSection;
    }

    public SimpleSnapshot(StateEntryStorage chunkSection, SimpleStatistics stateObjectStatistics) {
        this.chunkSection = chunkSection;
        this.stateObjectStatistics = stateObjectStatistics;
    }

    private StateEntryStorage getChunkSection() {
        return chunkSection;
    }

    private SimpleStatistics getStateObjectStatistics() {
        return stateObjectStatistics;
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
        return new SimpleSnapshot.Identifier(this.chunkSection);
    }

    @Override
    public Stream<IStateEntryInfo> stream() {
        return BlockPosStreamProvider.getForRange(StateEntrySize.current().getBitsPerBlockSide())
                .map(blockPos -> new SimpleSnapshot.StateEntry(
                        chunkSection.getBlockInformation(blockPos.getX(), blockPos.getY(), blockPos.getZ()),
                        blockPos,
                        this::setInAreaTarget,
                        this::clearInAreaTarget)
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

        return isInside(inBlockTarget);
    }

    @Override
    public IMultiStateSnapshot createSnapshot() {
        return new SimpleSnapshot(chunkSection.createSnapshot());
    }

    @Override
    public Stream<IStateEntryInfo> streamWithPositionMutator(final IPositionMutator positionMutator) {
        return BlockPosStreamProvider.getForRange(StateEntrySize.current().getBitsPerBlockSide())
                .map(positionMutator::mutate)
                .map(blockPos -> new SimpleSnapshot.StateEntry(
                        this.chunkSection.getBlockInformation(blockPos.getX(), blockPos.getY(), blockPos.getZ()),
                        blockPos,
                        this::setInAreaTarget,
                        this::clearInAreaTarget)
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
    public Optional<IStateEntryInfo> getInAreaTarget(final Vec3 inAreaTarget) {
        if (inAreaTarget.x() < 0 ||
                inAreaTarget.y() < 0 ||
                inAreaTarget.z() < 0 ||
                inAreaTarget.x() >= 1 ||
                inAreaTarget.y() >= 1 ||
                inAreaTarget.z() >= 1) {
            throw new IllegalArgumentException("Target is not in the current area.");
        }

        final BlockPos inAreaPos = VectorUtils.toBlockPos(inAreaTarget.multiply(StateEntrySize.current().getBitsPerBlockSide(),
                StateEntrySize.current().getBitsPerBlockSide(),
                StateEntrySize.current().getBitsPerBlockSide()));

        final BlockInformation currentState = this.chunkSection.getBlockInformation(
                inAreaPos.getX(),
                inAreaPos.getY(),
                inAreaPos.getZ()
        );

        return currentState.isAir() ? Optional.empty() : Optional.of(new SimpleSnapshot.StateEntry(
                currentState,
                inAreaPos,
                this::setInAreaTarget,
                this::clearInAreaTarget)
        );
    }

    @Override
    public void forEachWithPositionMutator(
            final IPositionMutator positionMutator, final Consumer<IStateEntryInfo> consumer) {
        BlockPosForEach.forEachInRange(StateEntrySize.current().getBitsPerBlockSide(), (blockPos) -> {
            final Vec3i target = positionMutator.mutate(blockPos);
            consumer.accept(new SimpleSnapshot.StateEntry(
                    this.chunkSection.getBlockInformation(target.getX(), target.getY(), target.getZ()),
                    target,
                    this::setInAreaTarget,
                    this::clearInAreaTarget));
        });
    }

    @Override
    public IStatistics getStatistics()
    {
        return stateObjectStatistics;
    }

    /**
     * Returns all entries in the current area in a mutable fashion. Includes all empty areas as areas containing an air state.
     *
     * @return A stream with a mutable state entry info for each mutable section in the area.
     */
    @Override
    public Stream<IMutableStateEntryInfo> mutableStream() {
        return BlockPosStreamProvider.getForRange(StateEntrySize.current().getBitsPerBlockSide())
                .map(blockPos -> new SimpleSnapshot.StateEntry(
                        chunkSection.getBlockInformation(blockPos.getX(), blockPos.getY(), blockPos.getZ()),
                        blockPos,
                        this::setInAreaTarget,
                        this::clearInAreaTarget)
                );
    }

    @Override
    public void setInAreaTarget(
            final BlockInformation blockInformation,
            final Vec3 inAreaTarget)
            throws SpaceOccupiedException {
        if (inAreaTarget.x() < 0 ||
                inAreaTarget.y() < 0 ||
                inAreaTarget.z() < 0 ||
                inAreaTarget.x() >= 1 ||
                inAreaTarget.y() >= 1 ||
                inAreaTarget.z() >= 1) {
            throw new IllegalArgumentException("Target is not in the current area.");
        }

        final BlockPos inAreaPos = VectorUtils.toBlockPos(inAreaTarget.multiply(StateEntrySize.current().getBitsPerBlockSide(),
                StateEntrySize.current().getBitsPerBlockSide(),
                StateEntrySize.current().getBitsPerBlockSide()));

        final BlockInformation currentState = this.chunkSection.getBlockInformation(inAreaPos.getX(), inAreaPos.getY(), inAreaPos.getZ());
        if (!currentState.isAir()) {
            throw new SpaceOccupiedException();
        }

        this.chunkSection.setBlockInformation(
                inAreaPos.getX(),
                inAreaPos.getY(),
                inAreaPos.getZ(),
                blockInformation
        );

        resetStatistics();
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
    public void setInBlockTarget(final BlockInformation blockInformation, final BlockPos inAreaBlockPosOffset, final Vec3 inBlockTarget) throws SpaceOccupiedException {
        if (!inAreaBlockPosOffset.equals(BlockPos.ZERO)) {
            throw new IllegalStateException(String.format("The given in area block pos offset is not inside the current block: %s", inAreaBlockPosOffset));
        }

        this.setInAreaTarget(
                blockInformation,
                inBlockTarget);
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

        final BlockPos inAreaPos = VectorUtils.toBlockPos(inAreaTarget.multiply(StateEntrySize.current().getBitsPerBlockSide(),
                StateEntrySize.current().getBitsPerBlockSide(),
                StateEntrySize.current().getBitsPerBlockSide()));

        final BlockInformation blockState = BlockInformation.AIR;

        this.chunkSection.setBlockInformation(
                inAreaPos.getX(),
                inAreaPos.getY(),
                inAreaPos.getZ(),
                blockState
        );

        resetStatistics();
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
    public IMultiStateSnapshotType getType() {
        return ModMultiStateSnapshotTypes.SIMPLE.get();
    }

    /**
     * Converts the current snapshot to a variant which is itemstack capable.
     *
     * @return The multistate itemstack which is the itemstack nbt representation of the current snapshot.
     */
    @Override
    public IMultiStateItemStack toItemStack() {
        final ChiseledBlockItem chiseledBlockItem = ModItems.CHISELED_BLOCK.get();

        return new SingleBlockMultiStateItemStack(chiseledBlockItem, this.chunkSection.createSnapshot());
    }

    @Override
    public IMultiStateObjectStatistics getStatics() {
        if (this.stateObjectStatistics == null) {
            buildStatistics();
        }

        return this.stateObjectStatistics;
    }

    private void resetStatistics() {
        this.stateObjectStatistics = null;
    }

    private void buildStatistics() {
        this.stateObjectStatistics = new SimpleStatistics(this.chunkSection);
    }

    @Override
    public void rotate(final Direction.Axis axis, final int rotationCount) {
        this.chunkSection.rotate(axis, rotationCount);
    }

    @Override
    public void mirror(final Direction.Axis axis) {
        this.chunkSection.mirror(axis);
    }

    @Override
    public IMultiStateSnapshot clone() {
        return new SimpleSnapshot(
                this.chunkSection.createSnapshot()
        );
    }

    @Override
    public @NotNull AABB getBoundingBox() {
        return new AABB(0, 0, 0, 1, 1, 1);
    }

    private static class StateEntry implements IMutableStateEntryInfo {
        private final BlockInformation blockInformation;
        private final Vec3 startPoint;
        private final Vec3 endPoint;
        private final StateSetter stateSetter;
        private final StateClearer stateClearer;

        private StateEntry(
                final BlockInformation blockInformation,
                final Vec3i startPoint,
                final StateSetter stateSetter,
                final StateClearer stateClearer) {
            this.blockInformation = blockInformation;
            this.startPoint = Vec3.atLowerCornerOf(startPoint)
                    .multiply(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit());
            this.endPoint = Vec3.atLowerCornerOf(startPoint)
                    .multiply(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit())
                    .add(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit());
            this.stateSetter = stateSetter;
            this.stateClearer = stateClearer;
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

        @Override
        public void setBlockInformation(final BlockInformation blockInformation) throws SpaceOccupiedException {
            stateSetter.set(blockInformation, getStartPoint());
        }

        @Override
        public void clear() {
            stateClearer.accept(getStartPoint());
        }
    }

    private static class Identifier implements IArrayBackedAreaShapeIdentifier {
        private final StateEntryStorage snapshot;

        private Identifier(final StateEntryStorage section) {
            this.snapshot = section.createSnapshot();
        }

        @Override
        public int hashCode() {
            return snapshot.hashCode();
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof final IArrayBackedAreaShapeIdentifier that)) {
                return false;
            }
            return Arrays.equals(this.getBackingData(), that.getBackingData()) && that.getPalette().equals(this.getPalette());
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


    public static final class SimpleStatistics implements IMultiStateObjectStatistics {

        public static final Codec<SimpleStatistics> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockInformation.CODEC.fieldOf(NbtConstants.BLOCK_INFORMATION).forGetter(SimpleStatistics::getPrimaryState),
                CBCodecs.unboundedComplexMap(BlockInformation.CODEC, Codec.INT).fieldOf(NbtConstants.STATE_COUNTS).forGetter(SimpleStatistics::getStateCounts)
        ).apply(instance, SimpleStatistics::new));

        public static final MapCodec<SimpleStatistics> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                BlockInformation.CODEC.fieldOf(NbtConstants.BLOCK_INFORMATION).forGetter(SimpleStatistics::getPrimaryState),
                CBCodecs.unboundedComplexMap(BlockInformation.CODEC, Codec.INT).fieldOf(NbtConstants.STATE_COUNTS).forGetter(SimpleStatistics::getStateCounts)
        ).apply(instance, SimpleStatistics::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, SimpleStatistics> STREAM_CODEC = StreamCodec.composite(
                BlockInformation.STREAM_CODEC,
                SimpleStatistics::getPrimaryState,
                ByteBufCodecs.map(HashMap::new, BlockInformation.STREAM_CODEC, ByteBufCodecs.INT),
                SimpleStatistics::getStateCounts,
                SimpleStatistics::new
        );

        private final BlockInformation primaryState;
        private final Map<BlockInformation, Integer> stateCounts;

        public SimpleStatistics(StateEntryStorage storage) {
            this.primaryState = determinePrimaryState(storage);
            this.stateCounts = determineStateCounts(storage);
        }

        public SimpleStatistics(BlockInformation primaryState, Map<BlockInformation, Integer> stateCounts) {
            this.primaryState = primaryState;
            this.stateCounts = stateCounts;
        }

        private static BlockInformation determinePrimaryState(StateEntryStorage storage) {
            final Map<BlockInformation, Integer> countMap = Maps.newHashMap();

            storage.count(countMap::put);

            BlockInformation maxState = BlockInformation.AIR;
            int maxCount = 0;
            for (final Map.Entry<BlockInformation, Integer> blockStateIntegerEntry : countMap.entrySet()) {
                if (maxCount < blockStateIntegerEntry.getValue() && !blockStateIntegerEntry.getKey().isAir()) {
                    maxState = blockStateIntegerEntry.getKey();
                    maxCount = blockStateIntegerEntry.getValue();
                }
            }

            return maxState;
        }

        private static Map<BlockInformation, Integer> determineStateCounts(StateEntryStorage storage) {
            return storage.count();
        }

        @Override
        public BlockInformation getPrimaryState ()
        {
            return primaryState;
        }

        @Override
        public Map<BlockInformation, Integer> getStateCounts ()
        {
            return stateCounts;
        }

        @Override
        public boolean shouldCheckWeakPower ()
        {
            throw new NotImplementedException("Is a snapshot");
        }

        @Override
        public float getFullnessFactor ()
        {
            throw new NotImplementedException("Is a snapshot");
        }

        @Override
        public float getSlipperiness ()
        {
            throw new NotImplementedException("Is a snapshot");
        }

        @Override
        public float getLightEmissionFactor ()
        {
            throw new NotImplementedException("Is a snapshot");
        }

        @Override
        public float getLightBlockingFactor ()
        {
            throw new NotImplementedException("Is a snapshot");
        }

        @Override
        public float getRelativeBlockHardness ( final Player player)
        {
            throw new NotImplementedException("Is a snapshot");
        }

        @Override
        public boolean canPropagateSkylight ()
        {
            throw new NotImplementedException("Is a snapshot");
        }

        @Override
        public boolean canSustainGrassBelow ()
        {
            throw new NotImplementedException("Is a snapshot");
        }

        @Override
        public boolean isEmpty ()
        {
            return !primaryState.isAir();
        }

        @Override
        public Codec<?> codec() {
            return CODEC;
        }

        @Override
        public MapCodec<?> mapCodec() {
            return MAP_CODEC;
        }

        @Override
        public StreamCodec<?, ?> streamCodec() {
            return STREAM_CODEC;
        }
    }


}

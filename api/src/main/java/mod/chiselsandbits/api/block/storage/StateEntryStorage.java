package mod.chiselsandbits.api.block.storage;

import com.google.common.collect.Maps;
import com.google.common.math.LongMath;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.config.IServerConfiguration;
import mod.chiselsandbits.api.multistate.StateEntrySize;
import mod.chiselsandbits.api.multistate.mutator.IMirrorAndRotateble;
import mod.chiselsandbits.api.serialization.CBCodecs;
import mod.chiselsandbits.api.serialization.CBStreamCodecs;
import mod.chiselsandbits.api.serialization.Serializable;
import mod.chiselsandbits.api.util.*;
import mod.chiselsandbits.api.util.constants.NbtConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;

import java.math.RoundingMode;
import java.util.*;
import java.util.function.BiConsumer;

public final class StateEntryStorage implements IMirrorAndRotateble, IWithBatchableMutationSupport, Serializable<StateEntryStorage, RegistryFriendlyByteBuf> {

    public static final Codec<StateEntryStorage> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    StateEntryPalette.CODEC.fieldOf(NbtConstants.PALETTE).forGetter(StateEntryStorage::palette),
                    CBCodecs.BIT_SET.fieldOf(NbtConstants.DATA).forGetter(StateEntryStorage::getData)
            ).apply(instance, StateEntryStorage::new)
    );

    public static final Codec<StateEntryStorage> LEGACY_CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    StateEntryPalette.LEGACY_CODEC.fieldOf(NbtConstants.PALETTE).forGetter(StateEntryStorage::palette),
                    CBCodecs.BIT_SET.fieldOf(NbtConstants.DATA).forGetter(StateEntryStorage::getData)
            ).apply(instance, StateEntryStorage::new)
    );

    public static final MapCodec<StateEntryStorage> MAP_CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    StateEntryPalette.MAP_CODEC.fieldOf(NbtConstants.PALETTE).forGetter(StateEntryStorage::palette),
                    CBCodecs.BIT_SET.fieldOf(NbtConstants.DATA).forGetter(StateEntryStorage::getData)
            ).apply(instance, StateEntryStorage::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, StateEntryStorage> STREAM_CODEC = StreamCodec.composite(
            StateEntryPalette.STREAM_CODEC,
            StateEntryStorage::palette,
            CBStreamCodecs.BIT_SET,
            StateEntryStorage::getData,
            StateEntryStorage::new
    );

    private final int size;
    private final StateEntryPalette palette;
    private final List<IBatchMutation> ongoingBatchMutations = new ArrayList<>();

    private BitSet data = new BitSet();
    private int entryWidth = 0;

    public StateEntryStorage() {
        this(IServerConfiguration.getInstance().getBitSize().get().getBitsPerBlockSide());
    }

    private StateEntryStorage(final StateEntryStorage stateEntryStorage) {
        this.size = stateEntryStorage.size;
        this.palette = new StateEntryPalette(stateEntryStorage.palette);
        this.data = BitSet.valueOf(stateEntryStorage.data.toLongArray());
        this.entryWidth = stateEntryStorage.entryWidth;
    }

    public StateEntryStorage(final int size) {
        this.size = size;
        this.palette = new StateEntryPalette();
    }

    public StateEntryStorage(final StateEntryPalette palette, final BitSet data) {
        this.size = StateEntrySize.ONE_SIXTEENTH.getBitsPerBlockSide();
        this.palette = palette;
        this.data = data;
        this.entryWidth = LongMath.log2(palette.size(), RoundingMode.CEILING);
    }

    public int getSize() {
        return size;
    }

    private int getTotalEntryCount() {
        return size * size * size;
    }

    public void clear() {
        this.data = new BitSet();
        this.entryWidth = 0;
        this.palette.clear().whenSizeChanged(this::onPaletteResize);
    }

    private void resetData() {
        this.data = new BitSet();
    }

    public void initializeWith(final BlockInformation currentState) {
        clear();
        if (currentState.blockState() == Blocks.AIR.defaultBlockState()) {
            return;
        }

        final var result = palette.getIndex(currentState);
        result.whenSizeChanged(this::onPaletteResize);
        final int blockStateId = result.value();
        this.data = ByteArrayUtils.fill(blockStateId, entryWidth, getTotalEntryCount());
    }

    public void loadFromChunkSection(final LevelChunkSection chunkSection) {
        if (this.size != StateEntrySize.ONE_SIXTEENTH.getBitsPerBlockSide())
            throw new IllegalStateException("Updating to the new storage format is only possible on the default 1/16th size.");

        this.clear();

        try (IBatchMutation ignored = batch()) {
            BlockPosStreamProvider.getForRange(StateEntrySize.ONE_SIXTEENTH.getBitsPerBlockSide())
                    .forEach(position -> setBlockInformation(
                            position.getX(),
                            position.getY(),
                            position.getZ(),
                            new BlockInformation(
                                    chunkSection.getBlockState(position.getX(), position.getY(), position.getZ()),
                                    Optional.empty()
                            )
                    ));
        }
    }

    public BlockInformation getBlockInformation(final int x, final int y, final int z) {
        final int offSetIndex = doCalculatePositionIndex(x, y, z);
        final int blockStateId = ByteArrayUtils.getValueAt(data, entryWidth, offSetIndex);

        return palette.getBlockState(blockStateId);
    }

    public void setBlockInformation(final int x, final int y, final int z, final BlockInformation blockState) {
        final int offSetIndex = doCalculatePositionIndex(x, y, z);
        final var result = palette.getIndex(blockState);
        result.whenSizeChanged(this::onPaletteResize);
        final int blockStateId = result.value();

        ensureCapacity();

        ByteArrayUtils.setValueAt(data, blockStateId, entryWidth, offSetIndex);
    }

    private void ensureCapacity() {
        final int requiredSize = (int) Math.ceil((getTotalEntryCount() * entryWidth) / (float) Long.SIZE);
        if (data.length() < requiredSize) {
            final long[] rawData = getRawData();
            final long[] newData = new long[requiredSize];
            System.arraycopy(rawData, 0, newData, 0, rawData.length);
            this.data = BitSet.valueOf(newData);
        } else if (this.ongoingBatchMutations.isEmpty()) {
            this.data = BitSet.valueOf(getRawData());
        }
    }

    private int doCalculatePositionIndex(final int x, final int y, final int z) {
        return x * size * size + y * size + z;
    }

    private Vec3i doCalculatePosition(final int index) {
        final int x = index / (size * size);
        final int y = (index - x * size * size) / size;
        final int z = index - x * size * size - y * size;

        return new Vec3i(x, y, z);
    }

    public void count(final BiConsumer<BlockInformation, Integer> storageConsumer) {
        count().forEach(storageConsumer);
    }

    public Map<BlockInformation, Integer> count() {
        final Map<BlockInformation, Integer> countMap = Maps.newHashMap();

        BlockPosStreamProvider.getForRange(this.getSize())
                .map(position -> getBlockInformation(position.getX(), position.getY(), position.getZ()))
                .forEach(blockState -> countMap.compute(blockState, (state, count) -> count == null ? 1 : count + 1));

        return countMap;
    }

    public BitSet getData() {
        return data;
    }

    public long[] getRawData() {
        return this.data.toLongArray();
    }

    public StateEntryStorage createSnapshot() {
        return new StateEntryStorage(this);
    }

    public void fillFromBottom(final BlockInformation state, final int entries) {
        clear();
        final int loopCount = Math.max(0, Math.min(entries, StateEntrySize.current().getBitsPerBlock()));
        if (loopCount == 0)
            return;

        int count = 0;
        try (IBatchMutation ignored = batch()) {
            for (int y = 0; y < getSize(); y++) {
                for (int x = 0; x < getSize(); x++) {
                    for (int z = 0; z < getSize(); z++) {
                        setBlockInformation(
                                x, y, z,
                                state
                        );

                        count++;
                        if (count == loopCount)
                            return;
                    }
                }
            }
        }
    }

    public StateEntryStorage limitedToProgress(final float progress)
    {
        final var noneAirCounts = count().entrySet()
            .stream()
            .filter(e -> !e.getKey().isAir())
            .mapToInt(Map.Entry::getValue)
            .sum();

        final var allowedCounts = (int) Math.floor(noneAirCounts * progress);
        final var result = new StateEntryStorage();
        if (allowedCounts == 0)
            return result;

        int currentCount = 0;
        for (int y = 0; y < this.size; y++)
        {
            for (int z = 0; z < this.size; z++)
            {
                for (int x = 0; x < this.size; x++)
                {
                    final BlockInformation target = getBlockInformation(x, y, z);
                    if (target.isAir())
                        continue;

                    currentCount++;
                    result.setBlockInformation(x,y,z, target);

                    if (currentCount == allowedCounts)
                        return result;
                }
            }
        }

        return result;
    }

    public StateEntryPalette palette() {
        return palette;
    }

    public List<BlockInformation> states() {
        return palette.states();
    }

    public void rotate(final Direction.Axis axis, final int rotationCount) {
        if (rotationCount == 0)
            return;

        final StateEntryStorage clone = this.createSnapshot();
        resetData();

        final Vec3 centerVector = new Vec3(7.5d, 7.5d, 7.5d);

        try (IBatchMutation ignored = batch()) {
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        final Vec3 workingVector = new Vec3(x, y, z);
                        Vec3 rotatedVector = workingVector.subtract(centerVector);
                        for (int i = 0; i < rotationCount; i++) {
                            rotatedVector = VectorUtils.rotate90Degrees(rotatedVector, axis);
                        }

                        final BlockPos sourcePos = VectorUtils.toBlockPos(workingVector);
                        final Vec3 offsetPos = rotatedVector.add(centerVector).multiply(1000, 1000, 1000);
                        final Vec3 exactTargetPos = new Vec3(Math.round(offsetPos.x()), Math.round(offsetPos.y()), Math.round(offsetPos.z())).multiply(1 / 1000d, 1 / 1000d, 1 / 1000d);
                        final BlockPos targetPos = VectorUtils.toBlockPos(exactTargetPos);

                        this.setBlockInformation(
                                targetPos.getX(),
                                targetPos.getY(),
                                targetPos.getZ(),
                                clone.getBlockInformation(
                                        sourcePos.getX(),
                                        sourcePos.getY(),
                                        sourcePos.getZ()
                                )
                        );
                    }
                }
            }
        }
    }

    public void mirror(final Direction.Axis axis) {
        final StateEntryStorage clone = this.createSnapshot();
        resetData();

        try (IBatchMutation ignored = batch()) {
            for (int y = 0; y < getSize(); y++) {
                for (int x = 0; x < getSize(); x++) {
                    for (int z = 0; z < getSize(); z++) {
                        final BlockInformation blockInformation = clone.getBlockInformation(x, y, z);

                        final int mirroredX = axis == Direction.Axis.X ? (getSize() - x - 1) : x;
                        final int mirroredY = axis == Direction.Axis.Y ? (getSize() - y - 1) : y;
                        final int mirroredZ = axis == Direction.Axis.Z ? (getSize() - z - 1) : z;

                        this.setBlockInformation(
                                mirroredX, mirroredY, mirroredZ,
                                blockInformation
                        );
                    }
                }
            }
        }
    }

    private void onPaletteResize(final int newSize) {
        final int currentEntryWidth = this.entryWidth;
        this.entryWidth = LongMath.log2(newSize, RoundingMode.CEILING);

        if (this.entryWidth != currentEntryWidth) {
            //We need to update the data array to match the new palette size
            final BitSet rawData = this.data;

            this.data = new BitSet(getTotalEntryCount() * entryWidth);
            BlockPosStreamProvider.getForRange(getSize())
                    .mapToInt(pos -> doCalculatePositionIndex(pos.getX(), pos.getY(), pos.getZ()))
                    .mapToObj(index -> Pair.of(index, ByteArrayUtils.getValueAt(rawData, currentEntryWidth, index)))
                    .forEach(pair -> ByteArrayUtils.setValueAt(this.data, pair.getSecond(), this.entryWidth, pair.getFirst()));
        }
    }

    public IBatchMutation batch() {
        final IBatchMutation mutation = new IBatchMutation() {
            public void close() {
                StateEntryStorage.this.ongoingBatchMutations.remove(this);
            }
        };

        ongoingBatchMutations.add(mutation);

        this.data = BitSet.valueOf(this.data.toLongArray());

        return mutation;
    }

    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof final StateEntryStorage that)) {
            return false;
        }

        if (entryWidth != that.entryWidth) {
            return false;
        }
        if (!palette.equals(that.palette)) {
            return false;
        }
        return getData().equals(that.getData());
    }

    public int hashCode() {
        int result = palette.hashCode();
        result = 31 * result + getData().hashCode();
        result = 31 * result + entryWidth;
        return result;
    }

    public String toString() {
        return "SimpleStateEntryStorage{" +
                "palette=" + palette +
                ", data=" + data +
                ", entryWidth=" + entryWidth +
                '}';
    }

    public Codec<StateEntryStorage> codec() {
        return CODEC;
    }

    public MapCodec<StateEntryStorage> mapCodec() {
        return MAP_CODEC;
    }

    public StreamCodec<RegistryFriendlyByteBuf, StateEntryStorage> streamCodec() {
        return STREAM_CODEC;
    }
}

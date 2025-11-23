package mod.chiselsandbits.multistate.snapshot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import mod.chiselsandbits.api.axissize.CollisionType;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.exceptions.SpaceOccupiedException;
import mod.chiselsandbits.api.item.multistate.IMultiStateItemStack;
import mod.chiselsandbits.api.multistate.StateEntrySize;
import mod.chiselsandbits.api.multistate.accessor.IAreaAccessor;
import mod.chiselsandbits.api.multistate.accessor.IStateEntryInfo;
import mod.chiselsandbits.api.multistate.accessor.identifier.IAreaShapeIdentifier;
import mod.chiselsandbits.api.multistate.accessor.sortable.IPositionMutator;
import mod.chiselsandbits.api.multistate.mutator.IAreaMutator;
import mod.chiselsandbits.api.multistate.mutator.IMutableStateEntryInfo;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshot;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshotType;
import mod.chiselsandbits.api.multistate.statistics.IMultiStateObjectStatistics;
import mod.chiselsandbits.api.serialization.CBCodecs;
import mod.chiselsandbits.api.serialization.CBStreamCodecs;
import mod.chiselsandbits.api.util.BlockPosForEach;
import mod.chiselsandbits.api.util.BlockPosStreamProvider;
import mod.chiselsandbits.api.util.VectorUtils;
import mod.chiselsandbits.api.util.constants.NbtConstants;
import mod.chiselsandbits.registrars.ModMultiStateSnapshotTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MultiBlockMultiStateSnapshot implements IMultiStateSnapshot {

    public static final Codec<MultiBlockMultiStateSnapshot> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    CBCodecs.unboundedComplexMap(
                            BlockPos.CODEC,
                            IMultiStateSnapshot.CODEC
                    ).fieldOf(NbtConstants.SNAPSHOTS).forGetter(MultiBlockMultiStateSnapshot::getSnapshots),
                    Vec3.CODEC.fieldOf(NbtConstants.START).forGetter(MultiBlockMultiStateSnapshot::getStartPoint),
                    Vec3.CODEC.fieldOf(NbtConstants.END).forGetter(MultiBlockMultiStateSnapshot::getEndPoint)
            ).apply(instance, MultiBlockMultiStateSnapshot::new)
    );

    public static final MapCodec<MultiBlockMultiStateSnapshot> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            CBCodecs.unboundedComplexMap(
                    BlockPos.CODEC,
                    IMultiStateSnapshot.CODEC
            ).fieldOf(NbtConstants.SNAPSHOTS).forGetter(MultiBlockMultiStateSnapshot::getSnapshots),
            Vec3.CODEC.fieldOf(NbtConstants.START).forGetter(MultiBlockMultiStateSnapshot::getStartPoint),
            Vec3.CODEC.fieldOf(NbtConstants.END).forGetter(MultiBlockMultiStateSnapshot::getEndPoint)
    ).apply(instance, MultiBlockMultiStateSnapshot::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MultiBlockMultiStateSnapshot> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, BlockPos.STREAM_CODEC, IMultiStateSnapshot.STREAM_CODEC),
            MultiBlockMultiStateSnapshot::getSnapshots,
            CBStreamCodecs.VEC_3,
            MultiBlockMultiStateSnapshot::getStartPoint,
            CBStreamCodecs.VEC_3,
            MultiBlockMultiStateSnapshot::getEndPoint,
            MultiBlockMultiStateSnapshot::new
    );

    private Map<BlockPos, IMultiStateSnapshot> snapshots;
    private Vec3 startPoint;
    private Vec3 endPoint;

    public MultiBlockMultiStateSnapshot(final Map<BlockPos, IMultiStateSnapshot> snapshots, final Vec3 startPoint, final Vec3 endPoint) {
        this.snapshots = snapshots;
        this.startPoint = startPoint;
        this.endPoint = endPoint;

        if (!BlockPosStreamProvider.getForRange(startPoint, endPoint)
                .allMatch(snapshots::containsKey)) {
            throw new IllegalArgumentException("Not all required block positions are part of the given range.");
        }
    }

    private Map<BlockPos, IMultiStateSnapshot> getSnapshots() {
        return snapshots;
    }

    private Vec3 getStartPoint() {
        return startPoint;
    }

    private Vec3 getEndPoint() {
        return endPoint;
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
        return new Identifier(this.snapshots.values());
    }

    @Override
    public Stream<IStateEntryInfo> stream() {
        return snapshots.values()
                .stream()
                .flatMap(IAreaAccessor::stream);
    }

    @Override
    public boolean isInside(final Vec3 inAreaTarget) {
        final BlockPos inAreaOffset = VectorUtils.toBlockPos(inAreaTarget);

        return isInside(inAreaOffset, inAreaTarget.subtract(Vec3.atLowerCornerOf(inAreaOffset)));
    }

    @Override
    public boolean isInside(final BlockPos inAreaBlockPosOffset, final Vec3 inBlockTarget) {
        final BlockPos targetPos = VectorUtils.toBlockPos(startPoint).offset(inAreaBlockPosOffset);

        return snapshots.containsKey(targetPos) && snapshots.get(targetPos).isInside(BlockPos.ZERO, inBlockTarget);
    }

    @Override
    public IMultiStateSnapshot createSnapshot() {
        final Map<BlockPos, IMultiStateSnapshot> copiedSnapshots = snapshots.keySet()
                .stream()
                .collect(Collectors.toMap(
                                Function.identity(),
                                pos -> snapshots.get(pos).createSnapshot()
                        )
                );

        return new MultiBlockMultiStateSnapshot(
                copiedSnapshots,
                startPoint,
                endPoint
        );
    }

    @Override
    public Stream<IStateEntryInfo> streamWithPositionMutator(final IPositionMutator positionMutator) {
        return BlockPosStreamProvider.getForRange(
                        startPoint.multiply(StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide()),
                        endPoint.multiply(StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide())
                )
                .map(positionMutator::mutate)
                .map(position -> Vec3.atLowerCornerOf(position).multiply(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit()))
                .map(position -> {
                    final BlockPos blockPos = VectorUtils.toBlockPos(position);
                    final Vec3 inBlockOffset = position.subtract(Vec3.atLowerCornerOf(blockPos));

                    return getInBlockTarget(blockPos, inBlockOffset);
                })
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    @Override
    public void forEachWithPositionMutator(
            final IPositionMutator positionMutator, final Consumer<IStateEntryInfo> consumer) {
        BlockPosForEach.forEachInRange(
                startPoint.multiply(StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide()),
                endPoint.multiply(StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide()),
                (blockPos) -> {
                    final Vec3i target = positionMutator.mutate(blockPos);
                    final Vec3 scaledTarget = Vec3.atLowerCornerOf(target).multiply(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit());

                    final BlockPos blockTarget = VectorUtils.toBlockPos(scaledTarget);
                    final Vec3 inBlockOffset = scaledTarget.subtract(Vec3.atLowerCornerOf(blockPos));

                    Optional<IStateEntryInfo> targetCandidate = getInBlockTarget(blockPos, inBlockOffset);
                    targetCandidate.ifPresent(consumer);
                }
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
        final BlockPos inAreaOffset = VectorUtils.toBlockPos(inAreaTarget);

        return getInBlockTarget(inAreaOffset, inAreaTarget.subtract(Vec3.atLowerCornerOf(inAreaOffset)));
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
        final BlockPos targetPos = VectorUtils.toBlockPos(startPoint).offset(inAreaBlockPosOffset);

        if (!snapshots.containsKey(targetPos)) {
            throw new IllegalArgumentException("The given position is not in the current snapshot!");
        }

        return snapshots.get(targetPos).getInBlockTarget(BlockPos.ZERO, inBlockTarget);
    }

    /**
     * Returns all entries in the current area in a mutable fashion. Includes all empty areas as areas containing an air state.
     *
     * @return A stream with a mutable state entry info for each mutable section in the area.
     */
    @Override
    public Stream<IMutableStateEntryInfo> mutableStream() {
        return snapshots.values()
                .stream()
                .flatMap(IAreaMutator::mutableStream);
    }

    @Override
    public void setInAreaTarget(
            final BlockInformation blockInformation,
            final Vec3 inAreaTarget) throws SpaceOccupiedException {
        final Vec3 workingTarget = inAreaTarget.add(this.startPoint);

        final BlockPos offset = VectorUtils.toBlockPos(workingTarget);
        final Vec3 inBlockTarget = new Vec3(
                workingTarget.x() - offset.getX(),
                workingTarget.y() - offset.getY(),
                workingTarget.z() - offset.getZ()
        );

        this.setInBlockTarget(
                blockInformation,
                offset,
                inBlockTarget
        );
    }

    @Override
    public void setInBlockTarget(final BlockInformation blockInformation, final BlockPos inAreaBlockPosOffset, final Vec3 inBlockTarget) throws SpaceOccupiedException {
        final Vec3 workingTarget = Vec3.atLowerCornerOf(inAreaBlockPosOffset).add(inBlockTarget);
        if (workingTarget.x() < startPoint.x() ||
                workingTarget.y() < startPoint.y() ||
                workingTarget.z() < startPoint.z() ||
                workingTarget.x() > endPoint.x() ||
                workingTarget.y() > endPoint.y() ||
                workingTarget.z() > endPoint.z()
        ) {
            throw new IllegalArgumentException("The given target is outside of the operating range of this snapshot!");
        }

        if (!snapshots.containsKey(inAreaBlockPosOffset)) {
            throw new IllegalArgumentException("The given in area block pos offset is outside of the target range!");
        }

        this.snapshots.get(inAreaBlockPosOffset)
                .setInAreaTarget(blockInformation, inBlockTarget);
    }

    /**
     * Clears the current area, using the offset from the area as well as the in area target offset.
     *
     * @param inAreaTarget The in area offset.
     */
    @Override
    public void clearInAreaTarget(final Vec3 inAreaTarget) {
        final Vec3 workingTarget = inAreaTarget.add(this.startPoint);

        final BlockPos offset = VectorUtils.toBlockPos(workingTarget);
        final Vec3 inBlockTarget = new Vec3(
                workingTarget.x() - offset.getX(),
                workingTarget.y() - offset.getY(),
                workingTarget.z() - offset.getZ()
        );

        this.clearInBlockTarget(
                offset,
                inBlockTarget
        );
    }

    /**
     * Clears the current area, using the in area block position offset as well as the in block target offset to calculate the in area offset for setting.
     *
     * @param inAreaBlockPosOffset The offset of blocks in the current area.
     * @param inBlockTarget        The offset in the targeted block.
     */
    @Override
    public void clearInBlockTarget(final BlockPos inAreaBlockPosOffset, final Vec3 inBlockTarget) {
        final Vec3 workingTarget = Vec3.atLowerCornerOf(inAreaBlockPosOffset).add(inBlockTarget);
        if (workingTarget.x() < startPoint.x() ||
                workingTarget.y() < startPoint.y() ||
                workingTarget.z() < startPoint.z() ||
                workingTarget.x() > endPoint.x() ||
                workingTarget.y() > endPoint.y() ||
                workingTarget.z() > endPoint.z()
        ) {
            throw new IllegalArgumentException("The given target is outside of the operating range of this snapshot!");
        }

        if (!snapshots.containsKey(inAreaBlockPosOffset)) {
            throw new IllegalArgumentException("The given in area block pos offset is outside of the target range!");
        }

        this.snapshots.get(inAreaBlockPosOffset)
                .clearInAreaTarget(inBlockTarget);
    }

    @Override
    public IMultiStateSnapshotType getType() {
        return ModMultiStateSnapshotTypes.MULTI_BLOCK.get();
    }

    /**
     * Converts the current snapshot to a variant which is itemstack capable.
     *
     * @return The multistate itemstack which is the itemstack nbt representation of the current snapshot.
     */
    @Override
    public IMultiStateItemStack toItemStack() {
        throw new NotImplementedException("Multi block snapshots can not be contained in an itemstack as of now.");
    }

    @Override
    public IMultiStateObjectStatistics getStatics() {
        return new IMultiStateObjectStatistics() {
            @Override
            public Codec<?> codec() {
                throw new NotImplementedException("A multi block snapshots statistics are ephemeral.");
            }

            @Override
            public MapCodec<?> mapCodec() {
                throw new NotImplementedException("A multi block snapshots statistics are ephemeral.");
            }

            @Override
            public StreamCodec<?, ?> streamCodec() {
                throw new NotImplementedException("A multi block snapshots statistics are ephemeral.");
            }

            @Override
            public BlockInformation getPrimaryState() {
                return getStateCounts().entrySet().stream().max(Comparator.comparingInt(Map.Entry::getValue)).map(Map.Entry::getKey).orElse(BlockInformation.AIR);
            }

            @Override
            public boolean isEmpty() {
                final Map<BlockInformation, Integer> stateMap = getStateCounts();
                return stateMap.size() == 1 && stateMap.getOrDefault(BlockInformation.AIR, 0) > 0;
            }

            @Override
            public Map<BlockInformation, Integer> getStateCounts() {
                return stream().collect(Collectors.toMap(
                        IStateEntryInfo::getBlockInformation,
                        s -> 1,
                        Integer::sum
                ));
            }

            @Override
            public boolean shouldCheckWeakPower() {
                throw new NotImplementedException("Is a snapshot");
            }

            @Override
            public float getFullnessFactor() {
                throw new NotImplementedException("Is a snapshot");
            }

            @Override
            public float getSlipperiness() {
                throw new NotImplementedException("Is a snapshot");
            }

            @Override
            public float getLightEmissionFactor() {
                throw new NotImplementedException("Is a snapshot");
            }

            @Override
            public float getLightBlockingFactor() {
                throw new NotImplementedException("Is a snapshot");
            }

            @Override
            public float getRelativeBlockHardness(final Player player) {
                throw new NotImplementedException("Is a snapshot");
            }

            @Override
            public boolean canPropagateSkylight() {
                throw new NotImplementedException("Is a snapshot");
            }

            @Override
            public boolean canSustainGrassBelow() {
                throw new NotImplementedException("Is a snapshot");
            }
        };
    }

    @Override
    public void rotate(final Direction.Axis axis, final int rotationCount) {
        final Vec3 center = this.startPoint.add(this.endPoint).multiply(0.5, 0.5, 0.5);

        final Map<BlockPos, IMultiStateSnapshot> rotatedParts = this.snapshots
                .entrySet().stream()
                .collect(
                        Collectors.toMap(
                                e -> {
                                    final Vec3 offSetPos = Vec3.atLowerCornerOf(e.getKey()).subtract(center);
                                    final Vec3 rotatedOffset = VectorUtils.rotateMultipleTimes90Degrees(offSetPos, axis, rotationCount);
                                    return VectorUtils.toBlockPos(startPoint.add(rotatedOffset));
                                },
                                e -> {
                                    final IMultiStateSnapshot clone = e.getValue().clone();
                                    clone.rotate(axis, rotationCount);
                                    return clone;
                                }
                        )
                );

        final Vec3 rotatedStartPoint = VectorUtils.rotateMultipleTimes90Degrees(startPoint.subtract(center), axis, rotationCount).add(center);
        final Vec3 rotatedEndPoint = VectorUtils.rotateMultipleTimes90Degrees(endPoint.subtract(center), axis, rotationCount).add(center);
        final Vec3 newStartPoint = VectorUtils.minimize(rotatedStartPoint, rotatedEndPoint);
        final Vec3 newEndPoint = VectorUtils.maximize(rotatedStartPoint, rotatedEndPoint);

        this.snapshots = rotatedParts;
        this.startPoint = newStartPoint;
        this.endPoint = newEndPoint;
    }

    @Override
    public void mirror(final Direction.Axis axis) {
        final Vec3 center = this.startPoint.add(this.endPoint).multiply(0.5, 0.5, 0.5);

        this.snapshots = this.snapshots
                .entrySet().stream()
                .collect(
                        Collectors.toMap(
                                e -> {
                                    final int mirroredX =
                                            axis == Direction.Axis.X ? (int) (center.x() - e.getKey().getX()) : e.getKey().getX();
                                    final int mirroredY =
                                            axis == Direction.Axis.Y ? (int) (center.y() - e.getKey().getY()) : e.getKey().getY();
                                    final int mirroredZ =
                                            axis == Direction.Axis.Z ? (int) (center.z() - e.getKey().getZ()) : e.getKey().getZ();

                                    return new BlockPos(mirroredX, mirroredY, mirroredZ);
                                },
                                e -> {
                                    final IMultiStateSnapshot clone = e.getValue().clone();
                                    clone.mirror(axis);
                                    return clone;
                                }
                        )
                );
    }

    @Override
    public IMultiStateSnapshot clone() {
        final Map<BlockPos, IMultiStateSnapshot> clonedSnapshots = this.snapshots
                .entrySet().stream().collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().clone()
                        )
                );

        return new MultiBlockMultiStateSnapshot(
                clonedSnapshots,
                startPoint,
                endPoint
        );
    }

    @Override
    public IMultiStateSnapshot limitedToProgression(final float progress)
    {
        throw new NotImplementedException("MultiBlock Snapshots can not be limited to a progression yet!");
    }

    @Override
    public @NotNull AABB getBoundingBox() {
        return new AABB(
                startPoint.x(),
                startPoint.y(),
                startPoint.z(),
                endPoint.x(),
                endPoint.y(),
                endPoint.z()
        );
    }

    private static final class Identifier implements IAreaShapeIdentifier {
        private final Collection<IAreaShapeIdentifier> inners;

        public Identifier(final Collection<IMultiStateSnapshot> innerSnapshots) {
            this.inners = innerSnapshots.stream().map(IAreaAccessor::createNewShapeIdentifier).collect(Collectors.toList());
        }

        @Override
        public int hashCode() {
            return Objects.hash(inners);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof final Identifier that)) {
                return false;
            }
            return inners.equals(that.inners);
        }
    }
}

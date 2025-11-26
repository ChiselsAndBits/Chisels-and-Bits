package mod.chiselsandbits.chiseling.modes.draw;

import com.communi.suggestu.scena.core.registries.AbstractCustomRegistryEntry;
import com.google.common.collect.Maps;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.change.IChangeTrackerManager;
import mod.chiselsandbits.api.chiseling.IChiselingContext;
import mod.chiselsandbits.api.chiseling.mode.IChiselMode;
import mod.chiselsandbits.api.inventory.bit.IBitInventory;
import mod.chiselsandbits.api.inventory.management.IBitInventoryManager;
import mod.chiselsandbits.api.item.click.ClickProcessingState;
import mod.chiselsandbits.api.item.withmode.group.IToolModeGroup;
import mod.chiselsandbits.api.multistate.StateEntrySize;
import mod.chiselsandbits.api.multistate.accessor.IAreaAccessor;
import mod.chiselsandbits.api.multistate.accessor.IStateEntryInfo;
import mod.chiselsandbits.api.multistate.accessor.world.IInWorldStateEntryInfo;
import mod.chiselsandbits.api.multistate.accessor.world.IWorldAreaAccessor;
import mod.chiselsandbits.api.multistate.mutator.world.IWorldAreaMutator;
import mod.chiselsandbits.api.util.IBatchMutation;
import mod.chiselsandbits.api.util.LocalStrings;
import mod.chiselsandbits.api.util.RayTracingUtils;
import mod.chiselsandbits.api.util.VectorUtils;
import mod.chiselsandbits.registrars.ModChiselModeGroups;
import mod.chiselsandbits.registrars.ModMetadataKeys;
import mod.chiselsandbits.utils.BitInventoryUtils;
import mod.chiselsandbits.utils.ItemStackUtils;
import mod.chiselsandbits.utils.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DrawnWallChiselMode extends AbstractCustomRegistryEntry implements IChiselMode {
    private final MutableComponent displayName;
    private final MutableComponent multiLineDisplayName;
    private final Identifier       iconName;
    private final int              extensionWidth;

    DrawnWallChiselMode(final MutableComponent displayName, final MutableComponent multiLineDisplayName, final Identifier iconName, int extensionWidth) {
        this.displayName = displayName;
        this.multiLineDisplayName = multiLineDisplayName;
        this.iconName = iconName;
        this.extensionWidth = extensionWidth;
    }

    private static Direction.Axis getExtensionAxis(final Direction hitDirection) {
        return switch (hitDirection) {
            case DOWN, NORTH, SOUTH -> Direction.Axis.X;
            case UP, WEST, EAST -> Direction.Axis.Z;
        };
    }

    private static Vec3i getOffsetPosition(final Vec3i source, final int offset, Direction.Axis axis) {
        return switch (axis) {
            case X -> new Vec3i(source.getX() + offset, source.getY(), source.getZ());
            case Y -> new Vec3i(source.getX(), source.getY() + offset, source.getZ());
            case Z -> new Vec3i(source.getX(), source.getY(), source.getZ() + offset);
        };
    }

    @Override
    public boolean isSingleClickUse() {
        return false;
    }

    @Override
    public ClickProcessingState onLeftClickBy(
            final Player playerEntity, final IChiselingContext context) {
        return processRayTraceIntoContext(
                playerEntity,
                context,
                direction -> Vec3.atLowerCornerOf(direction.getOpposite().getUnitVec3i()).multiply(StateEntrySize.current().getSizePerHalfBitScalingVector())
        );
    }

    @Override
    public void onStoppedLeftClicking(final Player playerEntity, final IChiselingContext context) {
        onLeftClickBy(playerEntity, context);
        context.setComplete();

        if (context.isSimulation())
            return;

        context.getMutator().ifPresent(mutator -> {
            try (IBatchMutation ignored =
                         mutator.batch(IChangeTrackerManager.getInstance().getChangeTracker(playerEntity))) {
                final Map<BlockInformation, Integer> resultingBitCount = Maps.newHashMap();

                final Predicate<IStateEntryInfo> filter = context.getStateFilter()
                        .map(factory -> factory.apply(mutator))
                        .orElse((s) -> true);

                mutator.inWorldMutableStream()
                        .filter(filter)
                        .forEach(state -> {
                            final BlockInformation currentState = state.getBlockInformation();
                            if (context.tryDamageItem()) {
                                resultingBitCount.putIfAbsent(currentState, 0);
                                resultingBitCount.computeIfPresent(currentState, (s, currentCount) -> currentCount + 1);

                                state.clear();
                            }
                        });

                resultingBitCount.forEach((blockState, count) -> BitInventoryUtils.insertIntoOrSpawn(
                        playerEntity,
                        blockState,
                        count
                ));
            }
        });
    }

    @Override
    public ClickProcessingState onRightClickBy(final Player playerEntity, final IChiselingContext context) {
        return processRayTraceIntoContext(
                playerEntity,
                context,
                direction -> Vec3.atLowerCornerOf(direction.getUnitVec3i()).multiply(StateEntrySize.current().getSizePerHalfBitScalingVector())
        );
    }

    @Override
    public void onStoppedRightClicking(final Player playerEntity, final IChiselingContext context) {
        onRightClickBy(playerEntity, context);
        context.setComplete();

        if (context.isSimulation())
            return;

        context.getMutator().ifPresent(mutator -> {
            final BlockInformation heldBlockState = ItemStackUtils.getHeldBitBlockInformationFromPlayer(playerEntity);
            if (heldBlockState.isAir()) {
                return;
            }

            final Predicate<IStateEntryInfo> filter = context.getStateFilter()
                    .map(factory -> factory.apply(mutator))
                    .orElse((s) -> true);

            final int missingBitCount = (int) mutator.stream()
                    .filter(state -> state.getBlockInformation().isAir() && filter.test(state))
                    .count();

            final IBitInventory playerBitInventory = IBitInventoryManager.getInstance().create(playerEntity);

            context.setComplete();
            if (playerBitInventory.canExtract(heldBlockState, missingBitCount) || playerEntity.isCreative()) {
                if (!playerEntity.isCreative()) {
                    playerBitInventory.extract(heldBlockState, missingBitCount);
                }

                try (IBatchMutation ignored =
                             mutator.batch(IChangeTrackerManager.getInstance().getChangeTracker(playerEntity))) {
                    mutator.inWorldMutableStream()
                            .filter(state -> state.getBlockInformation().isAir() && filter.test(state))
                            .forEach(state -> state.overrideState(heldBlockState)); //We can use override state here to prevent the try-catch block.
                }
            } else {
                context.setError(LocalStrings.ChiselAttemptFailedNotEnoughBits.getText(heldBlockState.blockState().getBlock().getName()));
            }

            if (missingBitCount == 0) {
                context.validateBuildHeights();
            }
        });
    }

    private ClickProcessingState processRayTraceIntoContext(final Player playerEntity, final IChiselingContext context, Function<Direction, Vec3> offsetGenerator) {
        final HitResult rayTraceResult = RayTracingUtils.rayTracePlayer(playerEntity);
        if (rayTraceResult.getType() != HitResult.Type.BLOCK || !(rayTraceResult instanceof final BlockHitResult blockRayTraceResult)) {
            context.setError(LocalStrings.ChiselAttemptFailedNoBlock.getText());
            return ClickProcessingState.DEFAULT;
        }

        final Vec3 currentTarget = blockRayTraceResult.getLocation().add(offsetGenerator.apply(blockRayTraceResult.getDirection()));

        Optional<Vec3> anchor = context.getMetadata(ModMetadataKeys.ANCHOR.get());
        if (anchor.isEmpty()) {
            context.setMetadata(ModMetadataKeys.ANCHOR.get(), currentTarget);
            anchor = context.getMetadata(ModMetadataKeys.ANCHOR.get());
        }

        Optional<Direction.Axis> targetedAxis = context.getMetadata(ModMetadataKeys.TARGETED_AXIS.get());
        if (targetedAxis.isEmpty()) {
            targetedAxis = Optional.of(getExtensionAxis(blockRayTraceResult.getDirection()));
            context.setMetadata(ModMetadataKeys.TARGETED_AXIS.get(), targetedAxis.get());
        }

        context.resetMutator();
        context.include(anchor.orElseThrow());
        context.include(currentTarget);

        Optional<Vec3> finalAnchor = anchor;
        Optional<Direction.Axis> finalTargetedAxis = targetedAxis;
        context.setStateFilter(areaAccessor -> {
            if (areaAccessor instanceof IWorldAreaAccessor worldAreaAccessor) {
                return new WallAreaFilter(finalAnchor.get(), currentTarget, finalTargetedAxis.get(), extensionWidth);
            }

            return (s) -> false;
        });

        return ClickProcessingState.ALLOW;
    }

    @Override
    public Optional<IAreaAccessor> getCurrentAccessor(final IChiselingContext context) {
        return context.getMutator()
                .map(IAreaAccessor.class::cast);
    }

    @Override
    public VoxelShape getShape(final IChiselingContext context) {
        if (context.getMutator().isEmpty())
            return Shapes.empty();

        final IWorldAreaMutator mutator = context.getMutator().get();

        final Optional<Predicate<IStateEntryInfo>> filter = context.getStateFilter().map(factory -> factory.apply(mutator));
        if (filter.isEmpty())
            return Shapes.empty();

        final Predicate<IStateEntryInfo> stateFilter = filter.get();
        if (!(stateFilter instanceof WallAreaFilter lineAreaFilter))
            return Shapes.empty();

        final BlockPos offset = VectorUtils.invert(mutator.getInWorldStartBlockPoint());

        final List<Vec3i> startPoints = lineAreaFilter.anchors;
        return VoxelShapeUtils.batchCombine(Shapes.empty(), BooleanOp.OR, true, startPoints.stream()
                .map(p -> {
                    final Vec3i startPointScalar = getOffsetPosition(p, -lineAreaFilter.extensionWidth, lineAreaFilter.axis);
                    final Vec3i endPointScalar = getOffsetPosition(p, lineAreaFilter.extensionWidth, lineAreaFilter.axis);

                    final Vec3 startPoint = new Vec3(startPointScalar.getX(), startPointScalar.getY(), startPointScalar.getZ()).multiply(StateEntrySize.current().getSizePerBitScalingVector());
                    final Vec3 endPoint = new Vec3(endPointScalar.getX(), endPointScalar.getY(), endPointScalar.getZ()).multiply(StateEntrySize.current().getSizePerBitScalingVector());

                    final Vec3 finalEndpoint = new Vec3(endPoint.x(), endPoint.y() + (lineAreaFilter.height * StateEntrySize.current().getSizePerBit()), endPoint.z());

                    final Vec3 end = finalEndpoint.add(StateEntrySize.current().getSizePerBitScalingVector());
                    final Vec3 min = new Vec3(Math.min(startPoint.x(), end.x()), Math.min(startPoint.y(), end.y()), Math.min(startPoint.z(), end.z()));
                    final Vec3 max = new Vec3(Math.max(startPoint.x(), end.x()), Math.max(startPoint.y(), end.y()), Math.max(startPoint.z(), end.z()));

                    return Shapes.box(
                            min.x(), min.y(), min.z(),
                            max.x(), max.y(), max.z()
                    );
                })
                .collect(Collectors.toList())).move(offset.getX(), offset.getY(), offset.getZ());
    }

    @Override
    public Identifier getIcon() {
        return iconName;
    }

    @Override
    public @NotNull Optional<IToolModeGroup> getGroup() {
        return Optional.of(ModChiselModeGroups.DRAW);
    }

    @Override
    public Component getDisplayName() {
        return displayName;
    }

    @Override
    public Component getMultiLineDisplayName() {
        return multiLineDisplayName;
    }

    private static final class WallAreaFilter implements Predicate<IStateEntryInfo> {

        private final Vec3i origin;
        private final Vec3 magnitude;

        private final double height;
        private final List<Vec3i> included;
        private final List<Vec3i> anchors;
        private final Direction.Axis axis;
        private final int extensionWidth;

        private WallAreaFilter(Vec3 startPoint, Vec3 endPoint, Direction.Axis axis, int extensionWidth) {
            final Vec3 origin = startPoint.multiply(StateEntrySize.current().getBitsPerBlockSideScalingVector());
            this.origin = VectorUtils.toInteger(origin.x(), origin.y(), origin.z());

            final Vec3 magnitude = endPoint.multiply(StateEntrySize.current().getBitsPerBlockSideScalingVector()).subtract(origin);
            final Vec3i normalizedMagnitude = VectorUtils.toInteger(magnitude.x(), magnitude.y(), magnitude.z());
            this.height = normalizedMagnitude.getY();
            this.magnitude = new Vec3(normalizedMagnitude.getX(), 0, normalizedMagnitude.getZ());

            this.axis = axis;
            this.extensionWidth = extensionWidth;

            this.anchors = calculateAnchorPositions();
            this.included = calculateIncludedPositions();
        }

        private List<Vec3i> calculateIncludedPositions() {
            return anchors.stream()
                    .flatMap(i -> {
                        final Stream.Builder<Vec3i> builder = Stream.builder();

                        for (int j = -extensionWidth; j <= extensionWidth; j++) {
                            builder.add(getOffsetPosition(i, j, axis));
                        }

                        return builder.build();
                    })
                    .collect(Collectors.toList());
        }

        @Override
        public boolean test(IStateEntryInfo stateEntryInfo) {
            if (!(stateEntryInfo instanceof final IInWorldStateEntryInfo inWorldStateEntryInfo)) {
                return false;
            }

            final Vec3 pos = inWorldStateEntryInfo.getInWorldStartPoint().multiply(StateEntrySize.current().getBitsPerBlockSideScalingVector());
            final Vec3i candidatePos = VectorUtils.toInteger(pos.x(), this.origin.getY(), pos.z());

            final boolean included = this.included.contains(candidatePos);
            if (height < 0) {
                return included && candidatePos.getY() <= origin.getY() && candidatePos.getY() > origin.getY() + height;
            } else {
                return included && candidatePos.getY() >= origin.getY() && candidatePos.getY() < origin.getY() + height;
            }
        }

        private List<Vec3i> calculateAnchorPositions() {
            final List<Vec3i> positions = new ArrayList<>();
            final Vec3 direction = magnitude.normalize();
            final double xLen = (direction.scale(1 / direction.x())).length();
            final double yLen = (direction.scale(1 / direction.y())).length();
            final double zLen = (direction.scale(1 / direction.z())).length();
            final double reach = magnitude.length();

            double distanceFromStart = 0d;

            Vec3i pos = new Vec3i(
                    (int) (direction.x() > 0 ? Math.ceil(origin.getX()) - 1 : Math.floor(origin.getX())),
                    (int) (direction.y() > 0 ? Math.ceil(origin.getY()) - 1 : Math.floor(origin.getY())),
                    (int) (direction.z() > 0 ? Math.ceil(origin.getZ()) - 1 : Math.floor(origin.getZ()))
            );
            double xOff = direction.x() > 0 ? 1 + pos.getX() - origin.getX() : origin.getX() - pos.getX();
            double yOff = direction.y() > 0 ? 1 + pos.getY() - origin.getY() : origin.getY() - pos.getY();
            double zOff = direction.z() > 0 ? 1 + pos.getZ() - origin.getZ() : origin.getZ() - pos.getZ();

            double xDist = Double.isNaN(xLen) ? Double.POSITIVE_INFINITY : Math.abs(xOff * xLen);
            double yDist = Double.isNaN(yLen) ? Double.POSITIVE_INFINITY : Math.abs(yOff * yLen);
            double zDist = Double.isNaN(zLen) ? Double.POSITIVE_INFINITY : Math.abs(zOff * zLen);

            while (distanceFromStart <= reach) {
                positions.add(pos);

                if (xDist < yDist) {
                    if (xDist < zDist) {
                        distanceFromStart = xDist;
                        xDist += xLen;
                        pos = pos.offset(direction.x() > 0 ? 1 : -1, 0, 0);
                    } else {
                        distanceFromStart = zDist;
                        zDist += zLen;
                        pos = pos.offset(0, 0, direction.z() > 0 ? 1 : -1);
                    }
                } else {
                    if (yDist < zDist) {
                        distanceFromStart = yDist;
                        yDist += yLen;
                        pos = pos.offset(0, direction.y() > 0 ? 1 : -1, 0);
                    } else {
                        distanceFromStart = zDist;
                        zDist += zLen;
                        pos = pos.offset(0, 0, direction.z() > 0 ? 1 : -1);
                    }
                }
            }

            return positions;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof WallAreaFilter that)) return false;

            if (!origin.equals(that.origin)) return false;
            if (!magnitude.equals(that.magnitude)) return false;
            return included.equals(that.included);
        }

        @Override
        public int hashCode() {
            int result = origin.hashCode();
            result = 31 * result + magnitude.hashCode();
            result = 31 * result + included.hashCode();
            return result;
        }
    }
}

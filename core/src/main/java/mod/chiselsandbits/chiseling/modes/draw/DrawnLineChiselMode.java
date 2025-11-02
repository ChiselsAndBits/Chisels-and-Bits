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
import mod.chiselsandbits.client.icon.IconManager;
import mod.chiselsandbits.registrars.ModChiselModeGroups;
import mod.chiselsandbits.registrars.ModMetadataKeys;
import mod.chiselsandbits.utils.BitInventoryUtils;
import mod.chiselsandbits.utils.ItemStackUtils;
import mod.chiselsandbits.utils.VoxelShapeUtils;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
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

public class DrawnLineChiselMode extends AbstractCustomRegistryEntry implements IChiselMode {
    private final MutableComponent displayName;
    private final MutableComponent multiLineDisplayName;
    private final ResourceLocation iconName;

    DrawnLineChiselMode(final MutableComponent displayName, final MutableComponent multiLineDisplayName, final ResourceLocation iconName) {
        this.displayName = displayName;
        this.multiLineDisplayName = multiLineDisplayName;
        this.iconName = iconName;
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

        context.resetMutator();
        context.include(anchor.orElseThrow());
        context.include(currentTarget);

        Optional<Vec3> finalAnchor = anchor;
        context.setStateFilter(areaAccessor -> {
            if (areaAccessor instanceof IWorldAreaAccessor worldAreaAccessor) {
                return new LineAreaFilter(finalAnchor.get(), currentTarget);
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
        if (!(stateFilter instanceof LineAreaFilter lineAreaFilter))
            return Shapes.empty();

        final BlockPos offset = VectorUtils.invert(mutator.getInWorldEndBlockPoint());

        final List<Vec3> startPoints = lineAreaFilter.included.stream().map(v -> v.multiply(StateEntrySize.current().getSizePerBitScalingVector())).toList();
        return VoxelShapeUtils.batchCombine(Shapes.empty(), BooleanOp.OR, true, startPoints.stream()
                .map(p -> Shapes.box(
                        p.x,
                        p.y,
                        p.z,
                        p.x + StateEntrySize.current().getSizePerBit(),
                        p.y + StateEntrySize.current().getSizePerBit(),
                        p.z + StateEntrySize.current().getSizePerBit()))
                .collect(Collectors.toList())).move(offset.getX(), offset.getY(), offset.getZ());
    }

    @Override
    public TextureAtlasSprite getIcon() {
        return IconManager.getInstance().getIcon(iconName);
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

    private static final class LineAreaFilter implements Predicate<IStateEntryInfo> {

        private final Vec3 origin;
        private final Vec3 magnitude;
        private final List<Vec3> included;

        private LineAreaFilter(Vec3 startPoint, Vec3 endPoint) {
            this.origin = startPoint.multiply(StateEntrySize.current().getBitsPerBlockSideScalingVector());
            this.magnitude = endPoint.multiply(StateEntrySize.current().getBitsPerBlockSideScalingVector()).subtract(origin);

            included = calculateIncludedPositions();
        }

        @Override
        public boolean test(IStateEntryInfo stateEntryInfo) {
            if (!(stateEntryInfo instanceof final IInWorldStateEntryInfo inWorldStateEntryInfo)) {
                return false;
            }

            return this.included.contains(inWorldStateEntryInfo.getInWorldStartPoint().multiply(StateEntrySize.current().getBitsPerBlockSideScalingVector()));
        }

        private List<Vec3> calculateIncludedPositions() {
            final List<Vec3i> positions = new ArrayList<>();
            final Vec3 direction = magnitude.normalize();
            final double xLen = (direction.scale(1 / direction.x())).length();
            final double yLen = (direction.scale(1 / direction.y())).length();
            final double zLen = (direction.scale(1 / direction.z())).length();
            final double reach = magnitude.length();

            double distanceFromStart = 0d;

            Vec3i pos = new Vec3i(
                    (int) (direction.x() > 0 ? Math.ceil(origin.x()) - 1 : Math.floor(origin.x())),
                    (int) (direction.y() > 0 ? Math.ceil(origin.y()) - 1 : Math.floor(origin.y())),
                    (int) (direction.z() > 0 ? Math.ceil(origin.z()) - 1 : Math.floor(origin.z()))
            );
            double xOff = direction.x() > 0 ? 1 + pos.getX() - origin.x() : origin.x() - pos.getX();
            double yOff = direction.y() > 0 ? 1 + pos.getY() - origin.y() : origin.y() - pos.getY();
            double zOff = direction.z() > 0 ? 1 + pos.getZ() - origin.z() : origin.z() - pos.getZ();

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

            return positions.stream().map(i -> new Vec3(i.getX(), i.getY(), i.getZ())).collect(Collectors.toList());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LineAreaFilter that)) return false;

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

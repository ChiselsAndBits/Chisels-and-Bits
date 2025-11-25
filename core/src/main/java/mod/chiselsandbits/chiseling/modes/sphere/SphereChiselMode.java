package mod.chiselsandbits.chiseling.modes.sphere;

import com.communi.suggestu.scena.core.registries.AbstractCustomRegistryEntry;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Either;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.change.IChangeTrackerManager;
import mod.chiselsandbits.api.chiseling.ChiselingOperation;
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
import mod.chiselsandbits.api.util.*;
import mod.chiselsandbits.client.icon.IconManager;
import mod.chiselsandbits.registrars.ModChiselModeGroups;
import mod.chiselsandbits.registrars.ModMetadataKeys;
import mod.chiselsandbits.utils.BitInventoryUtils;
import mod.chiselsandbits.utils.BlockPosUtils;
import mod.chiselsandbits.utils.ItemStackUtils;
import mod.chiselsandbits.voxelshape.MultiStateBlockEntityDiscreteVoxelShape;
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
import net.minecraft.world.phys.shapes.CubeVoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.BitSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class SphereChiselMode extends AbstractCustomRegistryEntry implements IChiselMode {
    private final int diameter;
    private final MutableComponent displayName;
    private final MutableComponent multiLineDisplayName;
    private final ResourceLocation iconName;

    private final Map<StateEntrySize, VoxelShape> baseSphereShapes = Maps.newHashMap();

    SphereChiselMode(
            final int diameter,
            final MutableComponent displayName,
            final MutableComponent multiLineDisplayName,
            final ResourceLocation iconName) {
        this.diameter = diameter;
        this.displayName = displayName;
        this.multiLineDisplayName = multiLineDisplayName;
        this.iconName = iconName;

        for (final StateEntrySize size : StateEntrySize.values()) {
            baseSphereShapes.put(size, buildSphereShape(size, diameter));
        }
    }

    private static VoxelShape buildSphereShape(final StateEntrySize size, final int diameter) {
        final int blocks = StateEntrySize.ONE_SIXTEENTH.getBitsPerBlockSide() / size.getBitsPerBlockSide();
        final int fullBitAccuracy = blocks * StateEntrySize.ONE_SIXTEENTH.getBitsPerBlockSide();
        final int centerBitCoord = fullBitAccuracy / 2;
        final int radius = diameter / 2;

        final BlockPos centerPos = new BlockPos(centerBitCoord, centerBitCoord, centerBitCoord);
        final BlockPos cornerPos = centerPos.subtract(new Vec3i(radius, 0, 0));
        final double range = cornerPos.distSqr(centerPos);

        final BitSet bits = new BitSet(fullBitAccuracy * fullBitAccuracy * fullBitAccuracy);
        BlockPosForEach.forEachInRange(fullBitAccuracy, blockPos -> {
            final int index = BlockPosUtils.getCollisionIndex(blockPos, fullBitAccuracy, fullBitAccuracy);
            final double posRange = blockPos.distSqr(centerPos);

            bits.set(index, posRange <= range);
        });

        return new CubeVoxelShape(new MultiStateBlockEntityDiscreteVoxelShape(bits, fullBitAccuracy));
    }

    @Override
    public ClickProcessingState onLeftClickBy(
            final Player playerEntity, final IChiselingContext context) {
        final Either<ClickProcessingState, Vec3> rayTraceHandle = this.processRayTraceIntoContext(
                playerEntity,
                context,
                face -> Vec3.atLowerCornerOf(face.getOpposite().getUnitVec3i()),
                facing -> facing.multiply(-1, -1, -1)
        );

        if (rayTraceHandle.right().isPresent()) {
            context.setMetadata(ModMetadataKeys.ANCHOR.get(), rayTraceHandle.right().get());
        }

        if (context.isSimulation()) {
            return ClickProcessingState.DEFAULT;
        }

        context.setComplete();

        if (rayTraceHandle.left().isPresent()) {
            return rayTraceHandle.left().get();
        }

        if (rayTraceHandle.right().isEmpty()) {
            throw new IllegalArgumentException("Missing both a click processing result as well as a center vector for sphere processing");
        }

        return context.getMutator().map(mutator -> {
            try (IBatchMutation ignored =
                         mutator.batch(IChangeTrackerManager.getInstance().getChangeTracker(playerEntity))) {
                final Map<BlockInformation, Integer> resultingBitCount = Maps.newHashMap();

                final Predicate<IStateEntryInfo> filter = context.getStateFilter()
                        .map(factory -> factory.apply(mutator))
                        .orElse((s) -> true);

                final int totalModifiedStates = mutator.inWorldMutableStream()
                        .filter(filter)
                        .mapToInt(state -> {
                            final BlockInformation currentState = state.getBlockInformation();

                            return context.tryDamageItemAndDoOrSetBrokenError(
                                    () -> {
                                        resultingBitCount.putIfAbsent(currentState, 0);
                                        resultingBitCount.computeIfPresent(currentState, (s, currentCount) -> currentCount + 1);

                                        state.clear();
                                    });
                        })
                        .sum();

                if (totalModifiedStates == 0) {
                    context.setError(LocalStrings.ChiselAttemptFailedNoValidStateFound.getText());
                }

                resultingBitCount.forEach((blockState, count) -> BitInventoryUtils.insertIntoOrSpawn(
                        playerEntity,
                        blockState,
                        count
                ));
            }

            return ClickProcessingState.ALLOW;
        }).orElse(ClickProcessingState.DEFAULT);
    }

    @Override
    public void onStoppedLeftClicking(final Player playerEntity, final IChiselingContext context) {
        //Noop.
    }

    @Override
    public ClickProcessingState onRightClickBy(final Player playerEntity, final IChiselingContext context) {
        final Either<ClickProcessingState, Vec3> rayTraceHandle = this.processRayTraceIntoContext(
                playerEntity,
                context,
                face -> Vec3.atLowerCornerOf(face.getUnitVec3i()),
                Function.identity()
        );

        if (rayTraceHandle.right().isPresent()) {
            context.setMetadata(ModMetadataKeys.ANCHOR.get(), rayTraceHandle.right().get());
        }

        if (context.isSimulation()) {
            return ClickProcessingState.DEFAULT;
        }

        if (rayTraceHandle.left().isPresent()) {
            return rayTraceHandle.left().get();
        }

        if (rayTraceHandle.right().isEmpty()) {
            throw new IllegalArgumentException("Missing both a click processing result as well as a center vector for sphere processing");
        }

        return context.getMutator().map(mutator -> {
            final BlockInformation heldBlockState = ItemStackUtils.getHeldBitBlockInformationFromPlayer(playerEntity);
            if (heldBlockState.isAir()) {
                return ClickProcessingState.DEFAULT;
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
                if (!context.validateBuildHeights()) {
                    return ClickProcessingState.DENIED;
                }
            }

            return ClickProcessingState.ALLOW;
        }).orElse(ClickProcessingState.DEFAULT);
    }

    @Override
    public void onStoppedRightClicking(final Player playerEntity, final IChiselingContext context) {
        //Noop.
    }

    @Override
    public Optional<IAreaAccessor> getCurrentAccessor(final IChiselingContext context) {
        return context.getMutator().map(mutator -> mutator);
    }

    @Override
    public boolean isStillValid(final Player playerEntity, final IChiselingContext context, final ChiselingOperation modeOfOperation) {
        final Optional<Vec3> rayTraceHandle = modeOfOperation.isChiseling() ?
                this.processRayTraceIntoCenter(
                        playerEntity,
                        face -> Vec3.atLowerCornerOf(face.getOpposite().getUnitVec3i()),
                        facing -> facing.multiply(-1, -1, -1)
                )
                : this.processRayTraceIntoCenter(
                playerEntity,
                face -> Vec3.atLowerCornerOf(face.getUnitVec3i()),
                Function.identity()
        );

        final Optional<Vec3> contextAnchor = context.getMetadata(ModMetadataKeys.ANCHOR.get());
        return rayTraceHandle.map(d -> contextAnchor.filter(d::equals).isPresent()).orElseGet(contextAnchor::isPresent);
    }

    @Override
    public VoxelShape getShape(final IChiselingContext context) {
        final Optional<Vec3> centerCandidate = context.getMetadata(ModMetadataKeys.ANCHOR.get());
        if (centerCandidate.isEmpty()) {
            return Shapes.empty();
        }

        final Optional<IWorldAreaMutator> accessor = context.getMutator();
        if (accessor.isEmpty()) {
            return Shapes.empty();
        }

        final VoxelShape genericShape = baseSphereShapes.get(StateEntrySize.current());

        final Vec3 areaStart = accessor.get().getInWorldStartPoint();
        final BlockPos areaStartPos = VectorUtils.toBlockPos(areaStart);

        final Vec3 center = centerCandidate.get();

        final Vec3 offset = center.subtract(areaStartPos.getX(), areaStartPos.getY(), areaStartPos.getZ());

        final int blocks = StateEntrySize.ONE_SIXTEENTH.getBitsPerBlockSide() / StateEntrySize.current().getBitsPerBlockSide();
        final int fullBitAccuracy = blocks * StateEntrySize.ONE_SIXTEENTH.getBitsPerBlockSide();
        final int centerBitCoord = fullBitAccuracy / 2;
        final float centerBitCoordExact = centerBitCoord / 16f;

        final Vec3 shapeOffset = offset.subtract(centerBitCoordExact, centerBitCoordExact, centerBitCoordExact);

        return genericShape.move(shapeOffset.x(), shapeOffset.y(), shapeOffset.z());
    }

    private Optional<Vec3> processRayTraceIntoCenter(
            final Player playerEntity,
            final Function<Direction, Vec3> placementFacingAdapter,
            final Function<Vec3, Vec3> fullFacingVectorAdapter
    ) {
        final HitResult rayTraceResult = RayTracingUtils.rayTracePlayer(playerEntity);
        if (rayTraceResult.getType() != HitResult.Type.BLOCK || !(rayTraceResult instanceof final BlockHitResult blockRayTraceResult)) {
            return Optional.empty();
        }

        final Vec3 hitVector = blockRayTraceResult.getLocation().add(
                placementFacingAdapter.apply(blockRayTraceResult.getDirection())
                        .multiply(StateEntrySize.current().getSizePerHalfBit(), StateEntrySize.current().getSizePerHalfBit(), StateEntrySize.current().getSizePerHalfBit())
        );

        final Vec3 centeredHitVector = Vec3.atLowerCornerOf(
                VectorUtils.toBlockPos(
                        hitVector.multiply(StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide())
                )
        ).multiply(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit());

        final Vec3 center = centeredHitVector.add(
                new Vec3(
                        (diameter / 2d) / StateEntrySize.current().getBitsPerBlockSide(),
                        (diameter / 2d) / StateEntrySize.current().getBitsPerBlockSide(),
                        (diameter / 2d) / StateEntrySize.current().getBitsPerBlockSide()
                ).multiply(
                        fullFacingVectorAdapter.apply(Vec3.atLowerCornerOf(blockRayTraceResult.getDirection().getUnitVec3i())
                        )
                )
        );

        return Optional.of(center);
    }

    private Either<ClickProcessingState, Vec3> processRayTraceIntoContext(
            final Player playerEntity,
            final IChiselingContext context,
            final Function<Direction, Vec3> placementFacingAdapter,
            final Function<Vec3, Vec3> fullFacingVectorAdapter
    ) {
        final HitResult rayTraceResult = RayTracingUtils.rayTracePlayer(playerEntity);
        if (rayTraceResult.getType() != HitResult.Type.BLOCK || !(rayTraceResult instanceof final BlockHitResult blockRayTraceResult)) {
            context.setError(LocalStrings.ChiselAttemptFailedNoBlock.getText());
            return Either.left(ClickProcessingState.DEFAULT);
        }

        final Vec3 hitVector = blockRayTraceResult.getLocation().add(
                placementFacingAdapter.apply(blockRayTraceResult.getDirection())
                        .multiply(StateEntrySize.current().getSizePerHalfBit(), StateEntrySize.current().getSizePerHalfBit(), StateEntrySize.current().getSizePerHalfBit())
        );

        final Vec3 centeredHitVector = Vec3.atLowerCornerOf(
                VectorUtils.toBlockPos(
                        hitVector.multiply(StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide())
                )
        ).multiply(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit());

        final Vec3 center = centeredHitVector.add(
                new Vec3(
                        (diameter / 2d) / StateEntrySize.current().getBitsPerBlockSide(),
                        (diameter / 2d) / StateEntrySize.current().getBitsPerBlockSide(),
                        (diameter / 2d) / StateEntrySize.current().getBitsPerBlockSide()
                ).multiply(
                        fullFacingVectorAdapter.apply(Vec3.atLowerCornerOf(blockRayTraceResult.getDirection().getUnitVec3i())
                        )
                )
        );

        context.setStateFilter(areaAccessor -> {
            if (areaAccessor instanceof IWorldAreaAccessor) {
                return new SphereAreaFilter(context.getModeOfOperandus(), ((IWorldAreaAccessor) areaAccessor).getInWorldStartPoint(), center);
            }

            return new SphereAreaFilter(context.getModeOfOperandus(), Vec3.ZERO, center);
        });

        BlockPosStreamProvider.getForRange(diameter)
                .forEach(bitPos -> {
                    final Vec3 target = center
                            .add(Vec3.atLowerCornerOf(bitPos.subtract(new Vec3i(diameter / 2, diameter / 2, diameter / 2)))
                                    .multiply(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit()));

                    context.include(
                            target
                    );
                });

        return Either.right(center);
    }

    @Override
    public ResourceLocation getIcon() {
        return iconName;
    }

    @Override
    public Component getDisplayName() {
        return this.displayName;
    }

    @Override
    public Component getMultiLineDisplayName() {
        return this.multiLineDisplayName;
    }

    @NotNull
    @Override
    public Optional<IToolModeGroup> getGroup() {
        return Optional.of(
                ModChiselModeGroups.SPHERE
        );
    }

    private final class SphereAreaFilter implements Predicate<IStateEntryInfo> {

        private final ChiselingOperation operation;
        private final Vec3 startPoint;
        private final Vec3 center;

        private SphereAreaFilter(final ChiselingOperation operation, final Vec3 startPoint, final Vec3 center) {
            this.operation = operation;
            this.startPoint = startPoint;
            this.center = center;
        }

        @Override
        public boolean test(final IStateEntryInfo stateEntryInfo) {
            if (!(stateEntryInfo instanceof final IInWorldStateEntryInfo inWorldStateEntryInfo)) {
                return false;
            }

            return inWorldStateEntryInfo.getInWorldStartPoint().distanceTo(center) <= (diameter / 2f / StateEntrySize.current().getBitsPerBlockSide()) &&
                    (!stateEntryInfo.getBlockInformation().isAir() || operation.processesAir());
        }

        @Override
        public int hashCode() {
            int result = startPoint != null ? startPoint.hashCode() : 0;
            result = 31 * result + (center != null ? center.hashCode() : 0);
            return result;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof final SphereAreaFilter that)) {
                return false;
            }

            if (!Objects.equals(startPoint, that.startPoint)) {
                return false;
            }
            return Objects.equals(center, that.center);
        }
    }
}

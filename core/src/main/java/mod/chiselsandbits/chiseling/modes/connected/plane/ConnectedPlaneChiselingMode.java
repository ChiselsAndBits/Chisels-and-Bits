package mod.chiselsandbits.chiseling.modes.connected.plane;

import com.communi.suggestu.scena.core.registries.AbstractCustomRegistryEntry;
import com.google.common.collect.Maps;
import mod.chiselsandbits.api.axissize.CollisionType;
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
import mod.chiselsandbits.api.multistate.mutator.IMutatorFactory;
import mod.chiselsandbits.api.util.IBatchMutation;
import mod.chiselsandbits.api.util.IQuadFunction;
import mod.chiselsandbits.api.util.LocalStrings;
import mod.chiselsandbits.api.util.RayTracingUtils;
import mod.chiselsandbits.api.util.VectorUtils;
import mod.chiselsandbits.client.icon.IconManager;
import mod.chiselsandbits.registrars.ModChiselModeGroups;
import mod.chiselsandbits.registrars.ModMetadataKeys;
import mod.chiselsandbits.utils.BitInventoryUtils;
import mod.chiselsandbits.utils.ItemStackUtils;
import mod.chiselsandbits.voxelshape.VoxelShapeManager;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class ConnectedPlaneChiselingMode extends AbstractCustomRegistryEntry implements IChiselMode
{
    private final int              depth;
    private final MutableComponent displayName;
    private final MutableComponent multiLineDisplayName;
    private final ResourceLocation iconName;

    public ConnectedPlaneChiselingMode(
      final int depth,
      final MutableComponent displayName,
      final MutableComponent multiLineDisplayName,
      final ResourceLocation iconName)
    {
        this.depth = depth;
        this.displayName = displayName;
        this.multiLineDisplayName = multiLineDisplayName;
        this.iconName = iconName;
    }

    @Override
    public ClickProcessingState onLeftClickBy(
      final Player playerEntity, final IChiselingContext context)
    {
        final Optional<ClickProcessingState> rayTraceHandle = this.processRayTraceIntoContext(
          playerEntity,
          context,
          Direction::getOpposite,
          face -> Vec3.atLowerCornerOf(face.getOpposite().getUnitVec3i()),
          IQuadFunction.fourthIdentity(),
          position -> IMutatorFactory.getInstance().in(
            context.getWorld(),
            VectorUtils.toBlockPos(position)
          ),
          direction -> Vec3i.ZERO
        );

        if (context.isSimulation())
        {
            return ClickProcessingState.DEFAULT;
        }

        return rayTraceHandle.orElseGet(() -> context.getMutator().map(mutator -> {
              try (IBatchMutation ignored =
                     mutator.batch(IChangeTrackerManager.getInstance().getChangeTracker(playerEntity)))
              {
                  context.setComplete();

                  final Map<BlockInformation, Integer> resultingBitCount = Maps.newHashMap();

                  final Predicate<IStateEntryInfo> filter = context.getStateFilter()
                    .map(builder -> builder.apply(mutator))
                    .orElse((state) -> true);

                  final int totalDamage = mutator.inWorldMutableStream()
                    .filter(filter)
                    .mapToInt(state -> {
                        final BlockInformation currentState = state.getBlockInformation();

                        return context.tryDamageItemAndDoOrSetBrokenError(
                          () -> {
                              resultingBitCount.putIfAbsent(currentState, 0);
                              resultingBitCount.computeIfPresent(currentState, (s, currentCount) -> currentCount + 1);

                              state.clear();
                          });
                    }).sum();

                  if (totalDamage == 0) {
                      context.setError(LocalStrings.ChiselAttemptFailedNoValidStateFound.getText());
                  }

                  resultingBitCount.forEach((blockState, count) -> BitInventoryUtils.insertIntoOrSpawn(
                    playerEntity,
                    blockState,
                    count
                  ));
              }

              return ClickProcessingState.ALLOW;
          }).orElse(ClickProcessingState.DEFAULT)
        );
    }

    @Override
    public void onStoppedLeftClicking(final Player playerEntity, final IChiselingContext context)
    {
        //NOOP
    }

    @Override
    public ClickProcessingState onRightClickBy(final Player playerEntity, final IChiselingContext context)
    {
        final Optional<ClickProcessingState> rayTraceHandle = this.processRayTraceIntoContext(
          playerEntity,
          context,
          UnaryOperator.identity(),
          face -> Vec3.atLowerCornerOf(face.getUnitVec3i()),
          (hitPos, inBlockTargetedPosition, hitFace, candidatePosition) -> {
              if (inBlockTargetedPosition.equals(candidatePosition))
              {
                  return Vec3.atLowerCornerOf(hitPos)
                    .add(inBlockTargetedPosition)
                    .add(Vec3.atLowerCornerOf(
                          hitFace.getOpposite().getUnitVec3i()
                        )
                        .multiply(StateEntrySize.current().getSizePerBitScalingVector())
                    );
              }

              final Vec3 relevantAxisCandidate = candidatePosition.multiply(
                Math.abs(hitFace.getUnitVec3i().getX()),
                Math.abs(hitFace.getUnitVec3i().getY()),
                Math.abs(hitFace.getUnitVec3i().getZ())
              );
              final Vec3 noneRelevantAxisCandidates = candidatePosition.subtract(relevantAxisCandidate);
              final Vec3 relevantAxisTarget = inBlockTargetedPosition.multiply(
                Math.abs(hitFace.getUnitVec3i().getX()),
                Math.abs(hitFace.getUnitVec3i().getY()),
                Math.abs(hitFace.getUnitVec3i().getZ())
              );

              final Vec3 offset = candidatePosition.subtract(inBlockTargetedPosition);
              final Vec3 relevantAxisOffset = offset.multiply(
                Math.abs(hitFace.getUnitVec3i().getX()),
                Math.abs(hitFace.getUnitVec3i().getY()),
                Math.abs(hitFace.getUnitVec3i().getZ())
              );

              return noneRelevantAxisCandidates.add(relevantAxisTarget).subtract(relevantAxisOffset)
                .add(hitPos.getX(), hitPos.getY(), hitPos.getZ())
                .add(Vec3.atLowerCornerOf(
                      hitFace.getOpposite().getUnitVec3i()
                    )
                    .multiply(StateEntrySize.current().getSizePerBitScalingVector())
                );
          },
          position -> IMutatorFactory.getInstance().covering(
            context.getWorld(),
            VectorUtils.toBlockPos(position.subtract(1, 1, 1)),
            VectorUtils.toBlockPos(position.add(1, 1, 1))
          ),
          direction -> new Vec3i(
            direction.getUnitVec3i().getX() * depth,
            direction.getUnitVec3i().getY() * depth,
            direction.getUnitVec3i().getZ() * depth
          )
        );

        if (context.isSimulation())
        {
            return ClickProcessingState.DEFAULT;
        }

        return rayTraceHandle.orElseGet(() -> context.getMutator().map(mutator -> {
              final BlockInformation heldBlockState = ItemStackUtils.getHeldBitBlockInformationFromPlayer(playerEntity);
              if (heldBlockState.isAir())
              {
                  context.setError(LocalStrings.ChiselAttemptFailedNoPlaceableBitHeld.getText());
                  return ClickProcessingState.DEFAULT;
              }

              final Predicate<IStateEntryInfo> filter = context.getStateFilter()
                .map(builder -> builder.apply(mutator))
                .orElse((state) -> true);

              final int missingBitCount = (int) mutator.stream()
                .filter(state -> state.getBlockInformation().isAir() && filter.test(state))
                .count();

              final IBitInventory playerBitInventory = IBitInventoryManager.getInstance().create(playerEntity);

              context.setComplete();
              if (playerBitInventory.canExtract(heldBlockState, missingBitCount) || playerEntity.isCreative())
              {
                  if (!playerEntity.isCreative())
                  {
                      playerBitInventory.extract(heldBlockState, missingBitCount);
                  }

                  try (IBatchMutation ignored =
                         mutator.batch(IChangeTrackerManager.getInstance().getChangeTracker(playerEntity)))
                  {
                      mutator.inWorldMutableStream()
                        .filter(state -> state.getBlockInformation().isAir() && filter.test(state))
                        .forEach(state -> state.overrideState(heldBlockState)); //We can use override state here to prevent the try-catch block.
                  }
              }
              else
              {
                  context.setError(LocalStrings.ChiselAttemptFailedNotEnoughBits.getText(heldBlockState.blockState().getBlock().getName()));
              }

              if (missingBitCount == 0)
              {
                  if (!context.validateBuildHeights()) {
                      return ClickProcessingState.DENIED;
                  }
              }

              return ClickProcessingState.ALLOW;
          }).orElse(ClickProcessingState.DEFAULT)
        );
    }

    @Override
    public void onStoppedRightClicking(final Player playerEntity, final IChiselingContext context)
    {
        //NOOP
    }

    @Override
    public Optional<IAreaAccessor> getCurrentAccessor(final IChiselingContext context)
    {
        return context.getMutator().map(mutator -> mutator);
    }

    @Override
    public boolean isStillValid(final Player playerEntity, final IChiselingContext context, final ChiselingOperation modeOfOperation)
    {
        final Optional<Set<Vec3i>> validPositions = context.getMetadata(ModMetadataKeys.VALID_POSITIONS.get());
        final Optional<Direction> targetedSide = context.getMetadata(ModMetadataKeys.TARGETED_SIDE.get());
        final Optional<BlockPos> targetedBlockPos = context.getMetadata(ModMetadataKeys.TARGETED_BLOCK.get());

        if (validPositions.isEmpty() || targetedSide.isEmpty() || targetedBlockPos.isEmpty())
        {
            return false;
        }

        final HitResult rayTraceResult = RayTracingUtils.rayTracePlayer(playerEntity);
        if (rayTraceResult.getType() != HitResult.Type.BLOCK || !(rayTraceResult instanceof final BlockHitResult blockRayTraceResult))
        {
            return false;
        }

        if (blockRayTraceResult.getDirection() != targetedSide.get())
        {
            return false;
        }

        final Function<Direction, Vec3> placementFacingAdapter = modeOfOperation == ChiselingOperation.CHISELING ?
                                                                   face -> Vec3.atLowerCornerOf(face.getOpposite().getUnitVec3i()) :
                                                                                                                                  face -> Vec3.atLowerCornerOf(face.getUnitVec3i());

        final Vec3 hitVector = blockRayTraceResult.getLocation().add(
          placementFacingAdapter.apply(blockRayTraceResult.getDirection())
            .multiply(StateEntrySize.current().getSizePerHalfBit(), StateEntrySize.current().getSizePerHalfBit(), StateEntrySize.current().getSizePerHalfBit())
        );

        final BlockPos hitPos = VectorUtils.toBlockPos(hitVector);
        final Vec3 hitBlockPosVector = Vec3.atLowerCornerOf(hitPos);
        final Vec3 inBlockHitVector = hitVector.subtract(hitBlockPosVector);
        final Vec3i selectedPosition = VectorUtils.toInteger(
          inBlockHitVector.x() * StateEntrySize.current().getBitsPerBlockSide(),
          inBlockHitVector.y() * StateEntrySize.current().getBitsPerBlockSide(),
          inBlockHitVector.z() * StateEntrySize.current().getBitsPerBlockSide()
        );

        return validPositions.get().contains(selectedPosition) && hitPos.equals(targetedBlockPos.get());
    }

    private Optional<ClickProcessingState> processRayTraceIntoContext(
      final Player playerEntity,
      final IChiselingContext context,
      final UnaryOperator<Direction> searchDirectionAdapter,
      final Function<Direction, Vec3> placementFacingAdapter,
      final IQuadFunction<BlockPos, Vec3, Direction, Vec3, Vec3> stateExtractionAdapter,
      final Function<Vec3, IAreaAccessor> areaAccessorBuilder,
      final Function<Direction, Vec3i> filterOffsetProducer
    )
    {
        final HitResult rayTraceResult = RayTracingUtils.rayTracePlayer(playerEntity);
        if (rayTraceResult.getType() != HitResult.Type.BLOCK || !(rayTraceResult instanceof final BlockHitResult blockRayTraceResult))
        {
            context.setError(LocalStrings.ChiselAttemptFailedNoBlock.getText());
            return Optional.of(ClickProcessingState.DEFAULT);
        }

        final Vec3 hitVector = blockRayTraceResult.getLocation().add(
          placementFacingAdapter.apply(blockRayTraceResult.getDirection())
            .multiply(StateEntrySize.current().getSizePerHalfBit(), StateEntrySize.current().getSizePerHalfBit(), StateEntrySize.current().getSizePerHalfBit())
        );

        final BlockPos hitPos = VectorUtils.toBlockPos(hitVector);
        final Vec3 hitBlockPosVector = Vec3.atLowerCornerOf(hitPos);
        final Vec3 inBlockHitVector = hitVector.subtract(hitBlockPosVector);

        final Deque<Vec3i> toProcess = new LinkedList<>();
        final IAreaAccessor worldAccessor = areaAccessorBuilder.apply(hitVector);

        final Set<Vec3i> processed = new HashSet<>();
        final Set<Vec3i> validPositions = new HashSet<>();

        final Set<Vec3i> offsets = new HashSet<>();
        offsets.add(searchDirectionAdapter.apply(blockRayTraceResult.getDirection()).getUnitVec3i());
        Arrays.stream(Direction.values())
          .filter(direction -> direction.getAxis() != blockRayTraceResult.getDirection().getAxis())
          .map(Direction::getUnitVec3i)
          .forEach(offsets::add);

        final Vec3i selectedPosition = VectorUtils.toInteger(
          inBlockHitVector.x() * StateEntrySize.current().getBitsPerBlockSide(),
          inBlockHitVector.y() * StateEntrySize.current().getBitsPerBlockSide(),
          inBlockHitVector.z() * StateEntrySize.current().getBitsPerBlockSide()
        );
        toProcess.addLast(selectedPosition);

        final Vec3i relevantSelectedAxisVector =
          new Vec3i(
            selectedPosition.getX() * Math.abs(blockRayTraceResult.getDirection().getUnitVec3i().getX()),
            selectedPosition.getY() * Math.abs(blockRayTraceResult.getDirection().getUnitVec3i().getY()),
            selectedPosition.getZ() * Math.abs(blockRayTraceResult.getDirection().getUnitVec3i().getZ())
          );

        final Vec3 selectedInBlockPosition = Vec3.atLowerCornerOf(
          selectedPosition
        ).multiply(
          StateEntrySize.current().getSizePerBitScalingVector()
        );

        final Optional<IStateEntryInfo> targetedInfo = worldAccessor.getInAreaTarget(
          stateExtractionAdapter.apply(hitPos, selectedInBlockPosition, blockRayTraceResult.getDirection(), selectedInBlockPosition)
        );

        if (targetedInfo.isEmpty())
        {
            context.setError(LocalStrings.ChiselAttemptFailedTargetedBlockNotChiselable.getText());
            return Optional.of(ClickProcessingState.DEFAULT);
        }

        while (!toProcess.isEmpty())
        {
            final Vec3i targetedPosition = toProcess.removeFirst();
            final Vec3 targetedInBlockPosition = Vec3.atLowerCornerOf(
              targetedPosition
            ).multiply(
              StateEntrySize.current().getSizePerBitScalingVector()
            );
            final Optional<IStateEntryInfo> targetCandidate = worldAccessor.getInAreaTarget(
              stateExtractionAdapter.apply(hitPos, selectedInBlockPosition, blockRayTraceResult.getDirection(), targetedInBlockPosition)
            );

            processed.add(targetedPosition);

            if (targetCandidate.isEmpty())
            {
                continue;
            }

            IStateEntryInfo target = targetCandidate.get();

            if (target.getBlockInformation().equals(targetedInfo.get().getBlockInformation()))
            {
                //We are somehow connected to the targeted bit and of the same state.
                validPositions.add(targetedPosition);

                offsets.forEach(offset -> {
                    final Vec3i newTarget = new Vec3i(
                      targetedPosition.getX() + offset.getX(),
                      targetedPosition.getY() + offset.getY(),
                      targetedPosition.getZ() + offset.getZ()
                    );
                    if (newTarget.getX() >= 0 && newTarget.getX() < StateEntrySize.current().getBitsPerBlockSide() &&
                          newTarget.getY() >= 0 && newTarget.getY() < StateEntrySize.current().getBitsPerBlockSide() &&
                          newTarget.getZ() >= 0 && newTarget.getZ() < StateEntrySize.current().getBitsPerBlockSide())
                    {

                        final Vec3i relevantNewTargetAxisVector =
                          new Vec3i(
                            newTarget.getX() * Math.abs(blockRayTraceResult.getDirection().getUnitVec3i().getX()),
                            newTarget.getY() * Math.abs(blockRayTraceResult.getDirection().getUnitVec3i().getY()),
                            newTarget.getZ() * Math.abs(blockRayTraceResult.getDirection().getUnitVec3i().getZ())
                          );

                        final int targetedDepth = Math.abs(
                          relevantSelectedAxisVector.get(blockRayTraceResult.getDirection().getAxis()) -
                            relevantNewTargetAxisVector.get(blockRayTraceResult.getDirection().getAxis())
                        );

                        if (targetedDepth <= depth - 1)
                        {
                            //Valid offset found
                            if (!processed.contains(newTarget) && !toProcess.contains(newTarget))
                            {
                                toProcess.addLast(newTarget);
                            }
                        }
                    }
                    else
                    {
                        processed.add(newTarget);
                    }
                });
            }
        }

        context.include(hitPos, Vec3.ZERO);
        context.include(hitPos, new Vec3(0.9999, 0.9999, 0.9999));
        context.setStateFilter(accessor -> new SelectedBitStateFilter(filterOffsetProducer.apply(blockRayTraceResult.getDirection()), validPositions));

        context.setMetadata(ModMetadataKeys.VALID_POSITIONS.get(), validPositions);
        context.setMetadata(ModMetadataKeys.TARGETED_SIDE.get(), blockRayTraceResult.getDirection());
        context.setMetadata(ModMetadataKeys.TARGETED_BLOCK.get(), hitPos);

        return Optional.empty();
    }

    @Override
    public VoxelShape getShape(final IChiselingContext context)
    {
        if (context.getMutator().isEmpty())
            return Shapes.empty();

        return VoxelShapeManager.getInstance().get(context.getMutator().get(), CollisionType.ALL);
    }

    @Override
    public TextureAtlasSprite getIcon()
    {
        return IconManager.getInstance().getIcon(iconName);
    }

    @Override
    public @NotNull Optional<IToolModeGroup> getGroup()
    {
        return Optional.of(ModChiselModeGroups.CONNECTED_PLANE);
    }

    @Override
    public Component getDisplayName()
    {
        return displayName;
    }

    @Override
    public Component getMultiLineDisplayName()
    {
        return multiLineDisplayName;
    }

    private static final class SelectedBitStateFilter implements Predicate<IStateEntryInfo>
    {

        private final Vec3i      offset;
        private final Set<Vec3i> validPositions;

        public SelectedBitStateFilter(final Vec3i offset, final Set<Vec3i> validPositions)
        {
            this.offset = offset;
            this.validPositions = validPositions;
        }

        @Override
        public boolean test(final IStateEntryInfo iStateEntryInfo)
        {
            final Vec3i position = VectorUtils.toInteger(
              iStateEntryInfo.getStartPoint().x() * StateEntrySize.current().getBitsPerBlockSide(),
              iStateEntryInfo.getStartPoint().y() * StateEntrySize.current().getBitsPerBlockSide(),
              iStateEntryInfo.getStartPoint().z() * StateEntrySize.current().getBitsPerBlockSide()
            );

            return validPositions.contains(
              position
            );
        }

        @Override
        public int hashCode()
        {
            int result = offset.hashCode();
            result = 31 * result + validPositions.hashCode();
            return result;
        }

        @Override
        public boolean equals(final Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof final SelectedBitStateFilter that))
            {
                return false;
            }

            if (!offset.equals(that.offset))
            {
                return false;
            }
            return validPositions.equals(that.validPositions);
        }

        @Override
        public String toString()
        {
            return "SelectedBitStateFilter{" +
                     "offset=" + offset +
                     ", validPositions=" + validPositions +
                     '}';
        }
    }
}

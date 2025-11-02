package mod.chiselsandbits.chiseling.modes.replace;

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
import mod.chiselsandbits.api.util.LambdaExceptionUtils;
import mod.chiselsandbits.api.util.LocalStrings;
import mod.chiselsandbits.api.util.RayTracingUtils;
import mod.chiselsandbits.api.util.VectorUtils;
import mod.chiselsandbits.client.icon.IconManager;
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

public class ReplaceChiselingMode extends AbstractCustomRegistryEntry implements IChiselMode
{
    private final MutableComponent displayName;
    private final MutableComponent multiLineDisplayName;
    private final ResourceLocation iconName;

    public ReplaceChiselingMode(
      final MutableComponent displayName,
      final MutableComponent multiLineDisplayName,
      final ResourceLocation iconName) {
        this.displayName = displayName;
        this.multiLineDisplayName = multiLineDisplayName;
        this.iconName = iconName;
    }

    @Override
    public ClickProcessingState onLeftClickBy(
      final Player player, final IChiselingContext context)
    {
        final Optional<ClickProcessingState> rayTraceHandle = this.processRayTraceIntoContext(
          player,
          context
        );

        if (context.isSimulation())
        {
            return ClickProcessingState.DEFAULT;
        }

        return rayTraceHandle.orElseGet(() -> context.getMutator().map(mutator -> {
              try (IBatchMutation ignored =
                     mutator.batch(IChangeTrackerManager.getInstance().getChangeTracker(player)))
              {
                  final BlockInformation heldBlockState = ItemStackUtils.getHeldBitBlockInformationFromPlayer(player);
                  if (heldBlockState.isAir())
                  {
                      return ClickProcessingState.DEFAULT;
                  }

                  context.setComplete();

                  final Map<BlockInformation, Integer> resultingBitCount = Maps.newHashMap();

                  final Predicate<IStateEntryInfo> filter = context.getStateFilter()
                    .map(builder -> builder.apply(mutator))
                    .orElse((state) -> true);

                  final int missingBitCount = (int) mutator.stream()
                    .filter(filter)
                    .count();

                  final IBitInventory playerBitInventory = IBitInventoryManager.getInstance().create(player);

                  if (!player.isCreative() && !playerBitInventory.canExtract(heldBlockState, missingBitCount))
                  {
                      context.setError(LocalStrings.ChiselAttemptFailedNotEnoughBits.getText(heldBlockState.blockState().getBlock().getName()));
                      return ClickProcessingState.DEFAULT;
                  }

                  final int totalModifiedStates = mutator.inWorldMutableStream()
                    .filter(filter)
                    .mapToInt(LambdaExceptionUtils.rethrowToIntFunction(state -> {
                        final BlockInformation currentState = state.getBlockInformation();

                        return context.tryDamageItemAndDoOrSetBrokenError(
                          () -> {
                              resultingBitCount.putIfAbsent(currentState, 0);
                              resultingBitCount.computeIfPresent(currentState, (s, currentCount) -> currentCount + 1);

                              state.overrideState(heldBlockState);
                          });
                    }))
                    .sum();

                  if (totalModifiedStates == 0) {
                      context.setError(LocalStrings.ChiselAttemptFailedNoValidStateFound.getText());
                  }

                  if (!player.isCreative())
                        playerBitInventory.extract(heldBlockState, missingBitCount);

                  resultingBitCount.forEach((blockState, count) -> BitInventoryUtils.insertIntoOrSpawn(
                    player,
                    blockState,
                    count
                  ));
              }

              return ClickProcessingState.ALLOW;
          }).orElse(ClickProcessingState.DEFAULT)
        );
    }

    @Override
    public void onStoppedLeftClicking(final Player Player, final IChiselingContext context)
    {
        //NOOP
    }

    @Override
    public ClickProcessingState onRightClickBy(final Player Player, final IChiselingContext context)
    {
        return onLeftClickBy(Player, context);
    }

    @Override
    public void onStoppedRightClicking(final Player Player, final IChiselingContext context)
    {
        //NOOP
    }

    @Override
    public boolean isStillValid(final Player Player, final IChiselingContext context, final ChiselingOperation modeOfOperation)
    {
        final Optional<Set<Vec3i>> validPositions = context.getMetadata(ModMetadataKeys.VALID_POSITIONS.get());
        final Optional<Direction> targetedSide = context.getMetadata(ModMetadataKeys.TARGETED_SIDE.get());
        final Optional<BlockPos> targetedBlockPos = context.getMetadata(ModMetadataKeys.TARGETED_BLOCK.get());

        if (validPositions.isEmpty() || targetedSide.isEmpty() || targetedBlockPos.isEmpty())
            return false;

        final HitResult hitResult = RayTracingUtils.rayTracePlayer(Player);
        if (hitResult.getType() != HitResult.Type.BLOCK || !(hitResult instanceof final BlockHitResult blockHitResult))
        {
            return false;
        }

        if (blockHitResult.getDirection() != targetedSide.get())
            return false;

        final Function<Direction, Vec3> placementFacingAdapter = modeOfOperation == ChiselingOperation.CHISELING ?
                                                                       face -> Vec3.atLowerCornerOf(face.getOpposite().getUnitVec3i()) :
                                                                       face -> Vec3.atLowerCornerOf(face.getUnitVec3i());

        final Vec3 hitVector = blockHitResult.getLocation().add(
          placementFacingAdapter.apply(blockHitResult.getDirection())
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

    @Override
    public Optional<IAreaAccessor> getCurrentAccessor(final IChiselingContext context)
    {
        return context.getMutator().map(mutator -> mutator);
    }

    private Optional<ClickProcessingState> processRayTraceIntoContext(
      final Player Player,
      final IChiselingContext context
    )
    {
        final HitResult hitResult = RayTracingUtils.rayTracePlayer(Player);
        if (hitResult.getType() != HitResult.Type.BLOCK || !(hitResult instanceof final BlockHitResult blockHitResult))
        {
            context.setError(LocalStrings.ChiselAttemptFailedNoBlock.getText());
            return Optional.of(ClickProcessingState.DEFAULT);
        }

        final Function<Direction, Vec3> placementFacingAdapter = face -> Vec3.atLowerCornerOf(face.getOpposite().getUnitVec3i());
        final Vec3 hitVector = blockHitResult.getLocation().add(
          placementFacingAdapter.apply(blockHitResult.getDirection())
            .multiply(StateEntrySize.current().getSizePerHalfBit(), StateEntrySize.current().getSizePerHalfBit(), StateEntrySize.current().getSizePerHalfBit())
        );

        final BlockPos hitPos = VectorUtils.toBlockPos(hitVector);
        final Vec3 hitBlockPosVector = Vec3.atLowerCornerOf(hitPos);
        final Vec3 inBlockHitVector = hitVector.subtract(hitBlockPosVector);

        final Deque<Vec3i> toProcess = new LinkedList<>();
        final IAreaAccessor worldAccessor = IMutatorFactory.getInstance().in(context.getWorld(), hitPos);

        final Set<Vec3i> validPositions = new HashSet<>();

        final Vec3i selectedPosition = VectorUtils.toInteger(
          inBlockHitVector.x() * StateEntrySize.current().getBitsPerBlockSide(),
          inBlockHitVector.y() * StateEntrySize.current().getBitsPerBlockSide(),
          inBlockHitVector.z() * StateEntrySize.current().getBitsPerBlockSide()
        );
        toProcess.addLast(selectedPosition);

        final Vec3 selectedInBlockPosition = Vec3.atLowerCornerOf(
          selectedPosition
        ).multiply(
          StateEntrySize.current().getSizePerBitScalingVector()
        );

        final Optional<IStateEntryInfo> targetedInfo = worldAccessor.getInAreaTarget(
          selectedInBlockPosition
        );

        if (targetedInfo.isEmpty())
        {
            context.setError(LocalStrings.ChiselAttemptFailedTargetedBlockNotChiselable.getText());
            return Optional.of(ClickProcessingState.DEFAULT);
        }

        worldAccessor.stream()
          .filter(state -> state.getBlockInformation().equals(targetedInfo.get().getBlockInformation()))
          .map(state -> state.getStartPoint().multiply(StateEntrySize.current().getBitsPerBlockSideScalingVector()))
          .map(position -> VectorUtils.toInteger(
            position.x(),
            position.y(),
            position.z()
          ))
          .forEach(validPositions::add);

        context.include(hitPos, Vec3.ZERO);
        context.include(hitPos, new Vec3(0.9999, 0.9999, 0.9999));
        context.setStateFilter(accessor -> new SelectedBitStateFilter(validPositions));

        context.setMetadata(ModMetadataKeys.VALID_POSITIONS.get(), validPositions);
        context.setMetadata(ModMetadataKeys.TARGETED_SIDE.get(), blockHitResult.getDirection());
        context.setMetadata(ModMetadataKeys.TARGETED_BLOCK.get(), hitPos);

        return Optional.empty();
    }

    @Override
    public boolean requiresPlaceableEditStack()
    {
        return true;
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
        return Optional.empty();
    }

    @Override
    public Component getDisplayName()
    {
        return displayName;
    }

    @Override
    public Component getMultiLineDisplayName()
    {
        return this.multiLineDisplayName;
    }

    private static final class SelectedBitStateFilter implements Predicate<IStateEntryInfo> {

        private final Set<Vec3i> validPositions;

        public SelectedBitStateFilter( final Set<Vec3i> validPositions)
        {
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

            return validPositions.equals(that.validPositions);
        }

        @Override
        public int hashCode()
        {
            return validPositions.hashCode();
        }

        @Override
        public String toString()
        {
            return "SelectedBitStateFilter{" +
                     "validPositions=" + validPositions +
                     '}';
        }
    }
}

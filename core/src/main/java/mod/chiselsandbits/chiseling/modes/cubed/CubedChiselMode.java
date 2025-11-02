package mod.chiselsandbits.chiseling.modes.cubed;

import com.communi.suggestu.scena.core.registries.AbstractCustomRegistryEntry;
import com.google.common.collect.Maps;
import mod.chiselsandbits.api.axissize.CollisionType;
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
import mod.chiselsandbits.api.util.*;
import mod.chiselsandbits.client.icon.IconManager;
import mod.chiselsandbits.registrars.ModChiselModeGroups;
import mod.chiselsandbits.utils.BitInventoryUtils;
import mod.chiselsandbits.utils.ItemStackUtils;
import mod.chiselsandbits.voxelshape.VoxelShapeManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class CubedChiselMode extends AbstractCustomRegistryEntry implements IChiselMode
{
    private final int              bitsPerSide;
    private final boolean          aligned;
    private final MutableComponent displayName;
    private final MutableComponent multiLineDisplayName;
    private final ResourceLocation iconName;

    CubedChiselMode(
      final int bitsPerSide,
      final boolean aligned,
      final MutableComponent displayName,
      final MutableComponent multiLineDisplayName,
      final ResourceLocation iconName)
    {
        this.bitsPerSide = bitsPerSide;
        this.aligned = aligned;
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
          face -> Vec3.atLowerCornerOf(face.getOpposite().getUnitVec3i()),
          Function.identity()
        );

        if (context.isSimulation())
        {
            return ClickProcessingState.DEFAULT;
        }

        context.setComplete();
        return rayTraceHandle.orElseGet(() -> context.getMutator().map(mutator -> {
              try (IBatchMutation ignored =
                     mutator.batch(IChangeTrackerManager.getInstance().getChangeTracker(playerEntity)))
              {
                  final Map<BlockInformation, Integer> resultingBitCount = Maps.newHashMap();

                  final int totalItemDamage = mutator.inWorldMutableStream()
                    .mapToInt(state -> {
                        final BlockInformation currentState = state.getBlockInformation();
                        return context.tryDamageItemAndDoOrSetBrokenError(
                          () -> {
                              resultingBitCount.putIfAbsent(currentState, 0);
                              resultingBitCount.computeIfPresent(currentState, (s, currentCount) -> currentCount + 1);

                              state.clear();
                          });
                    }).sum();

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
        //Noop.
    }

    @Override
    public ClickProcessingState onRightClickBy(final Player playerEntity, final IChiselingContext context)
    {
        final Optional<ClickProcessingState> rayTraceHandle = this.processRayTraceIntoContext(
          playerEntity,
          context,
          face -> Vec3.atLowerCornerOf(face.getUnitVec3i()),
          facingVector -> aligned ? facingVector : facingVector.multiply(1, -1, 1)
        );

        if (context.isSimulation())
        {
            return ClickProcessingState.DEFAULT;
        }

        return rayTraceHandle.orElseGet(() -> context.getMutator().map(mutator -> {
              final BlockInformation heldBlockState = ItemStackUtils.getHeldBitBlockInformationFromPlayer(playerEntity);
              if (heldBlockState.isAir())
              {
                  return ClickProcessingState.DEFAULT;
              }

              final int missingBitCount = (int) mutator.stream()
                .filter(state -> state.getBlockInformation().isAir())
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
                        .filter(state -> state.getBlockInformation().isAir())
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
        //Noop.
    }

    @Override
    public Optional<IAreaAccessor> getCurrentAccessor(final IChiselingContext context)
    {
        return context.getMutator().map(mutator -> mutator);
    }

    @Override
    public VoxelShape getShape(final IChiselingContext context)
    {
        if (context.getMutator().isEmpty())
            return Shapes.empty();

        return VoxelShapeManager.getInstance().get(context.getMutator().get(), CollisionType.ALL);
    }

    private Optional<ClickProcessingState> processRayTraceIntoContext(
      final Player playerEntity,
      final IChiselingContext context,
      final Function<Direction, Vec3> placementFacingAdapter,
      final Function<Vec3, Vec3> fullFacingVectorAdapter
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

        Vec3 alignmentOffset = Vec3.ZERO;
        final Vec3 fullFacingVector = fullFacingVectorAdapter.apply(aligned ? new Vec3(1, 1, 1) : Vec3.atLowerCornerOf(
          RayTracingUtils.getFullFacingVector(playerEntity)
        ));

        if (aligned)
        {
            final Vec3 inBlockOffset = hitVector.subtract(Vec3.atLowerCornerOf(VectorUtils.toInteger(hitVector)));
            final BlockPos bitsInBlockOffset = VectorUtils.toBlockPos(
              inBlockOffset.multiply(
                      StateEntrySize.current().getBitsPerBlockSide(),
                      StateEntrySize.current().getBitsPerBlockSide(),
                      StateEntrySize.current().getBitsPerBlockSide()
              ));

            final BlockPos targetedSectionIndices = new BlockPos(
              bitsInBlockOffset.getX() / bitsPerSide,
              bitsInBlockOffset.getY() / bitsPerSide,
              bitsInBlockOffset.getZ() / bitsPerSide
            );

            final BlockPos targetedStartPoint = new BlockPos(
              targetedSectionIndices.getX() * bitsPerSide,
              targetedSectionIndices.getY() * bitsPerSide,
              targetedSectionIndices.getZ() * bitsPerSide
            );

            final BlockPos targetedBitsInBlockOffset = bitsInBlockOffset.subtract(targetedStartPoint);

            alignmentOffset = Vec3.atLowerCornerOf(targetedBitsInBlockOffset)
              .multiply(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit());
        }

        final Vec3 finalAlignmentOffset = alignmentOffset.multiply(fullFacingVector);
        BlockPosStreamProvider.getForRange(bitsPerSide)
          .forEach(bitPos -> context.include(
            hitVector
              .subtract(finalAlignmentOffset)
              .add(Vec3.atLowerCornerOf(bitPos)
                .multiply(fullFacingVector)
                .multiply(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit()))
          ));

        return Optional.empty();
    }

    @Override
    public TextureAtlasSprite getIcon()
    {
        return IconManager.getInstance().getIcon(iconName);
    }

    @Override
    public Component getDisplayName()
    {
        return this.displayName;
    }

    @Override
    public Component getMultiLineDisplayName()
    {
        return this.multiLineDisplayName;
    }

    @NotNull
    @Override
    public Optional<IToolModeGroup> getGroup()
    {
        return Optional.of(
          aligned ? ModChiselModeGroups.CUBED_ALIGNED : ModChiselModeGroups.CUBED
        );
    }
}

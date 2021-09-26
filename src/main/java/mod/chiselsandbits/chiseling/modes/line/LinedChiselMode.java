package mod.chiselsandbits.chiseling.modes.line;

import com.google.common.collect.Maps;
import mod.chiselsandbits.api.chiseling.IChiselingContext;
import mod.chiselsandbits.api.chiseling.mode.IChiselMode;
import mod.chiselsandbits.api.inventory.bit.IBitInventory;
import mod.chiselsandbits.api.inventory.management.IBitInventoryManager;
import mod.chiselsandbits.api.item.click.ClickProcessingState;
import mod.chiselsandbits.api.item.withmode.group.IToolModeGroup;
import mod.chiselsandbits.api.multistate.StateEntrySize;
import mod.chiselsandbits.api.multistate.accessor.IAreaAccessor;
import mod.chiselsandbits.api.multistate.mutator.batched.IBatchMutation;
import mod.chiselsandbits.api.util.RayTracingUtils;
import mod.chiselsandbits.registrars.ModChiselModeGroups;
import mod.chiselsandbits.utils.BitInventoryUtils;
import mod.chiselsandbits.utils.ItemStackUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.registries.ForgeRegistryEntry;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static mod.chiselsandbits.block.entities.ChiseledBlockEntity.ONE_THOUSANDS;

@SuppressWarnings("deprecation")
public class LinedChiselMode extends ForgeRegistryEntry<IChiselMode> implements IChiselMode
{
    private final int                       bitsPerSide;
    private final MutableComponent displayName;
    private final ResourceLocation          iconName;

    LinedChiselMode(final int bitsPerSide, final MutableComponent displayName, final ResourceLocation iconName)
    {
        this.bitsPerSide = bitsPerSide;
        this.displayName = displayName;
        this.iconName = iconName;
    }

    @Override
    public ClickProcessingState onLeftClickBy(
      final Player playerEntity, final IChiselingContext context)
    {
        final Optional<ClickProcessingState> rayTraceHandle = this.processRayTraceIntoContext(
          playerEntity,
          context,
          face -> Vec3.atLowerCornerOf(face.getOpposite().getNormal()),
          Direction::getOpposite,
          false
        );


        if (context.isSimulation())
        {
            return ClickProcessingState.DEFAULT;
        }

        return rayTraceHandle.orElseGet(() -> context.getMutator().map(mutator -> {
              try (IBatchMutation ignored =
                     mutator.batch())
              {
                  context.setComplete();

                  final Map<BlockState, Integer> resultingBitCount = Maps.newHashMap();

                  mutator.inWorldMutableStream()
                    .forEach(state -> {
                        final BlockState currentState = state.getState();

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

              return new ClickProcessingState(true, Event.Result.ALLOW);
          }).orElse(ClickProcessingState.DEFAULT)
        );
    }

    @Override
    public void onStoppedLeftClicking(final Player playerEntity, final IChiselingContext context)
    {

    }

    @Override
    public ClickProcessingState onRightClickBy(final Player playerEntity, final IChiselingContext context)
    {
        final Optional<ClickProcessingState> rayTraceHandle = this.processRayTraceIntoContext(
          playerEntity,
          context,
          face -> Vec3.atLowerCornerOf(face.getNormal()),
          Function.identity(),
          true
        );

        if (context.isSimulation())
        {
            return ClickProcessingState.DEFAULT;
        }

        return rayTraceHandle.orElseGet(() -> context.getMutator().map(mutator -> {
              final BlockState heldBlockState = ItemStackUtils.getHeldBitBlockStateFromPlayer(playerEntity);
            if (heldBlockState.isAir())
            {
                return ClickProcessingState.DEFAULT;
            }

              final int missingBitCount = (int) mutator.stream()
                                                  .filter(state -> state.getState().isAir())
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
                         mutator.batch())
                  {
                      mutator.inWorldMutableStream()
                        .filter(state -> state.getState().isAir())
                        .forEach(state -> state.overrideState(heldBlockState)); //We can use override state here to prevent the try-catch block.
                  }
              }

              return new ClickProcessingState(true, Event.Result.ALLOW);
          }).orElse(ClickProcessingState.DEFAULT)
        );
    }

    @Override
    public void onStoppedRightClicking(final Player playerEntity, final IChiselingContext context)
    {

    }

    @Override
    public Optional<IAreaAccessor> getCurrentAccessor(final IChiselingContext context)
    {
        return context.getMutator().map(mutator -> mutator);
    }

    private Optional<ClickProcessingState> processRayTraceIntoContext(
      final Player playerEntity,
      final IChiselingContext context,
      final Function<Direction, Vec3> placementFacingAdapter,
      final Function<Direction, Direction> iterationAdaptor,
      final boolean airOnly
    )
    {
        final HitResult rayTraceResult = RayTracingUtils.rayTracePlayer(playerEntity);
        if (rayTraceResult.getType() != HitResult.Type.BLOCK || !(rayTraceResult instanceof final BlockHitResult blockRayTraceResult))
        {
            return Optional.of(ClickProcessingState.DEFAULT);
        }

        final Vec3 hitVector = blockRayTraceResult.getLocation().add(
          placementFacingAdapter.apply(blockRayTraceResult.getDirection())
            .multiply(StateEntrySize.current().getSizePerHalfBit(), StateEntrySize.current().getSizePerHalfBit(), StateEntrySize.current().getSizePerHalfBit())
        );

        final Vec3 hitBlockPosVector = Vec3.atLowerCornerOf(new BlockPos(hitVector));
        final Vec3 inBlockHitVector = hitVector.subtract(hitBlockPosVector);
        final Vec3 inBlockBitVector = inBlockHitVector.multiply(StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide());

        final Direction iterationDirection = iterationAdaptor.apply(blockRayTraceResult.getDirection());
        switch (iterationDirection)
        {
            case DOWN -> includeDownAxis(context, airOnly, hitBlockPosVector, inBlockBitVector);
            case UP -> includeUpAxis(context, airOnly, hitBlockPosVector, inBlockBitVector);
            case NORTH -> includeNorthAxis(context, airOnly, hitBlockPosVector, inBlockBitVector);
            case SOUTH -> includeSouthAxis(context, airOnly, hitBlockPosVector, inBlockBitVector);
            case WEST -> includeWestAxis(context, airOnly, hitBlockPosVector, inBlockBitVector);
            case EAST -> includeEastAxis(context, airOnly, hitBlockPosVector, inBlockBitVector);
        }

        return Optional.empty();
    }

    private void includeDownAxis(final IChiselingContext context, final boolean airOnly, final Vec3 hitBlockPosVector, final Vec3 inBlockBitVector)
    {
        if (bitsPerSide == 1)
        {
            for (int yOff = 0; yOff < inBlockBitVector.y(); yOff++)
            {
                final Vec3 targetBit = inBlockBitVector.subtract(0, yOff, 0);
                final Vec3 inWorldOffset = clampVectorToBlock(targetBit.multiply(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit()));
                final Vec3 inWorldTarget = hitBlockPosVector.add(inWorldOffset);

                if (context.getInAreaTarget(inWorldTarget)
                      .map(state -> !state.getState().isAir() ^ airOnly)
                      .orElse(airOnly)
                )
                {
                    context.include(inWorldTarget);
                }
                else
                {
                    return;
                }
            }

            return;
        }

        for (int xOff = -bitsPerSide / 2; xOff < bitsPerSide / 2; xOff++)
        {
            for (int zOff = -bitsPerSide / 2; zOff < bitsPerSide / 2; zOff++)
            {
                for (int yOff = 0; yOff < inBlockBitVector.y(); yOff++)
                {
                    final Vec3 targetBit = inBlockBitVector.subtract(xOff, yOff, zOff);
                    final Vec3 inWorldOffset = clampVectorToBlock(targetBit.multiply(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit()));
                    final Vec3 inWorldTarget = hitBlockPosVector.add(inWorldOffset);

                    if (context.getInAreaTarget(inWorldTarget)
                          .map(state -> !state.getState().isAir() ^ airOnly)
                          .orElse(airOnly)
                    )
                    {
                        context.include(inWorldTarget);
                    }
                    else
                    {
                        break;
                    }
                }
            }
        }
    }

    private void includeUpAxis(final IChiselingContext context, final boolean airOnly, final Vec3 hitBlockPosVector, final Vec3 inBlockBitVector)
    {
        if (bitsPerSide == 1)
        {
            for (int yOff = 0; yOff < (StateEntrySize.current().getBitsPerBlockSide() - inBlockBitVector.y()); yOff++)
            {
                final Vec3 targetBit = inBlockBitVector.subtract(0, -yOff, 0);
                final Vec3 inWorldOffset = clampVectorToBlock(targetBit.multiply(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit()));
                final Vec3 inWorldTarget = hitBlockPosVector.add(inWorldOffset);

                if (context.getInAreaTarget(inWorldTarget)
                      .map(state -> !state.getState().isAir() ^ airOnly)
                      .orElse(airOnly)
                )
                {
                    context.include(inWorldTarget);
                }
                else
                {
                    return;
                }
            }

            return;
        }

        for (int xOff = -bitsPerSide / 2; xOff < bitsPerSide / 2; xOff++)
        {
            for (int zOff = -bitsPerSide / 2; zOff < bitsPerSide / 2; zOff++)
            {
                for (int yOff = 0; yOff < (StateEntrySize.current().getBitsPerBlockSide() - inBlockBitVector.y()); yOff++)
                {
                    final Vec3 targetBit = inBlockBitVector.subtract(xOff, -yOff, zOff);
                    final Vec3 inWorldOffset = clampVectorToBlock(targetBit.multiply(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit()));
                    final Vec3 inWorldTarget = hitBlockPosVector.add(inWorldOffset);

                    if (context.getInAreaTarget(inWorldTarget)
                          .map(state -> !state.getState().isAir() ^ airOnly)
                          .orElse(airOnly)
                    )
                    {
                        context.include(inWorldTarget);
                    }
                    else
                    {
                        break;
                    }
                }
            }
        }
    }

    private void includeNorthAxis(final IChiselingContext context, final boolean airOnly, final Vec3 hitBlockPosVector, final Vec3 inBlockBitVector)
    {
        if (bitsPerSide == 1)
        {
            for (int zOff = 0; zOff < inBlockBitVector.z(); zOff++)
            {
                final Vec3 targetBit = inBlockBitVector.subtract(0, 0, zOff);
                final Vec3 inWorldOffset = clampVectorToBlock(targetBit.multiply(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit()));
                final Vec3 inWorldTarget = hitBlockPosVector.add(inWorldOffset);

                if (context.getInAreaTarget(inWorldTarget)
                      .map(state -> !state.getState().isAir() ^ airOnly)
                      .orElse(airOnly)
                )
                {
                    context.include(inWorldTarget);
                }
                else
                {
                    return;
                }
            }

            return;
        }

        for (int xOff = -bitsPerSide / 2; xOff < bitsPerSide / 2; xOff++)
        {
            for (int yOff = -bitsPerSide / 2; yOff < bitsPerSide / 2; yOff++)
            {
                for (int zOff = 0; zOff < inBlockBitVector.z(); zOff++)
                {
                    final Vec3 targetBit = inBlockBitVector.subtract(xOff, yOff, zOff);
                    final Vec3 inWorldOffset = clampVectorToBlock(targetBit.multiply(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit()));
                    final Vec3 inWorldTarget = hitBlockPosVector.add(inWorldOffset);

                    if (context.getInAreaTarget(inWorldTarget)
                          .map(state -> !state.getState().isAir() ^ airOnly)
                          .orElse(airOnly)
                    )
                    {
                        context.include(inWorldTarget);
                    }
                    else
                    {
                        break;
                    }
                }
            }
        }
    }

    private void includeSouthAxis(final IChiselingContext context, final boolean airOnly, final Vec3 hitBlockPosVector, final Vec3 inBlockBitVector)
    {
        if (bitsPerSide == 1)
        {
            for (int zOff = 0; zOff < (StateEntrySize.current().getBitsPerBlockSide() - inBlockBitVector.z()); zOff++)
            {
                final Vec3 targetBit = inBlockBitVector.subtract(0, 0, -zOff);
                final Vec3 inWorldOffset = clampVectorToBlock(targetBit.multiply(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit()));
                final Vec3 inWorldTarget = hitBlockPosVector.add(inWorldOffset);

                if (context.getInAreaTarget(inWorldTarget)
                      .map(state -> !state.getState().isAir() ^ airOnly)
                      .orElse(airOnly)
                )
                {
                    context.include(inWorldTarget);
                }
                else
                {
                    return;
                }
            }

            return;
        }

        for (int xOff = -bitsPerSide / 2; xOff < bitsPerSide / 2; xOff++)
        {
            for (int yOff = -bitsPerSide / 2; yOff < bitsPerSide / 2; yOff++)
            {
                for (int zOff = 0; zOff < (StateEntrySize.current().getBitsPerBlockSide() - inBlockBitVector.z()); zOff++)
                {
                    final Vec3 targetBit = inBlockBitVector.subtract(xOff, yOff, -zOff);
                    final Vec3 inWorldOffset = clampVectorToBlock(targetBit.multiply(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit()));
                    final Vec3 inWorldTarget = hitBlockPosVector.add(inWorldOffset);

                    if (context.getInAreaTarget(inWorldTarget)
                          .map(state -> !state.getState().isAir() ^ airOnly)
                          .orElse(airOnly)
                    )
                    {
                        context.include(inWorldTarget);
                    }
                    else
                    {
                        break;
                    }
                }
            }
        }
    }

    private void includeWestAxis(final IChiselingContext context, final boolean airOnly, final Vec3 hitBlockPosVector, final Vec3 inBlockBitVector)
    {
        if (bitsPerSide == 1)
        {
            for (int xOff = 0; xOff < inBlockBitVector.x(); xOff++)
            {
                final Vec3 targetBit = inBlockBitVector.subtract(xOff, 0, 0);
                final Vec3 inWorldOffset = clampVectorToBlock(targetBit.multiply(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit()));
                final Vec3 inWorldTarget = hitBlockPosVector.add(inWorldOffset);

                if (context.getInAreaTarget(inWorldTarget)
                      .map(state -> !state.getState().isAir() ^ airOnly)
                      .orElse(airOnly)
                )
                {
                    context.include(inWorldTarget);
                }
                else
                {
                    return;
                }
            }

            return;
        }


        for (int zOff = -bitsPerSide / 2; zOff < bitsPerSide / 2; zOff++)
        {
            for (int yOff = -bitsPerSide / 2; yOff < bitsPerSide / 2; yOff++)
            {
                for (int xOff = 0; xOff < inBlockBitVector.x(); xOff++)
                {
                    final Vec3 targetBit = inBlockBitVector.subtract(xOff, yOff, zOff);
                    final Vec3 inWorldOffset = clampVectorToBlock(targetBit.multiply(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit()));
                    final Vec3 inWorldTarget = hitBlockPosVector.add(inWorldOffset);

                    if (context.getInAreaTarget(inWorldTarget)
                          .map(state -> !state.getState().isAir() ^ airOnly)
                          .orElse(airOnly)
                    )
                    {
                        context.include(inWorldTarget);
                    }
                    else
                    {
                        break;
                    }
                }
            }
        }
    }

    private void includeEastAxis(final IChiselingContext context, final boolean airOnly, final Vec3 hitBlockPosVector, final Vec3 inBlockBitVector)
    {
        if (bitsPerSide == 1)
        {
            for (int xOff = 0; xOff < (StateEntrySize.current().getBitsPerBlockSide() - inBlockBitVector.x()); xOff++)
            {
                final Vec3 targetBit = inBlockBitVector.subtract(-xOff, 0, 0);
                final Vec3 inWorldOffset = clampVectorToBlock(targetBit.multiply(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit()));
                final Vec3 inWorldTarget = hitBlockPosVector.add(inWorldOffset);

                if (context.getInAreaTarget(inWorldTarget)
                      .map(state -> !state.getState().isAir() ^ airOnly)
                      .orElse(airOnly)
                )
                {
                    context.include(inWorldTarget);
                }
                else
                {
                    return;
                }
            }

            return;
        }

        for (int zOff = -bitsPerSide / 2; zOff < bitsPerSide / 2; zOff++)
        {
            for (int yOff = -bitsPerSide / 2; yOff < bitsPerSide / 2; yOff++)
            {
                for (int xOff = 0; xOff < (StateEntrySize.current().getBitsPerBlockSide() - inBlockBitVector.x()); xOff++)
                {
                    final Vec3 targetBit = inBlockBitVector.subtract(-xOff, yOff, zOff);
                    final Vec3 inWorldOffset = clampVectorToBlock(targetBit.multiply(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit()));
                    final Vec3 inWorldTarget = hitBlockPosVector.add(inWorldOffset);

                    if (context.getInAreaTarget(inWorldTarget)
                          .map(state -> !state.getState().isAir() ^ airOnly)
                          .orElse(airOnly)
                    )
                    {
                        context.include(inWorldTarget);
                    }
                    else
                    {
                        break;
                    }
                }
            }
        }
    }

    private Vec3 clampVectorToBlock(final Vec3 v)
    {
        return new Vec3(
          v.x() < 0 ? 0 : (v.x() >= 1 ? 1 - ONE_THOUSANDS : v.x()),
          v.y() < 0 ? 0 : (v.y() >= 1 ? 1 - ONE_THOUSANDS : v.y()),
          v.z() < 0 ? 0 : (v.z() >= 1 ? 1 - ONE_THOUSANDS : v.z())
        );
    }

    @Override
    public @NotNull ResourceLocation getIcon()
    {
        return iconName;
    }

    @Override
    public Component getDisplayName()
    {
        return this.displayName;
    }

    @NotNull
    @Override
    public Optional<IToolModeGroup> getGroup()
    {
        return Optional.of(ModChiselModeGroups.LINE);
    }
}
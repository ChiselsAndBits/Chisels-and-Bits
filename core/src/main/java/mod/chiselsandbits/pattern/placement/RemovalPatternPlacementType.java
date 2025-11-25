package mod.chiselsandbits.pattern.placement;

import com.communi.suggestu.scena.core.registries.AbstractCustomRegistryEntry;
import mod.chiselsandbits.api.block.IMultiStateBlock;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.change.IChangeTrackerManager;
import mod.chiselsandbits.api.chiseling.eligibility.IEligibilityManager;
import mod.chiselsandbits.api.config.IClientConfiguration;
import mod.chiselsandbits.api.inventory.bit.IBitInventory;
import mod.chiselsandbits.api.inventory.management.IBitInventoryManager;
import mod.chiselsandbits.api.item.withmode.group.IToolModeGroup;
import mod.chiselsandbits.api.multistate.accessor.IStateEntryInfo;
import mod.chiselsandbits.api.multistate.mutator.IMutatorFactory;
import mod.chiselsandbits.api.multistate.mutator.world.IWorldAreaMutator;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshot;
import mod.chiselsandbits.api.pattern.placement.IPatternPlacementType;
import mod.chiselsandbits.api.placement.PlacementResult;
import mod.chiselsandbits.api.util.BlockPosStreamProvider;
import mod.chiselsandbits.api.util.IBatchMutation;
import mod.chiselsandbits.api.util.LocalStrings;
import mod.chiselsandbits.api.util.VectorUtils;
import mod.chiselsandbits.api.variant.state.IStateVariantManager;
import mod.chiselsandbits.client.icon.IconManager;
import mod.chiselsandbits.registrars.ModPatternPlacementTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static mod.chiselsandbits.api.util.constants.Constants.MOD_ID;

public class RemovalPatternPlacementType extends AbstractCustomRegistryEntry implements IPatternPlacementType
{

    @Override
    public VoxelShape buildVoxelShapeForWireframe(
      final IMultiStateSnapshot sourceSnapshot, final Player player, final Vec3 targetedPoint, final Direction hitFace)
    {
        final BlockPos targetedPosition = hitFace.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? VectorUtils.toBlockPos(targetedPoint) : VectorUtils.toBlockPos(targetedPoint).offset(hitFace.getOpposite().getUnitVec3i());
        final VoxelShape targetingShape = BlockPosStreamProvider.getForRange(
          player.isShiftKeyDown() ? targetedPoint : Vec3.atLowerCornerOf(targetedPosition) ,
          player.isShiftKeyDown() ? targetedPoint.add(0.9999, 0.9999,0.9999): Vec3.atLowerCornerOf(targetedPosition)
        ).map(position -> player.level().getBlockState(position).getShape(
          player.level(),
          position
        ).move(
          position.getX() - targetedPosition.getX(),
          position.getY() - targetedPosition.getY(),
          position.getZ() - targetedPosition.getZ()
          )
        ).reduce(Shapes.empty(), (voxelShape, voxelShape2) -> Shapes.joinUnoptimized(voxelShape, voxelShape2, BooleanOp.OR)).optimize();

        final Vec3 offSet = player.isShiftKeyDown() ?
                                  new Vec3(
                                    targetedPoint.x() - targetedPosition.getX(),
                                    targetedPoint.y() - targetedPosition.getY(),
                                    targetedPoint.z() - targetedPosition.getZ()
                                  ) : Vec3.ZERO;
        final VoxelShape ownShape = ModPatternPlacementTypes.REMOVAL.get().buildVoxelShapeForWireframe(
          sourceSnapshot, player, targetedPoint, hitFace
        ).move(
          offSet.x(),
          offSet.y(),
          offSet.z()
        );

        return Shapes.join(
          targetingShape, ownShape, BooleanOp.ONLY_FIRST
        );
    }

    @Override
    public PlacementResult performPlacement(
      final IMultiStateSnapshot source, final BlockPlaceContext context, final boolean simulate)
    {
        final Vec3 targetedPosition = context.getPlayer().isShiftKeyDown() ?
                                            context.getClickLocation()
                                            : Vec3.atLowerCornerOf(context.getClickedPos().offset(context.getClickedFace().getOpposite().getUnitVec3i()));
        final IWorldAreaMutator areaMutator =
          IMutatorFactory.getInstance().covering(
            context.getLevel(),
            targetedPosition,
            targetedPosition.add(0.9999,0.9999,0.9999)
          );

        final boolean isChiseledBlock = BlockPosStreamProvider.getForAccessor(areaMutator)
          .map(pos -> context.getLevel().getBlockState(pos))
          .allMatch(state -> state.getBlock() instanceof IMultiStateBlock);

        if (isChiseledBlock)
        {
            return PlacementResult.failure(
                    IClientConfiguration::getNotFittingPatternPlacementColor,
                    LocalStrings.PatternPlacementNotASolidBlock.getText());
        }

        final boolean isSupported = BlockPosStreamProvider.getForAccessor(areaMutator)
                .map(pos -> {
                    final BlockState state = context.getLevel().getBlockState(pos);
                    final BlockEntity blockEntity = context.getLevel().getBlockEntity(pos);
                    return new BlockInformation(state, IStateVariantManager.getInstance().getStateVariant(state, Optional.ofNullable(blockEntity)));
                })
          .allMatch(state -> IEligibilityManager.getInstance().canBeChiseled(state) || state.isAir());

        if (!isSupported)
        {
            return PlacementResult.failure(
                    IClientConfiguration::getNotFittingPatternPlacementColor,
                    LocalStrings.PatternPlacementNotASupportedBlock.getText());
        }

        final Map<BlockInformation, Integer> totalRemovedBits = source.stream()
          .filter(s -> !s.getBlockInformation().isAir())
          .filter(s -> {
              final Optional<IStateEntryInfo> o = areaMutator.getInAreaTarget(s.getStartPoint().add(areaMutator.getInWorldStartPoint()));

              return o
                .filter(os -> !os.getBlockInformation().isAir())
                .map(os -> !os.getBlockInformation().equals(s.getBlockInformation()))
                .orElse(false);
          })
          .map(s -> areaMutator.getInAreaTarget(s.getStartPoint().add(areaMutator.getInWorldStartPoint())))
          .filter(Optional::isPresent)
          .map(Optional::get)
          .collect(Collectors.toMap(
            IStateEntryInfo::getBlockInformation,
            s -> 1,
            Integer::sum
          ));

        final IBitInventory playerBitInventory = IBitInventoryManager.getInstance().create(context.getPlayer());
        final boolean hasRequiredSpace = context.getPlayer().isCreative() ||
                                           totalRemovedBits.entrySet().stream().allMatch(e -> playerBitInventory.canInsert(e.getKey(), e.getValue()));

        if (!hasRequiredSpace)
        {
            return PlacementResult.failure(
                    IClientConfiguration::getMissingBitsOrSpacePatternPlacementColor,
                    LocalStrings.PatternPlacementNoBitSpace.getText());
        }

        if (simulate)
        {
            return PlacementResult.success();
        }

        try (IBatchMutation ignored = areaMutator.batch(IChangeTrackerManager.getInstance().getChangeTracker(context.getPlayer())))
        {
            source.stream()
              .filter(s -> !s.getBlockInformation().isAir())
              .forEach(
                stateEntryInfo -> areaMutator.clearInAreaTarget(
                  stateEntryInfo.getStartPoint()
                )
              );
        }

        if (!context.getPlayer().isCreative())
        {
            totalRemovedBits.forEach(playerBitInventory::insertOrDiscard);
        }

        return PlacementResult.success();
    }

    @Override
    public Vec3 getTargetedPosition(final ItemStack heldStack, final Player playerEntity, final BlockHitResult blockRayTraceResult)
    {
        return Vec3.atLowerCornerOf(blockRayTraceResult.getBlockPos());
    }

    @Override
    public ResourceLocation getIcon()
    {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, "pattern_remove");
    }

    @Override
    public @NotNull Optional<IToolModeGroup> getGroup()
    {
        return Optional.empty();
    }

    @Override
    public Component getDisplayName()
    {
        return LocalStrings.PatternPlacementModeRemoval.getText();
    }
}

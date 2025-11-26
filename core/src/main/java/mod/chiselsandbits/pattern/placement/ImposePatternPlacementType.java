package mod.chiselsandbits.pattern.placement;

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
import mod.chiselsandbits.api.util.IBatchMutation;
import mod.chiselsandbits.api.multistate.mutator.world.IWorldAreaMutator;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshot;
import mod.chiselsandbits.api.pattern.placement.IPatternPlacementType;
import mod.chiselsandbits.api.placement.PlacementResult;
import mod.chiselsandbits.api.util.BlockPosStreamProvider;
import mod.chiselsandbits.api.util.LocalStrings;
import com.communi.suggestu.scena.core.registries.AbstractCustomRegistryEntry;
import mod.chiselsandbits.api.variant.state.IStateVariantManager;
import mod.chiselsandbits.registrars.ModPatternPlacementTypes;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static mod.chiselsandbits.api.util.constants.Constants.MOD_ID;

public class ImposePatternPlacementType extends AbstractCustomRegistryEntry implements IPatternPlacementType
{
    @Override
    public Identifier getIcon()
    {
        return Identifier.fromNamespaceAndPath(MOD_ID, "pattern_impose");
    }

    @Override
    public @NotNull Optional<IToolModeGroup> getGroup()
    {
        return Optional.empty();
    }

    @Override
    public VoxelShape buildVoxelShapeForWireframe(
            final IMultiStateSnapshot sourceSnapshot, final Player player, final Vec3 targetedPoint, final Direction hitFace)
    {
        return ModPatternPlacementTypes.IMPOSEMENT.get().buildVoxelShapeForWireframe(
                sourceSnapshot, player, targetedPoint, hitFace
        );
    }

    @Override
    public PlacementResult performPlacement(final IMultiStateSnapshot source, final BlockPlaceContext context, final boolean simulate)
    {
        final Vec3 targetedPosition = context.getPlayer().isShiftKeyDown() ?
                context.getClickLocation()
                : Vec3.atLowerCornerOf(context.getClickedPos().offset(context.getClickedFace().getOpposite().getUnitVec3i()));
        final IWorldAreaMutator areaMutator =
                IMutatorFactory.getInstance().covering(
                        context.getLevel(),
                        targetedPosition,
                        targetedPosition.add(0.9999, 0.9999, 0.9999)
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

        final Map<BlockInformation, Integer> extractedBitsCount = source.stream()
                .filter(s -> !s.getBlockInformation().isAir())
                .map(IStateEntryInfo::getStartPoint)
                .map(pos -> pos.add(areaMutator.getInWorldStartPoint()))
                .map(areaMutator::getInAreaTarget)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(s -> !s.getBlockInformation().isAir())
                .collect(
                        Collectors.toMap(
                                IStateEntryInfo::getBlockInformation,
                                s -> 1,
                                Integer::sum
                        )
                );

        final IBitInventory playerBitInventory = IBitInventoryManager.getInstance().create(context.getPlayer());
        final boolean hasRequiredSpace = context.getPlayer().isCreative() ||
                extractedBitsCount.entrySet().stream().allMatch(e -> playerBitInventory.canInsert(e.getKey(), e.getValue()));

        if (!hasRequiredSpace)
        {
            return PlacementResult.failure(
                    IClientConfiguration::getMissingBitsOrSpacePatternPlacementColor,
                    LocalStrings.PatternPlacementNoBitSpace.getText());
        }

        final boolean hasRequiredBits = context.getPlayer().isCreative() || source.getStatics().getStateCounts().entrySet().stream()
                .filter(e -> !e.getKey().isAir())
                .allMatch(e -> playerBitInventory.canExtract(e.getKey(), e.getValue()));

        if (!hasRequiredBits)
        {
            return PlacementResult.failure(
                    IClientConfiguration::getMissingBitsOrSpacePatternPlacementColor,
                    LocalStrings.PatternPlacementNotEnoughBits.getText());
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
                            stateEntryInfo ->
                            {
                                areaMutator.overrideInAreaTarget(
                                        stateEntryInfo.getBlockInformation(),
                                        stateEntryInfo.getStartPoint());
                            }
                    );
        }

        if (!context.getPlayer().isCreative())
        {
            extractedBitsCount.forEach(playerBitInventory::insertOrDiscard);
            source.getStatics().getStateCounts().entrySet().stream()
                    .filter(e -> !e.getKey().isAir())
                    .forEach(e -> playerBitInventory.extract(e.getKey(), e.getValue()));
        }

        return PlacementResult.success();
    }

    @Override
    public Vec3 getTargetedPosition(
            final ItemStack heldStack, final Player playerEntity, final BlockHitResult blockRayTraceResult)
    {
        if (playerEntity.isShiftKeyDown())
        {
            return blockRayTraceResult.getLocation();
        }

        return Vec3.atLowerCornerOf(blockRayTraceResult.getBlockPos());
    }

    @Override
    public Component getDisplayName()
    {
        return LocalStrings.PatternPlacementModeImposement.getText();
    }
}

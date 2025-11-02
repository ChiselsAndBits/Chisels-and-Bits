package mod.chiselsandbits.pattern.placement;

import com.communi.suggestu.scena.core.registries.AbstractCustomRegistryEntry;
import mod.chiselsandbits.api.axissize.CollisionType;
import mod.chiselsandbits.api.change.IChangeTrackerManager;
import mod.chiselsandbits.api.config.IClientConfiguration;
import mod.chiselsandbits.api.exceptions.SpaceOccupiedException;
import mod.chiselsandbits.api.inventory.bit.IBitInventory;
import mod.chiselsandbits.api.inventory.management.IBitInventoryManager;
import mod.chiselsandbits.api.item.withmode.group.IToolModeGroup;
import mod.chiselsandbits.api.multistate.mutator.IMutatorFactory;
import mod.chiselsandbits.api.multistate.mutator.world.IWorldAreaMutator;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshot;
import mod.chiselsandbits.api.pattern.placement.IPatternPlacementType;
import mod.chiselsandbits.api.placement.PlacementResult;
import mod.chiselsandbits.api.util.BlockPosStreamProvider;
import mod.chiselsandbits.api.util.IBatchMutation;
import mod.chiselsandbits.api.util.LocalStrings;
import mod.chiselsandbits.client.icon.IconManager;
import mod.chiselsandbits.voxelshape.VoxelShapeManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static mod.chiselsandbits.api.util.constants.Constants.MOD_ID;

public class PlacePatternPlacementType extends AbstractCustomRegistryEntry implements IPatternPlacementType
{

    @Override
    public VoxelShape buildVoxelShapeForWireframe(
        final IMultiStateSnapshot sourceSnapshot, final Player player, final Vec3 targetedPoint, final Direction hitFace)
    {
        return VoxelShapeManager.getInstance()
            .get(sourceSnapshot, CollisionType.NONE_AIR);
    }

    @Override
    public PlacementResult performPlacement(
        final IMultiStateSnapshot source, final BlockPlaceContext context, final boolean simulate)
    {
        final Vec3 targetedPosition = context.getPlayer() != null && context.getPlayer().isShiftKeyDown() ?
            context.getClickLocation()
            : Vec3.atLowerCornerOf(context.getClickedPos());
        final IWorldAreaMutator areaMutator =
            IMutatorFactory.getInstance().covering(
                context.getLevel(),
                targetedPosition,
                targetedPosition.add(0.9999, 0.9999, 0.9999)
            );

        final boolean isAir = BlockPosStreamProvider.getForAccessor(areaMutator)
            .map(context.getLevel()::getBlockState)
            .allMatch(BlockBehaviour.BlockStateBase::isAir);

        if (!isAir)
        {
            return PlacementResult.failure(
                IClientConfiguration::getNotFittingPatternPlacementColor,
                LocalStrings.PatternPlacementNotAnAirBlock.getText());
        }

        final IBitInventory playerBitInventory = IBitInventoryManager.getInstance().create(context.getPlayer());
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
            source.stream().sequential().forEach(
                stateEntryInfo -> {
                    try
                    {
                        areaMutator.setInAreaTarget(
                            stateEntryInfo.getBlockInformation(),
                            stateEntryInfo.getStartPoint());
                    }
                    catch (SpaceOccupiedException ignored1)
                    {
                    }
                }
            );
        }

        if (!context.getPlayer().isCreative())
        {
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

        return Vec3.atLowerCornerOf(blockRayTraceResult.getBlockPos().offset(blockRayTraceResult.getDirection().getUnitVec3i()));
    }

    @Override
    public boolean overridesOccupiedBits(ItemStack heldStack)
    {
        return false;
    }

    @Override
    public TextureAtlasSprite getIcon()
    {
        return IconManager.getInstance().getIcon(ResourceLocation.fromNamespaceAndPath(
            MOD_ID, "pattern_place"
        ));
    }

    @Override
    public @NotNull Optional<IToolModeGroup> getGroup()
    {
        return Optional.empty();
    }

    @Override
    public Component getDisplayName()
    {
        return LocalStrings.PatternPlacementModePlacement.getText();
    }
}

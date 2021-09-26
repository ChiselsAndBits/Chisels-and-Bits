package mod.chiselsandbits.item;

import mod.chiselsandbits.ChiselsAndBits;
import mod.chiselsandbits.api.block.entity.IMultiStateBlockEntity;
import mod.chiselsandbits.api.config.Configuration;
import mod.chiselsandbits.api.exceptions.SealingNotSupportedException;
import mod.chiselsandbits.api.exceptions.SpaceOccupiedException;
import mod.chiselsandbits.api.inventory.bit.IBitInventory;
import mod.chiselsandbits.api.inventory.management.IBitInventoryManager;
import mod.chiselsandbits.api.item.multistate.IMultiStateItemStack;
import mod.chiselsandbits.api.item.pattern.IMultiUsePatternItem;
import mod.chiselsandbits.api.item.pattern.IPatternItem;
import mod.chiselsandbits.api.multistate.accessor.IAreaAccessor;
import mod.chiselsandbits.api.multistate.mutator.IMutatorFactory;
import mod.chiselsandbits.api.multistate.mutator.batched.IBatchMutation;
import mod.chiselsandbits.api.multistate.mutator.world.IWorldAreaMutator;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshot;
import mod.chiselsandbits.api.util.LocalStrings;
import mod.chiselsandbits.item.multistate.SingleBlockMultiStateItemStack;
import mod.chiselsandbits.multistate.snapshot.EmptySnapshot;
import mod.chiselsandbits.network.packets.TileEntityUpdatedPacket;
import mod.chiselsandbits.registrars.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.Constants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SingleUsePatternItem extends Item implements IPatternItem
{

    public SingleUsePatternItem(final Properties builder)
    {
        super(builder);
    }

    /**
     * Creates an itemstack aware context wrapper that gives access to the multistate information contained within the given itemstack.
     *
     * @param stack The stack to get an {@link IMultiStateItemStack} for.
     * @return The {@link IMultiStateItemStack} that represents the data in the given itemstack.
     */
    @NotNull
    @Override
    public IMultiStateItemStack createItemStack(final ItemStack stack)
    {
        //Take care of an empty pattern.
        //Generally the case when this is a stack from the creative menu.
        if (stack.getOrCreateTag().isEmpty()) {
            return EmptySnapshot.Stack.INSTANCE;
        }

        return new SingleBlockMultiStateItemStack(stack);
    }

    @Override
    @NotNull
    public InteractionResult useOn(@NotNull UseOnContext context) {
        final IMultiStateItemStack contents = createItemStack(context.getItemInHand());
        if (contents.getStatistics().isEmpty()) {
            if (context.getPlayer() == null)
                return InteractionResult.FAIL;

            if (!context.getPlayer().isCreative())
                return InteractionResult.FAIL;

            if (!context.getPlayer().isCrouching())
                return InteractionResult.FAIL;

            final IWorldAreaMutator areaMutator = IMutatorFactory.getInstance().in(context.getLevel(), context.getClickedPos());
            final ItemStack snapshotPatternStack = areaMutator.createSnapshot().toItemStack().toPatternStack();
            context.getItemInHand().setTag(snapshotPatternStack.getOrCreateTag().copy());
            return InteractionResult.SUCCESS;
        }


        return this.tryPlace(new BlockPlaceContext(context));
    }

    @NotNull
    public InteractionResult tryPlace(@NotNull final BlockPlaceContext context)
    {
        if (context.getPlayer() == null)
            return InteractionResult.FAIL;

        final IAreaAccessor source = this.createItemStack(context.getItemInHand());
        final IMultiStateSnapshot sourceSnapshot = source.createSnapshot();
        final IWorldAreaMutator areaMutator = IMutatorFactory.getInstance().in(context.getLevel(), context.getClickedPos());
        final IMultiStateSnapshot attemptTarget = areaMutator.createSnapshot();

        final boolean noCollisions = source.stream().sequential()
                .allMatch(stateEntryInfo -> {
                    try
                    {
                        attemptTarget.setInAreaTarget(
                          stateEntryInfo.getState(),
                          stateEntryInfo.getStartPoint()
                        );

                        return true;
                    }
                    catch (SpaceOccupiedException exception)
                    {
                        return false;
                    }
                });

        final IBitInventory playerBitInventory = IBitInventoryManager.getInstance().create(context.getPlayer());
        final boolean hasRequiredBits = context.getPlayer().isCreative() || sourceSnapshot.getStatics().getStateCounts().entrySet().stream()
          .allMatch(e -> playerBitInventory.canExtract(e.getKey(), e.getValue()));

        if (noCollisions && hasRequiredBits) {
            try (IBatchMutation ignored = areaMutator.batch()) {
                source.stream().sequential().forEach(
                  stateEntryInfo -> {
                      try
                      {
                          areaMutator.setInAreaTarget(
                            stateEntryInfo.getState(),
                            stateEntryInfo.getStartPoint()
                          );
                      }
                      catch (SpaceOccupiedException ignored1)
                      {
                      }
                  }
                );
            }

            if (!context.getPlayer().isCreative()) {
                sourceSnapshot.getStatics().getStateCounts().forEach(playerBitInventory::extract);
            }

            final BlockEntity tileEntityCandidate = context.getLevel().getBlockEntity(context.getClickedPos());
            if (tileEntityCandidate instanceof IMultiStateBlockEntity multiStateBlockEntity) {
                final Direction placementDirection = context.getPlayer() == null ? Direction.NORTH : context.getPlayer().getDirection().getOpposite();
                final int horizontalIndex = placementDirection.get2DDataValue();

                int rotationCount = horizontalIndex - 4;
                if (rotationCount < 0) {
                    rotationCount += 4;
                }

                multiStateBlockEntity.rotate(Direction.Axis.Y, rotationCount);

                if (!context.getLevel().isClientSide()) {
                    context.getLevel().sendBlockUpdated(context.getClickedPos(), Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), Constants.BlockFlags.DEFAULT);

                    ChiselsAndBits.getInstance().getNetworkChannel().sendToTrackingChunk(
                      new TileEntityUpdatedPacket(tileEntityCandidate),
                      context.getLevel().getChunkAt(context.getClickedPos())
                    );
                }
            }

            return determineSuccessResult(context);
        }

        return InteractionResult.FAIL;
    }

    protected InteractionResult determineSuccessResult(final BlockPlaceContext context) {
        if (context.getPlayer() != null && context.getPlayer().isCreative())
        {
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public @NotNull ItemStack seal(@NotNull final ItemStack source) throws SealingNotSupportedException
    {
        if (source.getItem() == this)
        {
            if (!(source.getItem() instanceof IMultiUsePatternItem))
            {
                final ItemStack seal = new ItemStack(ModItems.MULTI_USE_PATTERN_ITEM.get());
                seal.setTag(source.getOrCreateTag().copy());
                return seal;
            }

            throw new SealingNotSupportedException();
        }

        return source;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(
      final @NotNull ItemStack stack, @Nullable final Level worldIn, final @NotNull List<Component> tooltip, final @NotNull TooltipFlag flagIn)
    {
        if ((Minecraft.getInstance().getWindow() != null && Screen.hasShiftDown())) {
            tooltip.add(new TextComponent("        "));
            tooltip.add(new TextComponent("        "));
        }

        Configuration.getInstance().getCommon().helpText(LocalStrings.HelpSimplePattern, tooltip);
    }
}
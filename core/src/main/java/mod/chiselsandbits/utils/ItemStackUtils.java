package mod.chiselsandbits.utils;

import com.communi.suggestu.scena.core.util.SingleBlockLevelReader;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.item.bit.IBitItem;
import mod.chiselsandbits.api.item.click.ILeftClickControllingItem;
import mod.chiselsandbits.api.item.click.IRightClickControllingItem;
import mod.chiselsandbits.api.item.multistate.IMultiStateItem;
import mod.chiselsandbits.api.item.pattern.IPatternItem;
import mod.chiselsandbits.api.item.withhighlight.IWithHighlightItem;
import mod.chiselsandbits.api.item.withmode.IWithModeItem;
import mod.chiselsandbits.api.variant.state.IStateVariantManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ItemStackUtils
{

    private ItemStackUtils()
    {
        throw new IllegalStateException("Can not instantiate an instance of: ItemStackUtils. This is a utility class");
    }

    /**
     * Mimics pick block.
     *
     * @param blockInformation the block and state we are creating an ItemStack for.
     * @return ItemStack fromt the BlockState.
     */
    public static ItemStack getItemStackFromBlockState(@NotNull final BlockInformation blockInformation)
    {
        final Optional<ItemStack> dynamicStack = IStateVariantManager.getInstance().getItemStack(blockInformation);
        if (dynamicStack.isPresent())
        {
            return dynamicStack.get();
        }

        if (blockInformation.blockState().getBlock() instanceof LiquidBlock liquidBlock)
        {
            return new ItemStack(liquidBlock.fluid.getBucket());
        }

        final Item item = getItem(blockInformation);
        if (item != Items.AIR && item != null)
        {
            return new ItemStack(item, 1);
        }

        return new ItemStack(blockInformation.blockState().getBlock(), 1);
    }

    public static Item getItem(@NotNull final BlockInformation blockInformation)
    {
        final Block block = blockInformation.blockState().getBlock();
        if (block.equals(Blocks.LAVA))
        {
            return Items.LAVA_BUCKET;
        }
        else if (block instanceof CropBlock)
        {
            final ItemStack stack = blockInformation.blockState().getCloneItemStack(
                new SingleBlockLevelReader.Builder()
                    .withBlockState(blockInformation.blockState())
                    .withBlockEntity(blockInformation::newBlockEntityAtZero)
                    .createSingleBlockLevelReader(),
                BlockPos.ZERO,
                true
            );
            if (!stack.isEmpty())
            {
                return stack.getItem();
            }

            return Items.WHEAT_SEEDS;
        }
        // oh no...
        else if (block instanceof FarmBlock || block instanceof DirtPathBlock)
        {
            return Blocks.DIRT.asItem();
        }
        else if (block instanceof FireBlock)
        {
            return Items.FLINT_AND_STEEL;
        }
        else if (block instanceof FlowerPotBlock)
        {
            return Items.FLOWER_POT;
        }
        else if (block == Blocks.BAMBOO_SAPLING)
        {
            return Items.BAMBOO;
        }
        else
        {
            return block.asItem();
        }
    }

    public static ItemStack getModeItemStackFromPlayer(@Nullable final Player playerEntity)
    {
        if (playerEntity == null)
        {
            return ItemStack.EMPTY;
        }

        if (playerEntity.getMainHandItem().getItem() instanceof IWithModeItem)
        {
            return playerEntity.getMainHandItem();
        }

        if (playerEntity.getOffhandItem().getItem() instanceof IWithModeItem)
        {
            return playerEntity.getOffhandItem();
        }

        return ItemStack.EMPTY;
    }

    public static ItemStack getHighlightItemStackFromPlayer(@Nullable final Player playerEntity)
    {
        if (playerEntity == null)
        {
            return ItemStack.EMPTY;
        }

        if (playerEntity.getMainHandItem().getItem() instanceof IWithHighlightItem)
        {
            return playerEntity.getMainHandItem();
        }

        if (playerEntity.getOffhandItem().getItem() instanceof IWithHighlightItem)
        {
            return playerEntity.getOffhandItem();
        }

        return ItemStack.EMPTY;
    }

    public static ItemStack getMultiStateItemStackFromPlayer(@Nullable final Player playerEntity)
    {
        if (playerEntity == null)
        {
            return ItemStack.EMPTY;
        }

        if (playerEntity.getMainHandItem().getItem() instanceof IMultiStateItem)
        {
            return playerEntity.getMainHandItem();
        }

        if (playerEntity.getOffhandItem().getItem() instanceof IMultiStateItem)
        {
            return playerEntity.getOffhandItem();
        }

        return ItemStack.EMPTY;
    }

    public static ItemStack getPatternItemStackFromPlayer(@Nullable final Player playerEntity)
    {
        if (playerEntity == null)
        {
            return ItemStack.EMPTY;
        }

        if (playerEntity.getMainHandItem().getItem() instanceof IPatternItem)
        {
            return playerEntity.getMainHandItem();
        }

        if (playerEntity.getOffhandItem().getItem() instanceof IPatternItem)
        {
            return playerEntity.getOffhandItem();
        }

        return ItemStack.EMPTY;
    }

    public static InteractionHand getPatternHandFromPlayer(@Nullable final Player playerEntity)
    {
        if (playerEntity == null)
        {
            return InteractionHand.MAIN_HAND;
        }

        if (playerEntity.getOffhandItem().getItem() instanceof IPatternItem)
        {
            return InteractionHand.OFF_HAND;
        }

        return InteractionHand.MAIN_HAND;
    }

    public static ItemStack getBitItemStackFromPlayer(@Nullable final Player playerEntity)
    {
        if (playerEntity == null)
        {
            return ItemStack.EMPTY;
        }

        if (playerEntity.getMainHandItem().getItem() instanceof IBitItem)
        {
            return playerEntity.getMainHandItem();
        }

        if (playerEntity.getOffhandItem().getItem() instanceof IBitItem)
        {
            return playerEntity.getOffhandItem();
        }

        return ItemStack.EMPTY;
    }

    public static ItemStack getLeftClickControllingItemStackFromPlayer(@Nullable final Player playerEntity)
    {
        if (playerEntity == null)
        {
            return ItemStack.EMPTY;
        }

        if (playerEntity.getMainHandItem().getItem() instanceof ILeftClickControllingItem)
        {
            return playerEntity.getMainHandItem();
        }

        if (playerEntity.getOffhandItem().getItem() instanceof ILeftClickControllingItem)
        {
            return playerEntity.getOffhandItem();
        }

        return ItemStack.EMPTY;
    }

    public static ItemStack getRightClickControllingItemStackFromPlayer(@Nullable final Player playerEntity)
    {
        if (playerEntity == null)
        {
            return ItemStack.EMPTY;
        }

        if (playerEntity.getMainHandItem().getItem() instanceof IRightClickControllingItem)
        {
            return playerEntity.getMainHandItem();
        }

        if (playerEntity.getOffhandItem().getItem() instanceof IRightClickControllingItem)
        {
            return playerEntity.getOffhandItem();
        }

        return ItemStack.EMPTY;
    }

    public static BlockInformation getHeldBitBlockInformationFromPlayer(@Nullable final Player playerEntity)
    {
        if (playerEntity == null)
        {
            return BlockInformation.AIR;
        }

        if (playerEntity.getMainHandItem().getItem() instanceof IBitItem)
        {
            return ((IBitItem) playerEntity.getMainHandItem().getItem()).getBlockInformation(playerEntity.getMainHandItem());
        }

        if (playerEntity.getOffhandItem().getItem() instanceof IBitItem)
        {
            return ((IBitItem) playerEntity.getOffhandItem().getItem()).getBlockInformation(playerEntity.getOffhandItem());
        }

        return BlockInformation.AIR;
    }

    public static BlockInformation getStateFromItem(
        final ItemStack is)
    {
        try
        {
            if (!is.isEmpty() && is.getItem() instanceof final BlockItem blockItem)
            {
                final BlockState blockState = blockItem.getBlock().defaultBlockState();
                return new BlockInformation(
                    blockState,
                    IStateVariantManager.getInstance().getStateVariant(blockState, is)
                );
            }
        }
        catch (final Throwable ignored)
        {
        }

        return BlockInformation.AIR;
    }
}

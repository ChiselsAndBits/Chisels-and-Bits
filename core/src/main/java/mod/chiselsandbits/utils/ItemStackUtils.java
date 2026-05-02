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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DirtPathBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.LiquidBlock;
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

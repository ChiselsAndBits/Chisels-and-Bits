package mod.chiselsandbits.item;

import com.communi.suggestu.scena.core.dist.Dist;
import com.communi.suggestu.scena.core.dist.DistExecutor;
import mod.chiselsandbits.ChiselsAndBits;
import mod.chiselsandbits.api.block.bitbag.IBitBagAcceptingBlock;
import mod.chiselsandbits.api.config.IClientConfiguration;
import mod.chiselsandbits.api.inventory.bit.IBitInventoryItem;
import mod.chiselsandbits.api.inventory.bit.IBitInventoryItemStack;
import mod.chiselsandbits.api.util.HelpTextUtils;
import mod.chiselsandbits.api.util.LocalStrings;
import mod.chiselsandbits.api.util.RayTracingUtils;
import mod.chiselsandbits.inventory.bit.SlottedBitInventoryItemStack;
import mod.chiselsandbits.network.packets.OpenBagGuiPacket;
import mod.chiselsandbits.registrars.ModItems;
import mod.chiselsandbits.utils.SimpleInstanceCache;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;

public class BitBagItem extends Item implements IBitInventoryItem
{

    private static final int BAG_STORAGE_SLOTS = 63;

    SimpleInstanceCache<ItemStack, IBitInventoryItemStack> tooltipCache = new SimpleInstanceCache<>();

    public BitBagItem(Properties properties)
    {
        super(properties.stacksTo(1));
    }

    @Override
    public @NotNull Component getName(final @NotNull ItemStack stack)
    {
        DyeColor color = getDyedColor(stack);
        final Component parent = super.getName(stack);
        if (parent instanceof MutableComponent && color != null)
        {
            return ((MutableComponent) parent).append(" - ").append(Component.translatable("chiselsandbits.color." + color.getName()));
        }
        else
        {
            return super.getName(stack);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull TooltipDisplay tooltipDisplay, @NotNull Consumer<Component> tooltipAdder, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, flag);
        HelpTextUtils.build(LocalStrings.HelpBitBag, tooltipAdder);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (!Minecraft.getInstance().hasShiftDown()) {
                tooltipAdder.accept(LocalStrings.ShiftDetails.getText());
                return;
            }

            final IBitInventoryItemStack inventoryItemStack;
            if (tooltipCache.needsUpdate(stack))
            {
                inventoryItemStack = create(stack);
                tooltipCache.updateCachedValue(inventoryItemStack);
            } else {
                inventoryItemStack = tooltipCache.getCached();
            }

            var contents = inventoryItemStack.listContents();
            contents.displayComponents().forEach(tooltipAdder);
        });
    }

    @Override
    public @NotNull InteractionResult use(
      final @NotNull Level worldIn,
      final @NotNull Player playerIn,
      final @NotNull InteractionHand hand)
    {
        final ItemStack itemStackIn = playerIn.getItemInHand(hand);

        final HitResult rayTraceResult = RayTracingUtils.rayTracePlayer(playerIn);
        if (rayTraceResult.getType() == HitResult.Type.BLOCK && rayTraceResult instanceof final BlockHitResult blockRayTraceResult) {
            final BlockState hitBlockState = worldIn.getBlockState(blockRayTraceResult.getBlockPos());
            if (hitBlockState.getBlock() instanceof IBitBagAcceptingBlock bitBagAcceptingBlock) {
                final var resultStack = bitBagAcceptingBlock.onBitBagInteraction(itemStackIn, playerIn, blockRayTraceResult);
                playerIn.setItemInHand(hand, resultStack);
                return InteractionResult.SUCCESS;
            }
        }

        if (worldIn.isClientSide())
        {
            ChiselsAndBits.getInstance().getNetworkChannel().sendToServer(new OpenBagGuiPacket());
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public IBitInventoryItemStack create(final ItemStack stack)
    {
        if (stack.getItem() != this)
            return new SlottedBitInventoryItemStack(ItemStack.EMPTY, 0);

        return new SlottedBitInventoryItemStack(
          stack,
          BAG_STORAGE_SLOTS
        );
    }

    @SuppressWarnings("unused")
    public boolean showDurabilityBar(
      final ItemStack stack)
    {
        if (!(stack.getItem() instanceof final IBitInventoryItem item))
            return false;

        final IBitInventoryItemStack inventoryItemStack = item.create(stack);

        return !inventoryItemStack. isEmpty();
    }

    @SuppressWarnings("unused")
    public double getDurabilityForDisplay(
      final ItemStack stack)
    {
        if (!(stack.getItem() instanceof final IBitInventoryItem item))
            return 0d;

        final IBitInventoryItemStack inventoryItemStack = item.create(stack);

        final double filledRatio = inventoryItemStack.getFilledRatio();
        return Math.min(1.0d, Math.max(0.0d, IClientConfiguration.getInstance().getInvertBitBagFullness().get() ? filledRatio : 1.0 - filledRatio));
    }

    public static ItemStack dyeBag(
      ItemStack bag,
      DyeColor color)
    {
        ItemStack copy = bag.copy();

        if (!copy.has(DataComponents.CUSTOM_DATA))
        {
            CustomData.set(DataComponents.CUSTOM_DATA, copy, new CompoundTag());
        }

        if (color == null && bag.getItem() == ModItems.ITEM_BIT_BAG_DYED.get())
        {
            final ItemStack unColoredStack = new ItemStack(ModItems.ITEM_BIT_BAG_DEFAULT.get());
            CustomData.set(DataComponents.CUSTOM_DATA, unColoredStack, copy.get(DataComponents.CUSTOM_DATA).copyTag());
            CustomData.update(DataComponents.CUSTOM_DATA, unColoredStack, compoundTag -> compoundTag.remove("color"));
            return unColoredStack;
        }
        else if (color != null)
        {
            ItemStack coloredStack = copy;
            if (coloredStack.getItem() == ModItems.ITEM_BIT_BAG_DEFAULT.get())
            {
                coloredStack = new ItemStack(ModItems.ITEM_BIT_BAG_DYED.get());
                CustomData.set(DataComponents.CUSTOM_DATA, coloredStack, copy.get(DataComponents.CUSTOM_DATA).copyTag());
            }

            CustomData.update(DataComponents.CUSTOM_DATA, coloredStack, compoundTag -> compoundTag.putString("color", color.getName()));
            return coloredStack;
        }

        return copy;
    }

    public static DyeColor getDyedColor(
      ItemStack stack)
    {
        if (stack.getItem() != ModItems.ITEM_BIT_BAG_DYED.get())
        {
            return null;
        }

        if (!stack.has(DataComponents.CUSTOM_DATA))
        {
            return null;
        }

        final CompoundTag tag = Objects.requireNonNull(stack.get(DataComponents.CUSTOM_DATA)).copyTag();

        if (tag.contains("color"))
        {
            String name = tag.getString("color").orElseThrow();
            for (DyeColor color : DyeColor.values())
            {
                if (name.equals(color.getSerializedName()))
                {
                    return color;
                }
            }
        }

        return null;
    }

    @SuppressWarnings("unused")
    public boolean shouldCauseReequipAnimation(final ItemStack oldStack, final ItemStack newStack, final boolean slotChanged)
    {
        return false;
    }
}

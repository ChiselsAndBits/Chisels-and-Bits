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
import mod.chiselsandbits.registrars.ModDataComponentTypes;
import mod.chiselsandbits.utils.SimpleInstanceCache;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.function.Consumer;

public class BitBagItem extends Item implements IBitInventoryItem
{

    private static final int BAG_STORAGE_SLOTS = 63;

    SimpleInstanceCache<ItemStack, IBitInventoryItemStack> tooltipCache = new SimpleInstanceCache<>();

    public BitBagItem(Properties properties)
    {
        super(properties.stacksTo(1));
    }

    public static ItemStack dyeBag(final ItemStack itemStack, final DyeColor color)
    {
        itemStack.set(DataComponents.DYED_COLOR, new DyedItemColor(color.getTextureDiffuseColor()));
        return itemStack;
    }

    @Override
    public @NotNull Component getName(final @NotNull ItemStack stack)
    {
        DyedItemColor color = getDyedColor(stack);
        final Component parent = super.getName(stack);
        if (parent instanceof MutableComponent mutableComponent && color != null)
        {
            for (final DyeColor value : DyeColor.values())
            {
                if (value.getTextureDiffuseColor() == color.rgb()) {
                    return mutableComponent.copy().append(" - ").append(Component.translatable("chiselsandbits.color." + value.getName()));
                }
            }

            return mutableComponent.copy().append(" - ").append(
                Component.translatable("item.color", String.format(Locale.ROOT, "#%06X", color.rgb())).withStyle(ChatFormatting.GRAY)
            );
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

        tooltipAdder.accept(
            LocalStrings.HelpBagPickupMode.getText(
                isPreferredPickupInventory(stack) ? LocalStrings.BagPicksUpFirst.getText() : LocalStrings.PlayerPicksUpFirst.getText(),
                isFilteredPickupInventory(stack) ? LocalStrings.Filtered.getText() : LocalStrings.NonFiltered.getText()
            )
        );

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

    @Override
    public boolean isPreferredPickupInventory(final ItemStack stack)
    {
        return stack.getOrDefault(ModDataComponentTypes.BAG_PREFERRED_PICK_UP.get(), false);
    }

    @Override
    public boolean isFilteredPickupInventory(final ItemStack stack)
    {
        return stack.getOrDefault(ModDataComponentTypes.BAG_FILTERED_PICK_UP.get(), false);
    }

    public void setPreferredPickupInventory(final ItemStack stack, final boolean preferred) {
        stack.set(ModDataComponentTypes.BAG_PREFERRED_PICK_UP.get(), preferred);
    }

    public void setFilteredPickupInventory(final ItemStack stack, final boolean filtered) {
        stack.set(ModDataComponentTypes.BAG_FILTERED_PICK_UP.get(), filtered);
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

    public static DyedItemColor getDyedColor(
      ItemStack stack)
    {
        return stack.get(DataComponents.DYED_COLOR);
    }

    @SuppressWarnings("unused")
    public boolean shouldCauseReequipAnimation(final ItemStack oldStack, final ItemStack newStack, final boolean slotChanged)
    {
        return false;
    }
}

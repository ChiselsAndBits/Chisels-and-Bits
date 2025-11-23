package mod.chiselsandbits.item;

import com.communi.suggestu.scena.core.dist.DistExecutor;
import com.communi.suggestu.scena.core.entity.IPlayerInventoryManager;
import mod.chiselsandbits.api.exceptions.SealingNotSupportedException;
import mod.chiselsandbits.api.item.tool.IUnsealItem;
import mod.chiselsandbits.api.sealing.ISupportsUnsealing;
import mod.chiselsandbits.api.util.HelpTextUtils;
import mod.chiselsandbits.api.util.LocalStrings;
import mod.chiselsandbits.api.util.VectorUtils;
import mod.chiselsandbits.components.data.InteractionData;
import mod.chiselsandbits.registrars.ModDataComponentTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class UnsealItem extends Item implements IUnsealItem {

    private final String CONST_INTERACTION = "Interaction";
    private final String CONST_SIMULATION = "Simulation";

    public UnsealItem(final Properties properties) {
        super(properties.enchantable(5));
    }

    @Override
    public boolean isInteracting(final ItemStack stack) {
        return stack.getItem() == this && stack.has(ModDataComponentTypes.INTERACTION_TARGET.get());
    }

    @Override
    public ItemStack getInteractionTarget(final ItemStack stack) {
        return stack.getOrDefault(ModDataComponentTypes.INTERACTION_TARGET.get(), InteractionData.EMPTY).stack();
    }

    @Override
    public boolean isRunningASimulatedInteraction(final ItemStack stack) {
        return isInteracting(stack) && stack.getOrDefault(ModDataComponentTypes.IS_SIMULATING.get(), false);
    }

    @Override
    public float getBobbingTickCount() {
        return 32;
    }

    @Override
    public int getUseDuration(ItemStack $$0, LivingEntity $$1) {
        return 64;
    }

    public static void spawnParticles(Vec3 location, ItemStack stack, Level world) {
        for (int i = 0; i < 20; i++) {
            Vec3 motion = VectorUtils.offsetRandomly(Vec3.ZERO, world.random, 1 / 8f);
            world.addParticle(new ItemParticleOption(ParticleTypes.ITEM, stack), location.x, location.y,
                    location.z, motion.x, motion.y, motion.z);
        }
    }

    @Override
    public boolean releaseUsing(@NotNull ItemStack stack, @NotNull Level worldIn, @NotNull LivingEntity entityLiving, int timeLeft) {
        if (!(entityLiving instanceof Player player))
            return false;

        if (isInteracting(stack)) {
            ItemStack interactionTarget = getInteractionTarget(stack);
            player.getInventory().placeItemBackInInventory(interactionTarget);
            stack.remove(ModDataComponentTypes.INTERACTION_TARGET.get());
            return true;
        }

        return false;
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level worldIn, @NotNull LivingEntity entityLiving) {
        if (!(entityLiving instanceof Player player))
            return stack;
        if (isInteracting(stack)) {
            ItemStack target = getInteractionTarget(stack);
            ItemStack pattern = createPattern(target);

            if (worldIn.isClientSide()) {
                spawnParticles(entityLiving.getEyePosition(1)
                                .add(entityLiving.getLookAngle()
                                        .scale(.5f)),
                        target, worldIn);
                return stack;
            }

            if (!pattern.isEmpty()) {
                IPlayerInventoryManager.getInstance().giveToPlayer(player, pattern);
            }
            stack.remove(ModDataComponentTypes.INTERACTION_TARGET.get());
            if (worldIn instanceof ServerLevel serverLevel && entityLiving instanceof ServerPlayer playerEntity) {
                stack.hurtAndBreak(1, serverLevel, playerEntity, item -> {
                    EquipmentSlot hand = EquipmentSlot.MAINHAND;
                    if (playerEntity.getOffhandItem() == stack)
                        hand = EquipmentSlot.OFFHAND;

                    player.onEquippedItemBroken(item, hand);
                });
            }
        }

        return stack;
    }

    @Override
    public @NotNull InteractionResult use(@NotNull Level worldIn, Player playerIn, @NotNull InteractionHand handIn) {
        ItemStack itemstack = playerIn.getItemInHand(handIn);

        if (isInteracting(itemstack)) {
            playerIn.startUsingItem(handIn);
            return InteractionResult.PASS;
        }

        InteractionHand otherHand = handIn == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack itemInOtherHand = playerIn.getItemInHand(otherHand);
        if (createPattern(itemInOtherHand).getItem() != itemInOtherHand.getItem() && itemInOtherHand.getItem() instanceof ISupportsUnsealing) {
            ItemStack item = itemInOtherHand.copy();
            ItemStack target = item.split(1);
            playerIn.startUsingItem(handIn);
            itemstack.set(ModDataComponentTypes.INTERACTION_TARGET.get(), new InteractionData(target));
            playerIn.setItemInHand(otherHand, item);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.SUCCESS;
    }

    private static ItemStack createPattern(final ItemStack targetStack) {
        if (targetStack.getItem() instanceof ISupportsUnsealing) {
            try {
                return ((ISupportsUnsealing) targetStack.getItem()).unseal(targetStack);
            } catch (SealingNotSupportedException e) {
                return targetStack;
            }
        }

        return targetStack;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void appendHoverText(
        final @NotNull ItemStack stack,
        final @NotNull TooltipContext context,
        final @NotNull TooltipDisplay tooltipDisplay,
        final @NotNull Consumer<Component> tooltipAdder,
        final @NotNull TooltipFlag flag)
    {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, flag);

        DistExecutor.unsafeExecuteForDist(
            () -> () -> HelpTextUtils.build(LocalStrings.HelpUnseal, tooltipAdder, Minecraft.getInstance().options.keyUse.getTranslatedKeyMessage()),
            () -> () -> HelpTextUtils.build(LocalStrings.HelpUnseal, tooltipAdder)
        );
    }
}

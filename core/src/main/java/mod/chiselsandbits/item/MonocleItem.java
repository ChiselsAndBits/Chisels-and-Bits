package mod.chiselsandbits.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public class MonocleItem extends Item
{
    public static final DispenseItemBehavior DISPENSE_ITEM_BEHAVIOR = new DefaultDispenseItemBehavior() {
        protected @NotNull ItemStack execute(@NotNull BlockSource source, @NotNull ItemStack stack) {
            return dispenseArmor(source, stack) ? stack : super.execute(source, stack);
        }
    };

    private static boolean dispenseArmor(BlockSource source, ItemStack stack) {
        BlockPos blockpos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
        List<LivingEntity> list = source.level().getEntitiesOfClass(LivingEntity.class, new AABB(blockpos), EntitySelector.NO_SPECTATORS.and(new MobCanWearArmorEntitySelector(stack)));
        if (list.isEmpty()) {
            return false;
        } else {
            LivingEntity targetEntity = list.get(0);
            EquipmentSlot slot = targetEntity.getEquipmentSlotForItem(stack);
            ItemStack stackToDispense = stack.split(1);
            targetEntity.setItemSlot(slot, stackToDispense);
            if (targetEntity instanceof Mob) {
                ((Mob)targetEntity).setDropChance(slot, 2.0F);
                ((Mob)targetEntity).setPersistenceRequired();
            }

            return true;
        }
    }

    public MonocleItem(final Properties itemProperties)
    {
        super(itemProperties.component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD).setEquipSound(ArmorMaterials.GOLD.equipSound()).build()));

        DispenserBlock.registerBehavior(this, DISPENSE_ITEM_BEHAVIOR);
    }

    public static class MobCanWearArmorEntitySelector implements Predicate<Entity>
    {
        private final ItemStack itemStack;

        public MobCanWearArmorEntitySelector(ItemStack itemstack) {
            this.itemStack = itemstack;
        }

        public boolean test(@Nullable Entity entity) {
            if (!(entity instanceof LivingEntity livingEntity))
                return false;

            if (!itemStack.has(DataComponents.EQUIPPABLE))
                return false;

            Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);
            assert equippable != null;
            if (!equippable.canBeEquippedBy(entity.getType())) {
                return false;
            }

            if (livingEntity.isEquippableInSlot(itemStack, equippable.slot()) && !livingEntity.hasItemInSlot(equippable.slot()) && entity.isAlive()) {
                return true;
            } else {
                return false;
            }
        }
    }
}

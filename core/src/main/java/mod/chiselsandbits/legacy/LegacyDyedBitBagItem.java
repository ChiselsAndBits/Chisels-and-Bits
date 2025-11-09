package mod.chiselsandbits.legacy;

import mod.chiselsandbits.utils.UpgradeUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Deprecated
public class LegacyDyedBitBagItem extends Item
{
    public LegacyDyedBitBagItem(final Properties properties)
    {
        super(properties);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, final @NotNull ServerLevel level, final @NotNull Entity entity, @Nullable final EquipmentSlot slot)
    {
        if (!(entity instanceof Player player))
            return;

        player.getInventory().removeItem(stack);
        stack = UpgradeUtils.consolidateBitBag(stack);
        player.getInventory().add(stack);
    }
}

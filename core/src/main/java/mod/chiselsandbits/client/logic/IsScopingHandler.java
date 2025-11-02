package mod.chiselsandbits.client.logic;

import com.communi.suggestu.scena.core.dist.DistExecutor;
import mod.chiselsandbits.item.MonocleItem;
import mod.chiselsandbits.keys.KeyBindingManager;
import mod.chiselsandbits.utils.ItemStackUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;

public class IsScopingHandler
{

    private IsScopingHandler()
    {
        throw new IllegalStateException("Can not instantiate an instance of: IsScopingHandler. This is a utility class");
    }

    public static boolean isScoping()
    {
        return DistExecutor.unsafeRunForDist(
          () -> () -> !ItemStackUtils.getHighlightItemStackFromPlayer(Minecraft.getInstance().player).isEmpty() &&
                        Minecraft.getInstance().player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof MonocleItem &&
                        KeyBindingManager.getInstance().isScopingKeyPressed(),
          () -> () -> false
        );
    }
}

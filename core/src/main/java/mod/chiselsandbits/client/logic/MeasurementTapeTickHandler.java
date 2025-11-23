package mod.chiselsandbits.client.logic;

import com.communi.suggestu.scena.core.dist.Dist;
import com.communi.suggestu.scena.core.dist.DistExecutor;
import mod.chiselsandbits.ChiselsAndBits;
import mod.chiselsandbits.item.MeasuringTapeItem;
import mod.chiselsandbits.keys.KeyBindingManager;
import mod.chiselsandbits.network.packets.MeasurementsResetPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MeasurementTapeTickHandler
{

    public static void tick() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (KeyBindingManager.getInstance().isResetMeasuringTapeKeyPressed()) {
                ItemStack stack = getItemStack();

                if (!stack.isEmpty() && stack.getItem() instanceof MeasuringTapeItem measuringTapeItem) {
                    measuringTapeItem.clear(stack);
                    ChiselsAndBits.getInstance().getNetworkChannel().sendToServer(new MeasurementsResetPacket());
                }
            } else if (Minecraft.getInstance().level != null) {
                ItemStack stack = getItemStack();

                if (!stack.isEmpty() && stack.getItem() instanceof MeasuringTapeItem measuringTapeItem) {
                    measuringTapeItem.clientTick(
                        stack,
                        Minecraft.getInstance().level,
                        Minecraft.getInstance().player
                    );
                }
            }
        });
    }

    private static @NotNull ItemStack getItemStack()
    {
        ItemStack stack = ItemStack.EMPTY;
        if (Minecraft.getInstance().player.getMainHandItem().getItem() instanceof MeasuringTapeItem) {
            stack = Minecraft.getInstance().player.getMainHandItem();
        }
        else if (Minecraft.getInstance().player.getOffhandItem().getItem() instanceof MeasuringTapeItem) {
            stack = Minecraft.getInstance().player.getOffhandItem();
        }
        return stack;
    }
}
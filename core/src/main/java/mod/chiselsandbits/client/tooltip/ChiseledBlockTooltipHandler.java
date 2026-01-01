package mod.chiselsandbits.client.tooltip;

import com.communi.suggestu.scena.core.client.event.IClientEvents;
import com.communi.suggestu.scena.core.client.rendering.IRenderingManager;
import com.communi.suggestu.scena.core.client.tooltip.IClientTooltipComponentConverter;
import mod.chiselsandbits.api.item.multistate.IMultiStateItem;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshot;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ChiseledBlockTooltipHandler
{

    public static void configure()
    {
        IRenderingManager.getInstance().registerClientTooltipComponentConverter(
            registrar -> registrar.registerConvert(Payload.class, new Converter())
        );

        IClientEvents.getInstance().getGatherTooltipComponentsEvent().register((itemStack, screenWidth, screenHeight, maxWidth, tooltipElements) -> {
            if (itemStack.getItem() instanceof IMultiStateItem multiStateItem)
            {
                tooltipElements.add(new Payload(multiStateItem.createItemStack(itemStack).createSnapshot()));
            }

            return true;
        });
    }

    public record Payload(IMultiStateSnapshot snapshot) implements TooltipComponent {}

    public record Client(ItemStack component) implements ClientTooltipComponent
    {
        @Override
        public int getHeight(final Font font)
        {
            return 20;
        }

        @Override
        public int getWidth(@NotNull Font font)
        {
            return 20;
        }

        @Override
        public void renderImage(final Font font, final int x, final int y, final int width, final int height, final GuiGraphics guiGraphics)
        {
            guiGraphics.renderItem(
                component,
                x + 2, y + 2
            );
        }
    }

    public record Converter() implements IClientTooltipComponentConverter
    {

        @Override
        public ClientTooltipComponent convert(TooltipComponent component) {
            if (!(component instanceof Payload payload)) {
                return null;
            }

            return new Client(payload.snapshot().toItemStack().toBlockStack());
        }
    }
}

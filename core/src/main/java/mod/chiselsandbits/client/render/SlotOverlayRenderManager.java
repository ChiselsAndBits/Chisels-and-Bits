package mod.chiselsandbits.client.render;

import mod.chiselsandbits.client.tool.mode.icon.SelectedToolModeRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

public class SlotOverlayRenderManager {
    private static final SlotOverlayRenderManager INSTANCE = new SlotOverlayRenderManager();

    public static SlotOverlayRenderManager getInstance() {
        return INSTANCE;
    }

    private SlotOverlayRenderManager() {
    }

    public void renderSlot(final int xOffset, final int yOffSet, final GuiGraphicsExtractor graphics, final ItemStack stack) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(xOffset, yOffSet);
        graphics.pose().pushMatrix();

        if (!Minecraft.getInstance().options.hideGui)
            SelectedToolModeRendererRegistry.getInstance().getCurrent()
                    .extractGraphics(graphics, stack);

        graphics.pose().popMatrix();
        graphics.pose().popMatrix();
    }
}

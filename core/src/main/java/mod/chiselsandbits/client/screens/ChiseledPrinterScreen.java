package mod.chiselsandbits.client.screens;

import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.container.ChiseledPrinterContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class ChiseledPrinterScreen extends AbstractContainerScreen<ChiseledPrinterContainer>
{

    private static final Identifier GUI_TEXTURES = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/container/chisel_printer.png");

    public ChiseledPrinterScreen(final ChiseledPrinterContainer screenContainer, final Inventory inv, final Component titleIn)
    {
        super(screenContainer, inv, titleIn);
    }

    @Override
    protected void init()
    {
        this.imageWidth = 176;
        this.imageHeight = 166;

        super.init();

        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected void renderBg(@NotNull final GuiGraphics guiGraphics, final float partialTicks, final int x, final int y)
    {
        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED,
            GUI_TEXTURES,
            this.leftPos,
            this.topPos,
            0.0F,
            0.0F,
            this.imageWidth,
            this.imageHeight,
            256,
            256
        );

        if (this.menu.getToolStack().isEmpty())
            return;

        guiGraphics.renderItem(this.menu.getToolStack(), this.leftPos + 81, this.topPos + 47);

        int scaledProgress = this.menu.getChiselProgressionScaled();

        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED,
            GUI_TEXTURES,
            this.leftPos + 83 + scaledProgress,
            this.topPos + 48,
            this.imageWidth + scaledProgress,
            0.0F,
            16 - scaledProgress,
            16,
            256,
            256
        );
    }
}
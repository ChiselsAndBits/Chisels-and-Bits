package mod.chiselsandbits.api.client.screen.widget;

import com.communi.suggestu.scena.core.client.rendering.IExtendedGuiGraphics;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * All chisels and bits widgets inherit from this class.
 * Most notably provides init support, invoked when the window itself has its init method called.
 */
public abstract class AbstractChiselsAndBitsWidget extends AbstractWidget implements IChiselsAndBitsWidget
{
    /**
     * Creates a new widget.
     *
     * @param x The x position.
     * @param y The y position.
     * @param width The width.
     * @param height The height.
     * @param narration The narration text when selected.
     */
    public AbstractChiselsAndBitsWidget(final int x, final int y, final int width, final int height, final Component narration)
    {
        super(x, y, width, height, narration);
    }

    /**
     * Gives access to the current instance of minecraft.
     *
     * @return The current instance of minecraft.
     */
    @NotNull
    public Minecraft getMinecraft() {
        return Minecraft.getInstance();
    }

    /**
     * The font used in this widget.
     *
     * @return The font used to render text in this widget.
     */
    @NotNull
    public Font getFont() {
        return Minecraft.getInstance().font;
    }

    @Override
    public void init() {
    }

    @Override
    public void removed() {
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }

    protected static IExtendedGuiGraphics extendGraphics(@NotNull GuiGraphics graphics) {
        return ((IExtendedGuiGraphics) graphics);
    }
}

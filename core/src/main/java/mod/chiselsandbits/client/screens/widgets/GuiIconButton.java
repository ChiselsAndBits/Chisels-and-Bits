package mod.chiselsandbits.client.screens.widgets;

import mod.chiselsandbits.api.client.screen.widget.AbstractChiselsAndBitsButton;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public class GuiIconButton extends AbstractChiselsAndBitsButton
{
    private static final WidgetSprites SPRITES = new WidgetSprites(
            Identifier.withDefaultNamespace("widget/button"), Identifier.withDefaultNamespace("widget/button_disabled"), Identifier.withDefaultNamespace("widget/button_highlighted")
    );
    public static final int SIZE = 20;
    private final TextureAtlasSprite defaultIcon;

	public GuiIconButton(
			final int x,
			final int y,
			final TextureAtlasSprite defaultIcon,
            Button.OnPress pressedAction,
            Tooltip tooltip)
	{
		super( x, y, SIZE, SIZE, Component.empty(), pressedAction, Button.DEFAULT_NARRATION);
		this.defaultIcon = defaultIcon;
        this.setTooltip(tooltip);
	}

    public GuiIconButton(
      final int x, final int y,
      final Component narration,
      final TextureAtlasSprite defaultIcon,
      final OnPress pressable)
    {
        super(x, y, SIZE, SIZE, narration, pressable, Button.DEFAULT_NARRATION);
        this.defaultIcon = defaultIcon;
    }

    public GuiIconButton(
      final int x, final int y,
      final Component narration,
      final TextureAtlasSprite defaultIcon,
      final OnPress pressable,
      final Tooltip tooltip)
    {
        super(x, y, SIZE, SIZE, narration, pressable, Button.DEFAULT_NARRATION);
        this.defaultIcon = defaultIcon;
        this.setTooltip(tooltip);
    }

    protected TextureAtlasSprite getDefaultIcon()
    {
        return defaultIcon;
    }

    @Override
    protected void extractContents(final @NonNull GuiGraphicsExtractor guiGraphics, final int mouseX, final int mouseY, final float a)
    {
        guiGraphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            SPRITES.get(this.active, this.isHoveredOrFocused()),
            this.getX(),
            this.getY(),
            this.getWidth(),
            this.getHeight()
        );

        guiGraphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            getDefaultIcon(),
            getX() + 2,
            getY() + 2,
            16,
            16,
            -1
        );
    }
}

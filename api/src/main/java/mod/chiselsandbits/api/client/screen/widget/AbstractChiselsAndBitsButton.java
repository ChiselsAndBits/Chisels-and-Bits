package mod.chiselsandbits.api.client.screen.widget;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * An abstract chisels and bits button.
 * Is used to be able to retroactively initialize a button when it's containing screen is initialized.
 */
public abstract class AbstractChiselsAndBitsButton extends Button implements IChiselsAndBitsWidget
{
    /**
     * Creates a new button with a tooltip.
     *
     * @param x The x position.
     * @param y The y position.
     * @param width The width.
     * @param height The height.
     * @param narration The narration message.
     * @param pressable The press callback.
     * @param tooltip The tooltip handling logic.
     */
    public AbstractChiselsAndBitsButton(
      final int x,
      final int y,
      final int width,
      final int height,
      final Component narration,
      final OnPress pressable,
      final CreateNarration tooltip)
    {
        super(x, y, width, height, narration, pressable, tooltip);
    }

    @Override
    public void init()
    {
    }

    @Override
    public void removed()
    {
    }
}

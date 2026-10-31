package mod.chiselsandbits.api.client.screen.widget;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;

/**
 * Contract for widgets integrated with Chisels &amp; Bits screens.
 */

public interface IChiselsAndBitsWidget extends Renderable, GuiEventListener {
	/**
	 * Invoked by the screen, when said screen is initialized.
	 */
	void init();

	/**
	 * Invoked by the screen, when it is removed from the display.
	 */
	void removed();
}

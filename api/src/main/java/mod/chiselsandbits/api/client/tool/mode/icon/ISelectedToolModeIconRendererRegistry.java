package mod.chiselsandbits.api.client.tool.mode.icon;

import mod.chiselsandbits.api.IChiselsAndBitsAPI;

/**
 * Registry for registering possible selected tool mode icon renderers,
 * and for getting the current renderer back out.
 */
public interface ISelectedToolModeIconRendererRegistry
{
	static ISelectedToolModeIconRendererRegistry getInstance() {
		return IChiselsAndBitsAPI.getInstance().getSelectedToolModeIconRenderer();
	}

	/**
     * The currently configured renderer as per configuration of the player.
     * @return The renderer.
     */
	ISelectedToolModeIconRenderer getCurrent();

	/**
     * Adds the passed in renderer instances as possible candidates.
     *
     * @param renderers The candidates.
     * @return The registry.
     */
	ISelectedToolModeIconRendererRegistry register(final ISelectedToolModeIconRenderer... renderers);
}

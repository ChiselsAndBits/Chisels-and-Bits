package mod.chiselsandbits.api.client.render.preview.chiseling;

import mod.chiselsandbits.api.IChiselsAndBitsAPI;

/**
 * Registry for registering possible chiseling context preview renderers,
 * and for getting the current renderer back out.
 */
public interface IChiselContextPreviewRendererRegistry
{
	static IChiselContextPreviewRendererRegistry getInstance() {
		return IChiselsAndBitsAPI.getInstance().getChiselContextPreviewRendererRegistry();
	}

	/**
     * The currently configured renderer as per configuration of the player.
     * @return The renderer.
     */
	IChiselContextPreviewRenderer getCurrent();

	/**
     * Adds the passed in renderer instances as possible candidates.
     *
     * @param renderers The candidates.
     * @return The registry.
     */
	IChiselContextPreviewRendererRegistry register(final IChiselContextPreviewRenderer... renderers);
}

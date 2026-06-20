package mod.chiselsandbits.api.client.render.preview.placement;

/**
 * Determines the way chiseled block and pattern placement previews will render.
 * This value can be independently set for how successful and failed placement renders.
 */
public enum PlacementPreviewRenderMode
{
	/**
	 * Causes the preview to render as a ghost of the model of the block to be placed
	 */
	GHOST_BLOCK_MODEL,

	/**
	 * Causes the preview to render the model of the block, such that the block's textures are
	 * ignored, and each quad is rendered with the RGBA value specified by the result of placement
	 */
	GHOST_BLOCK_MODEL_SOLID_COLOR,

	/**
	 * Causes the preview to render a wireframe comprised of the edges of the block's model
	 * with the RGB value specified by the result of placement
	 */
	WIREFRAME;

	public boolean isGhost() {
		return this == GHOST_BLOCK_MODEL;
	}

	public boolean isColoredGhost() {
		return this == GHOST_BLOCK_MODEL_SOLID_COLOR;
	}

	public boolean isWireframe() {
		return this == WIREFRAME;
	}
}

package mod.chiselsandbits.api.client.icon;

import mod.chiselsandbits.api.IChiselsAndBitsAPI;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;

/**
 * Provides atlas sprites used by Chisels &amp; Bits UI and interaction overlays.
 */
public interface IIconManager {
	/**
	 * Returns the icon manager exposed by the active API instance.
	 *
	 * @return the active icon manager
	 */
	static IIconManager getInstance() {
		return IChiselsAndBitsAPI.getInstance().getIconManager();
	}

	/**
	 * Looks up a sprite by its resource identifier.
	 *
	 * @param name sprite resource identifier
	 * @return the matching atlas sprite
	 */
	TextureAtlasSprite getIcon(Identifier name);

	/**
	 * Returns the icon used for swapping or exchanging items.
	 *
	 * @return the icon used for swapping or exchanging items
	 */
	TextureAtlasSprite getSwapIcon();

	/**
	 * Returns the icon used for placement actions.
	 *
	 * @return the icon used for placement actions
	 */
	TextureAtlasSprite getPlaceIcon();

	/**
	 * Returns the icon used for undo actions.
	 *
	 * @return the icon used for undo actions
	 */
	TextureAtlasSprite getUndoIcon();

	/**
	 * Returns the icon used for redo actions.
	 *
	 * @return the icon used for redo actions
	 */
	TextureAtlasSprite getRedoIcon();

	/**
	 * Returns the icon used for discarding or removing items.
	 *
	 * @return the icon used for discarding or removing items
	 */
	TextureAtlasSprite getTrashIcon();

	/**
	 * Returns the icon used for sorting inventory contents.
	 *
	 * @return the icon used for sorting inventory contents
	 */
	TextureAtlasSprite getSortIcon();

	/**
	 * Returns the icon used to roll or rotate content around the x axis.
	 *
	 * @return the icon used to roll or rotate content around the x axis
	 */
	TextureAtlasSprite getRollXIcon();

	/**
	 * Returns the icon used to roll or rotate content around the z axis.
	 *
	 * @return the icon used to roll or rotate content around the z axis
	 */
	TextureAtlasSprite getRollZIcon();

	/**
	 * Returns the solid white sprite used as a tintable UI texture.
	 *
	 * @return the solid white sprite used as a tintable UI texture
	 */
	TextureAtlasSprite getWhiteIcon();

	/**
	 * Returns the icon indicating filtered bit-bag pickup.
	 *
	 * @return the icon indicating filtered bit-bag pickup
	 */
	TextureAtlasSprite getFilterBagModeIcon();

	/**
	 * Returns the icon indicating normal bit-bag pickup.
	 *
	 * @return the icon indicating normal bit-bag pickup
	 */
	TextureAtlasSprite getNormalBagModeIcon();

	/**
	 * Returns the icon for the pickup mode that gives the bag first priority.
	 *
	 * @return the icon for the pickup mode that gives the bag first priority
	 */
	TextureAtlasSprite getBagPicksUpFirstIcon();

	/**
	 * Returns the icon for the pickup mode that gives the player first priority.
	 *
	 * @return the icon for the pickup mode that gives the player first priority
	 */
	TextureAtlasSprite getBagPicksUpSecondIcon();
}

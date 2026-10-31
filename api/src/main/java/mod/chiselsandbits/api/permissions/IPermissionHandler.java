package mod.chiselsandbits.api.permissions;

import mod.chiselsandbits.api.IChiselsAndBitsAPI;
import mod.chiselsandbits.api.multistate.accessor.world.IWorldAreaAccessor;

import net.minecraft.world.entity.player.Player;

/**
 * Permission handler for checking if a particular user can manipulate a particular area.
 */
public interface IPermissionHandler {
	/**
	 * Gives access to the current permission handler.
	 *
	 * @return The current permission handler.
	 */
	static IPermissionHandler getInstance() {
		return IChiselsAndBitsAPI.getInstance().getPermissionHandler();
	}

	/**
	 * Registers a new {@link IPermissionChecker} with the permission handler.
	 *
	 * @param checker The new permission checker
	 * @return The registration token which can be used to unregister the checker.
	 */
	ICheckerRegistration registerChecker(IPermissionChecker checker);

	/**
	 * Allows for external systems to check if a particular player
	 * can edit a particular area.
	 *
	 * @param player The player which wants to edit the area, possibly null if the player is not known.
	 * @param worldAreaAccessor The area which is being edited.
	 * @return {@code true} if the area is marked as editable, {@code false} otherwise.
	 */
	boolean canManipulate(Player player, IWorldAreaAccessor worldAreaAccessor);
}

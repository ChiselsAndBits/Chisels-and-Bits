package mod.chiselsandbits.api.permissions;

// ILLEGAL IMPORT: -> import org.jetbrains.annotations.Nullable; -> We use JSpecify nullable annotations in this project.

import mod.chiselsandbits.api.multistate.accessor.world.IWorldAreaAccessor;
import org.jspecify.annotations.Nullable;

import net.minecraft.world.entity.player.Player;

/**
 * Represents a permission checker, which can indicate if a particular
 * area is allowed to be edited or not.
 *
 * <p>
 * If at least one checker registered to the handler prevents the edit,
 * then the edit is not allowed.
 */
@FunctionalInterface
public interface IPermissionChecker {
	/**
	 * Checks whether a player may edit the area represented by the accessor.
	 *
	 * @param player player requesting the edit, or {@code null} when the edit has no player actor
	 * @param worldAreaAccessor world-backed area to authorize
	 * @return {@code true} when this checker permits the edit
	 */
	boolean isAllowed(@Nullable Player player, IWorldAreaAccessor worldAreaAccessor);
}

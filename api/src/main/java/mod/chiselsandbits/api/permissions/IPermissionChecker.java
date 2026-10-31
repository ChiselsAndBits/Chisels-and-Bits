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
	 *
	 * @param player
	 * @param worldAreaAccessor
	 * @return
	 */
	boolean isAllowed(@Nullable Player player, IWorldAreaAccessor worldAreaAccessor);
}

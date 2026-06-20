package mod.chiselsandbits.api.permissions;

import mod.chiselsandbits.api.multistate.accessor.world.IWorldAreaAccessor;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a permission checker, which can indicate if a particular
 * area is allowed to be edited or not.
 *
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
	boolean isAllowed(@Nullable final Player player, final IWorldAreaAccessor worldAreaAccessor);
}

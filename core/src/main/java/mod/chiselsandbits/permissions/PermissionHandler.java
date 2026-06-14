package mod.chiselsandbits.permissions;

import java.util.Map;

import com.google.common.collect.Maps;
import mod.chiselsandbits.api.multistate.accessor.world.IWorldAreaAccessor;
import mod.chiselsandbits.api.permissions.ICheckerRegistration;
import mod.chiselsandbits.api.permissions.IPermissionChecker;
import mod.chiselsandbits.api.permissions.IPermissionHandler;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.entity.player.Player;

public final class PermissionHandler implements IPermissionHandler
{
	private static final PermissionHandler INSTANCE = new PermissionHandler();

	public static PermissionHandler getInstance()
	{
		return INSTANCE;
	}

	private final Map<ICheckerRegistration, IPermissionChecker> checkers = Maps.newConcurrentMap();

	private PermissionHandler()
	{
	}

	@Override
	public ICheckerRegistration registerChecker(final IPermissionChecker checker)
	{
		final ICheckerRegistration registration = new ICheckerRegistration() {
			@Override
			public void close()
			{
				checkers.remove(this);
			}
		};

		checkers.put(registration, checker);

		return registration;
	}

	@Override
	public boolean canManipulate(
	  final @NotNull Player player, final IWorldAreaAccessor worldAreaAccessor)
	{
		if (checkers.size() == 0)
			return true;

		return checkers.values().stream().allMatch(c -> c.isAllowed(player, worldAreaAccessor));
	}
}

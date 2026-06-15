package mod.chiselsandbits.keys.contexts;

import com.communi.suggestu.scena.core.client.key.IKeyConflictContext;
import mod.chiselsandbits.api.item.withmode.IWithModeItem;

import net.minecraft.client.Minecraft;

public final class HoldsWithToolItemInHandKeyConflictContext implements IKeyConflictContext
{
	private static final HoldsWithToolItemInHandKeyConflictContext INSTANCE = new HoldsWithToolItemInHandKeyConflictContext();

	public static HoldsWithToolItemInHandKeyConflictContext getInstance()

	{
		return INSTANCE;
	}

	private HoldsWithToolItemInHandKeyConflictContext()

	{
	}

	/**
     * @return true if conditions are met to activate keybindings with this context
     */
	@Override
	public boolean isActive()

	{
		return Minecraft.getInstance().player != null &&
				 (Minecraft.getInstance().player.getMainHandItem().getItem() instanceof IWithModeItem ||
					Minecraft.getInstance().player.getOffhandItem().getItem() instanceof IWithModeItem);
	}

	/**
     * @param other The other context.
     * @return true if the other context can have keybindings conflicts with this one. This will be called on both contexts to check for conflicts.
     */
	@Override
	public boolean conflicts(final IKeyConflictContext other)

	{
		return other == this;
	}
}

package mod.chiselsandbits.keys.contexts;

import java.util.function.Predicate;

import com.communi.suggestu.scena.core.client.key.IKeyConflictContext;
import mod.chiselsandbits.api.item.change.IChangeTrackingItem;
import mod.chiselsandbits.registrars.ModItems;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;

public enum HoldsSpecificItemInHandKeyConflictContext implements IKeyConflictContext
{
	MEASURING_TAPE(item -> ModItems.MEASURING_TAPE.get() == item),
	CHANGE_TRACKING_ITEM(IChangeTrackingItem.class::isInstance);
	private final Predicate<Item> item;

	HoldsSpecificItemInHandKeyConflictContext(final Predicate<Item> item) {this.item = item; }

	@Override
	public boolean isActive() {
		return Minecraft.getInstance().player != null && (item.test(Minecraft.getInstance().player.getMainHandItem().getItem())
				|| item.test(Minecraft.getInstance().player.getOffhandItem().getItem())) && !IKeyConflictContext.getGui().isActive();
	}

	@Override
	public boolean conflicts(final IKeyConflictContext other) {
		return this == other;
	}
}

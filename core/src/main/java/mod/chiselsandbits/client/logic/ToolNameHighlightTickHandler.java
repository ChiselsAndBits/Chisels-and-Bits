package mod.chiselsandbits.client.logic;

import mod.chiselsandbits.api.item.named.IDynamicallyHighlightedNameItem;
import mod.chiselsandbits.api.item.named.IPermanentlyHighlightedNameItem;
import mod.chiselsandbits.registrars.ModDataComponentTypes;
import mod.chiselsandbits.utils.ItemStackUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public class ToolNameHighlightTickHandler {
	public static void handleClientTickForMagnifyingGlass() {
		if (Minecraft.getInstance().player != null && Minecraft.getInstance().level != null) {
			if (Minecraft.getInstance().player.getMainHandItem().getItem() instanceof IPermanentlyHighlightedNameItem
					|| Minecraft.getInstance().player.getOffhandItem().getItem() instanceof IPermanentlyHighlightedNameItem) {
				Minecraft.getInstance().gui.toolHighlightTimer = 40;
			}

			if (!ItemStackUtils.getPatternItemStackFromPlayer(Minecraft.getInstance().player).isEmpty()) {
				if (Minecraft.getInstance().gui.toolHighlightTimer == 0) {
					final ItemStack stack = ItemStackUtils.getPatternItemStackFromPlayer(Minecraft.getInstance().player);

					if (stack.has(ModDataComponentTypes.HIGHLIGHT_START_TIME.get())) {
						final long startTime = stack.getOrDefault(ModDataComponentTypes.HIGHLIGHT_START_TIME.get(), -50L);

						if (Minecraft.getInstance().level.getGameTime() + 2 < startTime || startTime + 40 < Minecraft.getInstance().level.getGameTime()) {
							stack.remove(ModDataComponentTypes.HIGHLIGHT_START_TIME.get());
						}
					}
				}
			}

			if (!Minecraft.getInstance().gui.lastToolHighlight.isEmpty() && Minecraft.getInstance().gui.lastToolHighlight.getItem() instanceof IDynamicallyHighlightedNameItem dynamicallyHighlightedNameItem) {
				Minecraft.getInstance().gui.lastToolHighlight = dynamicallyHighlightedNameItem.adaptItemStack(Minecraft.getInstance().gui.lastToolHighlight);
			}
		}
	}
}

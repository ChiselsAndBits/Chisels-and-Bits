package mod.chiselsandbits.client.logic;

import java.util.List;

import com.google.common.collect.Lists;
import mod.chiselsandbits.ChiselsAndBits;
import mod.chiselsandbits.api.config.IClientConfiguration;
import mod.chiselsandbits.api.item.withmode.IWithModeItem;
import mod.chiselsandbits.network.packets.HeldToolModeChangedPacket;
import mod.chiselsandbits.utils.ItemStackUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public class ScrollBasedModeChangeHandler {
	public static boolean onScroll(final double scrollDelta) {
		if (Minecraft.getInstance().player == null || !Minecraft.getInstance().player.isShiftKeyDown()) {
			return false;
		}

		final ItemStack stack = ItemStackUtils.getModeItemStackFromPlayer(Minecraft.getInstance().player);

		if (stack.isEmpty()) {
			return false;
		}

		if (!(stack.getItem() instanceof final IWithModeItem<?> toolModeItem)) {
			return false;
		}

		if (!IClientConfiguration.getInstance().getShouldScrollInteractionsChangeMode().get()) {
			return false;
		}

		final List<?> modes = Lists.newArrayList(toolModeItem.getPossibleModes());
		int workingIndex = modes.indexOf(toolModeItem.getMode(stack));

		if (scrollDelta < 0) {
			workingIndex++;
		} else {
			workingIndex--;
		}

		if (workingIndex < 0) {
			workingIndex = modes.size() + workingIndex;
		} else if (workingIndex >= modes.size()) {
			workingIndex -= modes.size();
		}

		ChiselsAndBits.getInstance().getNetworkChannel().sendToServer(new HeldToolModeChangedPacket(workingIndex));
		return true;
	}
}

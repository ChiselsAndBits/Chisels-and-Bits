package mod.chiselsandbits.logic;

import mod.chiselsandbits.chiseling.ChiselingManager;

import net.minecraft.world.entity.player.Player;

public class ChiselingManagerCountDownResetHandler {
	public static void doResetFor(Player player)

	{
		ChiselingManager.getInstance().resetLastChiselCountdown(player);
	}
}

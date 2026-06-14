package mod.chiselsandbits.client.time;

import net.minecraft.client.Minecraft;

public class TickHandler
{
	private static long clientTicks = 0;

	private static long nonePausedTicks = 0;

	public static void onClientTick()
	{
		clientTicks++;
		if (!Minecraft.getInstance().isPaused()) {
			nonePausedTicks++;
		}
	}

	public static long getClientTicks()
	{
		return clientTicks;
	}

	public static long getNonePausedTicks()
	{
		return nonePausedTicks;
	}
}

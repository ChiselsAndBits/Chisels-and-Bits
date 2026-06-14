package mod.chiselsandbits.logic;

import mod.chiselsandbits.chiseling.ChiselingManager;

public class ServerStartHandler
{
	public static void onServerStart() {
		ChiselingManager.getInstance().onServerStarting();
	}
}

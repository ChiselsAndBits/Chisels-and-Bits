package mod.chiselsandbits.logic;

import mod.chiselsandbits.change.ChangeTrackerSyncManager;

public class ServerTickHandler {
	public static void onPostServerTick() {
		ChangeTrackerSyncManager.getInstance().sync();
	}
}

package mod.chiselsandbits.change;

import java.util.ArrayList;
import java.util.List;

import mod.chiselsandbits.ChiselsAndBits;
import mod.chiselsandbits.network.packets.ChangeTrackerUpdatedPacket;

import net.minecraft.server.level.ServerPlayer;

public final class ChangeTrackerSyncManager {
	private static final ChangeTrackerSyncManager INSTANCE = new ChangeTrackerSyncManager();

	public static ChangeTrackerSyncManager getInstance() {
		return INSTANCE;
	}

	private final List<Entry> entries = new ArrayList<>();

	private ChangeTrackerSyncManager() {
	}

	private record Entry(ChangeTracker tracker, ServerPlayer serverPlayer) {
	}

	public void add(final ChangeTracker tracker, final ServerPlayer serverPlayer) {
		entries.add(new Entry(tracker, serverPlayer));
	}

	public void sync() {
		entries.forEach(e -> {
				ChiselsAndBits.getInstance().getNetworkChannel().sendToPlayer(
					new ChangeTrackerUpdatedPacket(e.tracker().getChanges()),
						e.serverPlayer()
				);
		}

		);
		entries.clear();
	}
}

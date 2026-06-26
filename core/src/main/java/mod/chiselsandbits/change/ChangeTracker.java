package mod.chiselsandbits.change;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import mod.chiselsandbits.api.IChiselsAndBitsAPI;
import mod.chiselsandbits.api.change.IChangeTracker;
import mod.chiselsandbits.api.change.changes.IChange;
import mod.chiselsandbits.api.change.changes.IllegalChangeAttempt;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshot;
import mod.chiselsandbits.change.changes.BitChange;
import mod.chiselsandbits.change.changes.CombinedChange;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class ChangeTracker implements IChangeTracker {
	protected final Player player;
	protected final LinkedList<IChange> changes = new LinkedList<>();
	protected int currentIndex = 0;

	public ChangeTracker() {
		this.player = null;
	}

	public ChangeTracker(final Player player) {this.player = player; }

	public void reset() {
		changes.clear();
		sendUpdate();
	}

	@Override
	public void onBlocksUpdated(
			final Map<BlockPos, IMultiStateSnapshot> beforeStates, final Map<BlockPos, IMultiStateSnapshot> afterState) {
		if (!beforeStates.keySet().containsAll(afterState.keySet()) || !afterState.keySet().containsAll(beforeStates.keySet())) {
			throw new IllegalArgumentException("Initial States and Target States reference difference block positions");
		}

		changes.addFirst(
			new CombinedChange(
					beforeStates.entrySet().stream()
					.map(e -> new BitChange(
							e.getKey(),
							e.getValue(),
							afterState.get(e.getKey())
					))
					.collect(Collectors.toList())
			)
		);

		currentIndex = 0;

		final int maxSize = IChiselsAndBitsAPI.getInstance().getConfiguration().getServer().getChangeTrackerSize().get();

		if (changes.size() > maxSize) {
			while (changes.size() > maxSize) {
				changes.removeLast();
			}
		}

		sendUpdate();
	}

	@Override
	public Deque<IChange> getChanges() {
		return new LinkedList<>(changes);
	}

	@Override
	public void setChanges(Deque<IChange> changes) {
		this.changes.clear();
		this.changes.addAll(changes);
		sendUpdate();
	}

	@Override
	public void clear() {
		changes.clear();
		sendUpdate();
	}

	public Optional<IChange> getCurrentUndo() {
		if (getChanges().size() <= currentIndex || currentIndex < 0) {
			return Optional.empty();
		}

		return Optional.of(changes.get(currentIndex));
	}

	public Optional<IChange> getCurrentRedo() {
		if (getChanges().size() < currentIndex || currentIndex < 1) {
			return Optional.empty();
		}

		return Optional.of(changes.get(currentIndex - 1));
	}

	@Override
	public boolean canUndo(final Player player) {
		return getCurrentUndo().map(c -> c.canUndo(player)).orElse(false);
	}

	@Override
	public boolean canRedo(final Player player) {
		return getCurrentRedo().map(c -> c.canRedo(player)).orElse(false);
	}

	@Override
	public void undo(final Player player) throws IllegalChangeAttempt {
		if (!canUndo(player)) {
			throw new IllegalChangeAttempt();
		}

		if (getCurrentUndo().isPresent()) {
			final IChange change = getCurrentUndo().get();
			change.undo(player);
			currentIndex = Math.min(changes.size(), currentIndex + 1);
			sendUpdate();
		}
	}

	@Override
	public void redo(final Player player) throws IllegalChangeAttempt {
		if (!canRedo(player)) {
			throw new IllegalChangeAttempt();
		}

		if (getCurrentRedo().isPresent()) {
			final IChange change = getCurrentRedo().get();
			change.redo(player);
			currentIndex = Math.max(0, currentIndex - 1);
			sendUpdate();
		}
	}

	private void sendUpdate() {
		if (player != null && player instanceof ServerPlayer serverPlayer && !serverPlayer.level().isClientSide()) {
			ChangeTrackerSyncManager.getInstance().add(this, serverPlayer);
		}
	}
}

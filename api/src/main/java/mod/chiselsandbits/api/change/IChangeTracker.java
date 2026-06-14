package mod.chiselsandbits.api.change;

import com.google.common.collect.ImmutableMap;
import mod.chiselsandbits.api.change.changes.IChange;
import mod.chiselsandbits.api.change.changes.IChangeHandler;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshot;
import net.minecraft.core.BlockPos;

import java.util.Deque;
import java.util.Map;

/**
 * The change tracker for tracking changes to bit blocks.
 * Currently still work in progress.
 */
public interface IChangeTracker extends IChangeHandler
{
	/**
     * Invoked when a chiseled block is updated from one state to the next.
     *
     * @param blockPos The position of the block updated.
     * @param before The before state.
     * @param after The after state.
     */
	default void onBlockUpdated(final BlockPos blockPos, final IMultiStateSnapshot before, final IMultiStateSnapshot after) {
		this.onBlocksUpdated(
		  ImmutableMap.of(blockPos, before),
		  ImmutableMap.of(blockPos, after)
		);
	}

	/**
     * Invoked when several chiseled blocks are updated from one state to the next.
     *
     * @param beforeStates The states before the update.
     * @param afterState The states after the update.
     */
	void onBlocksUpdated(Map<BlockPos, IMultiStateSnapshot> beforeStates, final Map<BlockPos, IMultiStateSnapshot> afterState);

	/**
     * Gets a readonly-copy of the changes in the queue.
     * @return The changes last performed and recorded by this tracker.
     */
	Deque<IChange> getChanges();

	/**
     * Sets the changes in the tracker.
     * @param changes The changes to set.
     */
	void setChanges(Deque<IChange> changes);

	/**
     * Clears the tracker from all currently stored changes.
     */
	void clear();
}

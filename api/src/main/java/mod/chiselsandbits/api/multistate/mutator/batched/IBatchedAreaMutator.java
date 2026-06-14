package mod.chiselsandbits.api.multistate.mutator.batched;

import mod.chiselsandbits.api.change.IChangeTracker;
import mod.chiselsandbits.api.multistate.mutator.IAreaMutator;
import mod.chiselsandbits.api.util.IBatchMutation;
import mod.chiselsandbits.api.util.IWithBatchableMutationSupport;

/**
 * A mutator which supports making mutations in batches.
 */
public interface IBatchedAreaMutator extends IAreaMutator, IWithBatchableMutationSupport {
	/**
     * Triggers a batch mutation start for block placement.
     * Enables tracking of the changes.
     *
     * @param changeTracker The change tracker to apply the changes to.
     * @return The batch mutation, which will record the changes automatically.
     */
	IBatchMutation batch(final IChangeTracker changeTracker);
}

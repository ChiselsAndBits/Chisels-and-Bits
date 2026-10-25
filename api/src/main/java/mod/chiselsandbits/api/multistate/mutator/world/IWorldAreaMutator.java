package mod.chiselsandbits.api.multistate.mutator.world;

import java.util.stream.Stream;

import mod.chiselsandbits.api.multistate.accessor.world.IWorldAreaAccessor;
import mod.chiselsandbits.api.multistate.mutator.IAreaMutator;
import mod.chiselsandbits.api.multistate.mutator.batched.IBatchedAreaMutator;

/**
 * Allows for a given area in the world to be mutated.
 */
public interface IWorldAreaMutator extends IWorldAreaAccessor, IAreaMutator, IBatchedAreaMutator {
	/**
	 * Returns all entries in the current area in a mutable fashion.
	 * Includes all empty areas as areas containing an air state.
	 *
	 * @return A stream with a mutable state entry info for each mutable section in the area.
	 */
	Stream<IInWorldMutableStateEntryInfo> inWorldMutableStream();
}

package mod.chiselsandbits.api.multistate.snapshot;

import mod.chiselsandbits.api.IChiselsAndBitsAPI;
import mod.chiselsandbits.api.blockinformation.BlockInformation;

/**
 * A factory to create simple snapshots.
 */
public interface ISnapshotFactory {
	/**
	 * Returns the shared snapshot factory instance.
	 *
	 * @return the factory provided by the active Chisels &amp; Bits API instance
	 */
	static ISnapshotFactory getInstance() {
		return IChiselsAndBitsAPI.getInstance().getSnapshotFactory();
	}

	/**
	 * Creates a new simple single block snapshot.
	 *
	 * @return The new snapshot.
	 */
	IMultiStateSnapshot singleBlock();

	/**
	 * Creates a new simple single block snapshot.
	 *
	 * @param blockInformation The block information which will fill up the entire snapshot once returned.
	 * @return The new snapshot.
	 */
	IMultiStateSnapshot singleBlock(BlockInformation blockInformation);
}

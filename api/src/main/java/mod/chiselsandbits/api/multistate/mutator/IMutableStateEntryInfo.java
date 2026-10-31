package mod.chiselsandbits.api.multistate.mutator;

import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.exceptions.SpaceOccupiedException;
import mod.chiselsandbits.api.multistate.accessor.IStateEntryInfo;

/**
 * Mutable view of one state entry within a multistate area.
 */

public interface IMutableStateEntryInfo extends IStateEntryInfo {
	/**
	 * Sets the current entries block information.
	 *
	 * @param blockInformation The new block information of the entry.
	 * @throws SpaceOccupiedException When the space is not clear and as such the bit can not be set.
	 */
	void setBlockInformation(BlockInformation blockInformation) throws SpaceOccupiedException;

	/**
	 * Clears the current state entries blockstate.
	 * Effectively setting the current blockstate to air.
	 */
	void clear();

	/**
	 * Overrides the current entries block information.
	 *
	 * @param blockInformation The new block information of the entry.
	 */
	default void overrideState(final BlockInformation blockInformation) {
		try {
			clear();
			setBlockInformation(blockInformation);
		} catch (SpaceOccupiedException ignored) { //Should never be thrown, due to the clear call;
		}
	}
}

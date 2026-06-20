package mod.chiselsandbits.api.multistate.mutator;

import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.exceptions.SpaceOccupiedException;
import mod.chiselsandbits.api.multistate.accessor.IAreaAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.stream.Stream;

/**
 * A mutator for a given area.
 */
public interface IAreaMutator extends IAreaAccessor {
	/**
	 * Returns all entries in the current area in a mutable fashion.
	 * Includes all empty areas as areas containing an air state.
	 *
	 * @return A stream with a mutable state entry info for each mutable section in the area.
	 */
	Stream<IMutableStateEntryInfo> mutableStream();

	/**
	 * Sets the target block information in the current area, using the offset from the area as well as the in area target offset.
	 *
	 * @param blockInformation The block information.
	 * @param inAreaTarget The in area offset.
	 * @throws SpaceOccupiedException When the space is not clear and as such the bit can not be set.
	 */
	void setInAreaTarget(
			BlockInformation blockInformation,
			Vec3 inAreaTarget
			) throws SpaceOccupiedException;

	/**
	 * Sets the target block information in the current area, using the in area block position offset
	 * as well as the in block target offset to calculate the in area offset for setting.
	 *
	 * @param blockInformation The block information.
	 * @param inAreaBlockPosOffset The offset of blocks in the current area.
	 * @param inBlockTarget The offset in the targeted block.
	 * @throws SpaceOccupiedException When the space is not clear and as such the bit can not be set.
	 */
	void setInBlockTarget(
			BlockInformation blockInformation,
			BlockPos inAreaBlockPosOffset,
			Vec3 inBlockTarget
			) throws SpaceOccupiedException;

	/**
	 * Clears the current area, using the offset from the area as well as the in area target offset.
	 *
	 * @param inAreaTarget The in area offset.
	 */
	void clearInAreaTarget(
			Vec3 inAreaTarget
	);

	/**
	 * Clears the current area, using the in area block position offset as well as the in block target offset to calculate the in area offset for setting.
	 *
	 * @param inAreaBlockPosOffset The offset of blocks in the current area.
	 * @param inBlockTarget The offset in the targeted block.
	 */
	void clearInBlockTarget(
			BlockPos inAreaBlockPosOffset,
			Vec3 inBlockTarget
	);

	/**
	 * Overrides the target block information in the current area, using the offset from the
	 * area as well as the in area target offset.
	 *
	 * @param blockInformation The block information.
	 * @param inAreaTarget The in area offset.
	 */
	default void overrideInAreaTarget(
			BlockInformation blockInformation,
			Vec3 inAreaTarget
			) {
		try {
			clearInAreaTarget(inAreaTarget);
			setInAreaTarget(blockInformation, inAreaTarget);
		} catch (SpaceOccupiedException ignored) {
		}
	}

	/**
	 * Overrides the target block information in the current area, using the in area block position offset as well
	 * as the in block target offset to calculate the in area offset for setting.
	 *
	 * @param blockInformation The block information.
	 * @param inAreaBlockPosOffset The offset of blocks in the current area.
	 * @param inBlockTarget The offset in the targeted block.
	 */
	default void overrideInAreaTarget(
			BlockInformation blockInformation,
			BlockPos inAreaBlockPosOffset,
			Vec3 inBlockTarget
			) {
		try {
			clearInBlockTarget(inAreaBlockPosOffset, inBlockTarget);
			setInBlockTarget(blockInformation, inAreaBlockPosOffset, inBlockTarget);
		} catch (SpaceOccupiedException ignored) {
		}
	}
}

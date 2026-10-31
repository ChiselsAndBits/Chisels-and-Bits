package mod.chiselsandbits.api.multistate.mutator;

import net.minecraft.core.Direction;

/**
 * Contract for multistate storage that supports mirroring and rotation.
 */

public interface IMirrorAndRotateble {
	/**
	 * Rotates the current multistate block 90 degrees around the given axis with the given rotation count.
	 *
	 * @param axis The axis to rotate around.
	 * @param rotationCount The amount of times to rotate the
	 */
	void rotate(Direction.Axis axis, int rotationCount);

	/**
	 * Rotates the current multistate block exactly once 90 degrees around the given axis.
	 *
	 * @param axis The axis to rotate around.
	 */
	default void rotate(final Direction.Axis axis) {
		this.rotate(axis, 1);
	}

	/**
	 * Mirrors the current multistate block around the given axis.
	 *
	 * @param axis The axis to mirror over.
	 */
	void mirror(Direction.Axis axis);
}

package mod.chiselsandbits.api.measuring;

import mod.chiselsandbits.api.item.withmode.group.IToolModeGroup;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Defines how a measuring mode snaps clicked positions and measurement corners to a grid.
 */
public interface IMeasuringType extends IToolModeGroup {
	/**
	 * Adapts the hit location used as the initial measurement point.
	 *
	 * @param blockHitResult hit result for the clicked block
	 * @return the adapted measurement position
	 */
	Vec3 adaptClickedPosition(BlockHitResult blockHitResult);

	/**
	 * Adapts the first corner of a measurement to this mode's grid.
	 *
	 * @param startPosition clicked start position
	 * @param endPosition other measurement endpoint
	 * @param hitFace face used to resolve alignment
	 * @return adapted start corner
	 */
	Vec3 adaptStartCorner(Vec3 startPosition, Vec3 endPosition, Direction hitFace);

	/**
	 * Adapts the second corner of a measurement to this mode's grid.
	 *
	 * @param startPosition other measurement endpoint
	 * @param endPosition clicked end position
	 * @param hitFace face used to resolve alignment
	 * @return adapted end corner
	 */
	Vec3 adaptEndCorner(Vec3 startPosition, Vec3 endPosition, Direction hitFace);

	/**
	 * Returns the coordinate spacing used by this measuring mode.
	 *
	 * @return the requested value.
	 */
	Vec3 getResolution();

	/**
	 * Returns whether adapted measurement positions should be normalized.
	 *
	 * @return whether the requested condition holds.
	 */
	boolean isNeedsNormalization();

	/**
	 * Adapts a pair of measurement endpoints using the clicked face.
	 */
	@FunctionalInterface
	interface IPositionAdapter {
		/**
		 * Adapts the two endpoints to the measuring mode's coordinate rules.
		 *
		 * @param startPosition first endpoint
		 * @param endPosition second endpoint
		 * @param hitFace face used to resolve alignment
		 * @return adapted position
		 */
		Vec3 adapt(Vec3 startPosition, Vec3 endPosition, Direction hitFace);
	}

	/**
	 * Adapts the location reported by a block hit before it becomes a measurement endpoint.
	 */
	@FunctionalInterface
	interface IClickedPositionAdapter {
		/**
		 * Creates an adapter that returns the hit result's location unchanged.
		 *
		 * @return an adapter that uses the hit location without modification
		 */
		static IClickedPositionAdapter identity() {
			return HitResult::getLocation;
		}

		/**
		 * Adapts the location from a block hit.
		 *
		 * @param startPosition hit result to adapt
		 * @return adapted hit location
		 */
		Vec3 adapt(BlockHitResult startPosition);
	}
}

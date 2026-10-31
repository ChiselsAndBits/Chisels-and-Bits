package mod.chiselsandbits.api.util;

import java.util.function.Function;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Computes placement geometry for a {@link net.minecraft.world.phys.BlockHitResult}.
 */
public class BlockHitResultUtils {
	private BlockHitResultUtils() {
		throw new IllegalStateException("Can not instantiate an instance of: BlockHitResultUtils.This is a utility class");
	}

	/**
	 * Computes the center point of an object being placed at a block hit result, offset by the given object size.
	 *
	 * @param blockHitResult the ray-hit result indicating the placement location and facing
	 * @param objectSize the full extent (size) of the target object along each axis
	 * @return the center coordinate of the placed object
	 */
	public static Vec3 getCenterOfHitObject(final BlockHitResult blockHitResult, final Vec3 objectSize) {
		return getCenterOfHitObject(
				blockHitResult,
				objectSize,
				face -> Vec3.atLowerCornerOf(face.getOpposite().getUnitVec3i())
		);
	}

	private static Vec3 getCenterOfHitObject(
			final BlockHitResult blockHitResult,
			final Vec3 objectSize,
			final Function<Direction, Vec3> placementFacingAdapter
	) {
		final Vec3 halfSize = objectSize.multiply(0.5, 0.5, 0.5);
		final Vec3 scaleSize = VectorUtils.divide(VectorUtils.ONE, objectSize);

		final Vec3 hitCenterVector = blockHitResult.getLocation().add(
				placementFacingAdapter.apply(blockHitResult.getDirection())
				.multiply(halfSize)
		);

		final Vec3 scaledHitCenterVector = hitCenterVector.multiply(scaleSize);
		final Vec3 scaledHitCenterVectorFloor = Vec3.atLowerCornerOf(VectorUtils.toBlockPos(scaledHitCenterVector));

		final Vec3 lowerLeftCornerOfTargetedObject = scaledHitCenterVectorFloor.multiply(objectSize);
		return lowerLeftCornerOfTargetedObject.add(halfSize);
	}
}

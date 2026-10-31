package mod.chiselsandbits.api.util;

import org.joml.Vector4f;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Utility methods for converting, normalizing, and transforming Minecraft vectors.
 */
public class VectorUtils {
	/**
	 * Factor for converting degrees to radians.
	 */
	public static final double DEG_TO_RAD_FACTOR = (2 * Math.PI) / 360d;
	/**
	 * Unit vector with all three coordinates equal to one.
	 */
	public static final Vec3 ONE = new Vec3(1, 1, 1);

	private VectorUtils() {
		throw new IllegalStateException("Can not instantiate an instance of: VectorUtils.This is a utility class");
	}

	/**
	 * Rotates a vector around an axis by a multiple of 90 degrees.
	 *
	 * @param vector3d vector to rotate
	 * @param axis rotation axis
	 * @param times number of quarter-turns
	 * @return the rotated vector
	 */
	public static Vec3 rotateMultipleTimes90Degrees(final Vec3 vector3d, final Direction.Axis axis, final int times) {
		return rotateDegrees(vector3d, axis, (times * 90) % 360);
	}

	/**
	 * Rotates a vector 90 degrees around the given axis.
	 *
	 * @param vector3d vector to rotate
	 * @param axis axis about which to rotate
	 * @return the vector after a positive quarter-turn around {@code axis}
	 */
	public static Vec3 rotate90Degrees(final Vec3 vector3d, final Direction.Axis axis) {
		return rotateDegrees(vector3d, axis, 90);
	}

	/**
	 * Rotates a vector by half a radian around the given axis.
	 *
	 * @param vector3d vector to rotate
	 * @param axis axis about which to rotate
	 * @return the vector after a half-radian rotation around {@code axis}
	 */
	public static Vec3 rotateHalfRadian(final Vec3 vector3d, final Direction.Axis axis) {
		return rotate(vector3d, axis, 0.5 * Math.PI);
	}

	/**
	 * Rotates a vector by an angle in degrees around the given axis.
	 *
	 * @param vector3d vector to rotate
	 * @param axis axis about which to rotate
	 * @param angleInDegrees rotation angle in degrees
	 * @return the rotated vector
	 */
	public static Vec3 rotateDegrees(final Vec3 vector3d, final Direction.Axis axis, final double angleInDegrees) {
		return rotate(vector3d, axis, angleInDegrees * DEG_TO_RAD_FACTOR);
	}

	/**
	 * Rotates a vector by an angle in radians around the given axis.
	 *
	 * @param vector3d vector to rotate
	 * @param axis axis about which to rotate
	 * @param angleInRadian rotation angle in radians
	 * @return the rotated vector
	 */
	public static Vec3 rotate(final Vec3 vector3d, final Direction.Axis axis, final double angleInRadian) {
		switch (axis) {
		case X:
			return new Vec3(
					vector3d.x(),
					vector3d.y() * Math.cos((float) angleInRadian) - vector3d.z() * Math.sin((float) angleInRadian),
					vector3d.y() * Math.sin((float) angleInRadian) + vector3d.z() * Math.cos((float) angleInRadian)
			);
		case Y:
			return new Vec3(
					vector3d.x() * Math.cos((float) angleInRadian) + vector3d.z() * Math.sin((float) angleInRadian),
					vector3d.y(),
					-vector3d.x() * Math.sin((float) angleInRadian) + vector3d.z() * Math.cos((float) angleInRadian)
			);
		case Z:
			return new Vec3(
					vector3d.x() * Math.cos((float) angleInRadian) - vector3d.y() * Math.sin((float) angleInRadian),
					vector3d.x() * Math.sin((float) angleInRadian) + vector3d.y() * Math.cos((float) angleInRadian),
					vector3d.z()
			);
		default:
			throw new IllegalArgumentException(String.format("Unknown axis : %s", axis));
		}
	}

	/**
	 * Scales a vector so that its largest absolute component is one.
	 *
	 * @param v vector to scale
	 * @return a vector whose largest absolute component is one, or {@link Vec3#ZERO} for a zero vector
	 */
	public static Vec3 scaleToOne(final Vec3 v) {
		final double maxSize = Math.abs(getMaximalComponent(v));

		if (MathUtil.almostEqual(maxSize, 0)) {
			return Vec3.ZERO;
		}

		final double scale = 1 / maxSize;

		return v.scale(scale);
	}

	/**
	 * Returns the signed component with the greatest absolute magnitude.
	 *
	 * @param v vector whose components are compared
	 * @return the component with the greatest absolute magnitude, retaining its sign
	 */
	public static double getMaximalComponent(final Vec3 v) {
		final double x = Math.abs(v.x());
		final double y = Math.abs(v.y());
		final double z = Math.abs(v.z());

		if (x >= y && x >= z) {
			return v.x();
		}

		if (y >= x && y >= z) {
			return v.y();
		}

		if (z >= x && z >= y) {
			return v.z();
		}

		return 0;
	}

	/**
	 * Negates all components of the supplied vector.
	 *
	 * @param v vector to negate
	 * @return a new vector with each component negated
	 */
	public static Vec3 invert(final Vec3 v) {
		return v.multiply(-1, -1, -1);
	}

	/**
	 * Negates all coordinates of the supplied block position.
	 *
	 * @param v block position to negate
	 * @return a new block position with each coordinate negated
	 */
	public static BlockPos invert(final BlockPos v) {
		return new BlockPos(-1 * v.getX(), -1 * v.getY(), -1 * v.getZ());
	}

	/**
	 * Chooses, component by component, the value closer to zero between two vectors.
	 *
	 * @param start first vector
	 * @param end second vector
	 * @return a vector containing the component-wise values closer to zero
	 */
	public static Vec3 minimizeTowardsZero(final Vec3 start, final Vec3 end) {
		return new Vec3(
				MathUtil.minimizeTowardsZero(start.x(), end.x()),
				MathUtil.minimizeTowardsZero(start.y(), end.y()),
				MathUtil.minimizeTowardsZero(start.z(), end.z())
		);
	}

	/**
	 * Chooses, component by component, the value farther from zero between two vectors.
	 *
	 * @param start first vector
	 * @param end second vector
	 * @return a vector containing the component-wise values farther from zero
	 */
	public static Vec3 maximizeAwayFromZero(final Vec3 start, final Vec3 end) {
		return new Vec3(
				MathUtil.maximizeAwayFromZero(start.x(), end.x()),
				MathUtil.maximizeAwayFromZero(start.y(), end.y()),
				MathUtil.maximizeAwayFromZero(start.z(), end.z())
		);
	}

	/**
	 * Returns a vector containing the absolute value of each component.
	 *
	 * @param vector3d vector whose components are made non-negative
	 * @return a vector containing the absolute values of the input components
	 */
	public static Vec3 absolute(final Vec3 vector3d) {
		return new Vec3(
				vector3d.x() < 0 ? -1 * vector3d.x() : vector3d.x(),
				vector3d.y() < 0 ? -1 * vector3d.y() : vector3d.y(),
				vector3d.z() < 0 ? -1 * vector3d.z() : vector3d.z()
		);
	}

	/**
	 * Adds a random offset in the range {@code [-radius, radius]} to each component.
	 *
	 * @param source vector to offset
	 * @param random random source for the three offsets
	 * @param radius maximum absolute offset on each component
	 * @return the offset vector
	 */
	public static Vec3 offsetRandomly(Vec3 source, RandomSource random, float radius) {
		return new Vec3(source.x + (random.nextFloat() -.5f) * 2 * radius, source.y + (random.nextFloat() -.5f) * 2 * radius,
				source.z + (random.nextFloat() -.5f) * 2 * radius);
	}

	/**
	 * Returns the component-wise minimum of two vectors.
	 *
	 * @param a first vector
	 * @param b second vector
	 * @return a vector containing the smaller component from each input pair
	 */
	public static Vec3 minimize(final Vec3 a, final Vec3 b) {
		return new Vec3(
				Math.min(a.x(), b.x()),
				Math.min(a.y(), b.y()),
				Math.min(a.z(), b.z())
		);
	}

	/**
	 * Returns the component-wise maximum of two vectors.
	 *
	 * @param a first vector
	 * @param b second vector
	 * @return a vector containing the larger component from each input pair
	 */
	public static Vec3 maximize(final Vec3 a, final Vec3 b) {
		return new Vec3(
				Math.max(a.x(), b.x()),
				Math.max(a.y(), b.y()),
				Math.max(a.z(), b.z())
		);
	}

	/**
	 * Makes each component of a block-relative offset non-negative.
	 *
	 * @param inBlockOffset block-relative offset to normalize
	 * @return a vector with each component in the range {@code [0, 1)}
	 */
	public static Vec3 makePositive(final Vec3 inBlockOffset) {
		return new Vec3(
				MathUtil.makePositive(inBlockOffset.x()),
				MathUtil.makePositive(inBlockOffset.y()),
				MathUtil.makePositive(inBlockOffset.z())
		);
	}

	/**
	 * Tests whether all four components lie within the inclusive interval.
	 *
	 * @param vector4f four-component vector to test
	 * @param min inclusive lower bound for each component
	 * @param max inclusive upper bound for each component
	 * @return {@code true} if every component is between {@code min} and {@code max}, inclusive
	 */
	public static boolean allValuesBetweenInclusive(final Vector4f vector4f, final float min, final float max) {
		return min <= vector4f.x() && vector4f.x() <= max
				&& min <= vector4f.y() && vector4f.y() <= max
				&& min <= vector4f.z() && vector4f.z() <= max
				&& min <= vector4f.w() && vector4f.w() <= max;
	}

	/**
	 * Replaces negative vector components with zero.
	 *
	 * @param input vector to clamp
	 * @return a vector with negative components replaced by zero
	 */
	public static Vec3 nullifyNegatives(final Vec3 input) {
		return new Vec3(
				Math.max(0, input.x()),
				Math.max(0, input.y()),
				Math.max(0, input.z())
		);
	}

	/**
	 * Replaces positive vector components with zero.
	 *
	 * @param input vector to clamp
	 * @return a vector with positive components replaced by zero
	 */
	public static Vec3 nullifyPositives(final Vec3 input) {
		return new Vec3(
				Math.min(0, input.x()),
				Math.min(0, input.y()),
				Math.min(0, input.z())
		);
	}

	/**
	 * Divides each component of the left vector by the corresponding right component.
	 *
	 * @param left vector providing the numerators
	 * @param right vector providing the denominators
	 * @return the component-wise quotient
	 */
	public static Vec3 divide(final Vec3 left, final Vec3 right) {
		return new Vec3(
				left.x() / right.x(),
				left.y() / right.y(),
				left.z() / right.z()
		);
	}

	/**
	 * Converts a vector to block coordinates by flooring each component.
	 *
	 * @param vec3 vector to convert.
	 * @return the converted value.
	 */
	public static BlockPos toBlockPos(final Vec3 vec3) {
		final int x = (int) Math.floor(vec3.x());
		final int y = (int) Math.floor(vec3.y());
		final int z = (int) Math.floor(vec3.z());

		return new BlockPos(x, y, z);
	}

	/**
	 * Converts a vector to integer coordinates by flooring each component.
	 *
	 * @param vec3 vector to convert.
	 * @return the converted value.
	 */
	public static Vec3i toInteger(final Vec3 vec3) {
		final int x = (int) Math.floor(vec3.x());
		final int y = (int) Math.floor(vec3.y());
		final int z = (int) Math.floor(vec3.z());

		return new Vec3i(x, y, z);
	}

	/**
	 * Creates integer coordinates by flooring each supplied component.
	 *
	 * @param y y coordinate.
	 * @param x x coordinate.
	 * @param z z coordinate.
	 * @return the converted value.
	 */
	public static Vec3i toInteger(final double x, final double y, final double z) {
		final int xInt = (int) Math.floor(x);
		final int yInt = (int) Math.floor(y);
		final int zInt = (int) Math.floor(z);

		return new Vec3i(xInt, yInt, zInt);
	}
}

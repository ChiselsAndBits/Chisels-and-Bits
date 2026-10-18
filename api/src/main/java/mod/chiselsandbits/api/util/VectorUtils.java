package mod.chiselsandbits.api.util;

import org.joml.Vector4f;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public class VectorUtils {
	public final static double DEG_TO_RAD_FACTOR = (2*Math.PI) / 360d;
	public static final Vec3 ONE = new Vec3(1, 1, 1);

	private VectorUtils() {
		throw new IllegalStateException("Can not instantiate an instance of: VectorUtils.This is a utility class");
	}

	public static Vec3 rotateMultipleTimes90Degrees(final Vec3 vector3d, final Direction.Axis axis, final int times) {
		return rotateDegrees(vector3d, axis, (times * 90) % 360);
	}

	public static Vec3 rotate90Degrees(final Vec3 vector3d, final Direction.Axis axis) {
		return rotateDegrees(vector3d, axis, 90);
	}

	public static Vec3 rotateHalfRadian(final Vec3 vector3d, final Direction.Axis axis) {
		return rotate(vector3d, axis, 0.5 * Math.PI);
	}

	public static Vec3 rotateDegrees(final Vec3 vector3d, final Direction.Axis axis, final double angleInDegrees) {
		return rotate(vector3d, axis, angleInDegrees * DEG_TO_RAD_FACTOR);
	}

	public static Vec3 rotate(final Vec3 vector3d, final Direction.Axis axis, final double angleInRadian) {
		switch (axis) {
			case X
			: return new Vec3(
					vector3d.x(),
					vector3d.y() * Math.cos((float) angleInRadian) - vector3d.z() * Math.sin((float) angleInRadian),
					vector3d.y() * Math.sin((float) angleInRadian) + vector3d.z() * Math.cos((float) angleInRadian)
			);
			case Y
			: return new Vec3(
					vector3d.x() * Math.cos((float) angleInRadian) + vector3d.z() * Math.sin((float) angleInRadian),
					vector3d.y(),
					-vector3d.x() * Math.sin((float) angleInRadian) + vector3d.z() * Math.cos((float) angleInRadian)
			);
			case Z
			: return new Vec3(
					vector3d.x() * Math.cos((float) angleInRadian) - vector3d.y() * Math.sin((float) angleInRadian),
					vector3d.x() * Math.sin((float) angleInRadian) + vector3d.y() * Math.cos((float) angleInRadian),
					vector3d.z()
			);
			default
			: throw new IllegalArgumentException(String.format("Unknown axis : %s", axis));
		}
	}

	public static Vec3 scaleToOne(final Vec3 v) {
		final double maxSize = Math.abs(getMaximalComponent(v));

		if (MathUtil.almostEqual(maxSize, 0)) {
			return Vec3.ZERO;
		}

		final double scale = 1 / maxSize;

		return v.scale(scale);
	}

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

	public static Vec3 invert(final Vec3 v) {
		return v.multiply(-1, -1, -1);
	}

	public static BlockPos invert(final BlockPos v) {
		return new BlockPos(-1 * v.getX(), -1 * v.getY(), -1 * v.getZ());
	}

	public static Vec3 minimizeTowardsZero(final Vec3 start, final Vec3 end) {
		return new Vec3(
				MathUtil.minimizeTowardsZero(start.x(), end.x()),
				MathUtil.minimizeTowardsZero(start.y(), end.y()),
				MathUtil.minimizeTowardsZero(start.z(), end.z())
		);
	}

	public static Vec3 maximizeAwayFromZero(final Vec3 start, final Vec3 end) {
		return new Vec3(
				MathUtil.maximizeAwayFromZero(start.x(), end.x()),
				MathUtil.maximizeAwayFromZero(start.y(), end.y()),
				MathUtil.maximizeAwayFromZero(start.z(), end.z())
		);
	}

	public static Vec3 absolute(final Vec3 vector3d) {
		return new Vec3(
				vector3d.x() < 0 ? -1 * vector3d.x() : vector3d.x(),
				vector3d.y() < 0 ? -1 * vector3d.y() : vector3d.y(),
				vector3d.z() < 0 ? -1 * vector3d.z() : vector3d.z()
		);
	}

	public static Vec3 offsetRandomly(Vec3 source, RandomSource random, float radius) {
		return new Vec3(source.x + (random.nextFloat() -.5f) * 2 * radius, source.y + (random.nextFloat() -.5f) * 2 * radius,
				source.z + (random.nextFloat() -.5f) * 2 * radius);
	}

	public static Vec3 minimize(final Vec3 a, final Vec3 b) {
		return new Vec3(
				Math.min(a.x(), b.x()),
				Math.min(a.y(), b.y()),
				Math.min(a.z(), b.z())
		);
	}

	public static Vec3 maximize(final Vec3 a, final Vec3 b) {
		return new Vec3(
				Math.max(a.x(), b.x()),
				Math.max(a.y(), b.y()),
				Math.max(a.z(), b.z())
		);
	}

	public static Vec3 makePositive(final Vec3 inBlockOffset) {
		return new Vec3(
				MathUtil.makePositive(inBlockOffset.x()),
				MathUtil.makePositive(inBlockOffset.y()),
				MathUtil.makePositive(inBlockOffset.z())
		);
	}

	public static boolean allValuesBetweenInclusive(final Vector4f vector4f, final float min, final float max) {
		return min <= vector4f.x() && vector4f.x() <= max
				&& min <= vector4f.y() && vector4f.y() <= max
				&& min <= vector4f.z() && vector4f.z() <= max
				&& min <= vector4f.w() && vector4f.w() <= max;
	}

	public static Vec3 nullifyNegatives(final Vec3 input) {
		return new Vec3(
				Math.max(0, input.x()),
				Math.max(0, input.y()),
				Math.max(0, input.z())
		);
	}

	public static Vec3 nullifyPositives(final Vec3 input) {
		return new Vec3(
				Math.min(0, input.x()),
				Math.min(0, input.y()),
				Math.min(0, input.z())
		);
	}

	public static Vec3 divide(final Vec3 left, final Vec3 right) {
		return new Vec3(
				left.x() / right.x(),
				left.y() / right.y(),
				left.z() / right.z()
		);
	}

	public static BlockPos toBlockPos(final Vec3 vec3) {
		final int x = (int) Math.floor(vec3.x());
		final int y = (int) Math.floor(vec3.y());
		final int z = (int) Math.floor(vec3.z());

		return new BlockPos(x, y, z);
	}

	public static Vec3i toInteger(final Vec3 vec3) {
		final int x = (int) Math.floor(vec3.x());
		final int y = (int) Math.floor(vec3.y());
		final int z = (int) Math.floor(vec3.z());

		return new Vec3i(x, y, z);
	}

	public static Vec3i toInteger(final double x, final double y, final double z) {
		final int xInt = (int) Math.floor(x);
		final int yInt = (int) Math.floor(y);
		final int zInt = (int) Math.floor(z);

		return new Vec3i(xInt, yInt, zInt);
	}
}

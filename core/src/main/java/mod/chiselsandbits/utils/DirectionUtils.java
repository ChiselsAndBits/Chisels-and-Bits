package mod.chiselsandbits.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import mod.chiselsandbits.api.util.MathUtil;
import mod.chiselsandbits.api.util.VectorUtils;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class DirectionUtils {
	private DirectionUtils() {
		throw new IllegalStateException("Can not instantiate an instance of: DirectionUtils.This is a utility class");
	}

	public static List<Direction> getNotOnAxisOf(final Direction direction) {
		List<Direction> list = new ArrayList<>();

		for (Direction d : Direction.values()) {
			if (d.getAxis() != direction.getAxis()) {
				list.add(d);
			}
		}

		return list;
	}

	public static Optional<Direction> getDirectionVectorBetweenIfAligned(final Vec3 l, final Vec3 r) {
		return getDirectionVectorIfAligned(l.subtract(r));
	}

	public static Optional<Direction> getDirectionVectorIfAligned(final Vec3 vector) {
		final Vec3 scaledToOne = VectorUtils.scaleToOne(vector);

		if (MathUtil.almostEqual(scaledToOne.x(), 0)) {
			if (MathUtil.almostEqual(scaledToOne.y(), 0)) {
				if (MathUtil.almostEqual(scaledToOne.z(), 0)) {
					return Optional.empty();
				} else {
					return Optional.of(scaledToOne.z() <0 ? Direction.NORTH : Direction.SOUTH);
				}
			} else {
				if (MathUtil.almostEqual(scaledToOne.z(), 0)) {
					return Optional.of(scaledToOne.y() <0 ? Direction.DOWN : Direction.UP);
				} else {
					return Optional.empty();
				}
			}
		} else {
			if (MathUtil.almostEqual(scaledToOne.y(), 0)) {
				if (MathUtil.almostEqual(scaledToOne.z(), 0)) {
					return Optional.of(scaledToOne.z() <0 ? Direction.WEST : Direction.EAST);
				} else {
					return Optional.empty();
				}
			} else {
				return Optional.empty();
			}
		}
	}
}

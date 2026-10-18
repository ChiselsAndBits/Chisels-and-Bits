package mod.chiselsandbits.utils;

import mod.chiselsandbits.api.util.IWorldObject;

import net.minecraft.world.phys.Vec3;

public class WorldObjectUtils {
	private WorldObjectUtils() {
		throw new IllegalStateException("Can not instantiate an instance of: WorldObjectUtils.This is a utility class");
	}

	public static boolean isInsideOrCoveredBy(final IWorldObject a, final IWorldObject b) {
		return isAInsideOrCoveredByB(a, b) || isAInsideOrCoveredByB(b, a);
	}

	public static boolean isAInsideOrCoveredByB(final IWorldObject a, final IWorldObject b) {
		return isAInsideB(a, b) || isACoveringB(a, b);
	}

	public static boolean isAInsideB(final IWorldObject a, final IWorldObject b) {
		final Vec3 aStart = a.getInWorldStartPoint();
		final Vec3 aEnd = a.getInWorldEndPoint();

		final Vec3 bStart = b.getInWorldStartPoint();
		final Vec3 bEnd = b.getInWorldEndPoint();

		return bStart.x() <= aStart.x() && aEnd.x() <= bEnd.x()
				&& bStart.y() <= aStart.y() && aEnd.y() <= bEnd.y()
				&& bStart.z() <= aStart.z() && aEnd.z() <= bEnd.z();
	}

	public static boolean isACoveringB(final IWorldObject a, final IWorldObject b) {
		final Vec3 bLowerLeftFront = b.getInWorldStartPoint();
		final Vec3 bUpperRightBack = b.getInWorldEndPoint();

		final Vec3 bLowerLeftBack = new Vec3(bLowerLeftFront.x(), bLowerLeftFront.y(), bUpperRightBack.z());
		final Vec3 bUpperLeftFront = new Vec3(bLowerLeftFront.x(), bUpperRightBack.y(), bLowerLeftFront.z());
		final Vec3 bLowerRightFront = new Vec3(bUpperRightBack.x(), bLowerLeftFront.y(), bLowerLeftFront.z());
		final Vec3 bUpperLeftBack = new Vec3(bLowerLeftFront.x(), bUpperRightBack.y(), bUpperRightBack.z());
		final Vec3 bLowerRightBack = new Vec3(bUpperRightBack.x(), bLowerLeftFront.y(), bUpperRightBack.z());
		final Vec3 bUpperRightFront = new Vec3(bUpperRightBack.x(), bUpperRightBack.y(), bLowerLeftFront.z());

		return isPointInside(a, bLowerLeftFront)
				|| isPointInside(a, bUpperRightBack)
				|| isPointInside(a, bLowerLeftBack)
				|| isPointInside(a, bLowerRightFront)
				|| isPointInside(a, bUpperLeftFront)
				|| isPointInside(a, bLowerRightBack)
				|| isPointInside(a, bUpperLeftBack)
				|| isPointInside(a, bUpperRightFront);
	}

	public static boolean isPointInside(final IWorldObject a, final Vec3 point) {
		final Vec3 aStart = a.getInWorldStartPoint();
		final Vec3 aEnd = a.getInWorldEndPoint();

		return aStart.x() <= point.x() && point.x() <= aEnd.x()
				&& aStart.y() <= point.y() && point.y() <= aEnd.y()
				&& aStart.z() <= point.z() && point.z() <= aEnd.z();
	}
}

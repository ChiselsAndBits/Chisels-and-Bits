package mod.chiselsandbits.api.util;

public class MathUtil {
	private static final double DEFAULT_EPSILON = 1E-7;

	private MathUtil()

	{
		throw new IllegalStateException("Can not instantiate an instance of: MathUtil. This is a utility class");
	}

	public static boolean almostEqual(final Number l, final Number r) {
		return almostEqual(l, r, DEFAULT_EPSILON);
	}

	public static boolean almostEqual(final Number l, final Number r, final Number epsilon) {
		final double diff = Math.abs(l.doubleValue() - r.doubleValue());
		return diff < epsilon.doubleValue();
	}

	public static double minimizeTowardsZero(final double l, final double r) {
		final double absL = Math.abs(l);
		final double absR = Math.abs(r);

		if (absL <= absR) {
			return l;
		}

		return r;
	}

	public static double maximizeAwayFromZero(final double l, final double r) {
		final double absL = Math.abs(l);
		final double absR = Math.abs(r);

		if (absL >= absR) {
			return l;
		}

		return r;
	}

	public static double makePositive(double d) {
		while (d < 0) {
			d++;
		}

		return d;
	}
}

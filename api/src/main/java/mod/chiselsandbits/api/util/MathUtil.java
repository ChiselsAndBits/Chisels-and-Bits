package mod.chiselsandbits.api.util;

/**
 * Numeric helpers for comparing and adjusting coordinate values.
 */
public class MathUtil {
	private static final double DEFAULT_EPSILON = 1E-7;

	private MathUtil() {
		throw new IllegalStateException("Can not instantiate an instance of: MathUtil.This is a utility class");
	}

	/**
	 * Compares two numbers using the utility's default floating-point tolerance.
	 *
	 * @param l the first number to compare
	 * @param r the second number to compare
	 * @return {@code true} when the numbers differ by less than the default tolerance
	 */
	public static boolean almostEqual(final Number l, final Number r) {
		return almostEqual(l, r, DEFAULT_EPSILON);
	}

	/**
	 * Compares two numbers using an explicit absolute-difference tolerance.
	 *
	 * @param l the first number to compare
	 * @param r the second number to compare
	 * @param epsilon the largest accepted absolute difference
	 * @return {@code true} when the absolute difference is less than {@code epsilon}
	 */
	public static boolean almostEqual(final Number l, final Number r, final Number epsilon) {
		final double diff = Math.abs(l.doubleValue() - r.doubleValue());
		return diff < epsilon.doubleValue();
	}

	/**
	 * Returns whichever value has the smaller absolute magnitude.
	 *
	 * @param l the first value
	 * @param r the second value
	 * @return the value closer to zero; ties return {@code l}
	 */
	public static double minimizeTowardsZero(final double l, final double r) {
		final double absL = Math.abs(l);
		final double absR = Math.abs(r);

		if (absL <= absR) {
			return l;
		}

		return r;
	}

	/**
	 * Returns whichever value has the greater absolute magnitude.
	 *
	 * @param l the first value
	 * @param r the second value
	 * @return the value farther from zero; ties return {@code l}
	 */
	public static double maximizeAwayFromZero(final double l, final double r) {
		final double absL = Math.abs(l);
		final double absR = Math.abs(r);

		if (absL >= absR) {
			return l;
		}

		return r;
	}

	/**
	 * Adds whole units until a coordinate is non-negative.
	 *
	 * @param d the coordinate to adjust
	 * @return the equivalent coordinate in the range {@code [0, 1)}
	 */
	public static double makePositive(double d) {
		while (d < 0) {
			d++;
		}

		return d;
	}
}

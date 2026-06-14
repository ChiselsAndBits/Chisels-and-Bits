package mod.chiselsandbits.client.util;

public class FloatUtils {
	private static final float EPSILON = 0.000001f;

	private FloatUtils() {
		throw new IllegalStateException("Can not instantiate an instance of: FloatUtils. This is a utility class");
	}

	public static boolean isEqual(final float left, final float right) {
		return Math.abs(left - right) <= EPSILON;
	}

	public static boolean isNotEqual(final float left, final float right) {
		return !isEqual(left, right);
	}
}

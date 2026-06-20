package mod.chiselsandbits.utils;

public class ModelUtil {
	private ModelUtil() {
		throw new IllegalStateException("Can not instantiate an instance of: ModelUtil.This is a utility class");
	}

	public static boolean isZero(
			final float v) {
		return Math.abs(v) <0.01;
	}

	public static boolean isOne(
			final float v) {
		return is(v, 1f);
	}

	public static boolean is(
			final float v,
			final float t) {
		return (Math.abs(v - t)) <0.01;
	}
}

package mod.chiselsandbits.api.util;

import java.util.function.IntFunction;

/**
 * Utility class for manipulating arrays.
 */
public class ArrayUtils {
	private ArrayUtils()

	{
		throw new IllegalStateException("Can not instantiate an instance of: ArrayUtils. This is a utility class");
	}

	/**
     * Multiplies the given array by the given factor.
     *
     * @param input The input array.
     * @param count The factor to multiply the arrays values by.
     * @return A new array with the values of the old array multiplied by the given factor.
     */
	public static float[] multiply(final float[] input, final float count) {
		final float[] result = new float[input.length];
		for (int i = 0; i < input.length; i++)

		{
			result[i] = input[i] * count;
		}

		return result;
	}

	public static <T extends ISnapshotable<? extends T>> T[] createDeepClone(final T[] additionalStateInfos, IntFunction<T[]> arrayCreator)

	{
		final T[] clone = arrayCreator.apply(additionalStateInfos.length);

		for (int i = 0; i < additionalStateInfos.length; i++) {
			if (additionalStateInfos[i] != null) {
				clone[i] = additionalStateInfos[i].createSnapshot();
			}

			else {
				clone[i] = null;
			}
		}

		return clone;
	}
}

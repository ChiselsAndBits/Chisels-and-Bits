package mod.chiselsandbits.api.util;

/**
 * Utility class of helpers for working with raw bits and their masks.
 */
public class BitUtils {
	private BitUtils() {
		throw new IllegalStateException("Can not instantiate an instance of: BitUtils.This is a utility class");
	}

	/**
	 * Returns an int whose lowest {@code bitWidth} bits are all set to one; when {@code bitWidth} equals 32 the full 32-bit mask is returned.
	 *
	 * @param bitWidth The number of low bits to set.
	 * @return A bitmask with the given number of trailing set bits, or all 32 bits set if {@code bitWidth} is 32.
	 */
	public static int getBitMask(final int bitWidth) {
		if (bitWidth == Integer.SIZE) {
			return 0xFFFFFFFF;
		}

		return (1 << bitWidth) - 1;
	}

	/**
	 * Returns the number of bytes required to store {@code totalBitCount} bits, rounding up so any partial last byte counts as a full byte.
	 *
	 * @param totalBitCount The total number of bits to encode.
	 * @return The count of bytes needed for the given bit count, rounded up to a whole byte.
	 */
	public static int getByteCount(final int totalBitCount) {
		return (int) Math.ceil(totalBitCount / (float) Byte.SIZE);
	}

	/**
	 * Returns the width in bits of {@code bitMask}, equal to one plus the position of its highest set bit (and 32 when every bit is set).
	 *
	 * @param bitMask The mask whose bit-width is measured.
	 * @return The number of low bits spanned by the mask, or 32 if all bits are already set.
	 */
	public static int getMaskWidth(final int bitMask) {
		if (bitMask == 0xFFFFFFFF) {
			return 32;
		}

		final int maxBit = bitMask + 1;
		return Integer.SIZE - Integer.numberOfLeadingZeros(maxBit) - 1;
	}
}

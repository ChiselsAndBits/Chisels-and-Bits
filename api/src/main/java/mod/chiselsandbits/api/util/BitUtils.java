package mod.chiselsandbits.api.util;

public class BitUtils
{
	private BitUtils()

	{
		throw new IllegalStateException("Can not instantiate an instance of: BitUtils. This is a utility class");
	}

	public static int getBitMask(final int bitWidth) {
		if (bitWidth == Integer.SIZE) {
			return 0xFFFFFFFF;
		}

		return (1 << bitWidth) - 1;
	}

	public static int getByteCount(final int totalBitCount)

	{
		return (int) Math.ceil(totalBitCount / (float) Byte.SIZE);
	}

	public static int getMaskWidth(final int bitMask)

	{
		if (bitMask == 0xFFFFFFFF) {
			return 32;
		}

		final int maxBit = bitMask + 1;
		return Integer.SIZE - Integer.numberOfLeadingZeros(maxBit) - 1;
	}
}

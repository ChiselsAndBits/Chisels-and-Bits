package mod.chiselsandbits.api.util;

import java.util.BitSet;

public class ByteArrayUtils {
	private ByteArrayUtils()

	{
		throw new IllegalStateException("Can not instantiate an instance of: ByteArrayUtils. This is a utility class");
	}

	public static BitSet fill(final int value, final int bitWidth, final int count) {
		final int valueMask = BitUtils.getBitMask(bitWidth);

		final int totalBitCount = bitWidth * count;
		final int byteCount = BitUtils.getByteCount(totalBitCount);

		final BitSet result = new BitSet(totalBitCount);

		for (int insertionIndex = 0; insertionIndex < count; insertionIndex++)

		{
			final int bitOffset = insertionIndex * bitWidth;
			setValue(result, value, valueMask, bitOffset);
		}

		return result;
	}

	public static void setValueAt(final BitSet target, final int value, final int bitWidth, final int index) {
		setValue(target, value, BitUtils.getBitMask(bitWidth), index * bitWidth);
	}

	public static void setValueWith(final BitSet target, final int value, final int bitMask, final int index) {
		final int maskWidth = BitUtils.getMaskWidth(bitMask);
		setValue(target, value, bitMask, index * maskWidth);
	}

	public static void setValue(final BitSet target, final int value, final int bitMask, final int bitOffset) {
		final int maskWidth = BitUtils.getMaskWidth(bitMask);
		target.clear(bitOffset, bitOffset + maskWidth);

		for (int i = 0; i < maskWidth; i++)

		{
			final int inValueOffset = maskWidth - i - 1;

			final boolean isSet = ((value >> i) & 1) != 0;
			target.set(bitOffset + i, isSet);
		}
	}

	public static int getValueAt(final BitSet target, final int bitWidth, final int index) {
		return getValue(target, BitUtils.getBitMask(bitWidth), index * bitWidth);
	}

	public static int getValue(final BitSet target, final int bitMask, final int bitOffset) {
		final int maskWidth = BitUtils.getMaskWidth(bitMask);

		int result = 0;
		for (int i = 0; i < maskWidth; i++)

		{
			final boolean isSet = target.get(bitOffset + i); //Based of the bitset getter.
			result |= (isSet ? 1 : 0) << i;
		}

		return result;
	}
}

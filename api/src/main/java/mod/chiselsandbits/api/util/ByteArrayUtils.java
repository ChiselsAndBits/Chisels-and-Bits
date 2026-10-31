package mod.chiselsandbits.api.util;

import java.util.BitSet;

/**
 * Utility class for reading and writing integer values inside a {@link BitSet}, packing one value into a fixed range of bits.
 */
public class ByteArrayUtils {
	private ByteArrayUtils() {
		throw new IllegalStateException("Can not instantiate an instance of: ByteArrayUtils.This is a utility class");
	}

	/**
	 * Builds a new {@link BitSet} containing {@code value} repeated {@code count} times, each copy occupying {@code bitWidth} consecutive bits at successive offsets.
	 *
	 * @param value The value to store in each slot.
	 * @param bitWidth The number of bits used for each stored value.
	 * @param count How many values to pack into the set.
	 * @return A fresh BitSet sized {@code bitWidth * count}, with each of the first {@code count} slots holding a masked copy of {@code value}.
	 */
	public static BitSet fill(final int value, final int bitWidth, final int count) {
		final int valueMask = BitUtils.getBitMask(bitWidth);

		final int totalBitCount = bitWidth * count;
		final int byteCount = BitUtils.getByteCount(totalBitCount);

		final BitSet result = new BitSet(totalBitCount);

		for (int insertionIndex = 0; insertionIndex < count; insertionIndex++) {
			final int bitOffset = insertionIndex * bitWidth;
			setValue(result, value, valueMask, bitOffset);
		}

		return result;
	}

	/**
	 * Overwrites the {@code bitWidth}-bit field starting at slot {@code index} within {@code target} with the given value.
	 *
	 * @param target The BitSet to modify in place.
	 * @param value The value to store, masked to {@code bitWidth} bits.
	 * @param bitWidth The number of bits used for this field.
	 * @param index The slot whose bits should be updated.
	 */
	public static void setValueAt(final BitSet target, final int value, final int bitWidth, final int index) {
		setValue(target, value, BitUtils.getBitMask(bitWidth), index * bitWidth);
	}

	/**
	 * Overwrites the field starting at slot {@code index} within {@code target}, deriving its width from {@code bitMask}.
	 *
	 * @param target The BitSet to modify in place.
	 * @param value The value to store.
	 * @param bitMask A mask whose width defines how many bits of the field are used.
	 * @param index The slot whose bits should be updated.
	 */
	public static void setValueWith(final BitSet target, final int value, final int bitMask, final int index) {
		final int maskWidth = BitUtils.getMaskWidth(bitMask);
		setValue(target, value, bitMask, index * maskWidth);
	}

	/**
	 * Writes {@code value} into {@code target} starting at {@code bitOffset}, clearing the field first and setting only the bits selected by {@code bitMask}.
	 *
	 * @param target The BitSet to modify in place.
	 * @param value The value to store.
	 * @param bitMask A mask selecting which of the low bits carry data; its width determines how many bits are written.
	 * @param bitOffset The position in the set at which the field begins.
	 */
	public static void setValue(final BitSet target, final int value, final int bitMask, final int bitOffset) {
		final int maskWidth = BitUtils.getMaskWidth(bitMask);
		target.clear(bitOffset, bitOffset + maskWidth);

		for (int i = 0; i < maskWidth; i++) {
			final int inValueOffset = maskWidth - i - 1;

			final boolean isSet = ((value >> i) & 1) != 0;
			target.set(bitOffset + i, isSet);
		}
	}

	/**
	 * Reads the {@code bitWidth}-bit field starting at slot {@code index} from {@code target}.
	 *
	 * @param target The BitSet to read from.
	 * @param bitWidth The number of bits used for this field.
	 * @param index The slot whose value should be read.
	 * @return The value stored in the field, reconstructed from its constituent bits.
	 */
	public static int getValueAt(final BitSet target, final int bitWidth, final int index) {
		return getValue(target, BitUtils.getBitMask(bitWidth), index * bitWidth);
	}

	/**
	 * Reads a value from {@code target} starting at {@code bitOffset}, reconstructing it from the bits selected by {@code bitMask}.
	 *
	 * @param target The BitSet to read from.
	 * @param bitMask A mask selecting which low bits carry data; its width determines how many bits are read.
	 * @param bitOffset The position in the set at which the field begins.
	 * @return The value formed by the selected bits, with the least-significant bit aligned to {@code bitOffset}.
	 */
	public static int getValue(final BitSet target, final int bitMask, final int bitOffset) {
		final int maskWidth = BitUtils.getMaskWidth(bitMask);

		int result = 0;

		for (int i = 0; i < maskWidth; i++) {
			final boolean isSet = target.get(bitOffset + i); //Based of the bitset getter.
			result |= (isSet ? 1 : 0) << i;
		}

		return result;
	}
}

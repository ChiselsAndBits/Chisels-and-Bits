package mod.chiselsandbits.components.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.util.constants.NbtConstants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the data of a slotted bit inventory.
 *
 * @see mod.chiselsandbits.inventory.bit.SlottedBitInventory
 */
public record SlottedBitInventoryData(Map<Integer, BitSlotData> data) {
	public static final SlottedBitInventoryData EMPTY = new SlottedBitInventoryData(Collections.emptyMap());

	private static final Codec<Integer> KEY_CODEC = Codec.STRING
			.xmap(Integer::parseInt, Object::toString);

	public static final Codec<SlottedBitInventoryData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.unboundedMap(KEY_CODEC, BitSlotData.CODEC).fieldOf(NbtConstants.DATA).forGetter(SlottedBitInventoryData::data)
			).apply(instance, SlottedBitInventoryData::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, SlottedBitInventoryData> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.map(HashMap::new, ByteBufCodecs.VAR_INT, BitSlotData.STREAM_CODEC),
			SlottedBitInventoryData::data,
			SlottedBitInventoryData::new
	);

	public SlottedBitInventoryData(Map<Integer, BitSlotData> data) {
		if (data == null) {
			throw new IllegalArgumentException("Data cannot be null");
		}

		this.data = Collections.unmodifiableMap(data);
	}

	public record BitSlotData(BlockInformation blockInformation, int count) {
		public static final Codec<BitSlotData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				BlockInformation.CODEC.fieldOf(NbtConstants.BLOCK_INFORMATION).forGetter(BitSlotData::blockInformation),
				Codec.INT.fieldOf(NbtConstants.COUNT).forGetter(BitSlotData::count)
				).apply(instance, BitSlotData::new));

		public static final StreamCodec<RegistryFriendlyByteBuf, BitSlotData> STREAM_CODEC = StreamCodec.composite(
				BlockInformation.STREAM_CODEC,
				BitSlotData::blockInformation,
				ByteBufCodecs.VAR_INT,
				BitSlotData::count,
				BitSlotData::new
		);
	}
}

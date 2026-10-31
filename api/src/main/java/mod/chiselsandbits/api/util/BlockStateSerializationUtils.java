package mod.chiselsandbits.api.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Serializes and deserializes {@link net.minecraft.world.level.block.state.BlockState} values across JSON string, registry-ID buffer, and legacy formats.
 */
public class BlockStateSerializationUtils {
	private static final Gson GSON = new Gson();

	/**
	 * A {@link com.mojang.serialization.Codec} that parses {@link BlockState} values from their serialized form; serialization is unsupported and always throws.
	 */
	public static Codec<BlockState> LEGACY_BLOCK_STATE_CODEC = Codec.STRING.comapFlatMap(
			BlockStateSerializationUtils::deserialize,
			blockState -> {
				throw new IllegalStateException("Cannot serialize BlockState into a legacy format");
			});

	private BlockStateSerializationUtils() {
		throw new IllegalStateException("Can not instantiate an instance of: BlockStateSerializationUtils.This is a utility class");
	}

	/**
	 * Parses a BlockState from its serialized JSON string representation.
	 *
	 * @param string the JSON-encoded block state
	 * @return the parsed BlockState, or an error describing why decoding failed
	 */
	public static DataResult<BlockState> deserialize(final String string) {
		final Dynamic<JsonElement> parsingInput = new Dynamic<>(JsonOps.INSTANCE, GSON.fromJson(string, JsonElement.class));
		return BlockState.CODEC.parse(parsingInput);
	}

	/**
	 * Serializes a BlockState to its JSON string representation.
	 *
	 * @param blockState the block state to encode
	 * @return the JSON-encoded form of the block state
	 */
	public static String serialize(final BlockState blockState) {
		final DataResult<JsonElement> encodedElement = BlockState.CODEC.encodeStart(JsonOps.INSTANCE, blockState);

		if (encodedElement.result().isEmpty()) {
			throw new IllegalStateException("Could not encode BlockState: " + blockState + ".Resulting error: " + encodedElement.error().orElseThrow().message());
		}

		return GSON.toJson(encodedElement.result().get());
	}

	/**
	 * Reads a BlockState from a packet buffer using the block-state registry ID.
	 *
	 * @param buffer the buffer to read the serialized block state from
	 * @return the decoded block state
	 */
	public static BlockState deserialize(final FriendlyByteBuf buffer) {
		return buffer.readById(Block.BLOCK_STATE_REGISTRY::byId);
	}

	/**
	 * Writes a BlockState to a packet buffer using its block-state registry ID.
	 *
	 * @param buf the buffer to write into
	 * @param blockState the block state to encode into the buffer
	 */
	public static void serialize(final FriendlyByteBuf buf, final BlockState blockState) {
		buf.writeById(Block.BLOCK_STATE_REGISTRY::getId, blockState);
	}
}

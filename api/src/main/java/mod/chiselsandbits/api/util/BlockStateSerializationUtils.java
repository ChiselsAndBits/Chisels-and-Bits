package mod.chiselsandbits.api.util;

import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;

import net.minecraft.client.main.GameConfig;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class BlockStateSerializationUtils
{
	private static final Gson GSON = new Gson();

	public static Codec<BlockState> LEGACY_BLOCK_STATE_CODEC = Codec.STRING.comapFlatMap(
			BlockStateSerializationUtils::deserialize,
			blockState -> {
				throw new IllegalStateException("Cannot serialize BlockState into a legacy format");
			}
	);

	private BlockStateSerializationUtils()

	{
		throw new IllegalStateException("Can not instantiate an instance of: BlockStateSerializationUtils. This is a utility class");
	}

	public static DataResult<BlockState> deserialize(final String string) {
		final Dynamic<JsonElement> parsingInput = new Dynamic<>(JsonOps.INSTANCE, GSON.fromJson(string, JsonElement.class));
		return BlockState.CODEC.parse(parsingInput);
	}

	public static String serialize(final BlockState blockState) {
		final DataResult<JsonElement> encodedElement = BlockState.CODEC.encodeStart(JsonOps.INSTANCE, blockState);

		if (encodedElement.result().isEmpty()) {
			throw new IllegalStateException("Could not encode BlockState: " + blockState + ". Resulting error: " + encodedElement.error().orElseThrow().message());
		}

		return GSON.toJson(encodedElement.result().get());
	}

	public static BlockState deserialize(final FriendlyByteBuf buffer) {
		return buffer.readById(Block.BLOCK_STATE_REGISTRY::byId);
	}

	public static void serialize(final FriendlyByteBuf buf, final BlockState blockState) {
		buf.writeById(Block.BLOCK_STATE_REGISTRY::getId, blockState);
	}
}

package mod.chiselsandbits.api.serialization;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Defines objects which can be serialized into an exact format.
 *
 * @param <TSelf> The type of the implementing class.
 * @param <TBuffer> The type of the buffer to serialize to/from.
 */
public interface Serializable<TSelf, TBuffer extends FriendlyByteBuf> extends RawSerializable {
	/**
	 * {@return The codec used for direct serialization.}
	 */
	Codec<TSelf> codec();

	/**
	 * {@return The codec used for complex serialization.}
	 */
	MapCodec<TSelf> mapCodec();

	/**
	 * {@return The codec used for streaming serialization.}
	 */
	StreamCodec<TBuffer, TSelf> streamCodec();

	/**
	 * Serializes specifically when a registry is involved.
	 *
	 * @param <TSelf> The type of the implementing class.
	 */
	interface Registry<TSelf> extends Serializable<TSelf, RegistryFriendlyByteBuf> {
	}
}

package mod.chiselsandbits.api.serialization;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.codec.StreamCodec;

/**
 * Defines objects which can be serialized into a raw format.
 */
public interface RawSerializable {
	/**
     * {@return The codec used for direct serialization.}
     */
	Codec<?> codec();

	/**
     * {@return The codec used for complex serialization.}
     */
	MapCodec<?> mapCodec();

	/**
     * {@return The codec used for streaming serialization.}
     */
	StreamCodec<?, ?> streamCodec();
}

package mod.chiselsandbits.api.multistate.snapshot;

import com.communi.suggestu.scena.core.registries.ICustomRegistryEntry;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The type of a multi state snapshot.
 */
public interface IMultiStateSnapshotType extends ICustomRegistryEntry {
	/**
     * Gets the codec for the snapshot.
     *
     * @return The codec.
     */
	MapCodec<? extends IMultiStateSnapshot> codec();

	/**
     * Gets the stream codec for the snapshot.
     *
     * @return The stream codec.
     */
	StreamCodec<RegistryFriendlyByteBuf, ? extends IMultiStateSnapshot> streamCodec();
}

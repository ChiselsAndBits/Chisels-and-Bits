package mod.chiselsandbits.api.change.changes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import mod.chiselsandbits.api.registries.IRegistryManager;
import mod.chiselsandbits.api.serialization.CBStreamCodecs;
import mod.chiselsandbits.api.serialization.RawSerializable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

import java.util.function.Function;

/**
 * Represents a single change that has been created with bits.
 */
public interface IChange extends IChangeHandler {
	Codec<IChange> CODEC = Codec.lazyInitialized(() -> IRegistryManager.getInstance().getChangeTypeRegistry().byNameCodec()
			.dispatch(IChange::getType, IChangeType::codec));

	StreamCodec<RegistryFriendlyByteBuf, IChange> STREAM_CODEC = CBStreamCodecs.lazyInitialized(() -> CBStreamCodecs.dispatch(
			IRegistryManager.getInstance().getChangeTypeRegistry().byNameStreamCodec(),
			IChangeType::streamCodec,
			IChange::getType
	));

	/**
     * {@return The type of the change.}
     */
	IChangeType getType();
}

package mod.chiselsandbits.api.change.changes;

import com.mojang.serialization.Codec;
import mod.chiselsandbits.api.registries.IRegistryManager;
import mod.chiselsandbits.api.serialization.CBStreamCodecs;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Represents a single change that has been created with bits.
 */
public interface IChange extends IChangeHandler {
	/**
	 * Codec for serializing changes by their registered change type.
	 */
	Codec<IChange> CODEC = Codec.lazyInitialized(() -> IRegistryManager.getInstance().getChangeTypeRegistry().byNameCodec()
			.dispatch(IChange::getType, IChangeType::codec));
	/**
	 * Network codec for serializing changes by their registered change type.
	 */
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

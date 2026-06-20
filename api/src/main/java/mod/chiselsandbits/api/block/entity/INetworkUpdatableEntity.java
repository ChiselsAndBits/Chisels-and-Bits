package mod.chiselsandbits.api.block.entity;

import mod.chiselsandbits.api.serialization.Serializable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Marks the entity as a network updatable entity.
 */
public interface INetworkUpdatableEntity<TPayload> {
	/**
	 * {@return The registry access for this entity.}
	 */
	RegistryAccess registryAccess();

	/**
	 * {@return The position of the block.}
	 */
	BlockPos blockPos();

	/**
	 * {@return The payload to transfer.}
	 */
	TPayload payload();

	/**
	 * {@return The stream codec used to serialize the payload.}
	 */
	StreamCodec<RegistryFriendlyByteBuf, TPayload> streamCodec();

	/**
	 * Called when the client receives a payload useful for updating the entity.
	 *
	 * @param payload The payload to receive.
	 */
	void receivePayload(final TPayload payload);
}

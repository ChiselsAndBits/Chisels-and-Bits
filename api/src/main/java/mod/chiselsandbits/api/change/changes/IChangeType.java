package mod.chiselsandbits.api.change.changes;

import com.communi.suggestu.scena.core.registries.ICustomRegistryEntry;
import com.mojang.serialization.MapCodec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * The type of registry entry that represents a change.
 */
public interface IChangeType extends ICustomRegistryEntry {
	/**
	 * {@return The codec for the change.}
	 */
	MapCodec<? extends IChange> codec();

	/**
	 * {@return The stream codec for the change.}
	 */
	StreamCodec<RegistryFriendlyByteBuf, ? extends IChange> streamCodec();
}

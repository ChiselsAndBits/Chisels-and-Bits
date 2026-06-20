package mod.chiselsandbits.network.packets;

import java.util.function.Function;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public abstract class ModPacket implements CustomPacketPayload {
	public static <P extends ModPacket> StreamCodec<RegistryFriendlyByteBuf, P> streamCodec(Function<RegistryFriendlyByteBuf, P> constructor) {
		return StreamCodec.of(
				(o, p) -> p.writePayload(o),
				constructor::apply
		);
	}

	public ModPacket() {
	}

	public void server(
			final ServerPlayer playerEntity) {
		throw new RuntimeException(getClass().getName() + " is not a server packet.");
	}

	public void client() {
		throw new RuntimeException(getClass().getName() + " is not a client packet.");
	}

	abstract public void writePayload(
			RegistryFriendlyByteBuf buffer);

	abstract public void readPayload(
			RegistryFriendlyByteBuf buffer);

	public void processPacket(
			final Player senderOnServer,
			final Boolean onServer) {
		if (!onServer) {
			client();
		} else if (senderOnServer instanceof ServerPlayer serverPlayer) {
			server(serverPlayer);
		}
	}
}

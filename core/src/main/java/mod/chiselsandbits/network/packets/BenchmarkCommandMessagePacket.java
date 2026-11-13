package mod.chiselsandbits.network.packets;

import com.communi.suggestu.scena.core.dist.Dist;
import com.communi.suggestu.scena.core.dist.DistExecutor;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.network.handlers.ClientPacketHandlers;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public class BenchmarkCommandMessagePacket extends ModPacket {
	public static final Identifier ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "benchmark_command_message");
	public static final CustomPacketPayload.Type<BenchmarkCommandMessagePacket> TYPE = new CustomPacketPayload.Type<>(ID);

	private boolean start;

	public BenchmarkCommandMessagePacket(final boolean start) {
		this.start = start;
	}

	public BenchmarkCommandMessagePacket(final RegistryFriendlyByteBuf buffer) {
		readPayload(buffer);
	}

	@Override
	public void writePayload(final RegistryFriendlyByteBuf buffer) {
		buffer.writeBoolean(start);
	}

	@Override
	public void readPayload(final RegistryFriendlyByteBuf buffer) {
		start = buffer.readBoolean();
	}

	@Override
	public void client() {
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.handleBenchmarkCommandMessage(start));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

package mod.chiselsandbits.network.packets;

import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.container.BagContainer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class ConvertBagGuiPacket extends ModPacket {

    public static final Identifier                                    ID   = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "convert_bag_gui");
    public static final CustomPacketPayload.Type<ConvertBagGuiPacket> TYPE = new CustomPacketPayload.Type<>(ID);

    public  ConvertBagGuiPacket(RegistryFriendlyByteBuf buffer) {
        readPayload(buffer);
    }

    public ConvertBagGuiPacket() {

    }
    @Override
    public void writePayload(RegistryFriendlyByteBuf buffer) {

    }

    @Override
    public void readPayload(RegistryFriendlyByteBuf buffer) {

    }

    @Override
    public void server(
            final ServerPlayer player)
    {
        execute(player);
    }

    public void execute(final Player player) {
        if (player.containerMenu instanceof BagContainer)
        {
            ((BagContainer) player.containerMenu).convert(player);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

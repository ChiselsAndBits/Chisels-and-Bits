package mod.chiselsandbits.network.packets;

import mod.chiselsandbits.container.BagContainer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class ConvertBagGuiPacket extends ModPacket {

    public  ConvertBagGuiPacket(FriendlyByteBuf buffer) {
        readPayload(buffer);
    }

    public ConvertBagGuiPacket() {

    }
    @Override
    public void writePayload(FriendlyByteBuf buffer) {

    }

    @Override
    public void readPayload(FriendlyByteBuf buffer) {

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
}

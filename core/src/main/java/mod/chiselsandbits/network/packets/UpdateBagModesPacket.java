package mod.chiselsandbits.network.packets;

import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.container.BagContainer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class UpdateBagModesPacket extends ModPacket
{

    public static final Identifier                                     ID   = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "update_bag_modes");
    public static final CustomPacketPayload.Type<UpdateBagModesPacket> TYPE = new CustomPacketPayload.Type<>(ID);

    private boolean filtered;
    private boolean preferred;

    public UpdateBagModesPacket(final boolean filtered, final boolean preferred)
    {
        this.filtered = filtered;
        this.preferred = preferred;
    }

    public UpdateBagModesPacket(RegistryFriendlyByteBuf buf)
    {
        readPayload(buf);
    }

    @Override
    public void writePayload(final RegistryFriendlyByteBuf buffer)
    {
        buffer.writeBoolean(filtered);
        buffer.writeBoolean(preferred);
    }

    @Override
    public void readPayload(final RegistryFriendlyByteBuf buffer)
    {
        this.filtered = buffer.readBoolean();
        this.preferred = buffer.readBoolean();
    }

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    @Override
    public void server(final ServerPlayer playerEntity)
    {
        execute(playerEntity);
    }

    public void execute(final Player player) {
        if (player.containerMenu instanceof BagContainer bagContainer)
        {
            bagContainer.setBagProperties(filtered, preferred);
        }
    }
}

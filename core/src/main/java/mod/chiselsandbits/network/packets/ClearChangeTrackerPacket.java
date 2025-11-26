package mod.chiselsandbits.network.packets;

import mod.chiselsandbits.api.change.IChangeTracker;
import mod.chiselsandbits.api.change.IChangeTrackerManager;
import mod.chiselsandbits.api.util.constants.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class ClearChangeTrackerPacket extends ModPacket
{
    public static final Identifier                                         ID   = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "clear_change_tracker");
    public static final CustomPacketPayload.Type<ClearChangeTrackerPacket> TYPE = new CustomPacketPayload.Type<>(ID);

    public ClearChangeTrackerPacket(RegistryFriendlyByteBuf byteBuf)
    {
        this.readPayload(byteBuf);
    }

    public ClearChangeTrackerPacket()
    {
    }

    @Override
    public void writePayload(final RegistryFriendlyByteBuf buffer)
    {
    }

    @Override
    public void readPayload(final RegistryFriendlyByteBuf buffer)
    {
    }

    @Override
    public void server(final ServerPlayer playerEntity)
    {
        final IChangeTracker tracker = IChangeTrackerManager.getInstance().getChangeTracker(playerEntity);
        tracker.clear();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package mod.chiselsandbits.network.packets;

import mod.chiselsandbits.api.change.changes.IChange;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.network.handlers.ClientPacketHandlers;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.Deque;
import java.util.LinkedList;

public final class ChangeTrackerUpdatedPacket extends ModPacket
{

    public static final Identifier                                           ID   = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "change_tracker_updated");
    public static final CustomPacketPayload.Type<ChangeTrackerUpdatedPacket> TYPE = new CustomPacketPayload.Type<>(ID);

    private Deque<IChange> changes;

    public ChangeTrackerUpdatedPacket(RegistryFriendlyByteBuf byteBuf)
    {
        readPayload(byteBuf);
    }

    public ChangeTrackerUpdatedPacket(final Deque<IChange> changes)
    {
        this.changes = changes;
    }

    @Override
    public void writePayload(final RegistryFriendlyByteBuf buffer)
    {
        //We directly use the RFBB here as it is the same instance, and because we need that type.
        buffer.writeCollection(this.changes, (fbb, change) -> IChange.STREAM_CODEC.encode(buffer, change));
    }

    @Override
    public void readPayload(final RegistryFriendlyByteBuf buffer)
    {
        this.changes = buffer.readCollection(
                size -> new LinkedList<>(),
                (fbb) -> IChange.STREAM_CODEC.decode(buffer)
        );
    }

    @Override
    public void client()
    {
        ClientPacketHandlers.handleChangeTrackerUpdated(this.changes);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

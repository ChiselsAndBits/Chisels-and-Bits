package mod.chiselsandbits.network.packets;

import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.input.ProcessingInputTracker;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class InputTrackerStatusUpdatePacket extends ModPacket
{

    public static final Identifier                                               ID   = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "input_tracker_status_update");
    public static final CustomPacketPayload.Type<InputTrackerStatusUpdatePacket> TYPE = new CustomPacketPayload.Type<>(ID);

    private boolean isLeftMouse;
    private boolean started;

    public InputTrackerStatusUpdatePacket(final RegistryFriendlyByteBuf buffer)
    {
        readPayload(buffer);
    }

    public InputTrackerStatusUpdatePacket(final boolean isLeftMouse, final boolean started) {
        this.isLeftMouse = isLeftMouse;
        this.started = started;
    }

    @Override
    public void writePayload(final RegistryFriendlyByteBuf buffer)
    {
        buffer.writeBoolean(this.isLeftMouse);
        buffer.writeBoolean(this.started);
    }

    @Override
    public void readPayload(final RegistryFriendlyByteBuf buffer)
    {
        this.isLeftMouse = buffer.readBoolean();
        this.started = buffer.readBoolean();
    }

    @Override
    public void server(final ServerPlayer playerEntity)
    {
        if (isLeftMouse) {
            if (started) {
                ProcessingInputTracker.getInstance().onStartedLeftClicking(playerEntity);
            }
            else {
                ProcessingInputTracker.getInstance().onStoppedLeftClicking(playerEntity);
            }
        } else {
            if (started) {
                ProcessingInputTracker.getInstance().onStartedRightClicking(playerEntity);
            }
            else {
                ProcessingInputTracker.getInstance().onStoppedRightClicking(playerEntity);
            }
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

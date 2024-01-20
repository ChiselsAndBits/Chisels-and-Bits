package mod.chiselsandbits.network;

import com.communi.suggestu.scena.core.network.INetworkChannel;
import mod.chiselsandbits.network.packets.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.function.Function;

/**
 * Our wrapper for Forge network layer
 */
public class NetworkChannel
{
    private static final String        LATEST_PROTO_VER    = "1.0";
    private static final String          ACCEPTED_PROTO_VERS = LATEST_PROTO_VER;
    /**
     * Forge network channel
     */
    private final INetworkChannel rawChannel;

    /**
     * Creates a new instance of network channel.
     *
     * @param channelName unique channel name
     * @throws IllegalArgumentException if channelName already exists
     */
    public NetworkChannel(final String channelName)
    {
        rawChannel =
          INetworkChannel.create(
            new ResourceLocation("chiselsandbits", channelName),
            () -> LATEST_PROTO_VER,
            ACCEPTED_PROTO_VERS::equals,
            ACCEPTED_PROTO_VERS::equals
          );
    }

    /**
     * Registers all common messages.
     */
    @SuppressWarnings("UnusedAssignment")
    public void registerCommonMessages()
    {
        int index = -1;
        registerMessage(index++, HeldToolModeChangedPacket.class, HeldToolModeChangedPacket::new);
        registerMessage(index++, TileEntityUpdatedPacket.class, TileEntityUpdatedPacket::new);
        registerMessage(index++, BagGuiPacket.class, BagGuiPacket::new);
        registerMessage(index++, BagGuiStackPacket.class, BagGuiStackPacket::new);
        registerMessage(index++, ClearBagGuiPacket.class, ClearBagGuiPacket::new);
        registerMessage(index++, OpenBagGuiPacket.class, OpenBagGuiPacket::new);
        registerMessage(index++, SortBagGuiPacket.class, SortBagGuiPacket::new);
        registerMessage(index++, ConvertBagGuiPacket.class, ConvertBagGuiPacket::new);
        registerMessage(index++, MeasurementUpdatedPacket.class, MeasurementUpdatedPacket::new);
        registerMessage(index++, MeasurementsUpdatedPacket.class, MeasurementsUpdatedPacket::new);
        registerMessage(index++, MeasurementsResetPacket.class, MeasurementsResetPacket::new);
        registerMessage(index++, NeighborBlockUpdatedPacket.class, NeighborBlockUpdatedPacket::new);
        registerMessage(index++, ChangeTrackerUpdatedPacket.class, ChangeTrackerUpdatedPacket::new);
        registerMessage(index++, RequestChangeTrackerOperationPacket.class, RequestChangeTrackerOperationPacket::new);
        registerMessage(index++, ClearChangeTrackerPacket.class, ClearChangeTrackerPacket::new);
        registerMessage(index++, InputTrackerStatusUpdatePacket.class, InputTrackerStatusUpdatePacket::new);
        registerMessage(index++, AddMultiStateItemStackToClipboardPacket.class, AddMultiStateItemStackToClipboardPacket::new);
        registerMessage(index++, ExportPatternCommandMessagePacket.class, ExportPatternCommandMessagePacket::new);
        registerMessage(index++, ImportPatternCommandMessagePacket.class, ImportPatternCommandMessagePacket::new);
        registerMessage(index++, GivePlayerPatternCommandPacket.class, GivePlayerPatternCommandPacket::new);
    }

    /**
     * Register a message into rawChannel.
     *
     * @param <MSG>      message class type
     * @param id         network id
     * @param msgClazz   message class
     * @param msgCreator supplier with new instance of msgClazz
     */
    public <MSG extends ModPacket> void registerMessage(final int id, final Class<MSG> msgClazz, final Function<FriendlyByteBuf, MSG> msgCreator)
    {
        rawChannel.register(
          id,
          msgClazz,
          ModPacket::writePayload,
          msgCreator,
          (msg, serverSide, player, executor) -> executor.accept(() -> msg.processPacket(player, serverSide))
        );
    }

    /**
     * Sends to server.
     *
     * @param msg message to send
     */
    public void sendToServer(final ModPacket msg)
    {
        rawChannel.sendToServer(msg);
    }

    /**
     * Sends to player.
     *
     * @param msg    message to send
     * @param player target player
     */
    public void sendToPlayer(final ModPacket msg, final ServerPlayer player)
    {
        rawChannel.sendToPlayer(msg, player);
    }

    /**
     * Sends to everyone.
     *
     * @param msg message to send
     */
    public void sendToEveryone(final ModPacket msg)
    {
        rawChannel.sendToEveryone(msg);
    }

    /**
     * Sends to everyone in given chunk.
     *
     * @param msg   message to send
     * @param chunk target chunk to look at
     */
    public void sendToTrackingChunk(final ModPacket msg, final LevelChunk chunk)
    {
        rawChannel.sendToTrackingChunk(msg, chunk);
    }
}

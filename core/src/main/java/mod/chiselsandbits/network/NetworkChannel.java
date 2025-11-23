package mod.chiselsandbits.network;

import com.communi.suggestu.scena.core.network.INetworkChannel;
import mod.chiselsandbits.network.packets.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Our wrapper for Forge network layer
 */
public class NetworkChannel
{
    private static final String LATEST_PROTO_VER = "1.0";
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
            LATEST_PROTO_VER
          );
    }

    /**
     * Registers all common messages.
     */
    public void registerCommonMessages()
    {
        register(HeldToolModeChangedPacket.TYPE, HeldToolModeChangedPacket.streamCodec(HeldToolModeChangedPacket::new));
        register(UpdateBlockEntityPacket.TYPE, UpdateBlockEntityPacket.streamCodec(UpdateBlockEntityPacket::new));
        register(BagGuiPacket.TYPE, BagGuiPacket.streamCodec(BagGuiPacket::new));
        register(BagGuiStackPacket.TYPE, BagGuiStackPacket.streamCodec(BagGuiStackPacket::new));
        register(ClearBagGuiPacket.TYPE, ClearBagGuiPacket.streamCodec(ClearBagGuiPacket::new));
        register(OpenBagGuiPacket.TYPE, OpenBagGuiPacket.streamCodec(OpenBagGuiPacket::new));
        register(SortBagGuiPacket.TYPE, SortBagGuiPacket.streamCodec(SortBagGuiPacket::new));
        register(ConvertBagGuiPacket.TYPE, ConvertBagGuiPacket.streamCodec(ConvertBagGuiPacket::new));
        register(UpdateBagModesPacket.TYPE, UpdateBagModesPacket.streamCodec(UpdateBagModesPacket::new));
        register(MeasurementUpdatedPacket.TYPE, MeasurementUpdatedPacket.streamCodec(MeasurementUpdatedPacket::new));
        register(MeasurementsUpdatedPacket.TYPE, MeasurementsUpdatedPacket.streamCodec(MeasurementsUpdatedPacket::new));
        register(MeasurementsResetPacket.TYPE, MeasurementsResetPacket.streamCodec(MeasurementsResetPacket::new));
        register(NeighborBlockUpdatedPacket.TYPE, NeighborBlockUpdatedPacket.streamCodec(NeighborBlockUpdatedPacket::new));
        register(ChangeTrackerUpdatedPacket.TYPE, ChangeTrackerUpdatedPacket.streamCodec(ChangeTrackerUpdatedPacket::new));
        register(RequestChangeTrackerOperationPacket.TYPE, RequestChangeTrackerOperationPacket.streamCodec(RequestChangeTrackerOperationPacket::new));
        register(ClearChangeTrackerPacket.TYPE, ClearChangeTrackerPacket.streamCodec(ClearChangeTrackerPacket::new));
        register(InputTrackerStatusUpdatePacket.TYPE, InputTrackerStatusUpdatePacket.streamCodec(InputTrackerStatusUpdatePacket::new));
        register(AddMultiStateItemStackToClipboardPacket.TYPE, AddMultiStateItemStackToClipboardPacket.streamCodec(AddMultiStateItemStackToClipboardPacket::new));
        register(ExportPatternCommandMessagePacket.TYPE, ExportPatternCommandMessagePacket.streamCodec(ExportPatternCommandMessagePacket::new));
        register(ImportPatternCommandMessagePacket.TYPE, ImportPatternCommandMessagePacket.streamCodec(ImportPatternCommandMessagePacket::new));
        register(GivePlayerPatternCommandPacket.TYPE, GivePlayerPatternCommandPacket.streamCodec(GivePlayerPatternCommandPacket::new));
        register(ModificationRecipesUpdated.TYPE, ModificationRecipesUpdated.streamCodec(ModificationRecipesUpdated::new));
    }

    private <T extends ModPacket> void register(CustomPacketPayload.Type<T> type, StreamCodec<RegistryFriendlyByteBuf, T> codec)
    {
        rawChannel.register(
                type,
                codec,
                (packet, serverSide, player, consumer) -> consumer.accept(() -> packet.processPacket(player, serverSide))
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

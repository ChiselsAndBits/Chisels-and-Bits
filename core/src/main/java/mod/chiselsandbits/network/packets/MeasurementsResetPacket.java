package mod.chiselsandbits.network.packets;

import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.measures.MeasuringManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class MeasurementsResetPacket extends ModPacket
{

    public static final Identifier                                        ID   = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "measurements_reset");
    public static final CustomPacketPayload.Type<MeasurementsResetPacket> TYPE = new CustomPacketPayload.Type<>(ID);

    public MeasurementsResetPacket()
    {
    }

    public MeasurementsResetPacket(final RegistryFriendlyByteBuf buffer)
    {
        readPayload(buffer);
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
        MeasuringManager.getInstance().resetMeasurementsFor(playerEntity);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

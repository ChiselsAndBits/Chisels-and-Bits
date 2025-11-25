package mod.chiselsandbits.network.packets;

import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.measures.Measurement;
import mod.chiselsandbits.measures.MeasuringManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public final class MeasurementUpdatedPacket extends ModPacket
{

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "measurement_updated");
    public static final CustomPacketPayload.Type<MeasurementUpdatedPacket> TYPE = new CustomPacketPayload.Type<>(ID);

    private Measurement measurement;

    public MeasurementUpdatedPacket(final Measurement measurement)
    {
        this.measurement = measurement;
    }

    public MeasurementUpdatedPacket(final RegistryFriendlyByteBuf buffer)
    {
        readPayload(buffer);
    }

    @Override
    public void writePayload(final RegistryFriendlyByteBuf buffer)
    {
        Measurement.STREAM_CODEC.encode(buffer, measurement);
    }

    @Override
    public void readPayload(final RegistryFriendlyByteBuf buffer)
    {
        this.measurement = Measurement.STREAM_CODEC.decode(buffer);
    }

    @Override
    public void server(final ServerPlayer playerEntity)
    {
        MeasuringManager.getInstance().addOrUpdate(measurement, true);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

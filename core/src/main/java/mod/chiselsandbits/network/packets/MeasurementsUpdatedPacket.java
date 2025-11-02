package mod.chiselsandbits.network.packets;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import mod.chiselsandbits.measures.MeasuringMode;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.measures.Measurement;
import mod.chiselsandbits.measures.MeasuringManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

public final class MeasurementsUpdatedPacket extends ModPacket
{

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "measurements_updated");
    public static final CustomPacketPayload.Type<MeasurementsUpdatedPacket> TYPE = new CustomPacketPayload.Type<>(ID);

    private Table<ResourceLocation, UUID, Map<MeasuringMode, Measurement>> measurements;

    public MeasurementsUpdatedPacket(final RegistryFriendlyByteBuf buffer)
    {
        readPayload(buffer);
    }

    public MeasurementsUpdatedPacket(final Table<ResourceLocation, UUID, Map<MeasuringMode, Measurement>> measurements)
    {
        this.measurements = measurements;
    }

    @Override
    public void writePayload(final RegistryFriendlyByteBuf buffer)
    {
        buffer.writeMap(
                measurements.rowMap(),
                FriendlyByteBuf::writeResourceLocation,
                (buffer5, value) -> buffer5.writeMap(
                        value,
                        (buffer4, value4) -> buffer4.writeUUID(value4),
                        (buffer3, value3) -> buffer3.writeMap(
                                value3,
                                FriendlyByteBuf::writeEnum,
                                Measurement.STREAM_CODEC
                        )
                )
        );
    }

    @Override
    public void readPayload(final RegistryFriendlyByteBuf buffer)
    {
        final Map<ResourceLocation, Map<UUID, Map<MeasuringMode, Measurement>>> bufferData = buffer.readMap(
                FriendlyByteBuf::readResourceLocation,
                buffer2 -> buffer2.readMap(
                        buffer1 -> buffer1.readUUID(),
                        buffer1 -> buffer1.readMap(
                                buffer3 -> buffer.readEnum(MeasuringMode.class),
                                Measurement.STREAM_CODEC
                        )
                )
        );

        this.measurements = HashBasedTable.create();
        bufferData.forEach((key, value) -> value.forEach((key2, value2) -> measurements.put(key, key2, value2)));
    }

    @Override
    public void server(final ServerPlayer playerEntity)
    {
        //Noop
    }

    @Override
    public void client()
    {
        MeasuringManager.getInstance().updateMeasurements(measurements);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

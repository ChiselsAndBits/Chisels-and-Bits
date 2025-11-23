package mod.chiselsandbits.measures;

import mod.chiselsandbits.ChiselsAndBits;
import mod.chiselsandbits.network.packets.MeasurementUpdatedPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class MeasurementNetworkUtil
{

    private MeasurementNetworkUtil()
    {
        throw new IllegalStateException("Can not instantiate an instance of: MeasurementNetworkUtil. This is a utility class");
    }

    public static void createAndSend(
      final Vec3 from, final Vec3 to, final Direction hitFace, final MeasuringMode mode
    ) {
        if (Minecraft.getInstance().level == null || Minecraft.getInstance().player == null)
        {
            return;
        }

        final Measurement measurement = MeasuringManager.getInstance().create(
          Minecraft.getInstance().level,
          Minecraft.getInstance().player,
          from,
          to,
          hitFace,
          mode
        );

        MeasuringManager.getInstance().addOrUpdate(measurement);

        final MeasurementUpdatedPacket packet = new MeasurementUpdatedPacket(measurement);

        ChiselsAndBits.getInstance().getNetworkChannel().sendToServer(packet);
    }
}

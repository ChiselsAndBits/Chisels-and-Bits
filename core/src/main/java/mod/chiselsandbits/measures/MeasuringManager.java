package mod.chiselsandbits.measures;

import com.communi.suggestu.scena.core.dist.Dist;
import com.communi.suggestu.scena.core.dist.DistExecutor;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import mod.chiselsandbits.ChiselsAndBits;
import mod.chiselsandbits.api.measuring.IMeasurement;
import mod.chiselsandbits.api.measuring.IMeasuringManager;
import mod.chiselsandbits.api.measuring.IMeasuringMode;
import mod.chiselsandbits.network.packets.MeasurementsUpdatedPacket;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public class MeasuringManager implements IMeasuringManager
{

    private static final MeasuringManager INSTANCE = new MeasuringManager();

    public static MeasuringManager getInstance()
    {
        return INSTANCE;
    }

    private final Table<ResourceLocation, UUID, Map<MeasuringMode, Measurement>> measurements = Tables.newCustomTable(
            new ConcurrentHashMap<>(),
            ConcurrentHashMap::new
    );

    private MeasuringManager()
    {
    }

    @Override
    public Collection<? extends IMeasurement> getInWorld(final ResourceLocation worldKey)
    {
        return measurements.row(worldKey).values().stream().flatMap(m -> m.values().stream()).collect(Collectors.toSet());
    }

    @Override
    public Collection<? extends IMeasurement> getForPlayer(final UUID playerId)
    {
        return measurements.column(playerId).values().stream().flatMap(m -> m.values().stream()).collect(Collectors.toSet());
    }

    @Override
    public Measurement create(
      final Level world, final Player playerEntity, final Vec3 from, final Vec3 to, final Direction hitFace, final IMeasuringMode mode)
    {
        return new Measurement(
          playerEntity.getUUID(),
          from,
          to,
          hitFace,
          mode,
          world.dimension().location()
        );
    }

    @Override
    public void resetMeasurementsFor(final UUID playerId)
    {
        measurements.columnMap().remove(playerId);
        syncToAll();
    }

    public void syncToAll()
    {
        ChiselsAndBits.getInstance().getNetworkChannel().sendToEveryone(
          new MeasurementsUpdatedPacket(this.measurements)
        );
    }

    public void syncTo(final ServerPlayer player)
    {
        ChiselsAndBits.getInstance().getNetworkChannel().sendToPlayer(
          new MeasurementsUpdatedPacket(this.measurements),
          player
        );
    }

    public void addOrUpdate(final Measurement measurement, final boolean updateAll)
    {
        if (!this.measurements.contains(measurement.getWorldKey(), measurement.getOwner()))
            this.measurements.put(measurement.getWorldKey(), measurement.getOwner(), new HashMap<>());

        Objects.requireNonNull(this.measurements.get(measurement.getWorldKey(), measurement.getOwner())).put(measurement.getMode(), measurement);

        if (updateAll)
        {
            this.syncToAll();
        }
    }

    public void createAndSend(
      final Vec3 from, final Vec3 to, final Direction hitFace, final MeasuringMode mode
    ) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MeasurementNetworkUtil.createAndSend(from, to, hitFace, mode));
    }

    public void updateMeasurements(Table<ResourceLocation, UUID, Map<MeasuringMode, Measurement>> measurements) {
        this.measurements.clear();
        this.measurements.putAll(measurements);
    }
}

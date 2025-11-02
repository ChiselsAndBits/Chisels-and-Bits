package mod.chiselsandbits.measures;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mod.chiselsandbits.api.measuring.IMeasurement;
import mod.chiselsandbits.api.measuring.IMeasuringMode;
import mod.chiselsandbits.api.serialization.CBStreamCodecs;
import mod.chiselsandbits.api.serialization.Serializable;
import mod.chiselsandbits.api.util.constants.NbtConstants;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class Measurement implements IMeasurement, Serializable<Measurement, FriendlyByteBuf>
{

    public static final Codec<Measurement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      UUIDUtil.CODEC.fieldOf(NbtConstants.OWNER).forGetter(Measurement::getOwner),
      Vec3.CODEC.fieldOf(NbtConstants.FROM).forGetter(Measurement::getFrom),
      Vec3.CODEC.fieldOf(NbtConstants.TO).forGetter(Measurement::getTo),
      MeasuringMode.CODEC.fieldOf(NbtConstants.MODE).forGetter(Measurement::getMode),
      ResourceLocation.CODEC.fieldOf(NbtConstants.LEVEL).forGetter(Measurement::getWorldKey)
    ).apply(instance, Measurement::new));

    public static final MapCodec<Measurement> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
      UUIDUtil.CODEC.fieldOf(NbtConstants.OWNER).forGetter(Measurement::getOwner),
      Vec3.CODEC.fieldOf(NbtConstants.FROM).forGetter(Measurement::getFrom),
      Vec3.CODEC.fieldOf(NbtConstants.TO).forGetter(Measurement::getTo),
      MeasuringMode.CODEC.fieldOf(NbtConstants.MODE).forGetter(Measurement::getMode),
      ResourceLocation.CODEC.fieldOf(NbtConstants.LEVEL).forGetter(Measurement::getWorldKey)
    ).apply(instance, Measurement::new));

    public static final StreamCodec<FriendlyByteBuf, Measurement> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            Measurement::getOwner,
            CBStreamCodecs.VEC_3,
            Measurement::getFrom,
            CBStreamCodecs.VEC_3,
            Measurement::getTo,
            MeasuringMode.STREAM_CODEC,
            Measurement::getMode,
            ResourceLocation.STREAM_CODEC,
            Measurement::getWorldKey,
            Measurement::new
    );

    private UUID owner;
    private Vec3 from;
    private Vec3         to;
    private MeasuringMode    mode;
    private ResourceLocation worldKey;

    private Measurement(UUID owner, Vec3 from, Vec3 to, IMeasuringMode mode, ResourceLocation worldKey) {
        this.owner = owner;
        this.from = from;
        this.to = to;
        this.mode = mode instanceof MeasuringMode m ? m : null;
        this.worldKey = worldKey;

        if (this.mode == null)
            throw new IllegalStateException("The measuring mode for now needs to be a built in mode!");
    }

    public Measurement(final UUID owner, final Vec3 from, final Vec3 to, final Direction hitFace, final IMeasuringMode mode, final ResourceLocation worldKey) {
        this.owner = owner;
        this.mode = mode instanceof MeasuringMode m ? m : null;
        this.worldKey = worldKey;

        if (this.mode == null)
            throw new IllegalStateException("The measuring mode for now needs to be a built in mode!");

        adaptPositions(from, to, hitFace, mode);
    }

    public Measurement()
    {
    }

    private void adaptPositions(final Vec3 from, final Vec3 to, final Direction hitFace, final IMeasuringMode mode)
    {
        this.from = mode.getType().isNeedsNormalization() ? new Vec3(
          Math.min(from.x(), to.x()),
          Math.min(from.y(), to.y()),
          Math.min(from.z(), to.z())
        ) : from;

        this.to = mode.getType().isNeedsNormalization() ? new Vec3(
          Math.max(from.x(), to.x()),
          Math.max(from.y(), to.y()),
          Math.max(from.z(), to.z())
        ) : to;

        this.from = mode.getType().adaptStartCorner(this.from, this.to, hitFace);
        this.to = mode.getType().adaptEndCorner(this.from, this.to, hitFace);
    }

    @Override
    public UUID getOwner()
    {
        return owner;
    }

    @Override
    public Vec3 getFrom()
    {
        return from;
    }

    @Override
    public Vec3 getTo()
    {
        return to;
    }

    @Override
    public MeasuringMode getMode()
    {
        return mode;
    }

    @Override
    public ResourceLocation getWorldKey()
    {
        return worldKey;
    }

    @Override
    public Codec<Measurement> codec() {
        return CODEC;
    }

    @Override
    public MapCodec<Measurement> mapCodec() {
        return MAP_CODEC;
    }

    @Override
    public StreamCodec<FriendlyByteBuf, Measurement> streamCodec() {
        return STREAM_CODEC;
    }
}

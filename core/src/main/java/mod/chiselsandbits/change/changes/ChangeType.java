package mod.chiselsandbits.change.changes;

import com.mojang.serialization.MapCodec;
import mod.chiselsandbits.api.change.changes.IChange;
import mod.chiselsandbits.api.change.changes.IChangeType;
import mod.chiselsandbits.api.util.constants.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public enum ChangeType implements IChangeType {

    BIT(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "bit"), BitChange.MAP_CODEC, BitChange.STREAM_CODEC),
    COMBINED(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "combined"), CombinedChange.MAP_CODEC, CombinedChange.STREAM_CODEC);

    private final Identifier                                              registryName;
    private final MapCodec<? extends IChange>                             codec;
    private final StreamCodec<RegistryFriendlyByteBuf, ? extends IChange> streamCodec;

    ChangeType(Identifier registryName, MapCodec<? extends IChange> codec, StreamCodec<RegistryFriendlyByteBuf, ? extends IChange> streamCodec) {
        this.registryName = registryName;
        this.codec = codec;
        this.streamCodec = streamCodec;
    }

    @Override
    public Identifier getRegistryName() {
        return registryName;
    }

    @Override
    public MapCodec<? extends IChange> codec() {
        return codec;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, ? extends IChange> streamCodec() {
        return streamCodec;
    }
}

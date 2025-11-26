package mod.chiselsandbits.multistate.snapshot;

import com.mojang.serialization.MapCodec;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshot;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshotType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public enum MultiStateSnapshotTypes implements IMultiStateSnapshotType {
    EMPTY(Identifier.fromNamespaceAndPath("chiselsandbits", "empty"), MapCodec.unit(EmptySnapshot.INSTANCE), StreamCodec.unit(EmptySnapshot.INSTANCE)),
    MULTI_BLOCK(Identifier.fromNamespaceAndPath("chiselsandbits", "multi_block"), MultiBlockMultiStateSnapshot.MAP_CODEC, MultiBlockMultiStateSnapshot.STREAM_CODEC),
    SIMPLE(Identifier.fromNamespaceAndPath("chiselsandbits", "simple"), SimpleSnapshot.MAP_CODEC, SimpleSnapshot.STREAM_CODEC);

    private final Identifier                                                          registryName;
    private final MapCodec<? extends IMultiStateSnapshot>                             codec;
    private final StreamCodec<RegistryFriendlyByteBuf, ? extends IMultiStateSnapshot> streamCodec;

    MultiStateSnapshotTypes(Identifier registryName, MapCodec<? extends IMultiStateSnapshot> codec, StreamCodec<RegistryFriendlyByteBuf, ? extends IMultiStateSnapshot> streamCodec) {
        this.registryName = registryName;
        this.codec = codec;
        this.streamCodec = streamCodec;
    }

    @Override
    public Identifier getRegistryName() {
        return registryName;
    }

    @Override
    public MapCodec<? extends IMultiStateSnapshot> codec() {
        return codec;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, ? extends IMultiStateSnapshot> streamCodec() {
        return streamCodec;
    }
}

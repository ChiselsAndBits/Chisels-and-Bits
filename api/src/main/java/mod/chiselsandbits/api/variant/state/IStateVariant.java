package mod.chiselsandbits.api.variant.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import mod.chiselsandbits.api.util.ISnapshotable;
import mod.chiselsandbits.api.serialization.Serializable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Object which provides additional information about a state.
 * <p>
 *     This has to be immutable as it is used in data components in the game.
 */
public interface IStateVariant extends Comparable<IStateVariant>, ISnapshotable<IStateVariant> {
    /**
     * The codec that can be used to serialize a state variant.
     */
    Codec<IStateVariant> CODEC = IStateVariantManager.getInstance().byNameCodec()
            .dispatch(IStateVariant::provider, IStateVariantProvider::mapCodec);

    /**
     * The map codec that can be used to serialize a state variant.
     */
    MapCodec<IStateVariant> MAP_CODEC = IStateVariantManager.getInstance().byNameCodec()
            .dispatchMap(IStateVariant::provider, IStateVariantProvider::mapCodec);

    /**
     * The stream codec that can be used to serialize a state variant.
     */
    StreamCodec<RegistryFriendlyByteBuf, IStateVariant> STREAM_CODEC = IStateVariantManager.getInstance().byNameStreamCodec()
            .dispatch(IStateVariant::provider, IStateVariantProvider::streamCodec);


    /**
     * {@return The provider that created this state variant.}
     */
    IStateVariantProvider provider();

    /**
     * Updates a block entity with the correct state of this variant.
     * <p>
     *     By default, this does nothing, however a particular implementation can decide to properly instantiate it so that
     *     the model extraction system can provide other minecraft subsystems, like coloring, the correct information needed
     *     for this variant.
     * </p>
     */
    default void updateBlockEntity(BlockEntity blockEntity) {}
}

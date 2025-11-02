package mod.chiselsandbits.api.blockinformation;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mod.chiselsandbits.api.block.state.id.IBlockStateIdManager;
import mod.chiselsandbits.api.util.BlockStateSerializationUtils;
import mod.chiselsandbits.api.util.ComparatorUtils;
import mod.chiselsandbits.api.util.ISnapshotable;
import mod.chiselsandbits.api.serialization.Serializable;
import mod.chiselsandbits.api.util.constants.NbtConstants;
import mod.chiselsandbits.api.variant.state.IStateVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Entity;

import java.util.Comparator;
import java.util.Optional;

public record BlockInformation(BlockState blockState,
                               Optional<IStateVariant> variant) implements Serializable.Registry<BlockInformation>, ISnapshotable<BlockInformation>, Comparable<BlockInformation> {

    public static final Codec<BlockInformation> LEGACY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockStateSerializationUtils.LEGACY_BLOCK_STATE_CODEC.fieldOf(NbtConstants.STATE).forGetter(BlockInformation::blockState),
            IStateVariant.CODEC.optionalFieldOf(NbtConstants.VARIANT).forGetter(BlockInformation::variant)
    ).apply(instance, BlockInformation::new));

    public static final Codec<BlockInformation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockState.CODEC.fieldOf(NbtConstants.STATE).forGetter(BlockInformation::blockState),
            IStateVariant.CODEC.optionalFieldOf(NbtConstants.VARIANT).forGetter(BlockInformation::variant)
    ).apply(instance, BlockInformation::new));

    public static final MapCodec<BlockInformation> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockState.CODEC.fieldOf(NbtConstants.STATE).forGetter(BlockInformation::blockState),
            IStateVariant.CODEC.optionalFieldOf(NbtConstants.VARIANT).forGetter(BlockInformation::variant)
    ).apply(instance, BlockInformation::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BlockInformation> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY),
            BlockInformation::blockState,
            ByteBufCodecs.optional(IStateVariant.STREAM_CODEC),
            BlockInformation::variant,
            BlockInformation::new
    );
    public static final BlockInformation AIR = new BlockInformation(Blocks.AIR.defaultBlockState(), Optional.empty());

    private static final Comparator<BlockState> STATE_COMPARATOR = Comparator.comparing(
            IBlockStateIdManager.getInstance()::getIdFrom
    );
    private static final Comparator<IStateVariant> VARIANT_COMPARATOR = Comparator.<IStateVariant, String>comparing(
            variant -> variant.getClass().getName()
    ).thenComparing(Comparator.naturalOrder());
    private static final Comparator<Optional<IStateVariant>> OPTIONAL_VARIANT_COMPARATOR = ComparatorUtils.createOptionalComparator(
            VARIANT_COMPARATOR
    );
    private static final Comparator<BlockInformation> COMPARATOR = Comparator.comparing(BlockInformation::blockState, STATE_COMPARATOR)
            .thenComparing(BlockInformation::variant, OPTIONAL_VARIANT_COMPARATOR);

    public boolean isFluid() {
        return blockState().getFluidState().isSource();
    }

    public boolean isAir() {
        return blockState().isAir();
    }

    @Override
    public BlockInformation createSnapshot() {
        return variant.map(stateVariant -> new BlockInformation(blockState, Optional.ofNullable(stateVariant.createSnapshot()))).orElseGet(() -> new BlockInformation(blockState, Optional.empty()));
    }

    @Override
    public int compareTo(@NotNull final BlockInformation blockInformation) {
        return COMPARATOR.compare(this, blockInformation);
    }

    @Override
    public Codec<BlockInformation> codec() {
        return CODEC;
    }

    @Override
    public MapCodec<BlockInformation> mapCodec() {
        return MAP_CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, BlockInformation> streamCodec() {
        return STREAM_CODEC;
    }

    @Nullable
    public BlockEntity newBlockEntityAtZero() {
        return newBlockEntity(BlockPos.ZERO);
    }

    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos) {
        if (!(blockState.getBlock() instanceof EntityBlock entityBlock))
            return null;

        final BlockEntity blockEntity = entityBlock.newBlockEntity(pos, blockState());
        if (blockEntity == null)
            return null;

        variant().ifPresent(variant -> variant.updateBlockEntity(blockEntity));

        return blockEntity;
    }
}

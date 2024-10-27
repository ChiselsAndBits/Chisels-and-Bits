package mod.chiselsandbits.blockinformation;

import com.mojang.serialization.DataResult;
import mod.chiselsandbits.api.block.state.id.IBlockStateIdManager;
import mod.chiselsandbits.api.blockinformation.IBlockInformation;
import mod.chiselsandbits.api.util.*;
import mod.chiselsandbits.api.util.constants.NbtConstants;
import mod.chiselsandbits.api.variant.state.IStateVariant;
import mod.chiselsandbits.api.variant.state.IStateVariantManager;
import mod.chiselsandbits.stateinfo.additional.StateVariantManager;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

public final class BlockInformation implements IBlockInformation {

    private static final Comparator<BlockState> STATE_COMPARATOR = Comparator.comparing(
            IBlockStateIdManager.getInstance()::getIdFrom
    );
    private static final Comparator<IStateVariant> VARIANT_COMPARATOR = Comparator.<IStateVariant, String>comparing(
            variant -> variant.getClass().getName()
    ).thenComparing(Comparator.naturalOrder());
    private static final Comparator<Optional<IStateVariant>> OPTIONAL_VARIANT_COMPARATOR = ComparatorUtils.createOptionalComparator(
            VARIANT_COMPARATOR
    );
    private static final Comparator<BlockInformation> COMPARATOR = Comparator.comparing(BlockInformation::getBlockState, STATE_COMPARATOR)
            .thenComparing(BlockInformation::getVariant, OPTIONAL_VARIANT_COMPARATOR);

    private BlockState blockState;
    private Optional<IStateVariant> variant;

    public BlockInformation(CompoundTag tag) {
        deserializeNBT(tag);
    }

    public BlockInformation(final FriendlyByteBuf packetBuffer) {
        deserializeFrom(packetBuffer);
    }

    public BlockInformation(BlockState blockState, Optional<IStateVariant> variant) {
        this.blockState = blockState;
        this.variant = variant;
    }

    @Override
    public CompoundTag serializeNBT() {
        final CompoundTag tag = new CompoundTag();
        tag.putString(NbtConstants.STATE, BlockStateSerializationUtils.serialize(getBlockState()));
        variant.ifPresent((info) -> tag.put(
                NbtConstants.VARIANT,
                IStateVariantManager.getInstance().serializeNBT(info))
        );
        return tag;
    }

    @Override
    public void deserializeNBT(final CompoundTag nbt) {
        DataResult<BlockState> dataResult = BlockStateSerializationUtils.deserialize(nbt.getString(NbtConstants.STATE));
        this.blockState = dataResult.result().orElseGet(Blocks.AIR::defaultBlockState);

        variant = Optional.empty();
        if (nbt.contains(NbtConstants.VARIANT)) {
            variant = Optional.of(IStateVariantManager.getInstance().deserializeNBT(nbt.getCompound(NbtConstants.VARIANT)));
        }
    }

    @Override
    public void serializeInto(final @NotNull FriendlyByteBuf packetBuffer) {
        BlockStateSerializationUtils.serialize(packetBuffer, getBlockState());
        packetBuffer.writeBoolean(variant.isPresent());
        variant.ifPresent((info) -> IStateVariantManager.getInstance().serializeInto(packetBuffer, info));
    }

    @Override
    public void deserializeFrom(final @NotNull FriendlyByteBuf packetBuffer) {
        blockState = BlockStateSerializationUtils.deserialize(packetBuffer);

        variant = Optional.empty();
        if (packetBuffer.readBoolean()) {
            variant = Optional.of(IStateVariantManager.getInstance().deserializeFrom(packetBuffer));
        }
    }

    @Override
    public BlockState getBlockState() {
        return blockState;
    }

    @Override
    public Optional<IStateVariant> getVariant() {
        return StateVariantManager.getInstance().unwrapVariant(variant);
    }

    @Override
    public boolean isFluid() {
        return getBlockState().getFluidState().isSource();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BlockInformation) obj;
        return Objects.equals(this.blockState, that.blockState) &&
                Objects.equals(this.variant, that.variant);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockState, variant);
    }

    @Override
    public String toString() {
        return "BlockInformation[" +
                "blockState=" + blockState + ", " +
                "variant=" + variant + ']';
    }

    @Override
    public boolean isAir() {
        return getBlockState().isAir();
    }

    @Override
    public boolean isSeeThrough() {
        return isAir() || isFluid() || !ItemBlockRenderTypes.getChunkRenderType(getBlockState()).equals(RenderType.solid());
    }

    @Override
    public BlockInformation createSnapshot() {
        return variant.map(stateVariant -> new BlockInformation(blockState, Optional.ofNullable(stateVariant.createSnapshot()))).orElseGet(() -> new BlockInformation(blockState, Optional.empty()));

    }

    @Override
    public int compareTo(@NotNull final IBlockInformation o) {
        if (!(o instanceof BlockInformation blockInformation)) {
            return -1;
        }

        return COMPARATOR.compare(this, blockInformation);
    }
}

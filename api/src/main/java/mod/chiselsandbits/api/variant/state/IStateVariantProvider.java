package mod.chiselsandbits.api.variant.state;

import com.communi.suggestu.scena.core.fluid.FluidInformation;
import com.mojang.serialization.MapCodec;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;

/**
 * Represents the state variant provider, which allows external mods to provide their own state variants.
 */
public interface IStateVariantProvider
{
    /**
     * Returns the registry name of the provider.
     *
     * @return The registry name.
     */
    Identifier getRegistryName();

    /**
     * Returns the state variant, if one exists, for the blockstate and block entity combination.
     *
     * @param blockState The block state.
     * @param blockEntity The block entity.
     * @return The state variant, if one exists.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    Optional<IStateVariant> getStateVariant(BlockState blockState, Optional<BlockEntity> blockEntity);

    /**
     * Returns the state variant, if one exists, for the fluid state.
     *
     * @param fluidStateIn The fluid state.
     * @return The state variant, if one exists.
     */
    Optional<IStateVariant> getStateVariant(FluidState fluidStateIn);

    /**
     * Returns the state variant, if one exists, for the block state and item stack combination.
     *
     * @param blockState The block state.
     * @param itemStack The item stack.
     * @return The state variant, if one exists.
     */
    Optional<IStateVariant> getStateVariant(final BlockState blockState, ItemStack itemStack);

    /**
     * Returns the state variant, if one exists, for the fluid information.
     *
     * @param fluidInformation The fluid information.
     * @return The state variant, if one exists.
     */
    Optional<IStateVariant> getStateVariant(FluidInformation fluidInformation);

    /**
     * Returns all the default variants for the given block state.
     *
     * @param state The block state.
     * @return The default variants.
     */
    Collection<? extends IStateVariant> getAllDefaultVariants(final BlockState state);

    /**
     * Returns the item stack for the given state variant.
     *
     * @param stateVariant The state variant.
     * @return The item stack.
     */
    Optional<ItemStack> getItemStack(BlockInformation stateVariant);

    /**
     * Returns the fluid information for the given state variant.
     *
     * @param stateVariant The state variant.
     * @param amount The amount of fluid.
     * @return The fluid information.
     */
    Optional<FluidInformation> getFluidInformation(IStateVariant stateVariant, long amount);

    /**
     * Returns the name of the given state variant.
     *
     * @param variant The state variant to get the name for.
     * @return The name of the variant if present.
     */
    Optional<Component> getName(BlockInformation variant);

    /**
     * Returns the map codec for the state variant.
     *
     * @return The map codec.
     */
    MapCodec<? extends IStateVariant> mapCodec();

    /**
     * Returns the stream codec for the state variant.
     *
     * @return The stream codec.
     */
    StreamCodec<? super RegistryFriendlyByteBuf, ? extends IStateVariant> streamCodec();

    /**
     * Sets the full block at the given position.
     *
     * @param levelAccessor The level accessor.
     * @param inWorldPos The position in the world.
     * @param blockInformation The block information.
     */
    void setFullBlock(LevelAccessor levelAccessor, BlockPos inWorldPos, BlockInformation blockInformation);

    /**
     * Returns the beacon color multiplier for the given block information.
     *
     * @param blockInformation The block information.
     * @param levelReader The level reader.
     * @param pos The position.
     * @param beaconPos The beacon position.
     * @return The beacon color multiplier.
     */
    @NotNull Optional<Integer> getBeaconColorMultiplier(BlockInformation blockInformation, LevelReader levelReader, BlockPos pos, BlockPos beaconPos);
}

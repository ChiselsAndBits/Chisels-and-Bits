package mod.chiselsandbits.stateinfo.additional;

import com.communi.suggestu.scena.core.blockstate.ILevelBasedPropertyAccessor;
import com.communi.suggestu.scena.core.fluid.FluidInformation;
import com.communi.suggestu.scena.core.util.SingleBlockLevelReader;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.variant.state.IStateVariant;
import mod.chiselsandbits.api.variant.state.IStateVariantManager;
import mod.chiselsandbits.api.variant.state.IStateVariantProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class StateVariantManager implements IStateVariantManager
{
    private static final StateVariantManager INSTANCE = new StateVariantManager();

    public static StateVariantManager getInstance()
    {
        return INSTANCE;
    }

    private final Map<Supplier<Block>, IStateVariantProvider> preBakeProviders = new ConcurrentHashMap<>();
    private final Map<Block, IStateVariantProvider>      providers        = new ConcurrentHashMap<>();
    private final Map<Identifier, IStateVariantProvider> providersByNames = new ConcurrentHashMap<>();

    private StateVariantManager()
    {
    }

    @Override
    public Codec<IStateVariantProvider> byNameCodec() {
        return Identifier.CODEC.comapFlatMap(
            name -> Optional.ofNullable(providersByNames.get(name)).map(DataResult::success).orElse(DataResult.error(() -> "Unknown registry name: " + name)),
                IStateVariantProvider::getRegistryName
        );
    }

    @Override
    public StreamCodec<@NotNull RegistryFriendlyByteBuf, @NotNull IStateVariantProvider> byNameStreamCodec() {
        return new StreamCodec<>() {
            @Override
            public @NotNull IStateVariantProvider decode(@NotNull RegistryFriendlyByteBuf buffer) {
                return providersByNames.get(buffer.readIdentifier());
            }

            @Override
            public void encode(@NotNull RegistryFriendlyByteBuf buffer, @NotNull IStateVariantProvider value) {
                buffer.writeIdentifier(value.getRegistryName());
            }
        };
    }

    @SafeVarargs
    @Override
    public final IStateVariantProvider registerProvider(IStateVariantProvider provider, Supplier<Block>... block) {
        if (providersByNames.containsKey(provider.getRegistryName()))
            throw new IllegalStateException("Provider with name " + provider.getRegistryName() + " already exists!");

        providersByNames.put(provider.getRegistryName(), provider);
        for (Supplier<Block> blockSupplier : block)
            preBakeProviders.put(blockSupplier, provider);
        return provider;
    }

    private void bakeProviders() {
        if (!providers.isEmpty())
            return;

        preBakeProviders.forEach((block, provider) -> providers.put(block.get(), provider));
        preBakeProviders.clear();
    }

    @Override
    public Optional<IStateVariant> getStateVariant(
      final BlockState blockState, final Optional<BlockEntity> blockEntity)
    {
        bakeProviders();
        if (!providers.containsKey(blockState.getBlock()))
            return Optional.empty();

        return Optional.of(providers.get(blockState.getBlock())).flatMap(provider -> provider.getStateVariant(blockState, blockEntity));
    }

    @Override
    public Optional<IStateVariant> getStateVariant(final FluidState fluidStateIn)
    {
        bakeProviders();
        if (!providers.containsKey(fluidStateIn.createLegacyBlock().getBlock()))
            return Optional.empty();

        return Optional.of(providers.get(fluidStateIn.createLegacyBlock().getBlock())).flatMap(provider -> provider.getStateVariant(fluidStateIn));
    }

    @Override
    public Optional<IStateVariant> getStateVariant(final BlockState blockState, final ItemStack itemStack)
    {
        bakeProviders();
        if (!providers.containsKey(blockState.getBlock()))
            return Optional.empty();

        return Optional.of(providers.get(blockState.getBlock())).flatMap(provider -> provider.getStateVariant(blockState, itemStack));
    }

    @Override
    public Optional<IStateVariant> getStateVariant(final FluidInformation fluidInformation)
    {
        bakeProviders();
        if (!providers.containsKey(fluidInformation.fluid().defaultFluidState().createLegacyBlock().getBlock()))
            return Optional.empty();

        return Optional.of(providers.get(fluidInformation.fluid().defaultFluidState().createLegacyBlock().getBlock()))
          .flatMap(provider -> provider.getStateVariant(fluidInformation));
    }

    @Override
    public Collection<BlockInformation> getAllDefaultVariants(final BlockState state)
    {
        bakeProviders();
        if (!providers.containsKey(state.getBlock()))
            return Collections.emptyList();

        return Optional.of(providers.get(state.getBlock())).map(provider -> provider.getAllDefaultVariants(state))
                 .stream()
                 .flatMap(Collection::stream)
                 .map(variant -> new BlockInformation(state, Optional.of(variant)))
                 .collect(Collectors.toSet());
    }

    @Override
    public Optional<ItemStack> getItemStack(final BlockInformation blockInformation)
    {
        bakeProviders();
        if (!providers.containsKey(blockInformation.blockState().getBlock()))
            return Optional.empty();

        return blockInformation.variant().flatMap(stateVariant -> providers.get(blockInformation.blockState().getBlock()).getItemStack(blockInformation));
    }

    @Override
    public Optional<FluidInformation> getFluidInformation(final BlockInformation blockInformation, final long amount)
    {
        bakeProviders();
        if (!providers.containsKey(blockInformation.blockState().getBlock()))
            return Optional.empty();

        return blockInformation.variant().flatMap(variant -> providers.get(blockInformation.blockState().getBlock()).getFluidInformation(variant, amount));
    }

    @Override
    public Optional<Component> getName(BlockInformation blockInformation) {
        bakeProviders();
        if (!providers.containsKey(blockInformation.blockState().getBlock()))
            return Optional.empty();

        return blockInformation.variant().flatMap(variant -> providers.get(blockInformation.blockState().getBlock()).getName(blockInformation));
    }

    @Override
    public void setFullBlock(LevelAccessor levelAccessor, BlockPos inWorldPos, BlockInformation primaryState) {
        bakeProviders();
        if (primaryState.variant().isEmpty()) {
            levelAccessor.setBlock(
                    inWorldPos,
                    primaryState.blockState(),
                    Block.UPDATE_ALL
            );
            return;
        }

        primaryState.variant().ifPresent(variant -> providers.get(primaryState.blockState().getBlock()).setFullBlock(levelAccessor, inWorldPos, primaryState));
    }

    @Override
    public Optional<Integer> getBeaconColorMultiplier(BlockInformation blockInformation, LevelReader levelReader, BlockPos pos, BlockPos beaconPos) {
        bakeProviders();

        if (!providers.containsKey(blockInformation.blockState().getBlock()))
        {
            return Optional.ofNullable(ILevelBasedPropertyAccessor.getInstance().getBeaconColorMultiplier(
                new SingleBlockLevelReader.Builder()
                    .withBlockState(blockInformation.blockState())
                    .withBlockEntity(() -> blockInformation.newBlockEntity(pos))
                    .withPos(pos)
                    .withSource(levelReader)
                    .createSingleBlockLevelReader(),
                    pos,
                    beaconPos
            ));
        }

        return blockInformation.variant().flatMap(variant -> providers.get(blockInformation.blockState().getBlock()).getBeaconColorMultiplier(blockInformation, levelReader, pos, beaconPos));
    }
}

package mod.chiselsandbits.client.variant.state;

import com.communi.suggestu.scena.core.client.models.data.IBlockModelData;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.client.variant.state.IClientStateVariantManager;
import mod.chiselsandbits.api.client.variant.state.IClientStateVariantProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ClientStateVariantManager implements IClientStateVariantManager {
    private static final ClientStateVariantManager INSTANCE = new ClientStateVariantManager();

    public static ClientStateVariantManager getInstance() {
        return INSTANCE;
    }

    private final Map<Supplier<Block>, IClientStateVariantProvider> preBakeProviders = new ConcurrentHashMap<>();
    private final Map<Block, IClientStateVariantProvider> providers = new ConcurrentHashMap<>();

    private ClientStateVariantManager() {
    }


    @Override
    public IClientStateVariantManager registerStateVariantProvider(Supplier<Block> block, IClientStateVariantProvider provider) {
        preBakeProviders.put(block, provider);
        return this;
    }

    private void bakeProviders() {
        if (!providers.isEmpty())
            return;

        preBakeProviders.forEach((block, provider) -> providers.put(block.get(), provider));
        preBakeProviders.clear();
    }

    @Override
    public IBlockModelData getBlockModelData(BlockInformation blockInformation) {
        bakeProviders();
        if (blockInformation.variant().isEmpty() || !providers.containsKey(blockInformation.blockState().getBlock()))
            return IBlockModelData.empty();

        return providers.get(blockInformation.blockState().getBlock()).getBlockModelData(blockInformation.variant().get());
    }

    @Override
    public void appendHoverText(
        final BlockInformation blockInformation,
        final Item.TooltipContext context,
        final TooltipDisplay tooltipDisplay,
        final Consumer<Component> tooltipAdder,
        final TooltipFlag flag)
    {
        bakeProviders();
        if (blockInformation.variant().isEmpty() || !providers.containsKey(blockInformation.blockState().getBlock()))
            return;

        final List<Component> variantLines = new ArrayList<>();
        blockInformation.variant().ifPresent(variant -> {
            providers.get(blockInformation.blockState().getBlock()).appendHoverText(variant, context, variantLines, flag);
        });

        if (!variantLines.isEmpty()) {
            tooltipAdder.accept(Component.literal(""));
            variantLines.forEach(tooltipAdder);
        }
    }
}

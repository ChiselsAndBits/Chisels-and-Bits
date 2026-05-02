package mod.chiselsandbits.client.model.item;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.item.bit.IBitItem;
import mod.chiselsandbits.api.variant.state.IStateVariantManager;
import mod.chiselsandbits.client.model.builder.BitBlockModelInformationBuilder;
import mod.chiselsandbits.client.model.information.BitBlockModelInformation;
import mod.chiselsandbits.client.model.parts.BitBlockModelPart;
import mod.chiselsandbits.client.time.TickHandler;
import mod.chiselsandbits.client.util.ItemModelUtils;
import mod.chiselsandbits.registrars.ModCreativeTabs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LiquidBlock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class BitBlockBakedModelManager
{
    private static final Logger                                                   LOGGER            = LogManager.getLogger();
    private static final BitBlockBakedModelManager                                INSTANCE          = new BitBlockBakedModelManager();
    private final        Cache<BlockInformation, BitBlockModelInformation> modelCache        = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES).build();
    private final        Cache<BlockInformation, BitBlockModelInformation> largeModelCache   = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES).build();
    private final        NonNullList<ItemStack>                                   alternativeStacks = NonNullList.create();

    private BitBlockBakedModelManager()
    {
    }

    public static BitBlockBakedModelManager getInstance()
    {
        return INSTANCE;
    }

    public void clearCache()
    {
        modelCache.asMap().clear();
        largeModelCache.asMap().clear();
    }

    public BitBlockModelInformation get(
        ItemStack stack,
        final ClientLevel world)
    {
        return get(
            stack,
            world,
            Minecraft.getInstance().hasShiftDown()
        );
    }

    public BitBlockModelInformation get(
        ItemStack stack,
        final ClientLevel world,
        final boolean large
    )
    {
        if (!(stack.getItem() instanceof IBitItem))
        {
            LOGGER.warn("Tried to get bit item model for non bit item");
            return BitBlockModelInformation.EMPTY;
        }

        return get(
            large,
            ((IBitItem) stack.getItem()).getBlockInformation(stack),
            world
        );
    }

    public BitBlockModelInformation get(
        final boolean large,
        @Nullable BlockInformation blockInformation,
        ClientLevel level)
    {
        if (level == null)
        {
            level = Minecraft.getInstance().level;

            if (level == null)
            {
                return BitBlockModelInformation.EMPTY;
            }
        }

        if (blockInformation == null || blockInformation.isAir())
        {
            if (alternativeStacks.isEmpty())
            {
                ModCreativeTabs.BITS.get().buildContents(new CreativeModeTab.ItemDisplayParameters(FeatureFlags.VANILLA_SET, false, level.registryAccess()));
                this.alternativeStacks.addAll(ModCreativeTabs.BITS.get().getDisplayItems());
            }

            final int alternativeIndex = (int) ((Math.floor(TickHandler.getClientTicks() / 20d)) % alternativeStacks.size());

            final ItemStack alternativeStack = this.alternativeStacks.get(alternativeIndex);
            if (!(alternativeStack.getItem() instanceof IBitItem))
            {
                throw new IllegalStateException("BitItem returned none bit item stack!");
            }

            blockInformation = ((IBitItem) alternativeStack.getItem()).getBlockInformation(alternativeStack);
        }

        final Cache<BlockInformation, BitBlockModelInformation> target = large ? largeModelCache : modelCache;
        final BlockInformation workingState = blockInformation;
        try
        {
            final @Nullable BlockInformation finalBlockInformation = blockInformation;
            final ClientLevel finalLevel = level;
            return target.get(blockInformation, () -> {
                if (large)
                {
                    boolean isBlock = true;
                    ItemStack lookupStack = IStateVariantManager.getInstance().getItemStack(workingState).orElseGet(
                        () -> new ItemStack(workingState.blockState().getBlock())
                    );
                    if (workingState.blockState().getBlock() instanceof LiquidBlock)
                    {
                        lookupStack = new ItemStack(workingState.blockState().getFluidState().getType().getBucket());
                        isBlock = false;
                    }

                    final ItemModel model = Minecraft.getInstance().getModelManager().getItemModel(
                        Objects.requireNonNull(lookupStack.get(DataComponents.ITEM_MODEL))
                    );

                    final ItemStackRenderState renderState = ItemModelUtils.update(lookupStack, model, ItemDisplayContext.GUI, null, null, 42);
                    final List<BitBlockModelPart> parts = new ArrayList<>();

                    for (final ItemStackRenderState.LayerRenderState layer : renderState.layers)
                    {
                        parts.add(
                            new BitBlockModelPart(
                                layer.prepareQuadList(),
                                layer.tintLayers()
                            )
                        );
                    }

                    return new BitBlockModelInformation(parts, isBlock, true);
                }
                else
                {
                    return new BitBlockModelInformationBuilder(finalBlockInformation, false).build(finalLevel);
                }
            });
        }
        catch (ExecutionException e)
        {
            LOGGER.warn("Failed to get a model for a bit: {} the model calculation got aborted.", blockInformation, e);
            return BitBlockModelInformation.EMPTY;
        }
    }
}

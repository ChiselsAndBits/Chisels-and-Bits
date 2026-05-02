package mod.chiselsandbits.client.model.block;

import com.communi.suggestu.scena.core.client.models.IModelManager;
import com.communi.suggestu.scena.core.util.SingleBlockBlockAndTintGetter;
import mod.chiselsandbits.api.config.IClientConfiguration;
import mod.chiselsandbits.api.multistate.accessor.ISingleBlockAxisAlignedAreaAccessor;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshot;
import mod.chiselsandbits.api.neighborhood.IBlockNeighborhood;
import mod.chiselsandbits.api.profiling.IProfilerSection;
import mod.chiselsandbits.client.model.builder.ChiseledBlockModelInformationBuilder;
import mod.chiselsandbits.client.model.information.ChiseledBlockModelCacheKey;
import mod.chiselsandbits.client.model.information.ChiseledBlockModelInformation;
import mod.chiselsandbits.components.data.MultiStateItemStackData;
import mod.chiselsandbits.profiling.ProfilingManager;
import mod.chiselsandbits.registrars.ModDataComponentTypes;
import mod.chiselsandbits.utils.SimpleMaxSizedCache;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ChiseledBlockStateModelManager
{
    private static final ChiseledBlockStateModelManager INSTANCE = new ChiseledBlockStateModelManager();

    private final SimpleMaxSizedCache<ChiseledBlockModelCacheKey, ChiseledBlockModelInformation> cache = new SimpleMaxSizedCache<>(
        () -> IClientConfiguration.getInstance().getModelCacheSize().get()
    );

    private ChiseledBlockStateModelManager()
    {
    }

    public static ChiseledBlockStateModelManager getInstance()
    {
        return INSTANCE;
    }

    public void clearCache()
    {
        cache.clear();
    }

    public ChiseledBlockModelInformation get(
        ItemStack stack
    ) {
        final MultiStateItemStackData data = stack.get(ModDataComponentTypes.MULTI_STATE_ITEM_STACK_DATA.get());
        if (data == null)
            return ChiseledBlockModelInformation.EMPTY;

        final IMultiStateSnapshot multiStateSnapshot = data.asSnapshot();
        if (!(multiStateSnapshot instanceof ISingleBlockAxisAlignedAreaAccessor singleBlockSnapshot))
            return ChiseledBlockModelInformation.EMPTY;

        final BlockAndTintGetter blockAndTintGetter = new SingleBlockBlockAndTintGetter.Builder()
            .withBlockState(Blocks.AIR.defaultBlockState())
            .createSingleBlockBlockAndTintGetter();

        return get(
            blockAndTintGetter,
            BlockPos.ZERO,
            singleBlockSnapshot,
            IBlockNeighborhood.EMPTY
        );
    }

    public ChiseledBlockModelInformation get(
        final BlockAndTintGetter blockAndTintGetter,
        final BlockPos blockPos,
        final ISingleBlockAxisAlignedAreaAccessor accessor,
        final IBlockNeighborhood blockNeighborhood
    )
    {
        try (IProfilerSection ignored1 = ProfilingManager.getInstance().withSection("Block based chiseled block model"))
        {
            if (accessor.getStatistics().getPrimaryState().isAir())
            {
                return ChiseledBlockModelInformation.EMPTY;
            }

            final List<@Nullable Object> cacheKeys = accessor.getStatistics().getContainedStates()
                .stream()
                .map(state -> IModelManager.getInstance().determineModelCacheKey(
                    state.blockState(),
                    () -> state.newBlockEntity(blockPos),
                    blockAndTintGetter,
                    blockPos
                ))
                .toList();

            final ChiseledBlockModelCacheKey key = new ChiseledBlockModelCacheKey(
                accessor.createNewShapeIdentifier(),
                accessor.getStatistics().getPrimaryState(),
                blockNeighborhood,
                cacheKeys
            );

            return cache.get(key,
                () -> {
                    try (IProfilerSection ignored3 = ProfilingManager.getInstance().withSection("Cache mis"))
                    {
                        return new ChiseledBlockModelInformationBuilder(
                            accessor,
                            key,
                            blockAndTintGetter,
                            blockPos
                        ).build();
                    }
                });
        }
    }
}

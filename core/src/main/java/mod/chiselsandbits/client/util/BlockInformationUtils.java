package mod.chiselsandbits.client.util;

import com.communi.suggestu.scena.core.client.rendering.type.IRenderTypeManager;
import com.communi.suggestu.scena.core.client.utils.RenderTypeUtils;
import com.communi.suggestu.scena.core.util.SingleBlockLevelReader;
import com.google.common.collect.Sets;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.ThreadSafeLegacyRandomSource;

import java.util.Set;

@SuppressWarnings("deprecation")
public final class BlockInformationUtils {

    private static final RandomSource RANDOM = new ThreadSafeLegacyRandomSource(42);

    private BlockInformationUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: IMultiStateObjectStatisticsUtils. This is a utility class");
    }

    public static Set<RenderType> extractRenderTypes(BlockInformation blockInformation) {
        return extractRenderTypes(Sets.newHashSet(blockInformation), Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FABULOUS);
    }

    public static Set<RenderType> extractRenderTypes(Set<BlockInformation> blockInformation) {
        return extractRenderTypes(blockInformation, Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FABULOUS);
    }

    public static Set<RenderType> extractRenderTypes(Set<BlockInformation> blocks, boolean entity) {
        final Set<RenderType> renderTypes = Sets.newHashSet();
        for (BlockInformation blockInformation : blocks) {
            if (blockInformation.isAir())
                continue;

            if (blockInformation.isFluid()) {
                final ChunkSectionLayer chunkSectionLayer = ItemBlockRenderTypes.getRenderLayer(blockInformation.blockState().getFluidState());
                final RenderType renderType = RenderTypeUtils.renderTypeFor(chunkSectionLayer);
                if (!entity || renderType != RenderType.translucentMovingBlock())
                    renderTypes.add(renderType);
                else
                    renderTypes.add(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));

                continue;
            }

            IRenderTypeManager.getInstance().getRenderTypesFor(
                    new SingleBlockLevelReader.Builder()
                        .withBlockState(blockInformation.blockState())
                        .withBlockEntity(blockInformation::newBlockEntityAtZero)
                        .createSingleBlockLevelReader(),
                    blockInformation::newBlockEntityAtZero,
                    BlockPos.ZERO,
                    blockInformation.blockState()
                ).stream()
                .map(RenderTypeUtils::renderTypeFor)
                .forEach(renderTypes::add);
        }

        return renderTypes;
    }
}

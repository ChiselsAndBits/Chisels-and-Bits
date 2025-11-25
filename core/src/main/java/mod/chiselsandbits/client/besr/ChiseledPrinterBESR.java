package mod.chiselsandbits.client.besr;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.chiselsandbits.api.config.IClientConfiguration;
import mod.chiselsandbits.api.multistate.accessor.identifier.IAreaShapeIdentifier;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshot;
import mod.chiselsandbits.block.entities.ChiseledPrinterBlockEntity;
import mod.chiselsandbits.utils.SimpleMaxSizedCache;
import mod.chiselsandbits.client.util.ItemModelUtils;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChiseledPrinterBESR implements BlockEntityRenderer<ChiseledPrinterBlockEntity, ChiseledPrinterBESR.RenderState> {
    private static final SimpleMaxSizedCache<CacheKey, IMultiStateSnapshot> STORAGE_CONTENTS_BLOB_CACHE = new SimpleMaxSizedCache<>(IClientConfiguration.getInstance().getPrinterContentCacheSize()::get);

    public ChiseledPrinterBESR() {
    }

    public static void clearCache() {
        STORAGE_CONTENTS_BLOB_CACHE.clear();
    }

    @Override
    public @NotNull RenderState createRenderState()
    {
        return new RenderState();
    }

    @Override
    public void extractRenderState(
        final @NotNull ChiseledPrinterBlockEntity blockEntity,
        final @NotNull RenderState renderState,
        final float partialTick,
        final @NotNull Vec3 cameraPosition,
        @Nullable final ModelFeatureRenderer.CrumblingOverlay breakProgress)
    {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPosition, breakProgress);
        renderState.progress = blockEntity.getProgress();
        renderState.snapshot = blockEntity.getRealisedPattern();
    }

    @Override
    public void submit(final @NotNull RenderState renderState, final @NotNull PoseStack poseStack, final @NotNull SubmitNodeCollector nodeCollector, final @NotNull CameraRenderState cameraRenderState)
    {
        if (renderState.snapshot.getStatics().isEmpty())
            return;

        final var shapeKey = renderState.snapshot.createNewShapeIdentifier();
        final var cacheKey = new CacheKey(shapeKey, renderState.progress);
        IMultiStateSnapshot innerModelBlob = STORAGE_CONTENTS_BLOB_CACHE.get(cacheKey);
        if (innerModelBlob == null) {
            innerModelBlob = renderState.snapshot.limitedToProgression(renderState.progress / 100f);
            STORAGE_CONTENTS_BLOB_CACHE.put(cacheKey, innerModelBlob);
        }

        final ItemStack modelStack = innerModelBlob.toItemStack().toBlockStack();

        poseStack.pushPose();
        poseStack.translate(8 / 16f, 4 / 16f, 8 / 16f);
        poseStack.scale(19 / 16f, 19 / 16f, 19 / 16f);

        ItemModelUtils.render(
            modelStack,
            nodeCollector,
            poseStack,
            renderState.lightCoords,
            OverlayTexture.NO_OVERLAY,
            0,
            ItemDisplayContext.GROUND,
            null,
            null,
            42
        );

        poseStack.popPose();
    }

    private record CacheKey(IAreaShapeIdentifier identifier, int progress) {}

    public static final class RenderState extends BlockEntityRenderState {
        private int progress = 0;
        private IMultiStateSnapshot snapshot;
    }
}

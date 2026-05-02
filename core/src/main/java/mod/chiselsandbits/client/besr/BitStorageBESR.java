package mod.chiselsandbits.client.besr;

import com.communi.suggestu.scena.core.fluid.FluidInformation;
import com.communi.suggestu.scena.core.fluid.IFluidManager;
import com.communi.suggestu.scena.core.util.SingleBlockBlockAndTintGetter;
import com.mojang.blaze3d.vertex.PoseStack;
import mod.chiselsandbits.api.block.storage.StateEntryStorage;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.config.IClientConfiguration;
import mod.chiselsandbits.api.neighborhood.IBlockNeighborhood;
import mod.chiselsandbits.block.entities.BitStorageBlockEntity;
import mod.chiselsandbits.client.model.block.ChiseledBlockStateModelManager;
import mod.chiselsandbits.client.model.information.ChiseledBlockModelInformation;
import mod.chiselsandbits.client.util.FluidCuboidUtils;
import mod.chiselsandbits.multistate.snapshot.SimpleSnapshot;
import mod.chiselsandbits.utils.SimpleMaxSizedCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class BitStorageBESR implements BlockEntityRenderer<BitStorageBlockEntity, BitStorageBESR.RenderState>
{
    private static final SimpleMaxSizedCache<CacheKey, StateEntryStorage> STORAGE_CONTENTS_BLOB_CACHE =
        new SimpleMaxSizedCache<>(IClientConfiguration.getInstance().getBitStorageContentCacheSize()::get);
    private static final RandomSource                                     RANDOM_SOURCE               = RandomSource.createThreadLocalInstance(42L);

    public BitStorageBESR()
    {
    }

    public static void clearCache()
    {
        STORAGE_CONTENTS_BLOB_CACHE.clear();
    }

    @Override
    public @NotNull RenderState createRenderState()
    {
        return new RenderState();
    }

    @Override
    public void extractRenderState(
        final @NotNull BitStorageBlockEntity blockEntity,
        final @NotNull RenderState renderState,
        final float partialTick,
        final @NotNull Vec3 cameraPosition,
        @Nullable final ModelFeatureRenderer.CrumblingOverlay breakProgress)
    {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPosition, breakProgress);
        renderState.containedFluid = blockEntity.getFluid().orElse(null);
        renderState.bitCount = blockEntity.getBits();
        renderState.blockInformation = blockEntity.getContainedBlockInformation();
    }

    @Override
    public void submit(
        final @NotNull RenderState renderState,
        final @NotNull PoseStack poseStack,
        final @NotNull SubmitNodeCollector nodeCollector,
        final @NotNull CameraRenderState cameraRenderState)
    {
        if (renderState.containedFluid != null)
        {
            final FluidInformation fluidStack = renderState.containedFluid;

            final float fullness = (float) fluidStack.amount() / (float) IFluidManager.getInstance().getBucketAmount();
            final float heightFactor = Math.clamp(fullness, 0, 1f);

            FluidCuboidUtils.renderScaledFluidCuboid(
                fluidStack,
                poseStack,
                nodeCollector,
                renderState.lightCoords,
                OverlayTexture.NO_OVERLAY,
                1,
                1,
                1,
                15,
                15 * heightFactor,
                15
            );
            return;
        }

        final int bits = renderState.bitCount;
        final BlockInformation blockInformation = renderState.blockInformation;
        if (bits <= 0 || blockInformation == null)
        {
            return;
        }

        final CacheKey cacheKey = new CacheKey(blockInformation, bits);
        StateEntryStorage innerModelBlob = STORAGE_CONTENTS_BLOB_CACHE.get(cacheKey);
        if (innerModelBlob == null)
        {
            innerModelBlob = new StateEntryStorage();

            innerModelBlob.fillFromBottom(blockInformation, bits);
            STORAGE_CONTENTS_BLOB_CACHE.put(cacheKey, innerModelBlob);
        }

        poseStack.pushPose();
        poseStack.translate(2 / 16f, 2 / 16f, 2 / 16f);
        poseStack.scale(12 / 16f, 12 / 16f, 12 / 16f);
        final StateEntryStorage finalInnerModelBlob = innerModelBlob;
        final BlockAndTintGetter blockAndTintGetter = new SingleBlockBlockAndTintGetter.Builder()
            .withBlockState(blockInformation.blockState())
            .withBlockEntity(blockInformation::newBlockEntityAtZero)
            .createSingleBlockBlockAndTintGetter();

        final ChiseledBlockModelInformation model =
            ChiseledBlockStateModelManager.getInstance().get(
                blockAndTintGetter,
                BlockPos.ZERO,
                new SimpleSnapshot(finalInnerModelBlob),
                IBlockNeighborhood.EMPTY
            );

        float r;
        float g;
        float b;

        final BlockTintSource source = Minecraft.getInstance().getBlockColors().getTintSource(blockInformation.blockState(), 0);
        final int color = source != null ? source.colorInWorld(
            blockInformation.blockState(),
            blockAndTintGetter,
            BlockPos.ZERO
        ) : Integer.MAX_VALUE;
        r = ((color >> 16) & 0xff) / 255f;
        g = ((color >> 8) & 0xff) / 255f;
        b = (color & 0xff) / 255f;

        var tintColor = ARGB.color((int) (r * 255f), (int) (g * 255f), (int) (b * 255f));

        var parts = new ArrayList<BlockStateModelPart>();
        model.collectParts(
            RANDOM_SOURCE,
            parts
        );

        nodeCollector.submitBlockModel(
            poseStack,
            Sheets.translucentBlockSheet(),
            parts,
            new int[] {tintColor},
            renderState.lightCoords,
            OverlayTexture.NO_OVERLAY,
            0);

        poseStack.popPose();
    }

    private record CacheKey(
        BlockInformation blockInformation,
        int bitCount) {}

    public static final class RenderState extends BlockEntityRenderState
    {
        @Nullable
        private FluidInformation containedFluid = null;
        private int              bitCount = 0;
        private BlockInformation blockInformation = BlockInformation.AIR;
    }
}

package mod.chiselsandbits.client.chiseling.preview.render;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.chiselsandbits.api.chiseling.IChiselingContext;
import mod.chiselsandbits.api.client.render.preview.chiseling.IChiselContextPreviewRenderer;
import mod.chiselsandbits.api.util.constants.Constants;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.resources.ResourceLocation;

public class NoopChiselContextPreviewRenderer implements IChiselContextPreviewRenderer
{
    static ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "noop");

    @Override
    public ResourceLocation getId()
    {
        return ID;
    }

    @Override
    public void renderExistingContextsBoundingBox(
        final LevelRenderer levelRenderer,
        final PoseStack matrixStack,
        final MultiBufferSource.BufferSource bufferSource,
        final boolean translucentPass,
        final LevelRenderState levelRenderState,
        final float partialTicks,
        final IChiselingContext currentContextSnapshot)
    {
        //Some people do not want this, so we have this.
    }
}

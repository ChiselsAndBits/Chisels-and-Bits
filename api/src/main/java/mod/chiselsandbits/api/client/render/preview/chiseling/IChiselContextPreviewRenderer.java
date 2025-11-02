package mod.chiselsandbits.api.client.render.preview.chiseling;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.chiselsandbits.api.chiseling.IChiselingContext;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer which is used to render the chiseling preview.
 */
public interface IChiselContextPreviewRenderer
{

    /**
     * The id of teh renderer.
     * Used to give the player a selection option for the preview renderer.
     *
     * @return The id of the preview renderer.
     */
    ResourceLocation getId();

    /**
     * Invoked by the engine to render previews of the given {@link IChiselingContext}.
     *
     * @param levelRenderer          The level renderer in which the bounding box is being rendered.
     * @param matrixStack            The matrix stack to render into.
     * @param bufferSource           The buffer source to get the outline vertex consumers for-
     * @param translucentPass        Whether we are rendering translucent object elements or not.
     * @param levelRenderState       The current level render state
     * @param partialTicks           The partial ticks
     * @param currentContextSnapshot The current snapshot to render.
     */
    void renderExistingContextsBoundingBox(
        final LevelRenderer levelRenderer,
        final PoseStack matrixStack,
        final MultiBufferSource.BufferSource bufferSource,
        final boolean translucentPass,
        final LevelRenderState levelRenderState,
        final float partialTicks,
        final IChiselingContext currentContextSnapshot);
}

package mod.chiselsandbits.api.item.withhighlight;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Indicates an item that is used to draw custom highlights,
 * using custom logic instead of the default one.
 */
public interface IWithHighlightItem
{
    /**
     * Indicates if the given player is allowed to use the
     * default render method.
     *
     * If this is false. The default highlight rendering is cancelled.
     *
     * @param playerEntity The player entity in question.
     *
     * @return True to let minecraft handle the highlight rendering, false when not.
     */
    boolean shouldDrawDefaultHighlight(@NotNull final Player playerEntity);

    /**
     * Renders the highlight for the current item.
     *
     * @param playerEntity     The player entity in question.
     * @param worldRenderer    The rendering world renderer.
     * @param matrixStack      The matrix stack used to render the world.
     * @param bufferSource     The buffer source to get the relevant vertex consumer from.
     * @param translucentPass  Whether we are rendering a translucent pass.
     * @param levelRenderState The current levels render state.
     * @param partialTicks     The partial ticks used for animations.
     */
    void renderHighlight(Player playerEntity, LevelRenderer worldRenderer, PoseStack matrixStack,
        final MultiBufferSource.BufferSource bufferSource,
        final boolean translucentPass,
        final LevelRenderState levelRenderState,
        float partialTicks);
}

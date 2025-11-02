package mod.chiselsandbits.client.logic;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.chiselsandbits.client.render.MeasurementRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.LevelRenderState;

public class MeasurementsRenderHandler
{

    public static void renderMeasurements(
        final LevelRenderer levelRenderer,
        final PoseStack poseStack,
        final MultiBufferSource.BufferSource bufferSource,
        final boolean translucentPass,
        final LevelRenderState levelRenderState,
        final float partialTickTime)
    {
        if (Minecraft.getInstance().player != null && !Minecraft.getInstance().player.isSpectator())
            MeasurementRenderer.getInstance().renderMeasurements(
                levelRenderer,
                poseStack,
                bufferSource,
                translucentPass,
                levelRenderState,
                partialTickTime
            );
    }

}

package mod.chiselsandbits.client.logic;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.chiselsandbits.client.render.MeasurementRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;

public class MeasurementsRenderHandler
{

    public static void renderMeasurements(
        final PoseStack poseStack,
        final MultiBufferSource.BufferSource bufferSource)
    {
        if (Minecraft.getInstance().player != null && !Minecraft.getInstance().player.isSpectator())
            MeasurementRenderer.getInstance().renderMeasurements(
                poseStack,
                bufferSource
            );
    }

}

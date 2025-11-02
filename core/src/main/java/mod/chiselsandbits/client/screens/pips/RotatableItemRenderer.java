package mod.chiselsandbits.client.screens.pips;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Quaternionf;

import javax.annotation.Nullable;

public class RotatableItemRenderer extends PictureInPictureRenderer<RotatableItemRenderer.RenderState>
{

    private final SubmitNodeCollector     submitNodeCollector;
    private final FeatureRenderDispatcher featureRenderDispatcher;
    @Nullable
    private Object lastModelIdentity = null;
    @Nullable
    private Quaternionf lastRotY  = null;

    public RotatableItemRenderer(MultiBufferSource.BufferSource bufferSource)
    {
        super(bufferSource);
        this.submitNodeCollector = Minecraft.getInstance().gameRenderer.getSubmitNodeStorage();
        this.featureRenderDispatcher = Minecraft.getInstance().gameRenderer.getFeatureRenderDispatcher();
    }

    @Override
    protected void renderToTexture(RenderState state, PoseStack poseStack)
    {
        TrackingItemStackRenderState renderState = state.renderState;

        poseStack.scale(1, -1, -1);
        poseStack.mulPose(state.rotation);

        Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_FLAT);
        renderState.submit(poseStack, submitNodeCollector, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        featureRenderDispatcher.renderAllFeatures();

        lastModelIdentity = renderState.getModelIdentity();
        lastRotY = state.rotation;
    }

    @Override
    protected float getTranslateY(int height, int guiScale)
    {
        return height / 2F;
    }

    @Override
    protected boolean textureIsReadyToBlit(RenderState state)
    {
        if (state.rotation != lastRotY) { return false; }

        TrackingItemStackRenderState renderState = state.renderState;
        return !renderState.isAnimated() && renderState.getModelIdentity().equals(lastModelIdentity);
    }

    @Override
    protected String getTextureLabel()
    {
        return "c&B rotatable item renderer";
    }

    @Override
    public Class<RenderState> getRenderStateClass()
    {
        return RenderState.class;
    }

    public record RenderState(
        TrackingItemStackRenderState renderState,
        Quaternionf rotation,
        int x0,
        int y0,
        int x1,
        int y1,
        float scale,
        @Nullable ScreenRectangle bounds,
        @Nullable ScreenRectangle scissorArea
    ) implements PictureInPictureRenderState
    {
        public RenderState(
            TrackingItemStackRenderState renderState,
            Quaternionf rotation,
            int x0,
            int y0,
            int x1,
            int y1,
            float scale,
            @Nullable ScreenRectangle scissorArea
        )
        {
            this(renderState, rotation, x0, y0, x1, y1, scale, PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea), scissorArea);
        }
    }
}


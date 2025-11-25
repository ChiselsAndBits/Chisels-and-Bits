package mod.chiselsandbits.client.screens.pips;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

import javax.annotation.Nullable;

public class RotatableItemRenderer extends PictureInPictureRenderer<RotatableItemRenderer.RenderState>
{
    @Nullable
    private ItemStack lastModelIdentity = null;
    @Nullable
    private Quaternionf lastRotY  = null;

    public RotatableItemRenderer(MultiBufferSource.BufferSource bufferSource)
    {
        super(bufferSource);
    }

    @Override
    protected void renderToTexture(RenderState state, PoseStack poseStack)
    {
        final var submitNodeCollector = Minecraft.getInstance().gameRenderer.getSubmitNodeStorage();
        final var featureRenderDispatcher = Minecraft.getInstance().gameRenderer.getFeatureRenderDispatcher();

        poseStack.scale(1, -1, -1);
        poseStack.mulPose(state.rotation);

        Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_FLAT);

        final ItemStackRenderState renderState = new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(
            renderState,
            state.stack,
            ItemDisplayContext.GUI,
            null,
            null,
            42
        );

        renderState.submit(
            poseStack,
            submitNodeCollector,
            LightTexture.FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY,
            0
        );

        featureRenderDispatcher.renderAllFeatures();

        lastModelIdentity = state.stack;
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
        if (state.rotation != lastRotY) return false;
        if (lastModelIdentity == null) return false;

        ItemStack renderState = state.stack;
        return ItemStack.isSameItemSameComponents(renderState, this.lastModelIdentity);
    }

    @Override
    protected @NotNull String getTextureLabel()
    {
        return "c&B rotatable item renderer";
    }

    @Override
    public @NotNull Class<RenderState> getRenderStateClass()
    {
        return RenderState.class;
    }

    public record RenderState(
        ItemStack stack,
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
            ItemStack stack,
            Quaternionf rotation,
            int x0,
            int y0,
            int x1,
            int y1,
            float scale,
            @Nullable ScreenRectangle scissorArea
        )
        {
            this(stack, rotation, x0, y0, x1, y1, scale, PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea), scissorArea);
        }
    }
}


package mod.chiselsandbits.client.screens.pips;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public class Torus extends PictureInPictureRenderer<Torus.RenderState>
{
    private static final float DRAWS = 720;

    public record RenderState(
        float startAngle,
        float sizeAngle,
        float inner,
        float outer,
        int color,
        int x0,
        int y0,
        int x1,
        int y1,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds) implements PictureInPictureRenderState
    {

        public RenderState(
            final float startAngle, final float sizeAngle,
            final float inner, final float outer, final int color,
            final int centerX, final int centerY,
            @Nullable final ScreenRectangle scissorArea)
        {
            this(
                startAngle,sizeAngle,
                inner,outer,color,
                calculateBoundingSquare(centerX, centerY, outer),
                scissorArea
            );
        }

        public RenderState(
            final float startAngle, final float sizeAngle,
            final float inner, final float outer, final int color,
            final ScreenRectangle bounds,
            @Nullable final ScreenRectangle scissorArea)
        {
            this(
                startAngle,
                sizeAngle,
                inner,
                outer,
                color,
                bounds.left(),
                bounds.top(),
                bounds.right(),
                bounds.bottom(),
                scissorArea,
                scissorArea != null ? scissorArea.intersection(bounds) : bounds
            );
        }

        public static ScreenRectangle calculateBoundingSquare(
            int centerX, int centerY,
            float outerRadius
        )
        {
            return new ScreenRectangle(
                (int) (centerX - outerRadius),
                (int) (centerY - outerRadius),
                (int) outerRadius * 2,
                (int) outerRadius * 2
            );
        }

        @Override
        public float scale()
        {
            return 1f;
        }
    }

    public Torus(final MultiBufferSource.BufferSource bufferSource)
    {
        super(bufferSource);
    }

    @Override
    public @NotNull Class<RenderState> getRenderStateClass()
    {
        return Torus.RenderState.class;
    }

    @Override
    protected void renderToTexture(final RenderState renderState, final @NotNull PoseStack poseStack)
    {
        VertexConsumer vertexBuffer = this.bufferSource.getBuffer(RenderTypes.debugFilledBox());
        poseStack.pushPose();
        Matrix4f matrix4f = poseStack.last().pose();
        float draws = DRAWS * (renderState.sizeAngle() / 360F);
        for (int i = 0; i < draws; i++)
        {
            float angle = (float) Math.toRadians(renderState.startAngle() + (i / DRAWS) * 360);
            float endAngle = ((float) Math.toRadians(renderState.startAngle() + ((i + 1) / DRAWS) * 360));
            final float outer = renderState.outer();
            final float inner = renderState.inner();
            vertexBuffer
                .addVertex(matrix4f, (float) ((outer) * Math.cos(angle)), (float) (outer * Math.sin(angle)), 0)
                .setColor(renderState.color());
            vertexBuffer
                .addVertex(matrix4f, (float) (inner * Math.cos(angle)), (float) (inner * Math.sin(angle)), 0)
                .setColor(renderState.color());
            vertexBuffer
                .addVertex(matrix4f, (float) (inner * Math.cos(endAngle)), (float) (inner * Math.sin(endAngle)), 0)
                .setColor(renderState.color());
            vertexBuffer
                .addVertex(matrix4f, (float) ((outer) * Math.cos(endAngle)), (float) (outer * Math.sin(endAngle)), 0)
                .setColor(renderState.color());

        }
        poseStack.popPose();
    }

    @Override
    protected @NotNull String getTextureLabel()
    {
        return "C&B Torus";
    }

    @Override
    protected float getTranslateY(final int height, final int guiScale)
    {
        return (float) height / 2;
    }
}

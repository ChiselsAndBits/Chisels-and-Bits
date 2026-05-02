package mod.chiselsandbits.client.render;

import com.communi.suggestu.scena.core.util.SingleBlockBlockAndTintGetter;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mod.chiselsandbits.api.client.render.preview.placement.PlacementPreviewRenderMode;
import mod.chiselsandbits.api.placement.PlacementResult;
import mod.chiselsandbits.client.model.block.ChiseledBlockStateModelManager;
import mod.chiselsandbits.client.model.information.ChiseledBlockModelInformation;
import mod.chiselsandbits.client.model.parts.ChiseledBlockModelPart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChiseledBlockGhostRenderer
{
    private static final ChiseledBlockGhostRenderer INSTANCE = new ChiseledBlockGhostRenderer();

    public static ChiseledBlockGhostRenderer getInstance()
    {
        return INSTANCE;
    }

    private ChiseledBlockGhostRenderer()
    {
    }

    public void renderGhost(
        final PoseStack poseStack,
        final MultiBufferSource.BufferSource bufferSource,
        final ItemStack renderStack,
        final Vec3 targetedRenderPos,
        final PlacementResult placementResult,
        final PlacementPreviewRenderMode success,
        final PlacementPreviewRenderMode failure,
        final boolean ignoreDepth)
    {
        poseStack.pushPose();

        // Offset/scale by an unnoticeable amount to prevent z-fighting
        final Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().position();
        poseStack.translate(
            targetedRenderPos.x - camera.x - 0.000125,
            targetedRenderPos.y - camera.y + 0.000125,
            targetedRenderPos.z - camera.z - 0.000125
        );
        poseStack.scale(1.001F, 1.001F, 1.001F);

        final Vector4f color = placementResult.getColor();
        final boolean renderColoredGhost = (placementResult.isSuccess() && success.isColoredGhost())
            || (!placementResult.isSuccess() && failure.isColoredGhost());

        final ChiseledBlockModelInformation information = ChiseledBlockStateModelManager
            .getInstance().get(
                renderStack
            );

        renderGhost(
            poseStack,
            bufferSource,
            information,
            renderColoredGhost,
            color,
            targetedRenderPos,
            ignoreDepth
        );

        poseStack.popPose();
    }

    private void renderGhost(
        final PoseStack poseStack,
        final MultiBufferSource.BufferSource bufferSource,
        final ChiseledBlockModelInformation model,
        final boolean renderColoredGhost,
        final Vector4f color,
        final Vec3 targetedRenderPos,
        final boolean ignoreDepth)
    {
        final RenderType renderType;
        if (renderColoredGhost)
        {
            renderType = ignoreDepth
                ? ModRenderTypes.GHOST_BLOCK_COLORED_PREVIEW_ALWAYS.get()
                : ModRenderTypes.GHOST_BLOCK_COLORED_PREVIEW.get();
        }
        else
        {
            renderType = ignoreDepth
                ? ModRenderTypes.GHOST_BLOCK_PREVIEW_GREATER.get()
                : ModRenderTypes.GHOST_BLOCK_PREVIEW.get();
        }

        if (renderColoredGhost)
        {
            renderModelLists(
                model,
                poseStack,
                bufferSource,
                color,
                renderType
            );
        }
        else
        {
            final BlockPos placementPosition = new BlockPos(
                (int) targetedRenderPos.x(),
                (int) targetedRenderPos.y(),
                (int) targetedRenderPos.z()
            );
            final BlockAndTintGetter blockAndTintGetter = new SingleBlockBlockAndTintGetter.Builder()
                .withBlockState(model.key().primaryState().blockState())
                .withBlockEntity(() -> model.key().primaryState().newBlockEntity(placementPosition))
                .withPos(placementPosition)
                .withSource(Minecraft.getInstance().level)
                .createSingleBlockBlockAndTintGetter();

            final BlockQuadOutput output = (x, y, z, quad, instance) -> putBakedQuad(poseStack, bufferSource, x, y, z, quad, instance, color);
            ModelBlockRenderer blockRenderer = new ModelBlockRenderer(
                Minecraft.getInstance().options.ambientOcclusion().get(),
                false,
                Minecraft.getInstance().getBlockColors());

            blockRenderer.tesselateBlock(
                output,
                0, 0, 0,
                blockAndTintGetter,
                placementPosition,
                model.key().primaryState().blockState(),
                model,
                OverlayTexture.NO_OVERLAY);
        }

        bufferSource.endBatch();
    }

    private static void putBakedQuad(
        PoseStack poseStack,
        MultiBufferSource.BufferSource bufferSource,
        float x,
        float y,
        float z,
        BakedQuad quad,
        QuadInstance instance,
        final Vector4f color
    ) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);

        VertexConsumer buffer = new AlphaSettingVertexConsumer(color.w(), bufferSource.getBuffer(RenderTypes.translucentMovingBlock()));
        buffer.putBakedQuad(poseStack.last(), quad, instance);
        poseStack.popPose();
    }

    private record AlphaSettingVertexConsumer(
        int alpha,
        VertexConsumer delegate) implements VertexConsumer
    {
        private AlphaSettingVertexConsumer(final float alpha, final VertexConsumer delegate)
        {
            this((int) (alpha * 255F), delegate);
        }

        @Override
        public @NotNull VertexConsumer addVertex(final float x, final float y, final float z)
        {
            return delegate.addVertex(x, y, z);
        }

        @Override
        public @NotNull VertexConsumer setColor(final int red, final int green, final int blue, final int alpha)
        {
            return delegate.setColor(red, green, blue, this.alpha);
        }

        @Override
        public @NotNull VertexConsumer setColor(final int color)
        {
            //Color is in RGBA format
            //Extract the channels
            final int red   = (color >> 24) & 0xFF;
            final int green = (color >> 16) & 0xFF;
            final int blue  = (color >> 8)  & 0xFF;
            final int alpha = color & 0xFF;

            return delegate.setColor(
                red,
                green,
                blue,
                alpha
            );
        }

        @Override
        public @NotNull VertexConsumer setUv(final float u, final float v)
        {
            return delegate.setUv(u, v);
        }

        @Override
        public @NotNull VertexConsumer setUv1(final int u, final int v)
        {
            return delegate.setUv1(u, v);
        }

        @Override
        public @NotNull VertexConsumer setUv2(final int u, final int v)
        {
            return delegate.setUv2(u, v);
        }

        @Override
        public @NotNull VertexConsumer setNormal(final float normalX, final float normalY, final float normalZ)
        {
            return delegate.setNormal(normalX, normalY, normalZ);
        }

        @Override
        public @NotNull VertexConsumer setLineWidth(final float lineWidth)
        {
            return delegate.setLineWidth(lineWidth);
        }
    }

    private static final float[] DIRECTIONAL_BRIGHTNESS = {0.5f, 1f, 0.7f, 0.7f, 0.6f, 0.6f};

    private static Vector3f[] getShadedColors(final Vector4f color)
    {
        // Directionally shade the color by the amount MC normally does
        return Arrays.stream(Direction.values())
            .map(direction ->
            {
                final float brightness = DIRECTIONAL_BRIGHTNESS[direction.get3DDataValue()];
                return new Vector3f(
                    color.x() * brightness,
                    color.y() * brightness,
                    color.z() * brightness);
            }).toArray(Vector3f[]::new);
    }

    private static Vector3f[] getNormals(final PoseStack.Pose pose)
    {
        // Transform the normal vector of each direction by the pose's normal matrix
        return Arrays.stream(Direction.values())
            .map(direction ->
            {
                final Vec3i faceNormal = direction.getUnitVec3i();
                final Vector3f normal = new Vector3f(faceNormal.getX(), faceNormal.getY(), faceNormal.getZ());
                normal.mul(pose.normal());
                return normal;
            }).toArray(Vector3f[]::new);
    }

    /**
     * Optimized version of ItemRenderer#renderModelLists that ignores textures, and renders a model's
     * quads with a single RGBA color shaded by the quads' direction to match MCs similar shading
     */
    private static void renderModelLists(
        final ChiseledBlockModelInformation model,
        final PoseStack poseStack,
        final MultiBufferSource.BufferSource bufferSource,
        final Vector4f color,
        final RenderType renderType)
    {
        final RandomSource random = RandomSource.create(42);

        // Setup normals and shaded colors for each direction
        final Vector3f[] normals = getNormals(poseStack.last());
        final Vector3f[] shadedColors = getShadedColors(color);

        // Initialize reusable position vector to avoid needless creation of new ones
        final Vector4f pos = new Vector4f();

        for (final ChiseledBlockModelPart part : model.parts())
        {
            for (final Direction direction : Direction.values())
            {
                // Render outer directional quads
                random.setSeed(42L);
                renderQuadList(poseStack.last().pose(),
                    bufferSource.getBuffer(renderType),
                    part.getQuads(direction),
                    normals,
                    shadedColors,
                    pos);
            }

            // Render quads of unspecified direction
            random.setSeed(42L);
            renderQuadList(poseStack.last().pose(),
                bufferSource.getBuffer(renderType),
                part.getQuads(null),
                normals,
                shadedColors,
                pos);
        }
    }

    /**
     * Optimized version of ItemRenderer#renderQuadList
     */
    private static void renderQuadList(
        final Matrix4f pose,
        final VertexConsumer buffer,
        final List<BakedQuad> quads,
        final Vector3f[] normals,
        final Vector3f[] shadedColors,
        final Vector4f pos)
    {
        for (final BakedQuad quad : quads)
        {
            putBulkData(
                buffer,
                pose,
                quad,
                shadedColors[quad.direction().ordinal()],
                normals[quad.direction().ordinal()],
                pos);
        }
    }

    /**
     * Optimized and stripped down version of IForgeVertexConsumer#putBulkData
     */
    private static void putBulkData(
        final VertexConsumer buffer,
        final Matrix4f pose,
        final BakedQuad bakedQuad,
        final Vector3f color,
        final Vector3f normal,
        final Vector4f pos)
    {
        // Get vertex data
        final Vector2f uv = new Vector2f();

        for (int v = 0; v < 4; ++v)
        {
            final var vertexPos = bakedQuad.position(v);
            pos.set(vertexPos.x(), vertexPos.y(),vertexPos.z(), 1f);
            pos.mul(pose);

            // UV is the next 2 Floats (4 bytes each)
            final long packedUv = bakedQuad.packedUV(v);
            uv.set(
                UVPair.unpackU(packedUv),
                UVPair.unpackV(packedUv)
            );

            buffer.addVertex(pos.x(), pos.y(), pos.z())
                .setColor(color.x(), color.y(), color.z(), 1f)
                .setUv(uv.x(), uv.y())
                .setUv1(Short.MAX_VALUE, Short.MAX_VALUE)
                .setUv2(LightCoordsUtil.block(LightCoordsUtil.FULL_BRIGHT), LightCoordsUtil.sky(LightCoordsUtil.FULL_SKY))
                .setNormal(normal.x(), normal.y(), normal.z());
        }
    }
}

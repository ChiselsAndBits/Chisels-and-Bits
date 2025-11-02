package mod.chiselsandbits.client.render;

import com.communi.suggestu.scena.core.client.utils.RenderTypeUtils;
import com.mojang.blaze3d.vertex.*;
import mod.chiselsandbits.api.client.render.preview.placement.PlacementPreviewRenderMode;
import mod.chiselsandbits.api.placement.PlacementResult;
import mod.chiselsandbits.client.model.block.ChiseledBlockStateModelManager;
import mod.chiselsandbits.client.model.information.ChiseledBlockModelInformation;
import mod.chiselsandbits.client.model.parts.ChiseledBlockModelPart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
        final Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
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

        renderModelLists(
            model,
            poseStack,
            bufferSource,
            color
        );

        bufferSource.endBatch();
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
        final Vector4f color)
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
                    bufferSource.getBuffer(Objects.requireNonNull(RenderTypeUtils.renderTypeFor(part.renderType()))),
                    part.getQuads(direction),
                    normals,
                    shadedColors,
                    pos);
            }

            // Render quads of unspecified direction
            random.setSeed(42L);
            renderQuadList(poseStack.last().pose(),
                bufferSource.getBuffer(Objects.requireNonNull(RenderTypeUtils.renderTypeFor(part.renderType()))),
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
        final int[] vertices = bakedQuad.vertices();
        final int vertexCount = vertices.length / (DefaultVertexFormat.BLOCK.getVertexSize() / 4);

        try (final MemoryStack memorystack = MemoryStack.stackPush())
        {
            // Setup buffers
            final ByteBuffer bytebuffer =
                memorystack.malloc(DefaultVertexFormat.BLOCK.getVertexSize()); //Exact amount of bytes in a vertex / 4 * 4 for each vertex results in no further operation compared to the byte count..
            final IntBuffer intbuffer = bytebuffer.asIntBuffer();

            for (int v = 0; v < vertexCount; ++v)
            {
                // Add vertex data to the buffer
                ((Buffer) intbuffer).clear();
                intbuffer.put(vertices, v * 8, 8);

                // Extract relative position, then transform it to the position in the world
                pos.set(bytebuffer.getFloat(0),
                    bytebuffer.getFloat(4),
                    bytebuffer.getFloat(8),
                    1f);
                pos.mul(pose);

                buffer.addVertex(pos.x(), pos.y(), pos.z())
                    .setColor(color.x(), color.y(), color.z(), 1f)
                    .setNormal(normal.x(), normal.y(), normal.z());
            }
        }
    }

    private static class BufferBuilderTransparent extends BufferBuilder
    {
        private float alphaPercentage;

        public BufferBuilderTransparent(ByteBufferBuilder memory, VertexFormat.Mode mode, VertexFormat format)
        {
            super(memory, mode, format);
        }

        public void setAlphaPercentage(final float alphaPercentage)
        {
            this.alphaPercentage = Mth.clamp(alphaPercentage, 0, 1);
        }

        @Override
        public @NotNull VertexConsumer setColor(int red, int green, int blue, int alpha)
        {
            return super.setColor(red, green, blue, (int) (alpha * alphaPercentage));
        }

        @Override
        public @NotNull VertexConsumer setColor(int argb)
        {
            final int newAlpha = (int) ((argb >> 24) * alphaPercentage);
            return super.setColor((newAlpha << 24) | (argb & 0x00ffffff));
        }

        @Override
        public void addVertex(
            float x, float y, float z, int argb, float texU,
            float texV, int overlayUV, int lightmapUV, float normalX, float normalY, float normalZ)
        {
            final int newAlpha = (int) ((argb >> 24) * alphaPercentage);
            super.addVertex(x, y, z, (newAlpha << 24) | (argb & 0x00ffffff), texU, texV, overlayUV, lightmapUV, normalX, normalY, normalZ);
        }

        public void putBulkData(
            PoseStack.Pose pose,
            BakedQuad quad,
            float[] brightness,
            float red,
            float green,
            float blue,
            float alpha,
            int[] lightmap,
            int packedOverlay,
            boolean readAlpha
        )
        {
            super.putBulkData(pose, quad, brightness, red, green, blue, alpha * alphaPercentage, lightmap, packedOverlay, readAlpha);
        }
    }
}

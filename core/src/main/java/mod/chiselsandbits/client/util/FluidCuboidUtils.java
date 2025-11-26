package mod.chiselsandbits.client.util;

import com.communi.suggestu.scena.core.client.fluid.IClientFluidManager;
import com.communi.suggestu.scena.core.client.rendering.IRenderingManager;
import com.communi.suggestu.scena.core.client.rendering.type.IRenderTypeManager;
import com.communi.suggestu.scena.core.fluid.FluidInformation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import static net.minecraft.core.Direction.*;

public class FluidCuboidUtils
{

    private FluidCuboidUtils()
    {
        throw new IllegalStateException("Can not instantiate an instance of: FluidCuboidUtils. This is a utility class");
    }

    /**
     * Renders a fluid block with offset from the matrices and from x1/y1/z1 to x2/y2/z2 using block model coordinates, so from 0-16
     */
    public static void renderScaledFluidCuboid(
      final FluidInformation fluid,
      final PoseStack matrices,
      final SubmitNodeCollector nodeCollector,
      final int combinedLight,
      final int combinedOverlay,
      final float x1,
      final float y1,
      final float z1,
      final float x2,
      final float y2,
      final float z2)
    {
        renderFluidCuboid(fluid, matrices, nodeCollector, combinedLight, combinedOverlay, x1 / 16, y1 / 16, z1 / 16, x2 / 16, y2 / 16, z2 / 16);
    }

    /**
     * Renders a fluid block with offset from the matrices and from x1/y1/z1 to x2/y2/z2 inside the block local coordinates, so from 0-1
     */
    public static void renderFluidCuboid(
      FluidInformation fluid,
      PoseStack matrices,
      SubmitNodeCollector nodeCollector,
      int combinedLight,
      final int combinedOverlay,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2)
    {
        int color = IClientFluidManager.getInstance().getFluidColor(fluid);
        renderFluidCuboid(fluid, matrices, nodeCollector, combinedLight, combinedOverlay, x1, y1, z1, x2, y2, z2, color);
    }

    /**
     * Renders a fluid block with offset from the matrices and from x1/y1/z1 to x2/y2/z2 inside the block local coordinates, so from 0-1
     */
    public static void renderFluidCuboid(
      FluidInformation fluid,
      PoseStack matrices,
      SubmitNodeCollector nodeCollector,
      int combinedLight,
      final int combinedOverlay,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      int color)
    {
        Identifier still = IRenderingManager.getInstance().getStillFluidTexture(fluid);
        Identifier flowing = IRenderingManager.getInstance().getFlowingFluidTexture(fluid);

        for (final ChunkSectionLayer renderLayer : ChunkSectionLayer.values())
        {
            if (!IRenderTypeManager.getInstance().canRenderInType(fluid.fluid().defaultFluidState(), renderLayer))
                return;

            final RenderType renderType = IRenderTypeManager.getInstance().getMovingBlockRenderType(renderLayer);

            renderFluidCuboid(still, flowing, color, matrices, renderType, nodeCollector, combinedOverlay, combinedLight, x1, y1, z1, x2, y2, z2);
        }
    }

    public static void renderFluidCuboid(
      Identifier still,
      Identifier flowing,
      int color,
      PoseStack matrices,
      RenderType renderType,
      SubmitNodeCollector nodeCollector,
      final int combinedOverlay,
      int combinedLight,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2)
    {
        matrices.pushPose();
        matrices.translate(x1, y1, z1);

        final TextureAtlasSprite stillSprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(still);
        final TextureAtlasSprite flowingSprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(flowing);

        nodeCollector.submitCustomGeometry(
            matrices,
            renderType,
            (pose, consumer) -> {
                // x/y/z2 - x/y/z1 is because we need the width/height/depth
                putTexturedQuad(consumer, pose, stillSprite, x2 - x1, y2 - y1, z2 - z1, DOWN, color, combinedOverlay, combinedLight, false);
                putTexturedQuad(consumer, pose, flowingSprite, x2 - x1, y2 - y1, z2 - z1, NORTH, color, combinedOverlay, combinedLight, true);
                putTexturedQuad(consumer, pose, flowingSprite, x2 - x1, y2 - y1, z2 - z1, Direction.EAST, color, combinedOverlay, combinedLight, true);
                putTexturedQuad(consumer, pose, flowingSprite, x2 - x1, y2 - y1, z2 - z1, Direction.SOUTH, color, combinedOverlay, combinedLight, true);
                putTexturedQuad(consumer, pose, flowingSprite, x2 - x1, y2 - y1, z2 - z1, Direction.WEST, color, combinedOverlay, combinedLight, true);
                putTexturedQuad(consumer, pose, stillSprite, x2 - x1, y2 - y1, z2 - z1, UP, color, combinedOverlay, combinedLight, false);
            }
        );


        matrices.popPose();
    }

    public static void putTexturedQuad(
      VertexConsumer renderer, PoseStack.Pose matrix, TextureAtlasSprite sprite, float w, float h, float d, Direction face,
      int color, final int overlay, int brightness, boolean flowing)
    {
        putTexturedQuad(renderer, matrix, sprite, w, h, d, face, color, overlay, brightness, flowing, false, false);
    }

    public static void putTexturedQuad(
      VertexConsumer renderer, PoseStack.Pose matrix, TextureAtlasSprite sprite, float w, float h, float d, Direction face,
      int color, final int overlay, int brightness, boolean flowing, boolean flipHorizontally, boolean flipVertically)
    {
        int l1 = brightness >> 0x10 & 0xFFFF;
        int l2 = brightness & 0xFFFF;

        int o1 = overlay >> 0x10 & 0xFFFF;
        int o2 = overlay & 0xFFFF;

        int a = color >> 24 & 0xFF;
        int r = color >> 16 & 0xFF;
        int g = color >> 8 & 0xFF;
        int b = color & 0xFF;

        putTexturedQuad(renderer, matrix, sprite, w, h, d, face, r, g, b, a, l1, l2, o1, o2, flowing, flipHorizontally, flipVertically);
    }

    /* Fluid cuboids */
// x and x+w has to be within [0,1], same for y/h and z/d
    public static void putTexturedQuad(
      VertexConsumer renderer, PoseStack.Pose matrices, TextureAtlasSprite sprite, float w, float h, float d, Direction face,
      int r, int g, int b, int a, int light1, int light2, final int overlay1, final int overlay2, boolean flowing, boolean flipHorizontally, boolean flipVertically)
    {
        // safety
        if (sprite == null)
        {
            return;
        }
        float minU;
        float maxU;
        float minV;
        float maxV;

        float size = 1f;
        if (flowing)
        {
            size = 0.5f;
        }

        float xt1 = 0;
        float xt2 = w;
        while (xt2 > 1f)
        {
            xt2 -= 1f;
        }
        float yt1 = 0;
        float yt2 = h;
        while (yt2 > 1f)
        {
            yt2 -= 1f;
        }
        float zt1 = 0;
        float zt2 = d;
        while (zt2 > 1f)
        {
            zt2 -= 1f;
        }

        // flowing stuff should start from the bottom, not from the start
        if (flowing)
        {
            float tmp = 1f - yt1;
            yt1 = 1f - yt2;
            yt2 = tmp;
        }

        switch (face)
        {
            case DOWN, UP -> {
                minU = sprite.getU(xt1 * size);
                maxU = sprite.getU(xt2 * size);
                minV = sprite.getV(zt1 * size);
                maxV = sprite.getV(zt2 * size);
            }
            case NORTH, SOUTH -> {
                minU = sprite.getU(xt2 * size);
                maxU = sprite.getU(xt1 * size);
                minV = sprite.getV(yt1 * size);
                maxV = sprite.getV(yt2 * size);
            }
            case WEST, EAST -> {
                minU = sprite.getU(zt2 * size);
                maxU = sprite.getU(zt1 * size);
                minV = sprite.getV(yt1 * size);
                maxV = sprite.getV(yt2 * size);
            }
            default -> {
                minU = sprite.getU0();
                maxU = sprite.getU1();
                minV = sprite.getV0();
                maxV = sprite.getV1();
            }
        }

        if (flipHorizontally)
        {
            float tmp = minV;
            minV = maxV;
            maxV = tmp;
        }

        if (flipVertically)
        {
            float tmp = minU;
            minU = maxU;
            maxU = tmp;
        }

        final Matrix4f worldMatrix = matrices.pose();
        final Matrix3f normalMatrix = matrices.normal();

        switch (face)
        {
            case DOWN -> {
                renderer.addVertex(worldMatrix, 0, 0, 0)
                  .setColor(r, g, b, a)
                  .setUv(minU, minV)
                  .setUv1(overlay1, overlay2)
                  .setUv2(light1, light2)
                  .setNormal(matrices, DOWN.getStepX(), DOWN.getStepY(), DOWN.getStepZ());
                renderer.addVertex(worldMatrix, w, 0, 0)
                  .setColor(r, g, b, a)
                  .setUv(maxU, minV)
                  .setUv1(overlay1, overlay2)
                  .setUv2(light1, light2)
                  .setNormal(matrices, DOWN.getStepX(), DOWN.getStepY(), DOWN.getStepZ());
                renderer.addVertex(worldMatrix, w, 0, d)
                  .setColor(r, g, b, a)
                  .setUv(maxU, maxV)
                  .setUv1(overlay1, overlay2)
                  .setUv2(light1, light2)
                  .setNormal(matrices, DOWN.getStepX(), DOWN.getStepY(), DOWN.getStepZ());
                renderer.addVertex(worldMatrix, 0, 0, d)
                  .setColor(r, g, b, a)
                  .setUv(minU, maxV)
                  .setUv1(overlay1, overlay2)
                  .setUv2(light1, light2)
                  .setNormal(matrices, DOWN.getStepX(), DOWN.getStepY(), DOWN.getStepZ());
            }
            case UP -> {
                renderer.addVertex(worldMatrix, 0, h, 0)
                  .setColor(r, g, b, a)
                  .setUv(minU, minV)
                  .setUv1(overlay1, overlay2)
                  .setUv2(light1, light2)
                  .setNormal(matrices, UP.getStepX(), UP.getStepY(), UP.getStepZ());
                renderer.addVertex(worldMatrix, 0, h, d)
                  .setColor(r, g, b, a)
                  .setUv(minU, maxV)
                  .setUv1(overlay1, overlay2)
                  .setUv2(light1, light2)
                  .setNormal(matrices, UP.getStepX(), UP.getStepY(), UP.getStepZ());
                renderer.addVertex(worldMatrix, w, h, d)
                  .setColor(r, g, b, a)
                  .setUv(maxU, maxV)
                  .setUv1(overlay1, overlay2)
                  .setUv2(light1, light2)
                  .setNormal(matrices, UP.getStepX(), UP.getStepY(), UP.getStepZ());
                renderer.addVertex(worldMatrix, w, h, 0)
                  .setColor(r, g, b, a)
                  .setUv(maxU, minV)
                  .setUv1(overlay1, overlay2)
                  .setUv2(light1, light2)
                  .setNormal(matrices, UP.getStepX(), UP.getStepY(), UP.getStepZ());
            }
            case NORTH -> {
                renderer.addVertex(worldMatrix, 0, 0, 0)
                  .setColor(r, g, b, a)
                  .setUv(minU, maxV)
                  .setUv1(overlay1, overlay2)
                  .setUv2(light1, light2)
                  .setNormal(matrices, NORTH.getStepX(), NORTH.getStepY(), NORTH.getStepZ());
                renderer.addVertex(worldMatrix, 0, h, 0)
                  .setColor(r, g, b, a)
                  .setUv(minU, minV)
                  .setUv1(overlay1, overlay2)
                  .setUv2(light1, light2)
                  .setNormal(matrices, NORTH.getStepX(), NORTH.getStepY(), NORTH.getStepZ());
                renderer.addVertex(worldMatrix, w, h, 0)
                  .setColor(r, g, b, a)
                  .setUv(maxU, minV)
                  .setUv1(overlay1, overlay2)
                  .setUv2(light1, light2)
                  .setNormal(matrices, NORTH.getStepX(), NORTH.getStepY(), NORTH.getStepZ());
                renderer.addVertex(worldMatrix, w, 0, 0)
                  .setColor(r, g, b, a)
                  .setUv(maxU, maxV)
                  .setUv1(overlay1, overlay2)
                  .setUv2(light1, light2)
                  .setNormal(matrices, NORTH.getStepX(), NORTH.getStepY(), NORTH.getStepZ());
            }
            case SOUTH -> {
                renderer.addVertex(worldMatrix, 0, 0, d)
                  .setColor(r, g, b, a)
                  .setUv(maxU, maxV)
                  .setUv1(overlay1, overlay2)
                  .setUv2(light1, light2)
                  .setNormal(matrices, SOUTH.getStepX(), SOUTH.getStepY(), SOUTH.getStepZ());
                renderer.addVertex(worldMatrix, w, 0, d)
                  .setColor(r, g, b, a)
                  .setUv(minU, maxV)
                  .setUv1(overlay1, overlay2)
                  .setUv2(light1, light2)
                  .setNormal(matrices, SOUTH.getStepX(), SOUTH.getStepY(), SOUTH.getStepZ());
                renderer.addVertex(worldMatrix, w, h, d)
                  .setColor(r, g, b, a)
                  .setUv(minU, minV)
                  .setUv1(overlay1, overlay2)
                  .setUv2(light1, light2)
                  .setNormal(matrices, SOUTH.getStepX(), SOUTH.getStepY(), SOUTH.getStepZ());
                renderer.addVertex(worldMatrix, 0, h, d)
                  .setColor(r, g, b, a)
                  .setUv(maxU, minV)
                  .setUv1(overlay1, overlay2)
                  .setUv2(light1, light2)
                  .setNormal(matrices, SOUTH.getStepX(), SOUTH.getStepY(), SOUTH.getStepZ());
            }
            case WEST -> {
                renderer.addVertex(worldMatrix, 0, 0, 0)
                  .setColor(r, g, b, a)
                  .setUv(maxU, maxV)
                  .setUv1(overlay1, overlay2)
                  .setUv2(light1, light2)
                  .setNormal(matrices, WEST.getStepX(), WEST.getStepY(), WEST.getStepZ());
                renderer.addVertex(worldMatrix, 0, 0, d)
                  .setColor(r, g, b, a)
                  .setUv(minU, maxV)
                  .setUv1(overlay1, overlay2)
                  .setUv2(light1, light2)
                  .setNormal(matrices, WEST.getStepX(), WEST.getStepY(), WEST.getStepZ());
                renderer.addVertex(worldMatrix, 0, h, d)
                  .setColor(r, g, b, a)
                  .setUv(minU, minV)
                  .setUv1(overlay1, overlay2)
                  .setUv2(light1, light2)
                  .setNormal(matrices, WEST.getStepX(), WEST.getStepY(), WEST.getStepZ());
                renderer.addVertex(worldMatrix, 0, h, 0)
                  .setColor(r, g, b, a)
                  .setUv(maxU, minV)
                  .setUv1(overlay1, overlay2)
                  .setUv2(light1, light2)
                  .setNormal(matrices, WEST.getStepX(), WEST.getStepY(), WEST.getStepZ());
            }
            case EAST -> {
                renderer.addVertex(worldMatrix, w, 0, 0)
                  .setColor(r, g, b, a)
                  .setUv(minU, maxV)
                  .setUv1(overlay1, overlay2)
                  .setUv2(light1, light2)
                  .setNormal(matrices, EAST.getStepX(), EAST.getStepY(), EAST.getStepZ());
                renderer.addVertex(worldMatrix, w, h, 0)
                  .setColor(r, g, b, a)
                  .setUv(minU, minV)
                  .setUv1(overlay1, overlay2)
                  .setUv2(light1, light2)
                  .setNormal(matrices, EAST.getStepX(), EAST.getStepY(), EAST.getStepZ());
                renderer.addVertex(worldMatrix, w, h, d)
                  .setColor(r, g, b, a)
                  .setUv(maxU, minV)
                  .setUv1(overlay1, overlay2)
                  .setUv2(light1, light2)
                  .setNormal(matrices, EAST.getStepX(), EAST.getStepY(), EAST.getStepZ());
                renderer.addVertex(worldMatrix, w, 0, d)
                  .setColor(r, g, b, a)
                  .setUv(maxU, maxV)
                  .setUv1(overlay1, overlay2)
                  .setUv2(light1, light2)
                  .setNormal(matrices, EAST.getStepX(), EAST.getStepY(), EAST.getStepZ());
            }
        }
    }
}

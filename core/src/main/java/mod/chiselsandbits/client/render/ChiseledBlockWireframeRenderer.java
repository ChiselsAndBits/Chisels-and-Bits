package mod.chiselsandbits.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.chiselsandbits.api.multistate.accessor.IStateEntryInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector4f;

import java.util.function.Predicate;

public class ChiseledBlockWireframeRenderer
{
    private static final ChiseledBlockWireframeRenderer INSTANCE = new ChiseledBlockWireframeRenderer();

    public static ChiseledBlockWireframeRenderer getInstance()
    {
        return INSTANCE;
    }

    private ChiseledBlockWireframeRenderer()
    {
    }

    public void renderShape(
            final PoseStack stack,
            final VoxelShape wireFrame,
            final Vec3 position,
            final Vector4f color,
            final boolean ignoreDepth)
    {
        stack.pushPose();

        final Vec3 vector3d = Minecraft.getInstance().gameRenderer.getMainCamera().position();
        final double xView = vector3d.x();
        final double yView = vector3d.y();
        final double zView = vector3d.z();

        final RenderType renderType = ignoreDepth
                ? ModRenderTypes.WIREFRAME_LINES_ALWAYS.get()
                : ModRenderTypes.WIREFRAME_LINES.get();

        //48/255f, 120/255f, 201/255f
        ShapeRenderer.renderShape(
          stack,
          Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(renderType),
          wireFrame,
          position.x() - xView, position.y() - yView, position.z() - zView,
            ARGB.colorFromFloat(1, color.x(), color.y(), color.z()),
            3f
        );
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch(renderType);

        stack.popPose();
    }
}

package mod.chiselsandbits.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import mod.chiselsandbits.api.util.constants.Constants;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

import java.util.OptionalDouble;
import java.util.function.Supplier;

public enum ModRenderTypes
{
    MEASUREMENT_LINES(() -> Internal.MEASUREMENT_LINES);

    private final Supplier<RenderType> typeSupplier;

    ModRenderTypes(final Supplier<RenderType> typeSupplier) {this.typeSupplier = typeSupplier;}

    public RenderType get() {
        return typeSupplier.get();
    }

    private static class Internal extends RenderType
    {
        private static final RenderType MEASUREMENT_LINES = RenderType.create(Constants.MOD_ID + ":measurement_lines",
          DefaultVertexFormat.POSITION_COLOR,
          VertexFormat.Mode.LINES,
          256,
          false,
          false,
          RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_LINES_SHADER)
            .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(2.5d)))
            .setLayeringState(VIEW_OFFSET_Z_LAYERING)
            .setTransparencyState(GLINT_TRANSPARENCY)
            .setOutputState(TRANSLUCENT_TARGET)
            .setWriteMaskState(COLOR_WRITE)
            .setCullState(NO_CULL)
            .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
            .createCompositeState(false));

        private Internal(String name, VertexFormat fmt, VertexFormat.Mode glMode, int size, boolean doCrumbling, boolean depthSorting, Runnable onEnable, Runnable onDisable)
        {
            super(name, fmt, glMode, size, doCrumbling, depthSorting, onEnable, onDisable);
            throw new IllegalStateException("This class must not be instantiated");
        }
    }
}
package mod.chiselsandbits.client.render;

import com.google.common.base.Suppliers;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;

import java.util.function.Function;
import java.util.function.Supplier;

public enum ModRenderTypes
{
    CHISEL_PREVIEW_INSIDE_BLOCKS(Internal.PREVIEW_INSIDE_BLOCKS),
    CHISEL_PREVIEW_OUTSIDE_BLOCKS(Internal.PREVIEW_OUTSIDE_BLOCKS),
    WIREFRAME_LINES(Internal.WIREFRAME),
    WIREFRAME_LINES_ALWAYS(Internal.WIREFRAME_ALWAYS),
    GHOST_BLOCK_PREVIEW(Internal.GHOST_BLOCK.apply(TextureAtlas.LOCATION_BLOCKS)),
    GHOST_BLOCK_PREVIEW_GREATER(Internal.GHOST_BLOCK_ALWAYS.apply(TextureAtlas.LOCATION_BLOCKS)),
    GHOST_BLOCK_COLORED_PREVIEW(Internal.GHOST_BLOCK_COLORED),
    GHOST_BLOCK_COLORED_PREVIEW_ALWAYS(Internal.GHOST_BLOCK_COLORED_ALWAYS);

    private final Supplier<RenderType> typeSupplier;

    ModRenderTypes(final Supplier<RenderType> typeSupplier)
    {
        this.typeSupplier = typeSupplier;
    }

    public RenderType get()
    {
        return typeSupplier.get();
    }

    private static class Internal
    {
        public static Supplier<RenderType> PREVIEW_INSIDE_BLOCKS = Suppliers.memoize(Internal::chiselPreviewInsideBlocks);

        private static RenderType chiselPreviewInsideBlocks()
        {
            var state = RenderSetup.builder(ModRenderPipelines.CHISEL_PREVIEW_IN_BLOCKS.pipeline())
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .bufferSize(256)
                .sortOnUpload()
                .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                .createRenderSetup();
            return RenderType.create("c_and_b_preview_inside_blocks", state);
        }

        public static Supplier<RenderType> PREVIEW_OUTSIDE_BLOCKS = Suppliers.memoize(Internal::chiselPreviewOutsideBlocks);

        private static RenderType chiselPreviewOutsideBlocks()
        {
            var state = RenderSetup.builder(ModRenderPipelines.CHISEL_PREVIEW_OUTSIDE_BLOCKS.pipeline())
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .bufferSize(256)
                .sortOnUpload()
                .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                .createRenderSetup();
            return RenderType.create("c_and_b_preview_outside_blocks", state);
        }

        public static Supplier<RenderType> WIREFRAME = Suppliers.memoize(Internal::wireframe);

        private static RenderType wireframe()
        {
            var state = RenderSetup.builder(ModRenderPipelines.WIREFRAME.pipeline())
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .bufferSize(256)
                .sortOnUpload()
                .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                .createRenderSetup();
            return RenderType.create("c_and_b_wireframe", state);
        }

        public static Supplier<RenderType> WIREFRAME_ALWAYS = Suppliers.memoize(Internal::wireframeAlways);

        private static RenderType wireframeAlways()
        {
            var state = RenderSetup.builder(ModRenderPipelines.WIREFRAME_ALWAYS.pipeline())
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .bufferSize(256)
                .sortOnUpload()
                .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                .createRenderSetup();
            return RenderType.create("c_and_b_wireframe_always", state);
        }

        public static Function<Identifier, Supplier<RenderType>> GHOST_BLOCK = (rl) -> Suppliers.memoize(() -> ghostBlock(rl));

        private static RenderType ghostBlock(Identifier texture)
        {
            var state = RenderSetup.builder(ModRenderPipelines.GHOST_BLOCK.pipeline())
                .withTexture("Sampler0", texture)
                .useLightmap()
                .useOverlay()
                .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                .bufferSize(256)
                .affectsCrumbling()
                .sortOnUpload()
                .createRenderSetup();

            return RenderType.create("c_and_b_ghost_block", state);
        }

        public static Function<Identifier, Supplier<RenderType>> GHOST_BLOCK_ALWAYS = (rl) -> Suppliers.memoize(() -> ghostBlockAlways(rl));

        private static RenderType ghostBlockAlways(Identifier texture)
        {
            var state = RenderSetup.builder(ModRenderPipelines.GHOST_BLOCK_ALWAYS.pipeline())
                .withTexture("Sampler0", texture)
                .useLightmap()
                .useOverlay()
                .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                .bufferSize(256)
                .affectsCrumbling()
                .sortOnUpload()
                .createRenderSetup();

            return RenderType.create("c_and_b_ghost_block_always", state);
        }

        public static Supplier<RenderType> GHOST_BLOCK_COLORED = Suppliers.memoize(Internal::ghostBlockColored);

        private static RenderType ghostBlockColored()
        {
            var state = RenderSetup.builder(ModRenderPipelines.GHOST_BLOCK_COLORED.pipeline())
                .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                .bufferSize(256)
                .affectsCrumbling()
                .sortOnUpload()
                .createRenderSetup();

            return RenderType.create("c_and_b_ghost_block_colored", state);
        }

        public static Supplier<RenderType> GHOST_BLOCK_COLORED_ALWAYS = Suppliers.memoize(Internal::ghostBlockColoredAlways);

        private static RenderType ghostBlockColoredAlways()
        {
            var state = RenderSetup.builder(ModRenderPipelines.GHOST_BLOCK_COLORED_ALWAYS.pipeline())
                .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                .bufferSize(256)
                .affectsCrumbling()
                .sortOnUpload()
                .createRenderSetup();

            return RenderType.create("c_and_b_ghost_block_colored", state);
        }
    }
}

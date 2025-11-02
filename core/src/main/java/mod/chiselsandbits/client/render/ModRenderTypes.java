package mod.chiselsandbits.client.render;

import com.google.common.base.Suppliers;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

import java.util.OptionalDouble;
import java.util.function.Function;
import java.util.function.Supplier;

public enum ModRenderTypes
{
    MEASUREMENT_LINES(Internal.MEASUREMENT_LINES),
    CHISEL_PREVIEW_INSIDE_BLOCKS(Internal.PREVIEW_INSIDE_BLOCKS),
    CHISEL_PREVIEW_OUTSIDE_BLOCKS(Internal.PREVIEW_OUTSIDE_BLOCKS),
    WIREFRAME_LINES(Internal.WIREFRAME),
    WIREFRAME_LINES_ALWAYS(Internal.WIREFRAME_ALWAYS),
    GHOST_BLOCK_PREVIEW(Internal.GHOST_BLOCK.apply(TextureAtlas.LOCATION_BLOCKS)),
    GHOST_BLOCK_PREVIEW_GREATER(Internal.GHOST_BLOCK_ALWAYS.apply(TextureAtlas.LOCATION_BLOCKS)),
    GHOST_BLOCK_COLORED_PREVIEW(Internal.GHOST_BLOCK_COLORED),
    GHOST_BLOCK_COLORED_PREVIEW_ALWAYS(Internal.GHOST_BLOCK_COLORED_ALWAYS);

    private final Supplier<RenderType> typeSupplier;

    ModRenderTypes(final Supplier<RenderType> typeSupplier) {
        this.typeSupplier = typeSupplier;
    }

    public RenderType get() {
        return typeSupplier.get();
    }

    private static class Internal
    {
        public static Supplier<RenderType> MEASUREMENT_LINES = Suppliers.memoize(Internal::measurementLines);

        private static RenderType measurementLines() {
            var state = RenderType.CompositeState.builder()
                .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(2.5d)))
                .setLayeringState(RenderStateShard.LayeringStateShard.VIEW_OFFSET_Z_LAYERING)
                .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                .createCompositeState(true);
            return RenderType.create("c_and_b_measurement_lines", 256, true, false, ModRenderPipelines.LINES.pipeline(), state);
        }

        public static Supplier<RenderType> PREVIEW_INSIDE_BLOCKS = Suppliers.memoize(Internal::chiselPreviewInsideBlocks);

        private static RenderType chiselPreviewInsideBlocks() {
            var state = RenderType.CompositeState.builder()
                .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(2.5d)))
                .setLayeringState(RenderStateShard.LayeringStateShard.VIEW_OFFSET_Z_LAYERING)
                .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                .createCompositeState(true);
            return RenderType.create("c_and_b_preview_inside_blocks", 256, true, false, ModRenderPipelines.CHISEL_PREVIEW_IN_BLOCKS.pipeline(), state);
        }

        public static Supplier<RenderType> PREVIEW_OUTSIDE_BLOCKS = Suppliers.memoize(Internal::chiselPreviewOutsideBlocks);

        private static RenderType chiselPreviewOutsideBlocks() {
            var state = RenderType.CompositeState.builder()
                .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(2.5d)))
                .setLayeringState(RenderStateShard.LayeringStateShard.VIEW_OFFSET_Z_LAYERING)
                .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                .createCompositeState(true);
            return RenderType.create("c_and_b_preview_outside_blocks", 256, true, false, ModRenderPipelines.CHISEL_PREVIEW_OUTSIDE_BLOCKS.pipeline(), state);
        }

        public static Supplier<RenderType> WIREFRAME = Suppliers.memoize(Internal::wireframe);

        private static RenderType wireframe() {
            var state = RenderType.CompositeState.builder()
                .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(3d)))
                .setLayeringState(RenderStateShard.LayeringStateShard.VIEW_OFFSET_Z_LAYERING)
                .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                .createCompositeState(true);
            return RenderType.create("c_and_b_wireframe", 256, true, true, ModRenderPipelines.WIREFRAME.pipeline(), state);
        }

        public static Supplier<RenderType> WIREFRAME_ALWAYS = Suppliers.memoize(Internal::wireframeAlways);

        private static RenderType wireframeAlways() {
            var state = RenderType.CompositeState.builder()
                .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(3d)))
                .setLayeringState(RenderStateShard.LayeringStateShard.VIEW_OFFSET_Z_LAYERING)
                .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                .createCompositeState(true);
            return RenderType.create("c_and_b_wireframe_always", 256, true, true, ModRenderPipelines.WIREFRAME_ALWAYS.pipeline(), state);
        }

        public static Function<ResourceLocation, Supplier<RenderType>> GHOST_BLOCK = (rl) -> Suppliers.memoize(() -> ghostBlock(rl));

        private static RenderType ghostBlock(ResourceLocation texture) {
            var state = RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(texture, false))
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .setOverlayState(RenderStateShard.OVERLAY)
                .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                .createCompositeState(true);

            return RenderType.create("c_and_b_ghost_block", 256, true, true, ModRenderPipelines.GHOST_BLOCK.pipeline(), state);
        }

        public static Function<ResourceLocation, Supplier<RenderType>> GHOST_BLOCK_ALWAYS = (rl) -> Suppliers.memoize(() -> ghostBlockAlways(rl));

        private static RenderType ghostBlockAlways(ResourceLocation texture) {
            var state = RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(texture, false))
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .setOverlayState(RenderStateShard.OVERLAY)
                .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                .createCompositeState(true);

            return RenderType.create("c_and_b_ghost_block_always", 256, true, true, ModRenderPipelines.GHOST_BLOCK_ALWAYS.pipeline(), state);
        }

        public static Supplier<RenderType> GHOST_BLOCK_COLORED = Suppliers.memoize(Internal::ghostBlockColored);

        private static RenderType ghostBlockColored() {
            var state = RenderType.CompositeState.builder()
                .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                .createCompositeState(true);

            return RenderType.create("c_and_b_ghost_block_colored", 256, true, true, ModRenderPipelines.GHOST_BLOCK_COLORED.pipeline(), state);
        }

        public static Supplier<RenderType> GHOST_BLOCK_COLORED_ALWAYS = Suppliers.memoize(Internal::ghostBlockColoredAlways);

        private static RenderType ghostBlockColoredAlways() {
            var state = RenderType.CompositeState.builder()
                .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                .createCompositeState(true);

            return RenderType.create("c_and_b_ghost_block_colored", 256, true, true, ModRenderPipelines.GHOST_BLOCK_COLORED_ALWAYS.pipeline(), state);
        }
    }
}

package mod.chiselsandbits.client.render;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;

import java.util.function.Supplier;

public enum ModRenderPipelineSnippets
{
    NO_DEPTH_TEST(() -> RenderPipeline.builder()
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .buildSnippet()
    ),

    GREATER_DEPTH_TEST(() -> RenderPipeline.builder()
        .withDepthTestFunction(DepthTestFunction.GREATER_DEPTH_TEST)
        .buildSnippet()
    ),

    LESS_DEPTH_TEST(() -> RenderPipeline.builder()
        .withDepthTestFunction(DepthTestFunction.LESS_DEPTH_TEST)
        .buildSnippet()
    );

    private final Supplier<RenderPipeline.Snippet> getter;

    ModRenderPipelineSnippets(final Supplier<RenderPipeline.Snippet> getter) {
        this.getter = Suppliers.memoize(getter::get);
    }

    public RenderPipeline.Snippet snippet() {
        return getter.get();
    }
}

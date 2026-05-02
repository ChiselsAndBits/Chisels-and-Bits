package mod.chiselsandbits.client.render;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;

import java.util.function.Supplier;

public enum ModRenderPipelineSnippets
{
    NO_DEPTH_TEST(() -> RenderPipeline.builder()
        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .buildSnippet()
    ),

    GREATER_DEPTH_TEST(() -> RenderPipeline.builder()
        .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN, false))
        .buildSnippet()
    ),

    LESS_DEPTH_TEST(() -> RenderPipeline.builder()
        .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN, false))
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

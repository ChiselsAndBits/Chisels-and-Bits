package mod.chiselsandbits.client.render;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import mod.chiselsandbits.api.util.constants.Constants;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.function.Supplier;

import static net.minecraft.client.renderer.RenderPipelines.*;

public enum ModRenderPipelines
{
    CHISEL_PREVIEW_IN_BLOCKS(() -> RenderPipeline.builder(
        LINES_SNIPPET,
        ModRenderPipelineSnippets.NO_DEPTH_TEST.snippet()
    ).withLocation(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "pipeline/chisel_preview_in_blocks")).build()),

    CHISEL_PREVIEW_OUTSIDE_BLOCKS(() -> RenderPipeline.builder(
        LINES_SNIPPET,
        ModRenderPipelineSnippets.LESS_DEPTH_TEST.snippet()
    ).withLocation(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "pipeline/chisel_preview_outside_blocks")).build()),

    WIREFRAME(() -> RenderPipeline.builder(
        LINES_SNIPPET,
        ModRenderPipelineSnippets.LESS_DEPTH_TEST.snippet()
    ).withLocation(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "pipeline/wireframe")).build()),

    WIREFRAME_ALWAYS(() -> RenderPipeline.builder(
        LINES_SNIPPET,
        ModRenderPipelineSnippets.NO_DEPTH_TEST.snippet()
    ).withLocation(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "pipeline/wireframe_always")).build()),

    GHOST_BLOCK(() ->
        RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withShaderDefine("PER_FACE_LIGHTING")
            .withSampler("Sampler1")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .withLocation(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "pipeline/ghost_block")).build()),

    GHOST_BLOCK_ALWAYS(() ->
        RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET, ModRenderPipelineSnippets.GREATER_DEPTH_TEST.snippet())
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withShaderDefine("PER_FACE_LIGHTING")
            .withSampler("Sampler1")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .withLocation(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "pipeline/ghost_block_always")).build()),

    GHOST_BLOCK_COLORED(() -> RenderPipeline.builder(
        RenderPipelines.DEBUG_FILLED_SNIPPET,
        ModRenderPipelineSnippets.LESS_DEPTH_TEST.snippet()
    ).withLocation(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "pipeline/wireframe_always")).build()),

    GHOST_BLOCK_COLORED_ALWAYS(() -> RenderPipeline.builder(
        RenderPipelines.DEBUG_FILLED_SNIPPET,
        ModRenderPipelineSnippets.NO_DEPTH_TEST.snippet()
    ).withLocation(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "pipeline/wireframe_always")).build());

    private final Supplier<RenderPipeline> getter;

    ModRenderPipelines(final Supplier<RenderPipeline> getter)
    {
        this.getter = Suppliers.memoize(getter::get);
    }

    public RenderPipeline pipeline()
    {
        return getter.get();
    }
}

package mod.chiselsandbits.forge.mixin.client;

import mod.chiselsandbits.debug.bench.BenchmarkCollector;
import mod.chiselsandbits.debug.bench.TerrainWorldMeshCapture;
import com.communi.suggestu.scena.core.client.rendering.sharding.TerrainCpuBatch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.client.renderer.chunk.SectionCompiler.Results;

@Mixin(SectionCompiler.class)
public abstract class SectionCompilerMixin {
	private static final ThreadLocal<Long> CHISELS_AND_BITS_COMPILE_START = new ThreadLocal<>();

	@Inject(
			method = "compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;Ljava/util/List;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;",
			at = @At("HEAD")
	)
	private void chiselsandbits$onCompileStart(final CallbackInfoReturnable<Results> cir) {
		TerrainWorldMeshCapture.onCompileStart();
		CHISELS_AND_BITS_COMPILE_START.set(BenchmarkCollector.getInstance().isActive() ? System.nanoTime() : null);
	}

	@Inject(
			method = "compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;Ljava/util/List;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;",
			at = @At("RETURN")
	)
	private void chiselsandbits$onCompileEnd(final CallbackInfoReturnable<Results> cir) {
		TerrainWorldMeshCapture.onCompiled(cir.getReturnValue());
		final Long startNanos = CHISELS_AND_BITS_COMPILE_START.get();
		CHISELS_AND_BITS_COMPILE_START.remove();

		if (startNanos == null) {
			return;
		}

		final Results results = cir.getReturnValue();
		int quadCount = 0;

		for (final var mesh : results.renderedLayers.values()) {
			quadCount += mesh.drawState().vertexCount() / 4;
		}
		quadCount = Math.toIntExact(quadCount + TerrainCpuBatch.additionalQuadCount(results));

		BenchmarkCollector.getInstance().onSectionCompiled(System.nanoTime() - startNanos, quadCount);
	}
}

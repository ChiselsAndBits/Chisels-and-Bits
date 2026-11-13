package mod.chiselsandbits.debug.bench;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.nio.ByteOrder;
import java.lang.management.ManagementFactory;

import com.sun.management.ThreadMXBean;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.BlockPos;

public final class TerrainWorldMeshCapture {
	private static final boolean ENABLED = Boolean.getBoolean("chiselsandbits.runTerrainWorldCheck");
	private static final boolean TIMING = ENABLED && Boolean.getBoolean("chiselsandbits.terrainCompileBenchmark");
	private static final ThreadMXBean ALLOCATIONS = allocationBean();
	private static final AtomicBoolean ARMED = new AtomicBoolean();
	private static final AtomicReference<Snapshot> CAPTURE = new AtomicReference<>();
	private static final ThreadLocal<Start> START = new ThreadLocal<>();

	private TerrainWorldMeshCapture() {
	}

	public static void arm() {
		CAPTURE.set(null);
		ARMED.set(true);
	}

	public static Snapshot capture() {
		return CAPTURE.get();
	}

	public static void onCompileStart() {
		if (!TIMING) {
			return;
		}
		if (ARMED.get()) {
			START.set(new Start(System.nanoTime(), allocatedBytes(),
					Boolean.getBoolean("scena.experimentalNativeTerrainGeometry")));
		} else {
			START.remove();
		}
	}

	public static void onNativeEmission(final BlockPos pos) {
		if (TIMING && ARMED.get()) {
			final Start start = START.get();
			if (start != null) {
				final int index = TerrainWorldCompileBenchmark.sceneIndex(pos);
				if (index >= 0) {
					start.nativeEmissions |= 1L << index;
				}
			}
		}
	}

	public static void onCompiled(final SectionCompiler.Results results) {
		final long endNanos = TIMING ? System.nanoTime() : 0;
		final long endBytes = TIMING ? allocatedBytes() : -1;
		final Start compileStart = TIMING ? START.get() : null;
		if (TIMING) {
			START.remove();
		}
		if (!ENABLED || !ARMED.get()) {
			return;
		}
		if (TIMING && compileStart == null) {
			return;
		}
		final var mesh = results.renderedLayers.get(ChunkSectionLayer.TRANSLUCENT);
		if (mesh == null) {
			return;
		}
		final var bytes = mesh.vertexBuffer().duplicate().order(ByteOrder.nativeOrder());
		final int vertexSize = mesh.drawState().format().getVertexSize();
		final int stride = vertexSize * 4;
		final List<String> quads = new ArrayList<>();
		boolean fixture = false;
		while (bytes.remaining() >= stride) {
			final int start = bytes.position();
			float minY = Float.POSITIVE_INFINITY;
			float maxY = Float.NEGATIVE_INFINITY;
			float minZ = Float.POSITIVE_INFINITY;
			float maxZ = Float.NEGATIVE_INFINITY;
			boolean onEastFace = true;
			for (int vertex = 0; vertex < 4; vertex++) {
				final int offset = start + vertex * vertexSize;
				onEastFace &= bytes.getFloat(offset) == 16f;
				minY = Math.min(minY, bytes.getFloat(offset + 4));
				maxY = Math.max(maxY, bytes.getFloat(offset + 4));
				minZ = Math.min(minZ, bytes.getFloat(offset + 8));
				maxZ = Math.max(maxZ, bytes.getFloat(offset + 8));
			}
			fixture |= onEastFace && minY == 8.375f && maxY == 8.5f && minZ == 6.25f && maxZ == 6.5f;
			final byte[] quad = new byte[stride];
			bytes.get(quad);
			quads.add(HexFormat.of().formatHex(quad));
		}
		if (fixture && ARMED.compareAndSet(true, false)) {
			quads.sort(String::compareTo);
			CAPTURE.set(new Snapshot(results.transparencyState != null && mesh.indexBuffer() != null, true,
					List.copyOf(quads), compileStart == null ? -1 : endNanos - compileStart.nanos,
					compileStart == null || compileStart.bytes < 0 || endBytes < 0 ? -1 : endBytes - compileStart.bytes,
					compileStart != null && compileStart.nativeEnabled,
					compileStart == null ? 0 : Long.bitCount(compileStart.nativeEmissions)));
		}
	}

	private static ThreadMXBean allocationBean() {
		if (TIMING && ManagementFactory.getThreadMXBean() instanceof ThreadMXBean bean
				&& bean.isThreadAllocatedMemorySupported() && bean.isThreadAllocatedMemoryEnabled()) {
			return bean;
		}
		return null;
	}

	private static long allocatedBytes() {
		return ALLOCATIONS == null ? -1 : ALLOCATIONS.getThreadAllocatedBytes(Thread.currentThread().threadId());
	}

	private static final class Start {
		private final long nanos;
		private final long bytes;
		private final boolean nativeEnabled;
		private long nativeEmissions;

		private Start(final long nanos, final long bytes, final boolean nativeEnabled) {
			this.nanos = nanos;
			this.bytes = bytes;
			this.nativeEnabled = nativeEnabled;
		}
	}

	public record Snapshot(boolean sorted, boolean fixture, List<String> quads, long compileNanos, long allocatedBytes,
			boolean nativeEnabled, int nativeEmissions) {
	}
}

package mod.chiselsandbits.debug.bench;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.minecraft.client.Minecraft;

/**
 * Client-side collector for rendering benchmarks. Started and stopped with <code>/candb bench</code>.
 * Recording methods are cheap no-ops while no benchmark is active.
 */
public final class BenchmarkCollector {
	private static final BenchmarkCollector INSTANCE = new BenchmarkCollector();
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String RESULTS_DIRECTORY = "chiselsandbits-benchmarks";
	private static final DateTimeFormatter FILE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
	private static final double NANOS_PER_MILLI = 1_000_000.0;

	private volatile boolean active;
	private final List<Double> sectionCompileMillis = Collections.synchronizedList(new ArrayList<>());
	private final List<Double> sectionQuads = Collections.synchronizedList(new ArrayList<>());
	private final List<Double> chiselLatencyMillis = Collections.synchronizedList(new ArrayList<>());
	private final List<Double> fpsSamples = new ArrayList<>();
	private long startedAtNanos;
	private LocalDateTime startedAt;

	private BenchmarkCollector() {
	}

	public static BenchmarkCollector getInstance() {
		return INSTANCE;
	}

	public boolean isActive() {
		return active;
	}

	public synchronized void start() {
		sectionCompileMillis.clear();
		sectionQuads.clear();
		chiselLatencyMillis.clear();
		fpsSamples.clear();
		startedAtNanos = System.nanoTime();
		startedAt = LocalDateTime.now();
		active = true;
	}

	/**
	 * Records the time a section compile took on a worker thread, and how many quads it produced.
	 */
	public void onSectionCompiled(final long compileNanos, final int quadCount) {
		if (!active) {
			return;
		}

		sectionCompileMillis.add(compileNanos / NANOS_PER_MILLI);
		sectionQuads.add((double) quadCount);
	}

	/**
	 * Records the time from the start of a chisel model update until the updated block was sent to the render thread.
	 *
	 * @param startNanos the {@link System#nanoTime()} at which the update was requested.
	 */
	public void onChiselUpdateVisible(final long startNanos) {
		if (!active) {
			return;
		}

		chiselLatencyMillis.add((System.nanoTime() - startNanos) / NANOS_PER_MILLI);
	}

	public void onClientTick() {
		if (!active) {
			return;
		}

		fpsSamples.add((double) Minecraft.getInstance().getFps());
	}

	/**
	 * Stops the benchmark and writes the results to a file in the game directory.
	 *
	 * @return The summary lines, the last of which names the results file. Empty if no benchmark was running.
	 */
	public synchronized List<String> stop() {
		if (!active) {
			return List.of();
		}

		active = false;
		final double durationSeconds = (System.nanoTime() - startedAtNanos) / 1_000_000_000.0;

		final List<String> lines = new ArrayList<>();
		lines.add("Chisels & Bits benchmark");
		lines.add(String.format(Locale.ROOT, "Started: %s, duration: %.1f s", startedAt, durationSeconds));
		lines.add(describeFps());
		lines.add(describe("Section compile (ms)", sectionCompileMillis));
		lines.add(describe("Quads per section", sectionQuads));
		lines.add(describe("Chisel to visible update (ms)", chiselLatencyMillis));

		if (sectionCompileMillis.isEmpty()) {
			lines.add("WARNING: No section compiles were recorded. This does not mean no sections were redrawn. "
					+ "Trigger a rebuild while recording; if this persists, check renderer hook compatibility.");
		}

		try {
			final Path path = writeResults(lines);
			lines.add("Results written to: " + path.toAbsolutePath());
		} catch (final IOException e) {
			LOGGER.error("Failed to write benchmark results", e);
			lines.add("Failed to write results file: " + e.getMessage());
		}

		return lines;
	}

	private String describeFps() {
		final List<Double> sorted = sortedCopy(fpsSamples);

		if (sorted.isEmpty()) {
			return "FPS: no samples";
		}

		final double average = sorted.stream().mapToDouble(Double::doubleValue).average().orElse(0);
		return String.format(Locale.ROOT, "FPS: samples=%d avg=%.1f p1(low)=%.1f", sorted.size(), average, percentile(sorted, 0.01));
	}

	private static String describe(final String label, final List<Double> values) {
		final List<Double> sorted = sortedCopy(values);

		if (sorted.isEmpty()) {
			return label + ": no samples";
		}

		return String.format(Locale.ROOT, "%s: count=%d p50=%.3f p99=%.3f max=%.3f",
				label, sorted.size(), percentile(sorted, 0.50), percentile(sorted, 0.99), sorted.get(sorted.size() - 1));
	}

	private static List<Double> sortedCopy(final List<Double> values) {
		final List<Double> copy;
		synchronized (values) {
			copy = new ArrayList<>(values);
		}

		copy.sort(null);
		return copy;
	}

	private static double percentile(final List<Double> sorted, final double percentile) {
		final int index = Math.max(0, Math.min(sorted.size() - 1, (int) Math.ceil(percentile * sorted.size()) - 1));
		return sorted.get(index);
	}

	private static Path writeResults(final List<String> lines) throws IOException {
		final Path directory = Minecraft.getInstance().gameDirectory.toPath().resolve(RESULTS_DIRECTORY);
		Files.createDirectories(directory);

		final Path file = directory.resolve("bench-" + LocalDateTime.now().format(FILE_TIME_FORMAT) + ".txt");
		Files.write(file, lines, StandardCharsets.UTF_8);
		return file;
	}
}

package mod.chiselsandbits.debug.bench;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import mod.chiselsandbits.api.block.storage.StateEntryStorage;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.block.entities.ChiseledBlockEntity;
import mod.chiselsandbits.registrars.ModBlocks;
import mod.chiselsandbits.registrars.ModModelProperties;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/** Opt-in comparison of repeated section compiles in the disposable terrain-check world. */
public final class TerrainWorldCompileBenchmark {
	private static final int WARMUP_PAIRS = 5;
	private static final int SAMPLE_PAIRS = 100;
	private static final List<BlockPos> POSITIONS = positions();
	private final List<TerrainWorldMeshCapture.Snapshot> nativeSamples = new ArrayList<>();
	private final List<TerrainWorldMeshCapture.Snapshot> bakedSamples = new ArrayList<>();
	private List<String> expectedQuads;
	private int completed;

	public static boolean enabled() {
		return Boolean.getBoolean("chiselsandbits.terrainCompileBenchmark");
	}

	public static void createScene(final ServerLevel level) {
		final StateEntryStorage[] templates = new StateEntryStorage[3];
		for (int pattern = 0; pattern < templates.length; pattern++) {
			final var storage = new StateEntryStorage(16);
			final var material = new BlockInformation(
					pattern == 1 ? Blocks.OAK_PLANKS.defaultBlockState() : Blocks.STONE.defaultBlockState(), Optional.empty());
			for (int x = 0; x < 16; x++) {
				for (int y = 0; y < 16; y++) {
					for (int z = 0; z < 16; z++) {
						if (pattern == 0 ? y < 8 : pattern == 1 ? x < 8 : z < 8) {
							storage.setBlockInformation(x, y, z, material);
						}
					}
				}
			}
			templates[pattern] = storage;
		}
		for (int index = 0; index < POSITIONS.size(); index++) {
			final BlockPos pos = POSITIONS.get(index);
			level.setBlockAndUpdate(pos, ModBlocks.CHISELED_BLOCK.get().defaultBlockState());
			((ChiseledBlockEntity) level.getBlockEntity(pos)).replaceStorage(templates[index % templates.length].createSnapshot());
		}
	}

	public static boolean sceneReady(final Minecraft client) {
		if (client.level == null) {
			throw new IllegalStateException("Benchmark world disconnected before the scene was ready");
		}
		for (final BlockPos pos : POSITIONS) {
			if (!(client.level.getBlockEntity(pos) instanceof ChiseledBlockEntity entity)) {
				return false;
			}
			final var data = entity.getBlockModelData().getData(ModModelProperties.TERRAIN_RENDER_DATA);
			if (data == null || !data.metadata().key().identifier().equals(entity.createNewShapeIdentifier())) {
				return false;
			}
		}
		return true;
	}

	public boolean useNative() {
		return completed % 2 == 0;
	}

	public boolean done() {
		return completed == 2 * (WARMUP_PAIRS + SAMPLE_PAIRS);
	}

	public void record(final TerrainWorldMeshCapture.Snapshot sample) {
		if (!sample.fixture() || !sample.sorted() || sample.compileNanos() < 0 || sample.nativeEnabled() != useNative()
				|| (useNative() ? sample.nativeEmissions() != POSITIONS.size() : sample.nativeEmissions() != 0)) {
			throw new IllegalStateException("Invalid timed fixture section: native=" + sample.nativeEnabled()
					+ ", emissions=" + sample.nativeEmissions());
		}
		if (expectedQuads == null) {
			expectedQuads = sample.quads();
		} else if (!expectedQuads.equals(sample.quads())) {
			throw new IllegalStateException("The benchmark fixture changed between compiles");
		}
		if (completed >= 2 * WARMUP_PAIRS) {
			(useNative() ? nativeSamples : bakedSamples).add(sample);
		}
		completed++;
	}

	public String result(final String loader) {
		if (!done() || nativeSamples.size() != SAMPLE_PAIRS || bakedSamples.size() != SAMPLE_PAIRS) {
			throw new IllegalStateException("Incomplete terrain section benchmark");
		}
		return "TERRAIN_SECTION_BENCHMARK loader=" + loader + " blocks=" + POSITIONS.size()
				+ " warmup_pairs=" + WARMUP_PAIRS + " samples_per_path=" + SAMPLE_PAIRS
				+ " native={" + describe(nativeSamples) + "} baked={" + describe(bakedSamples) + "}";
	}

	private static String describe(final List<TerrainWorldMeshCapture.Snapshot> samples) {
		final var times = samples.stream().map(TerrainWorldMeshCapture.Snapshot::compileNanos).sorted().toList();
		final var allocations = samples.stream().map(TerrainWorldMeshCapture.Snapshot::allocatedBytes)
				.sorted(Comparator.naturalOrder()).toList();
		return String.format(Locale.ROOT, "p50_ms=%.3f p99_ms=%.3f allocation_p50=%d allocation_p99=%d",
				percentile(times, 50) / 1_000_000d, percentile(times, 99) / 1_000_000d,
				percentile(allocations, 50), percentile(allocations, 99));
	}

	private static long percentile(final List<Long> sorted, final int percentage) {
		return sorted.get((int) Math.ceil(percentage * sorted.size() / 100d) - 1);
	}

	private static List<BlockPos> positions() {
		final List<BlockPos> result = new ArrayList<>();
		for (int x = 0; x < 3; x++) {
			for (int y = 0; y < 3; y++) {
				for (int z = 0; z < 3; z++) {
					result.add(new BlockPos(-7 + 2 * x, 53 + 2 * y, 2 + 2 * z));
				}
			}
		}
		return List.copyOf(result);
	}

	static int sceneIndex(final BlockPos pos) {
		final int x = pos.getX() + 7;
		final int y = pos.getY() - 53;
		final int z = pos.getZ() - 2;
		if (x < 0 || x > 4 || y < 0 || y > 4 || z < 0 || z > 4
				|| (x & 1) != 0 || (y & 1) != 0 || (z & 1) != 0) {
			return -1;
		}
		return (x / 2) * 9 + (y / 2) * 3 + z / 2;
	}
}

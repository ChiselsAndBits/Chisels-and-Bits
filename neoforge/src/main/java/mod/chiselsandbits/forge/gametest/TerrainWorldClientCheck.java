package mod.chiselsandbits.forge.gametest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.mojang.logging.LogUtils;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.communi.suggestu.scena.core.client.rendering.terrain.TerrainQuad;
import com.communi.suggestu.scena.core.client.rendering.terrain.TerrainVertex;
import com.communi.suggestu.scena.core.client.rendering.terrain.FaceTriangleClipper;
import mod.chiselsandbits.ChiselsAndBits;
import mod.chiselsandbits.api.block.storage.StateEntryStorage;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.multistate.accessor.ISingleBlockAxisAlignedAreaAccessor;
import mod.chiselsandbits.block.entities.ChiseledBlockEntity;
import mod.chiselsandbits.client.model.information.ChiseledBlockModelInformation;
import mod.chiselsandbits.client.model.meshing.ChiseledRenderSnapshot;
import mod.chiselsandbits.client.model.meshing.DirectTerrainGeometryBuilder;
import mod.chiselsandbits.debug.bench.TerrainWorldMeshCapture;
import mod.chiselsandbits.debug.bench.TerrainWorldCompileBenchmark;
import mod.chiselsandbits.network.packets.UpdateBlockEntityPacket;
import mod.chiselsandbits.registrars.ModBlocks;
import mod.chiselsandbits.registrars.ModModelProperties;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.joml.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/** Bounded, opt-in NeoForge client integration check using a newly created disposable world. */
public final class TerrainWorldClientCheck {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String PROPERTY = "chiselsandbits.runTerrainWorldCheck";
	private static final String GATE = "scena.experimentalNativeTerrainGeometry";
	private static final BlockPos LEFT = new BlockPos(-1, 56, 6);
	private static final BlockPos RIGHT = LEFT.east();
	private static final long TIMEOUT_NANOS = 180_000_000_000L;
	private static Phase phase = Phase.START;
	private static long started;
	private static int settleTicks;
	private static int mixedPass;
	private static Path worldPath;
	private static CompletableFuture<?> serverTask;
	private static TerrainWorldMeshCapture.Snapshot nativeMesh;
	private static TerrainWorldCompileBenchmark compileBenchmark;
	private static int benchmarkSettleTicks;

	private TerrainWorldClientCheck() {
	}

	public static void onClientTick() {
		if (!Boolean.getBoolean(PROPERTY) || phase == Phase.DONE) {
			return;
		}
		final Minecraft client = Minecraft.getInstance();
		if (started == 0) {
			started = System.nanoTime();
		}
		try {
			if (System.nanoTime() - started > TIMEOUT_NANOS) {
				throw new IllegalStateException("Timed out in " + phase);
			}
			tick(client);
		} catch (final Throwable failure) {
			finish(client, failure);
		}
	}

	private static void tick(final Minecraft client) throws IOException {
		switch (phase) {
		case START -> {
			if (!client.isGameLoadFinished() || ++settleTicks < 10) {
				return;
			}
			if (TerrainWorldCompileBenchmark.enabled()) {
				client.options.pauseOnLostFocus = false;
			}
			final Path saves = client.gameDirectory.toPath().resolve("saves").toAbsolutePath().normalize();
			Files.createDirectories(saves);
			final String name = "candb-terrain-check-" + UUID.randomUUID();
			final Path candidate = saves.resolve(name);
			if (Files.exists(candidate) || !saves.equals(candidate.getParent())) {
				throw new IOException("Disposable world path is not a new direct child of saves: " + candidate);
			}
			Files.createDirectory(candidate);
			worldPath = candidate;
			phase = Phase.WAIT_WORLD;
			client.createWorldOpenFlows().createFreshLevel(name,
					new LevelSettings(name, GameType.SPECTATOR, LevelSettings.DifficultySettings.DEFAULT, true,
							WorldDataConfiguration.DEFAULT),
					new WorldOptions(1337, false, false), WorldPresets::createTestWorldDimensions, null);
		}
		case WAIT_WORLD -> {
			if (client.level == null || client.player == null || client.getSingleplayerServer() == null) {
				return;
			}
			final var server = client.getSingleplayerServer();
			serverTask = server.submit(() -> {
				final var level = server.overworld();
				final var player = server.getPlayerList().getPlayers().getFirst();
				player.setGameMode(GameType.SPECTATOR);
				player.teleportTo(-5, 57, 10);
				level.setBlockAndUpdate(LEFT, ModBlocks.CHISELED_BLOCK.get().defaultBlockState());
				level.setBlockAndUpdate(RIGHT, ModBlocks.CHISELED_BLOCK.get().defaultBlockState());
				level.setBlockAndUpdate(RIGHT.above().south(), Blocks.GLOWSTONE.defaultBlockState());
				level.setBlockAndUpdate(RIGHT.below().north(), Blocks.STONE.defaultBlockState());
				((ChiseledBlockEntity) level.getBlockEntity(LEFT)).replaceStorage(waterFace(15, 4));
				((ChiseledBlockEntity) level.getBlockEntity(RIGHT)).replaceStorage(waterFace(0, 2));
			});
			phase = Phase.WAIT_FIXTURE;
		}
		case WAIT_FIXTURE -> {
			if (!serverTask.isDone()) {
				return;
			}
			serverTask.join();
			if (area(client, LEFT, Direction.EAST) != 8 || area(client, RIGHT, Direction.WEST) != 0) {
				return;
			}
			if (++settleTicks < 40) {
				return;
			}
			probeSplitLighting(client);
			TerrainWorldMeshCapture.arm();
			client.levelExtractor.setSectionDirty(LEFT.getX() >> 4, LEFT.getY() >> 4, LEFT.getZ() >> 4);
			phase = Phase.NATIVE_MESH;
		}
		case NATIVE_MESH -> {
			nativeMesh = TerrainWorldMeshCapture.capture();
			if (nativeMesh == null) {
				return;
			}
			System.setProperty(GATE, "false");
			TerrainWorldMeshCapture.arm();
			client.levelExtractor.setSectionDirty(LEFT.getX() >> 4, LEFT.getY() >> 4, LEFT.getZ() >> 4);
			phase = Phase.BAKED_MESH;
		}
		case BAKED_MESH -> {
			final var baked = TerrainWorldMeshCapture.capture();
			if (baked == null) {
				return;
			}
			if (!nativeMesh.sorted() || !baked.sorted() || !nativeMesh.fixture() || !baked.fixture()
					|| !nativeMesh.quads().equals(baked.quads())) {
				throw new IllegalStateException("NeoForge native/baked translucent section buffers differ: native="
						+ nativeMesh.quads().size() + ", baked=" + baked.quads().size()
						+ ", sorted=" + nativeMesh.sorted() + "/" + baked.sorted()
						+ ", fixture=" + nativeMesh.fixture() + "/" + baked.fixture());
			}
			LOGGER.info("TERRAIN_NEO_WORLD_LIGHT_SORT: native and baked lit vertices match and have sorted indices");
			System.setProperty(GATE, "true");
			client.level.getChunkSource().drop(new ChunkPos(RIGHT.getX() >> 4, RIGHT.getZ() >> 4));
			phase = Phase.UNLOADED;
		}
		case UNLOADED -> {
			if (client.level.isLoaded(RIGHT) || area(client, LEFT, Direction.EAST) != 16) {
				return;
			}
			final var server = client.getSingleplayerServer();
			serverTask = server.submit(() -> {
				final var level = server.overworld();
				final var chunk = level.getChunkAt(RIGHT);
				server.getPlayerList().getPlayers().getFirst().connection.send(
						new ClientboundLevelChunkWithLightPacket(chunk, level.getLightEngine(), null, null));
				ChiselsAndBits.getInstance().getNetworkChannel().sendToTrackingChunk(
						new UpdateBlockEntityPacket((ChiseledBlockEntity) level.getBlockEntity(RIGHT)), chunk);
			});
			phase = Phase.RELOADED;
		}
		case RELOADED -> {
			if (!serverTask.isDone()) {
				return;
			}
			serverTask.join();
			if (area(client, LEFT, Direction.EAST) != 8 || area(client, RIGHT, Direction.WEST) != 0) {
				return;
			}
			LOGGER.info("TERRAIN_NEO_WORLD_CHUNK_LIFECYCLE: unloaded and reloaded neighbour rebuilt automatically");
			serverTask = client.getSingleplayerServer().submit(() -> {
				final var mixed = waterFace(0, 2);
				mixed.setBlockInformation(1, 4, 4, new BlockInformation(Blocks.GLASS.defaultBlockState(), Optional.empty()));
				((ChiseledBlockEntity) client.getSingleplayerServer().overworld().getBlockEntity(RIGHT)).replaceStorage(mixed);
			});
			phase = Phase.MIXED_FALLBACK;
		}
		case MIXED_FALLBACK -> {
			if (!serverTask.isDone()) {
				return;
			}
			serverTask.join();
			if (!(client.level.getBlockEntity(RIGHT) instanceof ChiseledBlockEntity entity)
					|| !entity.stream().anyMatch(entry -> entry.getBlockInformation().blockState()
							.is(mixedPass == 0 ? Blocks.GLASS : Blocks.OAK_STAIRS))
					|| entity.getBlockModelData().getData(ModModelProperties.MODEL) == null
					|| area(client, LEFT, Direction.EAST) != 8 || area(client, RIGHT, Direction.WEST) != 0) {
				return;
			}
			final var leftData = ((ChiseledBlockEntity) client.level.getBlockEntity(LEFT)).getBlockModelData()
					.getData(ModModelProperties.TERRAIN_RENDER_DATA);
			if (bakedBoundaryArea(leftData.fallback(), Direction.EAST) != 8) {
				throw new IllegalStateException("Native candidate's baked fallback retained hidden fluid faces");
			}
			if (mixedPass++ == 0) {
				serverTask = client.getSingleplayerServer().submit(() -> {
					final var mixed = waterFace(0, 2);
					mixed.setBlockInformation(1, 4, 4, new BlockInformation(Blocks.OAK_STAIRS.defaultBlockState(), Optional.empty()));
					((ChiseledBlockEntity) client.getSingleplayerServer().overworld().getBlockEntity(RIGHT)).replaceStorage(mixed);
				});
				return;
			}
			serverTask = client.getSingleplayerServer().submit(() -> {
				final var mixed = waterFace(0, 2);
				mixed.setBlockInformation(0, 4, 4, new BlockInformation(Blocks.OAK_STAIRS.defaultBlockState(), Optional.empty()));
				((ChiseledBlockEntity) client.getSingleplayerServer().overworld().getBlockEntity(RIGHT)).replaceStorage(mixed);
			});
			phase = Phase.MIXED_BOUNDARY;
		}
		case MIXED_BOUNDARY -> {
			if (!serverTask.isDone()) {
				return;
			}
			serverTask.join();
			if (!(client.level.getBlockEntity(RIGHT) instanceof ChiseledBlockEntity entity)
					|| entity.stream().filter(entry -> !entry.getBlockInformation().isAir()).count() != 8
					|| !entity.stream().anyMatch(entry -> entry.getBlockInformation().blockState().is(Blocks.OAK_STAIRS))
					|| entity.getBlockModelData().getData(ModModelProperties.MODEL) == null
					|| area(client, LEFT, Direction.EAST) != 9 || area(client, RIGHT, Direction.WEST) < 0) {
				return;
			}
			final var model = entity.getBlockModelData().getData(ModModelProperties.MODEL);
			final boolean hasWater = model.parts().stream().anyMatch(part -> part.source().blockState().is(Blocks.WATER));
			final boolean hiddenWater = model.parts().stream().filter(part -> part.source().blockState().is(Blocks.WATER))
					.allMatch(part -> part.getQuads(Direction.WEST).isEmpty());
			final boolean stairVisible = model.parts().stream().filter(part -> part.source().blockState().is(Blocks.OAK_STAIRS))
					.anyMatch(part -> !part.getQuads(Direction.WEST).isEmpty());
			final var leftData = ((ChiseledBlockEntity) client.level.getBlockEntity(LEFT)).getBlockModelData()
					.getData(ModModelProperties.TERRAIN_RENDER_DATA);
			if (!hasWater || !hiddenWater || !stairVisible || bakedBoundaryArea(leftData.fallback(), Direction.EAST) != 9) {
				throw new IllegalStateException("Mixed boundary must hide matching water but retain the non-cubic stair face");
			}
			LOGGER.info("TERRAIN_NEO_WORLD_MIXED_FALLBACK: native and unsupported chiseled neighbours, including a boundary stair, culled matching fluid");
			serverTask = client.getSingleplayerServer().submit(() ->
					((ChiseledBlockEntity) client.getSingleplayerServer().overworld().getBlockEntity(RIGHT))
							.replaceStorage(waterFace(0, 2)));
			phase = Phase.RESTORED;
		}
		case RESTORED -> {
			if (!serverTask.isDone()) {
				return;
			}
			serverTask.join();
			if (area(client, LEFT, Direction.EAST) != 8 || area(client, RIGHT, Direction.WEST) != 0) {
				return;
			}
			if (TerrainWorldCompileBenchmark.enabled()) {
				serverTask = client.getSingleplayerServer().submit(() ->
						TerrainWorldCompileBenchmark.createScene(client.getSingleplayerServer().overworld()));
				phase = Phase.BENCHMARK_READY;
				return;
			}
			finish(client, null);
		}
		case BENCHMARK_READY -> {
			if (!serverTask.isDone()) {
				return;
			}
			serverTask.join();
			if (!TerrainWorldCompileBenchmark.sceneReady(client) || ++benchmarkSettleTicks < 20) {
				return;
			}
			compileBenchmark = new TerrainWorldCompileBenchmark();
			startBenchmarkCompile(client);
			phase = Phase.BENCHMARK;
		}
		case BENCHMARK -> {
			final var sample = TerrainWorldMeshCapture.capture();
			if (sample == null) {
				return;
			}
			compileBenchmark.record(sample);
			if (compileBenchmark.done()) {
				LOGGER.info(compileBenchmark.result("neoforge"));
				finish(client, null);
				return;
			}
			startBenchmarkCompile(client);
		}
		case DONE -> {
		}
		}
	}

	private static void startBenchmarkCompile(final Minecraft client) {
		System.setProperty(GATE, Boolean.toString(compileBenchmark.useNative()));
		TerrainWorldMeshCapture.arm();
		client.levelExtractor.setSectionDirty(LEFT.getX() >> 4, LEFT.getY() >> 4, LEFT.getZ() >> 4);
	}

	private static StateEntryStorage waterFace(final int x, final int height) {
		final var storage = new StateEntryStorage(16);
		final var water = new BlockInformation(Blocks.WATER.defaultBlockState(), Optional.empty());
		for (int y = 4; y < 4 + height; y++) {
			for (int z = 4; z < 8; z++) {
				storage.setBlockInformation(x, y, z, water);
			}
		}
		return storage;
	}

	private static void probeSplitLighting(final Minecraft client) {
		final var entity = (ChiseledBlockEntity) client.level.getBlockEntity(LEFT);
		final var direct = entity.getBlockModelData().getData(ModModelProperties.TERRAIN_RENDER_DATA);
		final var snapshot = ChiseledRenderSnapshot.of((ISingleBlockAxisAlignedAreaAccessor) entity.createSnapshot());
		final var original = DirectTerrainGeometryBuilder.buildWithoutSealedAir(direct.metadata(), snapshot, client.level, LEFT)
				.fallbackGeometry().parts().stream().flatMap(part -> part.quads().stream())
				.filter(quad -> quad.cullFace() == Direction.EAST).findFirst().orElseThrow();
		final var part = direct.geometry().parts().stream()
				.filter(candidate -> candidate.quads().stream().anyMatch(quad -> quad.cullFace() == Direction.EAST))
				.findFirst().orElseThrow();
		final var split = part.quads().stream().filter(quad -> quad.cullFace() == Direction.EAST).findFirst().orElseThrow();
		final var lighter = new BlockModelLighter();
		final var originalLight = new QuadInstance();
		final var splitLight = new QuadInstance();
		lighter.prepareQuadAmbientOcclusion(client.level, entity.getBlockState(), LEFT, lightingQuad(original), originalLight);
		lighter.prepareQuadAmbientOcclusion(client.level, entity.getBlockState(), LEFT, lightingQuad(split), splitLight);
		boolean uneven = false;
		for (int vertex = 1; vertex < 4; vertex++) {
			uneven |= originalLight.getColor(vertex) != originalLight.getColor(0)
					|| originalLight.getLightCoords(vertex) != originalLight.getLightCoords(0);
		}
		if (!uneven) {
			throw new IllegalStateException("Split-lighting fixture did not produce uneven original lighting");
		}
		final var clippedTriangles = FaceTriangleClipper.clip(original, split);
		float maxColorDifference = 0;
		float maxLightDifference = 0;
		float maxPreservedDifference = 0;
		for (int y = 1; y < 16; y++) {
			for (int z = 1; z < 16; z++) {
				final float sampleY = 0.375f + 0.125f * y / 16;
				final float sampleZ = 0.25f + 0.25f * z / 16;
				maxColorDifference = Math.max(maxColorDifference, Math.abs(interpolate(original, originalLight, sampleY, sampleZ, 0)
						- interpolate(split, splitLight, sampleY, sampleZ, 0)));
				for (int channel = 1; channel <= 2; channel++) {
					maxLightDifference = Math.max(maxLightDifference, Math.abs(interpolate(original, originalLight, sampleY, sampleZ, channel)
							- interpolate(split, splitLight, sampleY, sampleZ, channel)));
				}
				for (int channel = 0; channel <= 2; channel++) {
					maxPreservedDifference = Math.max(maxPreservedDifference,
							Math.abs(interpolate(original, originalLight, sampleY, sampleZ, channel)
									- interpolateClipped(clippedTriangles, originalLight, sampleY, sampleZ, channel)));
				}
			}
		}
		if (maxPreservedDifference > 0.0001f) {
			throw new IllegalStateException("Triangle clipping did not preserve original lighting: " + maxPreservedDifference);
		}
		LOGGER.info("TERRAIN_NEO_SPLIT_LIGHT_PROBE ao={} maxColorDifference={} maxLightDifference={} triangleReferenceDifference={} original={} split={}",
				part.ambientOcclusion(), maxColorDifference, maxLightDifference, maxPreservedDifference,
				lightVertices(original, originalLight), lightVertices(split, splitLight));
	}

	private static float interpolateClipped(
			final java.util.List<FaceTriangleClipper.Triangle> triangles, final QuadInstance light,
			final float y, final float z, final int channel
	) {
		for (final var triangle : triangles) {
			final var a = triangle.v0();
			final var b = triangle.v1();
			final var c = triangle.v2();
			final float denominator = (b.position().z() - c.position().z()) * (a.position().y() - c.position().y())
					+ (c.position().y() - b.position().y()) * (a.position().z() - c.position().z());
			final float wa = ((b.position().z() - c.position().z()) * (y - c.position().y())
					+ (c.position().y() - b.position().y()) * (z - c.position().z())) / denominator;
			final float wb = ((c.position().z() - a.position().z()) * (y - c.position().y())
					+ (a.position().y() - c.position().y()) * (z - c.position().z())) / denominator;
			final float wc = 1 - wa - wb;
			if (wa >= -0.00001f && wb >= -0.00001f && wc >= -0.00001f) {
				final int v0 = lightChannel(light, 0, channel), v1 = lightChannel(light, 1, channel);
				final int v2 = lightChannel(light, 2, channel), v3 = lightChannel(light, 3, channel);
				return wa * a.interpolate(v0, v1, v2, v3) + wb * b.interpolate(v0, v1, v2, v3)
						+ wc * c.interpolate(v0, v1, v2, v3);
			}
		}
		throw new IllegalStateException("Lighting sample is outside the clipped triangles");
	}

	private static float interpolate(final TerrainQuad quad, final QuadInstance light, final float y, final float z, final int channel) {
		final TerrainVertex[] vertices = {quad.v0(), quad.v1(), quad.v2(), quad.v3()};
		for (final int[] triangle : new int[][] {{0, 1, 2}, {2, 3, 0}}) {
			final TerrainVertex a = vertices[triangle[0]], b = vertices[triangle[1]], c = vertices[triangle[2]];
			final float denominator = (b.z() - c.z()) * (a.y() - c.y()) + (c.y() - b.y()) * (a.z() - c.z());
			final float wa = ((b.z() - c.z()) * (y - c.y()) + (c.y() - b.y()) * (z - c.z())) / denominator;
			final float wb = ((c.z() - a.z()) * (y - c.y()) + (a.y() - c.y()) * (z - c.z())) / denominator;
			final float wc = 1 - wa - wb;
			if (wa >= -0.00001f && wb >= -0.00001f && wc >= -0.00001f) {
				return wa * lightChannel(light, triangle[0], channel) + wb * lightChannel(light, triangle[1], channel)
						+ wc * lightChannel(light, triangle[2], channel);
			}
		}
		throw new IllegalStateException("Lighting sample is outside the fixture quad");
	}

	private static int lightChannel(final QuadInstance light, final int vertex, final int channel) {
		return switch (channel) {
		case 0 -> light.getColor(vertex) >> 16 & 0xff;
		case 1 -> light.getLightCoords(vertex) & 0xff;
		case 2 -> light.getLightCoords(vertex) >> 16 & 0xff;
		default -> throw new IllegalArgumentException("Invalid light channel");
		};
	}

	private static String lightVertices(final TerrainQuad quad, final QuadInstance light) {
		final TerrainVertex[] vertices = {quad.v0(), quad.v1(), quad.v2(), quad.v3()};
		final var result = new StringBuilder();
		for (int index = 0; index < 4; index++) {
			result.append(" [").append(vertices[index].y()).append(',').append(vertices[index].z())
					.append(" color=").append(Integer.toHexString(light.getColor(index)))
					.append(" light=").append(Integer.toHexString(light.getLightCoords(index))).append(']');
		}
		return result.toString();
	}

	private static BakedQuad lightingQuad(final TerrainQuad quad) {
		final var source = quad.material();
		final var material = new BakedQuad.MaterialInfo(source.sprite(), source.layer(), source.itemRenderType(),
				quad.tintIndex(), source.shade(), source.lightEmission(), source.ambientOcclusion());
		return new BakedQuad(position(quad.v0()), position(quad.v1()), position(quad.v2()), position(quad.v3()),
				uv(quad.v0()), uv(quad.v1()), uv(quad.v2()), uv(quad.v3()), quad.face(), material);
	}

	private static Vector3f position(final TerrainVertex vertex) {
		return new Vector3f(vertex.x(), vertex.y(), vertex.z());
	}

	private static long uv(final TerrainVertex vertex) {
		return UVPair.pack(vertex.u(), vertex.v());
	}

	private static int area(final Minecraft client, final BlockPos pos, final Direction face) {
		if (!(client.level.getBlockEntity(pos) instanceof ChiseledBlockEntity entity)) {
			return -1;
		}
		final var direct = entity.getBlockModelData().getData(ModModelProperties.TERRAIN_RENDER_DATA);
		if (direct == null) {
			final var model = entity.getBlockModelData().getData(ModModelProperties.MODEL);
			return model == null || !model.key().identifier().equals(entity.createNewShapeIdentifier())
					? -1 : bakedBoundaryArea(model, face);
		}
		if (!direct.metadata().key().identifier().equals(entity.createNewShapeIdentifier())) {
			return -1;
		}
		double result = 0;
		for (final var part : direct.geometry().parts()) {
			for (final var quad : part.quads()) {
				if (quad.cullFace() == face) {
					result += Math.abs((quad.v0().y() - quad.v2().y()) * (quad.v0().z() - quad.v2().z()));
				}
			}
		}
		return (int) Math.round(result * 256);
	}

	private static int bakedBoundaryArea(final ChiseledBlockModelInformation model, final Direction face) {
		double result = 0;
		for (final var part : model.parts()) {
			for (final var quad : part.getQuads(face)) {
				final var a = quad.position(0);
				final var b = quad.position(2);
				result += Math.abs((a.y() - b.y()) * (a.z() - b.z()));
			}
		}
		return (int) Math.round(result * 256);
	}

	private static void finish(final Minecraft client, final Throwable failure) {
		phase = Phase.DONE;
		System.setProperty(GATE, "true");
		try {
			if (client.level != null || client.getSingleplayerServer() != null) {
				client.disconnectWithSavingScreen();
			}
			if (worldPath != null && Files.exists(worldPath)) {
				final Path saves = client.gameDirectory.toPath().resolve("saves").toRealPath();
				final Path actual = worldPath.toRealPath();
				if (!saves.equals(actual.getParent()) || !actual.getFileName().toString().startsWith("candb-terrain-check-")) {
					throw new IOException("Refusing to delete unexpected test world " + actual);
				}
				FileUtils.deleteDirectory(actual.toFile());
			}
			if (failure == null) {
				LOGGER.info("TERRAIN_NEO_WORLD_PASS: disposable live-client terrain checks passed");
			} else {
				LOGGER.error("TERRAIN_NEO_WORLD_FAIL", failure);
			}
		} catch (final IOException cleanupFailure) {
			LOGGER.error("TERRAIN_NEO_WORLD_FAIL: disposable world cleanup failed", new UncheckedIOException(cleanupFailure));
		} finally {
			client.stop();
		}
	}

	private enum Phase {
		START, WAIT_WORLD, WAIT_FIXTURE, NATIVE_MESH, BAKED_MESH, UNLOADED, RELOADED, MIXED_FALLBACK, MIXED_BOUNDARY,
		RESTORED, BENCHMARK_READY, BENCHMARK, DONE
	}
}

package mod.chiselsandbits.fabric.gametest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.communi.suggestu.scena.core.client.models.data.IBlockModelData;
import com.communi.suggestu.scena.core.client.rendering.terrain.TerrainGeometry;
import com.mojang.logging.LogUtils;
import mod.chiselsandbits.ChiselsAndBits;
import mod.chiselsandbits.block.entities.ChiseledBlockEntity;
import mod.chiselsandbits.api.block.storage.StateEntryStorage;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.multistate.accessor.ISingleBlockAxisAlignedAreaAccessor;
import mod.chiselsandbits.api.neighborhood.IBlockNeighborhood;
import mod.chiselsandbits.client.model.block.ChiseledBlockStateModelManager;
import mod.chiselsandbits.client.model.builder.ChiseledBlockModelInformationBuilder;
import mod.chiselsandbits.client.model.data.ChiseledBlockModelDataExecutor;
import mod.chiselsandbits.client.model.information.ChiseledBlockModelInformation;
import mod.chiselsandbits.client.model.meshing.TerrainLegacyComparison;
import mod.chiselsandbits.client.reloading.ClientResourceReloadingManager;
import mod.chiselsandbits.debug.scenes.SceneGenerators;
import mod.chiselsandbits.debug.scenes.SceneType;
import mod.chiselsandbits.debug.bench.TerrainWorldMeshCapture;
import mod.chiselsandbits.debug.bench.TerrainWorldCompileBenchmark;
import mod.chiselsandbits.registrars.ModModelProperties;
import mod.chiselsandbits.registrars.ModBlocks;
import mod.chiselsandbits.multistate.snapshot.SimpleSnapshot;
import mod.chiselsandbits.network.packets.UpdateBlockEntityPacket;
import org.apache.commons.io.FileUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

public final class TerrainSubsetClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(final ClientGameTestContext context) {
		if (!Boolean.getBoolean("chiselsandbits.runTerrainWorldCheck")) {
			return;
		}
		if (TerrainWorldCompileBenchmark.enabled()) {
			context.runOnClient(client -> client.options.pauseOnLostFocus = false);
		}

		final BlockPos inset = new BlockPos(-6, 56, 6);
		final Path gameDirectory = context.computeOnClient(client -> client.gameDirectory.toPath());
		final Set<Path> existingSaves;
		try {
			final Path saves = gameDirectory.resolve("saves");
			if (Files.exists(saves)) {
				try (var entries = Files.list(saves)) {
					existingSaves = entries.map(path -> path.toAbsolutePath().normalize()).collect(Collectors.toSet());
				}
			} else {
				existingSaves = Set.of();
			}
		} catch (final IOException exception) {
			throw new UncheckedIOException(exception);
		}
		Path save = null;

		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(settings -> settings.setName("C&B terrain subset check")).create()) {
			save = world.getWorldSave().getSaveDirectory();
			world.getServer().runOnServer(server -> SceneGenerators.generate(
					SceneType.TERRAIN_SUBSET, world.getConnection().getServerLevel(), inset, 0
			));
			world.getServer().runCommand("gamemode spectator @a");
			world.getServer().runCommand("tp @a -5 57 10");
			context.waitFor(client -> client.level != null
					&& client.level.getBlockEntity(inset) instanceof ChiseledBlockEntity entity
					&& entity.getBlockModelData().getData(ModModelProperties.TERRAIN_RENDER_DATA) != null, 600);
			world.getConnection().waitForChunksRender();
			context.waitTicks(20);
			context.runOnClient(client -> {
				for (final BlockPos pos : new BlockPos[]{inset, inset.offset(2, 0, 0)}) {
					final IBlockModelData data = ((ChiseledBlockEntity) client.level.getBlockEntity(pos)).getBlockModelData();
					final var direct = data.getData(ModModelProperties.TERRAIN_RENDER_DATA);
					if (direct == null || direct.isFallbackBuilt() || data.getData(ModModelProperties.MODEL) != null) {
						throw new AssertionError("Native section compilation constructed a baked fallback at " + pos);
					}
				}
			});
			checkHookBypassFallback(context, world, inset);
			context.runOnClient(client -> assertPublishedGeometry(client, inset));
			context.runOnClient(client -> assertPublishedGeometry(client, inset.offset(2, 0, 0)));

			final IBlockModelData beforeReload = context.computeOnClient(client ->
					((ChiseledBlockEntity) client.level.getBlockEntity(inset)).getBlockModelData());
			final CompletableFuture<Void> reload = context.computeOnClient(Minecraft::reloadResourcePacks);
			context.waitFor(client -> reload.isDone(), 600);
			reload.join();
			context.waitFor(client -> ((ChiseledBlockEntity) client.level.getBlockEntity(inset)).getBlockModelData() != beforeReload, 600);
			context.runOnClient(client -> assertPublishedGeometry(client, inset));
			context.runOnClient(client -> assertPublishedGeometry(client, inset.offset(2, 0, 0)));

			for (int iteration = 0; iteration < 5; iteration++) {
				final AtomicInteger completed = new AtomicInteger();
				context.runOnClient(client -> {
					ClientResourceReloadingManager.getInstance().clearCaches();

					for (final BlockPos pos : new BlockPos[]{inset, inset.offset(2, 0, 0)}) {
						ChiseledBlockModelDataExecutor.updateModelDataCore(
								(ChiseledBlockEntity) client.level.getBlockEntity(pos), completed::incrementAndGet
						);
					}
				});
				context.waitFor(client -> completed.get() == 2, 600);
				context.runOnClient(client -> assertPublishedGeometry(client, inset));
				context.runOnClient(client -> assertPublishedGeometry(client, inset.offset(2, 0, 0)));
			}

			checkBoundaryEdits(context, world, inset.offset(2, 0, 0));
			world.getConnection().waitForChunksRender();
			context.runOnClient(client -> {
				final ChiseledBlockEntity boundary = (ChiseledBlockEntity) client.level.getBlockEntity(inset.offset(2, 0, 0));

				final TerrainGeometry geometry = boundary.getBlockModelData().getData(ModModelProperties.TERRAIN_GEOMETRY);
				if (geometry == null || geometry.parts().stream().flatMap(part -> part.quads().stream())
						.noneMatch(quad -> quad.cullFace() != null)) {
					throw new AssertionError("Boundary fixture must publish native directional cull groups");
				}
			});
			checkSealedCavity(context, world, inset.offset(2, 0, 0));
			checkCutoutMaterials(context, world, inset.offset(2, 0, 0));
			checkFluidBoundaryUpdates(context, world);
			checkGlassBoundaryUpdates(context, world);
			if (TerrainWorldCompileBenchmark.enabled()) {
				benchmarkSectionCompiles(context, world);
			}
			world.getConnection().waitForChunksRender();
			LogUtils.getLogger().info("TERRAIN_WORLD_CHECK_PASS: inset and boundary direct candidates survived reload and five cold-cache rebuilds; six-direction air/solid/chiseled neighbour changes and owner edits retained coherent native geometry");
		} finally {
			if (save != null) {
				try {
					final Path resolvedSave = save.toRealPath();
					final Path saves = gameDirectory.resolve("saves").toRealPath();
					if (!saves.equals(resolvedSave.getParent()) || existingSaves.contains(save.toAbsolutePath().normalize())) {
						throw new IOException("Test world is not directly inside the game saves directory: " + resolvedSave);
					}
					FileUtils.deleteDirectory(resolvedSave.toFile());
				} catch (final IOException exception) {
					throw new UncheckedIOException("Failed to remove disposable test world " + save, exception);
				}
			}
		}
	}

	private static void checkCutoutMaterials(
			final ClientGameTestContext context, final TestSingleplayerContext world, final BlockPos pos
	) {
		for (int variant = 0; variant <= 9; variant++) {
			final StateEntryStorage storage = new StateEntryStorage(16);
			final var leaves = new BlockInformation(Blocks.OAK_LEAVES.defaultBlockState(), Optional.empty());
			final var stone = new BlockInformation(Blocks.STONE.defaultBlockState(), Optional.empty());
			if (variant <= 1) {
				storage.initializeWith(leaves);
				if (variant == 1) {
					storage.setBlockInformation(8, 8, 8, BlockInformation.AIR);
				} else {
					for (int x = 0; x < 16; x++) {
						for (int y = 8; y < 16; y++) {
							for (int z = 0; z < 16; z++) {
								storage.setBlockInformation(x, y, z, BlockInformation.AIR);
							}
						}
					}
				}
			} else if (variant == 2) {
				storage.setBlockInformation(7, 7, 7, stone);
				storage.setBlockInformation(8, 7, 7, leaves);
			} else if (variant == 3) {
				storage.initializeWith(stone);
				storage.setBlockInformation(4, 4, 4, leaves);
				storage.setBlockInformation(8, 8, 8, BlockInformation.AIR);
			} else {
				if (variant == 8) {
					storage.setBlockInformation(7, 7, 7, new BlockInformation(Blocks.WATER.defaultBlockState(), Optional.empty()));
					storage.setBlockInformation(8, 7, 7, leaves);
				} else {
					final BlockInformation state = switch (variant) {
						case 4 -> new BlockInformation(Blocks.GLASS.defaultBlockState(), Optional.empty());
						case 5 -> new BlockInformation(Blocks.OAK_LEAVES.defaultBlockState()
								.setValue(BlockStateProperties.WATERLOGGED, true), Optional.empty());
						case 6, 9 -> new BlockInformation(Blocks.WATER.defaultBlockState(), Optional.empty());
						case 7 -> new BlockInformation(Blocks.SEA_LANTERN.defaultBlockState(), Optional.empty());
						default -> throw new IllegalStateException("Unknown material fixture " + variant);
					};
					storage.setBlockInformation(variant == 9 ? 0 : 7, 7, 7, state);
				}
			}
			final var identifier = new SimpleSnapshot(storage).createNewShapeIdentifier();
			world.getServer().runOnServer(server ->
					((ChiseledBlockEntity) world.getConnection().getServerLevel().getBlockEntity(pos)).replaceStorage(storage));
			final boolean supported = variant == 0 || variant == 1 || variant == 4 || variant == 6 || variant == 7 || variant == 9;
			LogUtils.getLogger().info("TERRAIN_WORLD_CHECK_MATERIAL_FIXTURE: {} expecting native={}", variant, supported);
			context.waitFor(client -> {
				final var entity = (ChiseledBlockEntity) client.level.getBlockEntity(pos);
				final var data = entity.getBlockModelData();
				final var direct = data.getData(ModModelProperties.TERRAIN_RENDER_DATA);
				final var legacy = data.getData(ModModelProperties.MODEL);
				return identifier.equals(entity.createNewShapeIdentifier()) && (supported
						? direct != null && identifier.equals(direct.metadata().key().identifier())
						: direct == null && legacy != null && identifier.equals(legacy.key().identifier()));
			}, 600);
			world.getConnection().waitForChunksRender();
			final int fixture = variant;
			context.runOnClient(client -> {
				final var entity = (ChiseledBlockEntity) client.level.getBlockEntity(pos);
				final var data = entity.getBlockModelData();
				final var direct = data.getData(ModModelProperties.TERRAIN_RENDER_DATA);
				if (!supported) {
					if (direct != null || data.getData(ModModelProperties.TERRAIN_GEOMETRY) != null) {
						throw new AssertionError("Unsupported transparent/fluid fixture published direct geometry");
					}
					return;
				}
				if (direct.isFallbackBuilt() || data.getData(ModModelProperties.MODEL) != null) {
					throw new AssertionError("Native cutout compilation constructed a baked fallback");
				}
				final var quads = direct.geometry().parts().stream().flatMap(part -> part.quads().stream()).toList();
				final int expected = fixture == 0 ? 6 : fixture == 1 ? 12 : 6;
				if (quads.size() != expected || (fixture < 2 && quads.stream().anyMatch(
						quad -> quad.material().layer() != ChunkSectionLayer.CUTOUT))) {
					throw new AssertionError("Cutout layer, cavity retention or occupied interface culling changed");
				}
				if (fixture >= 4) {
					if (fixture == 7) {
						// Vanilla sea lantern quads declare no per-quad emission, so this only proves the value is not
						// invented; nonzero per-quad emission is covered by TERRAIN_MODEL_PARITY_EMISSIVE_SOURCE.
						for (final var quad : quads) {
							final java.util.Set<Integer> sourceEmission = new java.util.HashSet<>();
							mod.chiselsandbits.client.model.face.FaceManager.getInstance().extractQuads(
									new BlockInformation(Blocks.SEA_LANTERN.defaultBlockState(), Optional.empty()),
									quad.face(), client.level, pos,
									component -> sourceEmission.add(component.material().lightEmission()));
							if (sourceEmission.size() != 1 || !sourceEmission.contains(quad.material().lightEmission())) {
								throw new AssertionError("Sea lantern changed its source quad light emission: "
										+ quad.material().lightEmission() + " vs " + sourceEmission);
							}
						}

					} else {
						if (quads.stream().anyMatch(quad -> quad.material().layer() != ChunkSectionLayer.TRANSLUCENT)) {
							throw new AssertionError("Fluid/glass geometry lost its native translucent layer");
						}
						if (fixture == 6 && quads.stream().anyMatch(quad ->
								quad.material().sprite().contents().isAnimated()
										&& direct.metadata().parts().stream()
												.noneMatch(part -> (part.materialFlags()
														& net.minecraft.client.resources.model.geometry.BakedQuad.FLAG_ANIMATED) != 0))) {
							throw new AssertionError("Animated fluid atlas sprite lost its animation material flag");
						}
						if (fixture == 9 && (quads.stream().filter(quad -> quad.cullFace() != null).count() != 1
								|| quads.stream().anyMatch(quad -> quad.cullFace() != null
										&& (quad.cullFace() != Direction.WEST || quad.face() != Direction.WEST)))) {
							throw new AssertionError("Translucent boundary faces must keep the legacy outer-face cull group");
						}
					}
				}
				final var accessor = (ISingleBlockAxisAlignedAreaAccessor) entity.createSnapshot();
				final var neighborhood = IBlockNeighborhood.around(entity);
				final var manager = ChiseledBlockStateModelManager.getInstance();
				final var key = manager.keyFor(client.level, pos, accessor, neighborhood);
				final var legacy = new ChiseledBlockModelInformationBuilder(
						accessor, key, client.level, pos).build();
				final String mismatch = TerrainLegacyComparison.legacyOutputMismatch(legacy, direct.geometry());
				if (mismatch != null || !legacy.materials().equals(direct.metadata().materials())) {
					throw new AssertionError("World terrain fixture " + fixture
							+ " geometry/cull group/source tint/UV metadata differs from independent legacy output: " + mismatch);
				}
				// Hook-bypass fallback must reproduce the legacy directional QuadCollection groups, not a no-cull variant.
				final String fallbackMismatch = TerrainLegacyComparison.legacyOutputMismatch(direct.fallback(), direct.geometry());
				if (fallbackMismatch != null || !direct.fallback().materials().equals(legacy.materials())) {
					throw new AssertionError("World terrain fixture " + fixture
							+ " lazy fallback cull groups differ from legacy output: " + fallbackMismatch);
				}
			});
			if (fixture == 9) {
				checkTranslucentBoundaryNeighbours(context, world, pos);
			}
		}
		LogUtils.getLogger().info("TERRAIN_WORLD_CHECK_MATERIALS: cutout leaves/glass, animated translucent water, sea lantern (block-light source, zero per-quad model emission) and translucent boundary emitted natively; waterlogged/mixed fluid-cutout interfaces retained fallback");
	}

	/**
	 * Host culling of the legacy WEST cull group is decided by the neighbour, exactly as for the legacy baked model:
	 * an opaque full block hides the translucent boundary face, while chiseled neighbours never occlude (their block
	 * state is non-occluding), so adjacent chiseled translucent faces render as they do on the legacy path.
	 */
	private static void checkTranslucentBoundaryNeighbours(
			final ClientGameTestContext context, final TestSingleplayerContext world, final BlockPos pos
	) {
		final BlockPos west = pos.west();
		final var original = world.getServer().computeOnServer(server ->
				world.getConnection().getServerLevel().getBlockState(west));
		try {
			for (final var neighbour : new net.minecraft.world.level.block.state.BlockState[]{
					Blocks.STONE.defaultBlockState(), Blocks.GLASS.defaultBlockState(), ModBlocks.CHISELED_BLOCK.get().defaultBlockState()}) {
				world.getServer().runOnServer(server -> {
					final var level = world.getConnection().getServerLevel();
					level.setBlockAndUpdate(west, neighbour);
					if (level.getBlockEntity(west) instanceof ChiseledBlockEntity chiseled) {
						final StateEntryStorage storage = new StateEntryStorage(16);
						storage.setBlockInformation(15, 7, 7, new BlockInformation(Blocks.WATER.defaultBlockState(), Optional.empty()));
						chiseled.replaceStorage(storage);
					}
				});
				context.waitFor(client -> client.level.getBlockState(west).getBlock() == neighbour.getBlock()
						&& (!(client.level.getBlockEntity(west) instanceof ChiseledBlockEntity chiseled) || nonAirBits(chiseled) == 1), 600);
				final AtomicInteger completed = new AtomicInteger();
				context.runOnClient(client -> ChiseledBlockModelDataExecutor.updateModelDataCore(
						(ChiseledBlockEntity) client.level.getBlockEntity(pos), completed::incrementAndGet));
				context.waitFor(client -> completed.get() > 0
						&& ((ChiseledBlockEntity) client.level.getBlockEntity(pos)).getBlockModelData()
								.getData(ModModelProperties.TERRAIN_RENDER_DATA) != null, 600);
				final boolean expectedVisible = !neighbour.is(Blocks.STONE);
				context.runOnClient(client -> {
					final var self = client.level.getBlockState(pos);
					final boolean visible = net.minecraft.world.level.block.Block.shouldRenderFace(
							self, client.level.getBlockState(west), Direction.WEST);
					if (visible != expectedVisible) {
						throw new AssertionError("Host culling of the WEST boundary group against " + neighbour
								+ " was " + visible + ", expected legacy semantics " + expectedVisible);
					}
					final var direct = ((ChiseledBlockEntity) client.level.getBlockEntity(pos)).getBlockModelData()
							.getData(ModModelProperties.TERRAIN_RENDER_DATA);
					final long faces = direct.geometry().parts().stream().flatMap(part -> part.quads().stream())
							.filter(quad -> quad.cullFace() == Direction.WEST).count();
					if (faces != (neighbour.is(ModBlocks.CHISELED_BLOCK.get()) ? 0 : 1)) {
						throw new AssertionError("Matching fluid boundary was not culled, or another neighbour hid it");
					}
				});
			}
		} finally {
			world.getServer().runOnServer(server ->
					world.getConnection().getServerLevel().setBlockAndUpdate(west, original));
		}
		LogUtils.getLogger().info("TERRAIN_WORLD_CHECK_BOUNDARY_NEIGHBOURS: opaque host culling and matching-fluid neighbour culling passed");
	}

	private static void checkFluidBoundaryUpdates(final ClientGameTestContext context, final TestSingleplayerContext world) {
		// The two positions are in different chunk sections.
		final BlockPos left = new BlockPos(-1, 56, 6);
		final BlockPos right = left.east();
		world.getServer().runOnServer(server -> {
			final var level = world.getConnection().getServerLevel();
			level.setBlockAndUpdate(left, ModBlocks.CHISELED_BLOCK.get().defaultBlockState());
			level.setBlockAndUpdate(right, ModBlocks.CHISELED_BLOCK.get().defaultBlockState());
			((ChiseledBlockEntity) level.getBlockEntity(left)).replaceStorage(fluidFace(15, 4, Blocks.WATER));
			((ChiseledBlockEntity) level.getBlockEntity(right)).replaceStorage(fluidFace(0, 2, Blocks.WATER));
		});
		awaitFluidFaces(context, left, right, 16, 8, 8, 0);
		checkTranslucentSectionBuffers(context, world, left);

		context.runOnClient(client -> client.level.getChunkSource().drop(new ChunkPos(right.getX() >> 4, right.getZ() >> 4)));
		context.waitFor(client -> !client.level.isLoaded(right)
				&& boundaryArea(client, left, Direction.EAST) == 16, 600);
		world.getServer().runOnServer(server -> {
			final var level = world.getConnection().getServerLevel();
			server.getPlayerList().getPlayers().getFirst().connection.send(
					new ClientboundLevelChunkWithLightPacket(level.getChunkAt(right), level.getLightEngine(), null, null));
			ChiselsAndBits.getInstance().getNetworkChannel().sendToTrackingChunk(
					new UpdateBlockEntityPacket((ChiseledBlockEntity) level.getBlockEntity(right)), level.getChunkAt(right));
		});
		try {
			awaitFluidFaces(context, left, right, 16, 8, 8, 0);
		} catch (final AssertionError failure) {
			context.runOnClient(client -> LogUtils.getLogger().error("Reload diagnostics: loaded={}, entities={},{}; areas={},{}",
					client.level.isLoaded(right), client.level.getBlockEntity(left), client.level.getBlockEntity(right),
					boundaryArea(client, left, Direction.EAST), boundaryArea(client, right, Direction.WEST)));
			throw failure;
		}
		LogUtils.getLogger().info("TERRAIN_WORLD_CHECK_CHUNK_LIFECYCLE: unloaded and reloaded neighbour chunk rebuilt automatically");

		for (final var extra : List.of(Blocks.GLASS, Blocks.OAK_STAIRS)) {
			world.getServer().runOnServer(server -> {
				final StateEntryStorage mixed = fluidFace(0, 2, Blocks.WATER);
				mixed.setBlockInformation(1, 4, 4, new BlockInformation(extra.defaultBlockState(), Optional.empty()));
				((ChiseledBlockEntity) world.getConnection().getServerLevel().getBlockEntity(right)).replaceStorage(mixed);
			});
			context.waitFor(client -> client.level.getBlockEntity(right) instanceof ChiseledBlockEntity entity
					&& nonAirBits(entity) == 9
					&& entity.stream().anyMatch(entry -> entry.getBlockInformation().blockState().is(extra))
					&& entity.getBlockModelData().getData(ModModelProperties.MODEL) != null
					&& boundaryArea(client, left, Direction.EAST) == 8
					&& boundaryArea(client, right, Direction.WEST) == 0, 600);
			context.runOnClient(client -> {
				final var leftData = ((ChiseledBlockEntity) client.level.getBlockEntity(left)).getBlockModelData()
						.getData(ModModelProperties.TERRAIN_RENDER_DATA);
				if (bakedBoundaryArea(leftData.fallback(), Direction.EAST) != 8) {
					throw new AssertionError("Native candidate's baked fallback retained hidden fluid faces");
				}
			});
		}
		world.getServer().runOnServer(server -> {
			final StateEntryStorage mixed = fluidFace(0, 2, Blocks.WATER);
			mixed.setBlockInformation(0, 4, 4, new BlockInformation(Blocks.OAK_STAIRS.defaultBlockState(), Optional.empty()));
			((ChiseledBlockEntity) world.getConnection().getServerLevel().getBlockEntity(right)).replaceStorage(mixed);
		});
		context.waitFor(client -> client.level.getBlockEntity(right) instanceof ChiseledBlockEntity entity
				&& nonAirBits(entity) == 8
				&& entity.stream().anyMatch(entry -> entry.getBlockInformation().blockState().is(Blocks.OAK_STAIRS))
				&& entity.getBlockModelData().getData(ModModelProperties.MODEL) != null
				&& boundaryArea(client, left, Direction.EAST) == 9
				&& boundaryArea(client, right, Direction.WEST) >= 0, 600);
		context.runOnClient(client -> {
			final var model = ((ChiseledBlockEntity) client.level.getBlockEntity(right)).getBlockModelData()
					.getData(ModModelProperties.MODEL);
			final boolean hasWater = model.parts().stream().anyMatch(part -> part.source().blockState().is(Blocks.WATER));
			final boolean hiddenWater = model.parts().stream().filter(part -> part.source().blockState().is(Blocks.WATER))
					.allMatch(part -> part.getQuads(Direction.WEST).isEmpty());
			final boolean stairVisible = model.parts().stream().filter(part -> part.source().blockState().is(Blocks.OAK_STAIRS))
					.anyMatch(part -> !part.getQuads(Direction.WEST).isEmpty());
			final var leftData = ((ChiseledBlockEntity) client.level.getBlockEntity(left)).getBlockModelData()
					.getData(ModModelProperties.TERRAIN_RENDER_DATA);
			if (!hasWater || !hiddenWater || !stairVisible || bakedBoundaryArea(leftData.fallback(), Direction.EAST) != 9) {
				throw new AssertionError("Mixed boundary must hide matching water but retain the non-cubic stair face");
			}
		});
		LogUtils.getLogger().info("TERRAIN_WORLD_CHECK_MIXED_FALLBACK: native and unsupported chiseled neighbours, including a boundary stair, culled matching fluid");

		world.getServer().runOnServer(server ->
				((ChiseledBlockEntity) world.getConnection().getServerLevel().getBlockEntity(right))
						.replaceStorage(fluidFace(0, 2, Blocks.LAVA)));
		awaitFluidFaces(context, left, right, 16, 8, 16, 8);

		world.getServer().runOnServer(server ->
				((ChiseledBlockEntity) world.getConnection().getServerLevel().getBlockEntity(right))
						.replaceStorage(fluidFace(0, 2, Blocks.WATER)));
		awaitFluidFaces(context, left, right, 16, 8, 8, 0);

		world.getServer().runOnServer(server ->
				((ChiseledBlockEntity) world.getConnection().getServerLevel().getBlockEntity(left))
						.replaceStorage(fluidFace(15, 2, Blocks.WATER)));
		awaitFluidFaces(context, left, right, 8, 8, 0, 0);

		world.getServer().runOnServer(server ->
				world.getConnection().getServerLevel().setBlockAndUpdate(right, Blocks.AIR.defaultBlockState()));
		context.waitFor(client -> client.level.getBlockState(right).isAir()
				&& boundaryArea(client, left, Direction.EAST) == 8, 600);
		LogUtils.getLogger().info("TERRAIN_WORLD_CHECK_FLUID_BOUNDARY: cross-chunk partial overlap, different fluid, edits and removal rebuilt without manual refresh");
	}

	private static void checkTranslucentSectionBuffers(
			final ClientGameTestContext context, final TestSingleplayerContext world, final BlockPos pos
	) {
		final String property = "scena.experimentalNativeTerrainGeometry";
		final String original = System.getProperty(property);
		try {
			world.getConnection().waitForChunksRender();
			context.waitTicks(10);
			final var nativeMesh = captureTranslucentSection(context, world, pos, true);
			final var bakedMesh = captureTranslucentSection(context, world, pos, false);
			if (!nativeMesh.sorted() || !bakedMesh.sorted() || !nativeMesh.fixture() || !bakedMesh.fixture()
					|| nativeMesh.quads().isEmpty()
					|| !nativeMesh.quads().equals(bakedMesh.quads())) {
				throw new AssertionError("Native and baked translucent section buffers differ: native="
						+ nativeMesh.quads().size() + ", baked=" + bakedMesh.quads().size()
						+ ", sorted=" + nativeMesh.sorted() + "/" + bakedMesh.sorted()
						+ ", fixture=" + nativeMesh.fixture() + "/" + bakedMesh.fixture());
			}
			LogUtils.getLogger().info("TERRAIN_WORLD_CHECK_TRANSLUCENT_BUFFERS: native and baked lit vertices match and both sections have sorted indices");
		} finally {
			context.runOnClient(client -> {
				if (original == null) {
					System.clearProperty(property);
				} else {
					System.setProperty(property, original);
				}
				client.level.setSectionDirtyWithNeighbors(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
			});
		}
	}

	private static TerrainWorldMeshCapture.Snapshot captureTranslucentSection(
			final ClientGameTestContext context, final TestSingleplayerContext world, final BlockPos pos, final boolean nativeGeometry
	) {
		context.runOnClient(client -> {
			System.setProperty("scena.experimentalNativeTerrainGeometry", Boolean.toString(nativeGeometry));
			TerrainWorldMeshCapture.arm();
			client.levelExtractor.setSectionDirty(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
		});
		world.getConnection().waitForChunksRender();
		context.waitFor(client -> TerrainWorldMeshCapture.capture() != null, 600);
		return TerrainWorldMeshCapture.capture();
	}

	private static void benchmarkSectionCompiles(final ClientGameTestContext context, final TestSingleplayerContext world) {
		final BlockPos left = new BlockPos(-1, 56, 6);
		final BlockPos right = left.east();
		world.getServer().runOnServer(server -> {
			final var level = world.getConnection().getServerLevel();
			((ChiseledBlockEntity) level.getBlockEntity(left)).replaceStorage(fluidFace(15, 4, Blocks.WATER));
			((ChiseledBlockEntity) level.getBlockEntity(right)).replaceStorage(fluidFace(0, 2, Blocks.WATER));
			TerrainWorldCompileBenchmark.createScene(level);
		});
		awaitFluidFaces(context, left, right, 16, 8, 8, 0);
		context.waitFor(TerrainWorldCompileBenchmark::sceneReady, 600);
		world.getConnection().waitForChunksRender();
		context.waitTicks(20);
		final var benchmark = new TerrainWorldCompileBenchmark();
		try {
			while (!benchmark.done()) {
				benchmark.record(captureTranslucentSection(context, world, left, benchmark.useNative()));
			}
			LogUtils.getLogger().info(benchmark.result("fabric"));
		} finally {
			context.runOnClient(client -> System.setProperty("scena.experimentalNativeTerrainGeometry", "true"));
		}
	}

	private static StateEntryStorage fluidFace(final int x, final int height, final net.minecraft.world.level.block.Block fluid) {
		final StateEntryStorage storage = new StateEntryStorage(16);
		final var material = new BlockInformation(fluid.defaultBlockState(), Optional.empty());
		for (int y = 4; y < 4 + height; y++) {
			for (int z = 4; z < 8; z++) {
				storage.setBlockInformation(x, y, z, material);
			}
		}
		return storage;
	}

	private static void checkGlassBoundaryUpdates(final ClientGameTestContext context, final TestSingleplayerContext world) {
		final BlockPos left = new BlockPos(-1, 56, 6);
		final BlockPos right = left.east();
		world.getServer().runOnServer(server -> {
			final var level = world.getConnection().getServerLevel();
			level.setBlockAndUpdate(right, ModBlocks.CHISELED_BLOCK.get().defaultBlockState());
			((ChiseledBlockEntity) level.getBlockEntity(left)).replaceStorage(fluidFace(15, 4, Blocks.GLASS));
			((ChiseledBlockEntity) level.getBlockEntity(right)).replaceStorage(fluidFace(0, 2, Blocks.GLASS));
		});
		awaitFluidFaces(context, left, right, 16, 8, 8, 0);
		world.getServer().runOnServer(server ->
				((ChiseledBlockEntity) world.getConnection().getServerLevel().getBlockEntity(right))
						.replaceStorage(fluidFace(0, 2, Blocks.STAINED_GLASS.red())));
		awaitFluidFaces(context, left, right, 16, 8, 16, 8);
		LogUtils.getLogger().info("TERRAIN_WORLD_CHECK_GLASS_BOUNDARY: matching partial glass culled, different glass retained");
	}

	private static void awaitFluidFaces(
			final ClientGameTestContext context, final BlockPos left, final BlockPos right,
			final int leftBits, final int rightBits, final int leftArea, final int rightArea
	) {
		context.waitFor(client -> client.level.getBlockEntity(left) instanceof ChiseledBlockEntity a
				&& client.level.getBlockEntity(right) instanceof ChiseledBlockEntity b
				&& nonAirBits(a) == leftBits && nonAirBits(b) == rightBits
				&& boundaryArea(client, left, Direction.EAST) == leftArea
				&& boundaryArea(client, right, Direction.WEST) == rightArea, 600);
	}

	private static int boundaryArea(final Minecraft client, final BlockPos pos, final Direction face) {
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
		double area = 0;
		for (final var part : direct.geometry().parts()) {
			for (final var quad : part.quads()) {
				if (quad.cullFace() == face) {
					final var a = quad.v0();
					final var b = quad.v2();
					area += Math.abs((a.y() - b.y()) * (a.z() - b.z()));
				}
			}
		}
		return (int) Math.round(area * 256);
	}

	private static int bakedBoundaryArea(final ChiseledBlockModelInformation model, final Direction face) {
		double area = 0;
		for (final var part : model.parts()) {
			for (final var quad : part.getQuads(face)) {
				final var a = quad.position(0);
				final var b = quad.position(2);
				area += Math.abs((a.y() - b.y()) * (a.z() - b.z()));
			}
		}
		return (int) Math.round(area * 256);
	}

	private static void checkHookBypassFallback(
			final ClientGameTestContext context, final TestSingleplayerContext world, final BlockPos pos
	) {
		final String property = "scena.experimentalNativeTerrainGeometry";
		final String original = System.getProperty(property);
		try {
			context.runOnClient(client -> {
				System.setProperty(property, "false");
				client.level.setSectionDirtyWithNeighbors(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
			});
			world.getConnection().waitForChunksRender();
			context.waitFor(client -> {
				final var direct = ((ChiseledBlockEntity) client.level.getBlockEntity(pos)).getBlockModelData()
						.getData(ModModelProperties.TERRAIN_RENDER_DATA);
				return direct != null && direct.isFallbackBuilt();
			}, 600);
			LogUtils.getLogger().info("TERRAIN_WORLD_CHECK_HOOK_BYPASS: real Indigo section compilation used lazy baked fallback with geometry hook disabled");
		} finally {
			context.runOnClient(client -> {
				if (original == null) {
					System.clearProperty(property);
				} else {
					System.setProperty(property, original);
				}
				client.level.setSectionDirtyWithNeighbors(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
			});
		}
	}

	private static void checkSealedCavity(
			final ClientGameTestContext context, final TestSingleplayerContext world, final BlockPos pos
	) {
		world.getServer().runOnServer(server -> {
			final StateEntryStorage storage = new StateEntryStorage(16);
			storage.initializeWith(new BlockInformation(Blocks.STONE.defaultBlockState(), Optional.empty()));
			storage.setBlockInformation(8, 8, 8, BlockInformation.AIR);
			((ChiseledBlockEntity) world.getConnection().getServerLevel().getBlockEntity(pos)).replaceStorage(storage);
		});
		context.waitFor(client -> {
			if (!(client.level.getBlockEntity(pos) instanceof ChiseledBlockEntity entity) || nonAirBits(entity) != 4095) {
				return false;
			}
			final IBlockModelData data = entity.getBlockModelData();
			final TerrainGeometry geometry = data.getData(ModModelProperties.TERRAIN_GEOMETRY);
			final var direct = data.getData(ModModelProperties.TERRAIN_RENDER_DATA);
			final ChiseledBlockModelInformation legacy = direct == null ? null : direct.fallback();
			return geometry != null && legacy != null
					&& geometry.parts().stream().flatMap(part -> part.quads().stream()).count() == 6
					&& geometry.parts().stream().flatMap(part -> part.quads().stream()).allMatch(quad -> quad.cullFace() != null)
					&& legacy.parts().stream().flatMap(part -> part.getQuads(null).stream()).count() == 6;
		}, 600);
		LogUtils.getLogger().info("TERRAIN_WORLD_CHECK_CAVITY: sealed opaque cavity omitted six internal quads");
	}

	private static void checkBoundaryEdits(
			final ClientGameTestContext context, final TestSingleplayerContext world, final BlockPos boundary
	) {
		for (final Direction direction : Direction.values()) {
			final BlockPos neighbourPos = boundary.relative(direction);
			for (int state = 0; state < 4; state++) {
				final int variant = state;
				world.getServer().runOnServer(server -> {
					final var level = world.getConnection().getServerLevel();
					level.setBlockAndUpdate(neighbourPos, variant == 0 ? Blocks.AIR.defaultBlockState()
							: variant == 1 ? Blocks.STONE.defaultBlockState() : ModBlocks.CHISELED_BLOCK.get().defaultBlockState());
					if (variant >= 2) {
						final StateEntryStorage storage = new StateEntryStorage(16);
						storage.setBlockInformation(4, 4, 4, new BlockInformation(Blocks.STONE.defaultBlockState(), Optional.empty()));
						if (variant == 3) {
							storage.setBlockInformation(5, 4, 4, new BlockInformation(Blocks.STONE.defaultBlockState(), Optional.empty()));
						}
						((ChiseledBlockEntity) level.getBlockEntity(neighbourPos)).replaceStorage(storage);
					}
				});
				context.waitFor(client -> variant == 0 ? client.level.getBlockState(neighbourPos).isAir()
						: variant == 1 ? client.level.getBlockState(neighbourPos).is(Blocks.STONE)
						: client.level.getBlockEntity(neighbourPos) instanceof ChiseledBlockEntity neighbour
								&& nonAirBits(neighbour) == variant - 1, 600);
				final AtomicInteger completed = new AtomicInteger();
				context.runOnClient(client -> ChiseledBlockModelDataExecutor.updateModelDataCore(
						(ChiseledBlockEntity) client.level.getBlockEntity(boundary), completed::incrementAndGet));
				context.waitFor(client -> completed.get() > 0, 600);
				context.runOnClient(client -> assertPublishedGeometry(client, boundary));
			}
			world.getServer().runOnServer(server ->
					world.getConnection().getServerLevel().setBlockAndUpdate(neighbourPos, Blocks.AIR.defaultBlockState()));
		}
		world.getServer().runOnServer(server -> {
			final StateEntryStorage storage = new StateEntryStorage(16);
			final var stone = new BlockInformation(Blocks.STONE.defaultBlockState(), Optional.empty());
			for (int x = 0; x < 16; x++) {
				for (int y = 0; y < 7; y++) {
					for (int z = 0; z < 16; z++) {
						storage.setBlockInformation(x, y, z, stone);
					}
				}
			}
			((ChiseledBlockEntity) world.getConnection().getServerLevel().getBlockEntity(boundary)).replaceStorage(storage);
		});
		context.waitFor(client -> client.level.getBlockEntity(boundary) instanceof ChiseledBlockEntity entity
				&& nonAirBits(entity) == 16 * 7 * 16
				&& entity.getBlockModelData().getData(ModModelProperties.TERRAIN_GEOMETRY) != null
				&& entity.getBlockModelData().getData(ModModelProperties.TERRAIN_GEOMETRY).parts().stream()
						.flatMap(part -> part.quads().stream()).flatMap(quad -> java.util.stream.Stream.of(quad.v0(), quad.v1(), quad.v2(), quad.v3()))
						.mapToDouble(vertex -> vertex.y()).max().orElse(1) == 7d / 16d, 600);
		context.runOnClient(client -> assertPublishedGeometry(client, boundary));
	}

	private static long nonAirBits(final ChiseledBlockEntity entity) {
		return entity.stream().filter(entry -> !entry.getBlockInformation().isAir()).count();
	}

	private static void assertPublishedGeometry(final Minecraft client, final BlockPos pos) {
		final ChiseledBlockEntity entity = (ChiseledBlockEntity) client.level.getBlockEntity(pos);
		final IBlockModelData data = entity.getBlockModelData();
		final TerrainGeometry geometry = data.getData(ModModelProperties.TERRAIN_GEOMETRY);
		final var direct = data.getData(ModModelProperties.TERRAIN_RENDER_DATA);
		final ChiseledBlockModelInformation legacy = direct == null ? null : direct.fallback();

		if (geometry == null || legacy == null) {
			throw new AssertionError("Stone fixture did not publish direct geometry at " + pos + "; inspect terrain rejection diagnostic");
		}

		final String mismatch = TerrainLegacyComparison.legacyOutputMismatch(legacy, geometry);

		if (mismatch != null) {
			throw new AssertionError("Published direct geometry differs from its coherent legacy model: " + mismatch);
		}
	}
}

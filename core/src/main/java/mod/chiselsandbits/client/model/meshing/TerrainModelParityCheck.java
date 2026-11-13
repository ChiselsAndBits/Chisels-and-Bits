package mod.chiselsandbits.client.model.meshing;

import mod.chiselsandbits.client.model.face.FaceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;
import java.util.concurrent.atomic.AtomicReference;
import java.lang.reflect.Proxy;

import com.communi.suggestu.scena.core.client.rendering.terrain.TerrainGeometry;
import com.communi.suggestu.scena.core.client.rendering.terrain.FaceTriangleClipper;
import com.communi.suggestu.scena.core.client.rendering.terrain.TerrainGeometryPart;
import com.communi.suggestu.scena.core.client.rendering.terrain.TerrainQuad;
import com.communi.suggestu.scena.core.client.rendering.terrain.TerrainVertex;
import com.communi.suggestu.scena.core.util.SingleBlockBlockAndTintGetter;
import com.mojang.logging.LogUtils;
import mod.chiselsandbits.api.block.storage.StateEntryStorage;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.multistate.accessor.ISingleBlockAxisAlignedAreaAccessor;
import mod.chiselsandbits.api.neighborhood.IBlockNeighborhood;
import mod.chiselsandbits.api.neighborhood.IBlockNeighborhoodBuilder;
import mod.chiselsandbits.client.model.block.ChiseledBlockStateModelManager;
import mod.chiselsandbits.client.model.block.ChiseledTerrainModelSelection;
import mod.chiselsandbits.client.model.block.ChiseledTerrainRenderData;
import mod.chiselsandbits.client.model.block.ChiseledTerrainColorScope;
import mod.chiselsandbits.client.model.block.ChiseledBlockStateModel;
import mod.chiselsandbits.client.model.block.ChiseledModelDataAccess;
import com.communi.suggestu.scena.core.client.models.data.IBlockModelData;
import com.communi.suggestu.scena.core.client.models.data.IModelDataBuilder;
import com.communi.suggestu.scena.core.client.rendering.terrain.VanillaTerrainGeometryContext;
import mod.chiselsandbits.registrars.ModModelProperties;
import mod.chiselsandbits.client.reloading.ClientResourceReloadingManager;
import mod.chiselsandbits.client.colors.ChiseledBlockColorProvider;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import mod.chiselsandbits.client.model.builder.ChiseledBlockModelInformationBuilder;
import mod.chiselsandbits.client.model.builder.ChiseledBlockModelMaterial;
import mod.chiselsandbits.client.model.information.ChiseledBlockModelCacheKey;
import mod.chiselsandbits.client.model.information.ChiseledBlockModelInformation;
import mod.chiselsandbits.client.util.QuadGenerationUtils;
import mod.chiselsandbits.multistate.snapshot.SimpleSnapshot;
import mod.chiselsandbits.registrars.ModBlocks;
import org.joml.Vector3fc;
import org.slf4j.Logger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.TriState;
import net.minecraft.world.level.block.Blocks;

public final class TerrainModelParityCheck {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String PROPERTY = "chiselsandbits.runTerrainModelParityCheck";
	private static final int TIMEOUT_TICKS = 1200;

	private static boolean finished;
	private static int ticks;

	private TerrainModelParityCheck() {
		throw new IllegalStateException("Cannot instantiate utility class");
	}

	public static boolean isEnabled() {
		return Boolean.getBoolean(PROPERTY);
	}

	public static void onClientTick() {
		if (finished || !isEnabled()) {
			return;
		}

		if (++ticks > TIMEOUT_TICKS) {
			finish(false, "client resources did not become ready within " + TIMEOUT_TICKS + " ticks", null);
			return;
		}

		if (ticks < 5 || !Minecraft.getInstance().isGameLoadFinished()) {
			return;
		}

		try {
			CompletableFuture.runAsync(TerrainModelParityCheck::runChecks)
					.orTimeout(30, TimeUnit.SECONDS).join();
			finish(true, "direct geometry matched legacy and rectangle test models; split tessellation fell back", null);
		} catch (final Throwable throwable) {
			finish(false, throwable.getMessage(), throwable);
		}
	}

	private static void runChecks() {
		for (final BlockPos pos : List.of(BlockPos.ZERO, new BlockPos(-6, 56, 6))) {
			runChecks(pos);
		}
	}

	private static void runChecks(final BlockPos pos) {
		final BlockAndTintGetter level = new SingleBlockBlockAndTintGetter.Builder()
				.withBlockState(Blocks.AIR.defaultBlockState())
				.withPos(pos)
				.createSingleBlockBlockAndTintGetter();

		checkCollidingPartOrder(level, pos);
		for (final Fixture fixture : fixtures()) {
			final ISingleBlockAxisAlignedAreaAccessor accessor = fixture.snapshot();
			final long fullBuilds = ChiseledBlockModelInformationBuilder.fullModelBuildCount();
			final var independentKey = ChiseledBlockStateModelManager.getInstance().keyFor(level, pos, accessor, IBlockNeighborhood.EMPTY);
			final var metadata = new ChiseledBlockModelInformationBuilder(accessor, independentKey, level, pos).describeTerrainSources();
			if (metadata == null) {
				throw new IllegalStateException(fixture.name() + ": independent source metadata rejected fixture");
			}
			final var independentResult = DirectTerrainGeometryBuilder.buildWithoutSealedAir(
					metadata, ChiseledRenderSnapshot.of(accessor), level, pos);
			if (!independentResult.supported()) {
				throw new IllegalStateException(fixture.name() + ": independent geometry rejected fixture: " + independentResult.rejection());
			}
			if (fixture.name().contains("leaves")) {
				checkCutoutFixture(fixture.name(), metadata, independentResult, ChiseledRenderSnapshot.of(accessor), level, pos);
			}
			checkSourceLayoutRejectsSplits(metadata,
					ChiseledRenderSnapshot.of(accessor).greedyRectangles(FaceCullingPolicy.ANY_OCCUPIED), level, pos);
			final var publication = new ChiseledTerrainRenderData(
					metadata, independentResult.geometry(), independentResult.fallbackGeometry());
			if (publication.isFallbackBuilt() || ChiseledBlockModelInformationBuilder.fullModelBuildCount() != fullBuilds) {
				throw new IllegalStateException(fixture.name() + ": independent native production built a full baked model");
			}
			checkCompileSnapshot(metadata, independentResult, level, pos);
			final ChiseledBlockModelInformation legacy = ChiseledBlockStateModelManager.getInstance()
					.get(level, pos, accessor, IBlockNeighborhood.EMPTY);
			if (!metadata.materials().equals(legacy.materials())
					|| !TerrainLegacyComparison.matchesLegacyOutput(legacy, independentResult.fallbackGeometry())
					|| !signature(legacy).equals(signature(publication.fallback()))) {
				throw new IllegalStateException(fixture.name() + ": independent metadata or lazy fallback differs from legacy: "
						+ TerrainLegacyComparison.legacyOutputMismatch(legacy, independentResult.fallbackGeometry())
						+ ", materials=" + metadata.materials().equals(legacy.materials())
						+ ", signature=" + signature(legacy).equals(signature(publication.fallback())));
			}
			final var firstFallback = publication.fallback();
			if (CompletableFuture.supplyAsync(publication::fallback,
					task -> Thread.ofPlatform().name("C&B fallback parity").start(task)).join() != firstFallback) {
				throw new IllegalStateException(fixture.name() + ": lazy fallback was not memoized across threads");
			}
			final ChiseledRenderSnapshot snapshot = ChiseledRenderSnapshot.of(accessor);
			if (!RectangleSubsetPreflight.supportsAllMaterials(snapshot)) {
				throw new IllegalStateException(fixture.name() + ": unsupported material in parity fixture");
			}
			if (!RectangleSubsetPreflight.supportsSourceFaces(legacy)) {
				throw new IllegalStateException(fixture.name() + ": legacy source-model quads are not supported by the rectangle subset");
			}

			final GreedyRectangles rectangles = snapshot.greedyRectangles(FaceCullingPolicy.ANY_OCCUPIED);
			checkResolvedVertices(fixture.name(), rectangles, level, pos);
			final DirectTerrainGeometryBuilder.BuildResult directResult =
					TerrainLegacyComparison.build(legacy, rectangles, level, pos);
			if (!directResult.supported()) {
				throw new IllegalStateException(fixture.name() + ": direct geometry fell back: " + directResult.rejection());
			}
			final TerrainGeometry directGeometry = directResult.geometry();
			if (!TerrainLegacyComparison.matchesLegacyOutput(legacy, directGeometry)) {
				throw new IllegalStateException(fixture.name() + ": direct geometry differs from ordered legacy output");
			}
			assertDifferingTessellationFallsBack(fixture.name(), legacy, directGeometry);
			assertOutputMutationsFallBack(fixture.name(), legacy, directGeometry);

			final ChiseledBlockModelCacheKey key = legacy.key();
			if (!RectangleSubsetPreflight.supportsStaticSourceModels(key)) {
				throw new IllegalStateException(fixture.name() + ": static source-model preflight rejected the fixture: "
						+ RectangleSubsetPreflight.sourceModelRejectionReason(key));
			}

			final ChiseledBlockModelInformation rectangle = new ChiseledBlockModelInformationBuilder(
					accessor,
					key,
					level,
					pos
			).buildFromRectangles(rectangles, legacy.particleMaterial());
			if (rectangle == null) {
				throw new IllegalStateException(fixture.name() + ": rectangle builder rejected source geometry");
			}
			if (!RectangleSubsetPreflight.supportsSourceFaces(rectangle)) {
				throw new IllegalStateException(fixture.name() + ": generated rectangle-model quads are not supported by the subset");
			}
			if (!RectangleSubsetPreflight.hasCompatibleTintIndices(legacy.materials(), rectangle.materials())) {
				throw new IllegalStateException(fixture.name() + ": candidate tint indices do not match the legacy tint provider");
			}

			final OutputSignature legacySignature = signature(legacy);
			final OutputSignature rectangleSignature = signature(rectangle);
			if (!legacySignature.equals(rectangleSignature)) {
				throw new IllegalStateException(fixture.name() + ": output mismatch; legacy="
						+ legacySignature.quadCount() + " quads, rectangle=" + rectangleSignature.quadCount()
						+ " quads; legacy-only=" + difference(legacySignature.quads(), rectangleSignature.quads())
						+ "; rectangle-only=" + difference(rectangleSignature.quads(), legacySignature.quads()));
			}

			LOGGER.info("TERRAIN_MODEL_PARITY_FIXTURE {} at {} passed ({} quads, {} materials)",
					fixture.name(), pos.toShortString(), legacySignature.quadCount(), legacy.materials().size());
		}
		checkBoundaryNeighbours(level, pos);
		checkSealedAirGeometry(level, pos);
		checkTintScope(level, pos);
		checkHiddenUnsupportedMaterials(level, pos);
		checkMixedCutoutLayoutFallback(level, pos);
		for (final var block : List.of(Blocks.GLASS, Blocks.WATER, Blocks.LAVA, Blocks.SEA_LANTERN, Blocks.OAK_LEAVES))
		for (final int x : new int[]{7, 0}) {
			final StateEntryStorage storage = new StateEntryStorage(16);
			storage.setBlockInformation(x, 7, 7, info(block));
			final var accessor = new SimpleSnapshot(storage);
			final var snapshot = ChiseledRenderSnapshot.of(accessor);
			final ChiseledBlockModelCacheKey sourceKey = ChiseledBlockStateModelManager.getInstance()
					.keyFor(level, pos, accessor, IBlockNeighborhood.EMPTY);
			final var metadata = new ChiseledBlockModelInformationBuilder(accessor, sourceKey, level, pos)
					.describeTerrainSources(snapshot);
			if (metadata == null) {
				throw new IllegalStateException("Supported static material did not produce source metadata: " + block);
			}
			final var legacy = ChiseledBlockStateModelManager.getInstance().get(level, pos, accessor, IBlockNeighborhood.EMPTY);
			final var rectangles = snapshot.greedyRectangles(FaceCullingPolicy.ANY_OCCUPIED);
			final var direct = DirectTerrainGeometryBuilder.buildWithoutSealedAir(metadata, snapshot, level, pos);
			if (!direct.supported() || !TerrainLegacyComparison.matchesLegacyOutput(legacy, direct.fallbackGeometry())
					|| !TerrainLegacyComparison.matchesLegacyOutput(legacy, direct.geometry())) {
				throw new IllegalStateException("Static source material failed direct/lazy-fallback validation: " + block
						+ " at x=" + x + ": " + direct.rejection());
			}
			final var quads = direct.geometry().parts().stream().flatMap(part -> part.quads().stream()).toList();
			if (quads.isEmpty()) {
				throw new IllegalStateException("Static source material produced no geometry: " + block);
			}
			// Every layer keeps the legacy outer-face group so the host culls it against opaque neighbours.
			final long boundaryCulled = quads.stream().filter(quad -> quad.cullFace() != null).count();
			if (boundaryCulled != (x == 0 ? 1 : 0) || quads.stream().anyMatch(quad -> quad.cullFace() != null
					&& (quad.cullFace() != Direction.WEST || quad.face() != Direction.WEST))) {
				throw new IllegalStateException("Boundary cull groups differ from legacy for " + block + " at x=" + x);
			}
			if (block == Blocks.WATER && quads.stream().anyMatch(quad -> quad.material().layer() != ChunkSectionLayer.TRANSLUCENT)) {
				throw new IllegalStateException("Water geometry lost its native translucent layer");
			}
			if (block == Blocks.WATER || block == Blocks.LAVA || block == Blocks.GLASS) {
				if (quads.stream().filter(quad -> quad.material().sprite().contents().isAnimated())
						.anyMatch(quad -> (direct.geometry().parts().stream()
								.filter(part -> part.quads().contains(quad))
								.anyMatch(part -> (part.materialFlags() & BakedQuad.FLAG_ANIMATED) == 0)))) {
					throw new IllegalStateException("Animated atlas sprite lost its material flag: " + block);
				}
			}
			// Layer and quad emission are the source model's per-quad value (vanilla sea lantern models declare none);
			// block light comes from the chiseled block state, so it must be retained, never invented.
			for (final var quad : quads) {
				final java.util.Set<Integer> sourceEmission = new java.util.HashSet<>();
				final java.util.Set<ChunkSectionLayer> sourceLayers = new java.util.HashSet<>();
				FaceManager.getInstance().extractQuads(info(block), quad.face(), level, pos, component -> {
					sourceEmission.add(component.material().lightEmission());
					sourceLayers.add(component.material().layer());
				});
				if (!sourceLayers.equals(java.util.Set.of(quad.material().layer()))) {
					throw new IllegalStateException("Direct layer differs from its source quad: " + block
							+ " " + quad.face() + " " + quad.material().layer() + " vs " + sourceLayers);
				}
				if (sourceEmission.size() != 1 || !sourceEmission.contains(quad.material().lightEmission())) {
					throw new IllegalStateException("Direct light emission differs from its source quad: " + block
							+ " " + quad.face() + " " + quad.material().lightEmission() + " vs " + sourceEmission);
				}
			}
		}
		checkUnsupportedMaterialFallback(level, pos);
		checkEmissiveSourceQuads(level, pos);
		checkFluidBoundaryClipping(level, pos);
		checkGlassBoundaryClipping(level, pos);
		LOGGER.info("TERRAIN_MODEL_PARITY_EXPANDED static cutout/translucent and animated fluid sources retained source layer/emission metadata at {}", pos);
	}

	private static void checkGlassBoundaryClipping(final BlockAndTintGetter level, final BlockPos pos) {
		for (final Direction direction : Direction.values()) {
			final StateEntryStorage owner = new StateEntryStorage(16);
			final StateEntryStorage neighbour = new StateEntryStorage(16);
			final int ownerLayer = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 15 : 0;
			for (int u = 4; u < 8; u++) {
				for (int v = 5; v < 9; v++) {
					owner.setBlockInformation(NeighbourBoundary.cellX(direction.getAxis(), ownerLayer, u, v),
							NeighbourBoundary.cellY(direction.getAxis(), ownerLayer, u, v),
							NeighbourBoundary.cellZ(direction.getAxis(), ownerLayer, u, v), info(Blocks.GLASS));
					if (u < 6) {
						final int opposite = 15 - ownerLayer;
						neighbour.setBlockInformation(NeighbourBoundary.cellX(direction.getAxis(), opposite, u, v),
								NeighbourBoundary.cellY(direction.getAxis(), opposite, u, v),
								NeighbourBoundary.cellZ(direction.getAxis(), opposite, u, v), info(Blocks.GLASS));
					}
				}
			}
			final var accessor = new SimpleSnapshot(owner);
			final var snapshot = ChiseledRenderSnapshot.of(accessor);
			final var key = ChiseledBlockStateModelManager.getInstance().keyFor(level, pos, accessor, IBlockNeighborhood.EMPTY);
			final var builder = new ChiseledBlockModelInformationBuilder(accessor, key, level, pos);
			final var metadata = Objects.requireNonNull(builder.describeTerrainSources(snapshot));
			final var boundary = NeighbourBoundary.NONE.withSnapshot(direction, ChiseledRenderSnapshot.of(neighbour));
			final var result = DirectTerrainGeometryBuilder.buildWithoutSealedAir(metadata, snapshot, level, pos, boundary);
			if (!result.supported() || !TerrainLegacyComparison.matchesLegacyOutput(builder.build(boundary), result.geometry())
					|| !TerrainLegacyComparison.matchesLegacyOutput(builder.build(boundary), result.bakedGeometry())) {
				throw new IllegalStateException("Glass clipping differs between native and fallback on " + direction);
			}
			final var faces = result.geometry().parts().stream().flatMap(part -> part.quads().stream())
					.filter(quad -> quad.cullFace() == direction).toList();
			for (int u = 4; u < 8; u++) {
				for (int v = 5; v < 9; v++) {
					final float a = (u + 0.5f) / 16;
					final float b = (v + 0.5f) / 16;
					if (faces.stream().filter(face -> coversBoundary(face, direction.getAxis(), a, b)).count() != (u < 6 ? 0 : 1)) {
						throw new IllegalStateException("Glass coverage failed on " + direction);
					}
				}
			}
			final var different = DirectTerrainGeometryBuilder.buildWithoutSealedAir(metadata, snapshot, level, pos,
					NeighbourBoundary.NONE.withUniform(direction, info(Blocks.STAINED_GLASS.red())));
			if (!different.supported() || !TerrainLegacyComparison.matchesLegacyOutput(builder.build(), different.geometry())) {
				throw new IllegalStateException("Different glass must retain its boundary on " + direction);
			}
			if (direction == Direction.EAST) {
				FaceManager.runWithSourceTransform((source, component) -> {
					if (source.blockState().getBlock() != Blocks.GLASS || component.cullDirection() != direction) {
						return component;
					}
					final var vertices = component.vertexData().clone();
					final var vertex = vertices[0];
					vertices[0] = new com.communi.suggestu.scena.core.client.models.processing.VertexData(
							vertex.x(), vertex.y(), vertex.z(), vertex.u() + 0.05f, vertex.v(), vertex.vertexIndex());
					return new com.communi.suggestu.scena.core.client.models.processing.DeconstructedModelPartComponent(
							component.part(), vertices, component.cullDirection(), component.sourceQuad(), component.material());
				}, () -> {
					final var unsafeMetadata = Objects.requireNonNull(builder.describeTerrainSources(snapshot));
					final var unsafe = DirectTerrainGeometryBuilder.buildWithoutSealedAir(unsafeMetadata, snapshot, level, pos, boundary);
					if (unsafe.supported() || !signature(builder.build()).equals(signature(builder.build(boundary)))) {
						throw new IllegalStateException("Non-affine glass UVs must retain the unsplit baked face");
					}
				});
			}
		}
		LOGGER.info("TERRAIN_MODEL_PARITY_GLASS_BOUNDARY: matching glass clipped on all sides; different glass retained");
	}

	private static void checkFluidBoundaryClipping(final BlockAndTintGetter level, final BlockPos pos) {
		for (final Direction direction : Direction.values()) {
			final StateEntryStorage owner = new StateEntryStorage(16);
			final StateEntryStorage neighbour = new StateEntryStorage(16);
			final int ownerLayer = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 15 : 0;
			final int neighbourLayer = 15 - ownerLayer;
			for (int u = 4; u < 8; u++) {
				for (int v = 5; v < 9; v++) {
					owner.setBlockInformation(NeighbourBoundary.cellX(direction.getAxis(), ownerLayer, u, v),
							NeighbourBoundary.cellY(direction.getAxis(), ownerLayer, u, v),
							NeighbourBoundary.cellZ(direction.getAxis(), ownerLayer, u, v), info(Blocks.WATER));
					if (u < 6) {
						neighbour.setBlockInformation(NeighbourBoundary.cellX(direction.getAxis(), neighbourLayer, u, v),
								NeighbourBoundary.cellY(direction.getAxis(), neighbourLayer, u, v),
								NeighbourBoundary.cellZ(direction.getAxis(), neighbourLayer, u, v), info(Blocks.WATER));
					}
				}
			}
			final var accessor = new SimpleSnapshot(owner);
			final var snapshot = ChiseledRenderSnapshot.of(accessor);
			final var key = ChiseledBlockStateModelManager.getInstance().keyFor(level, pos, accessor, IBlockNeighborhood.EMPTY);
			final var builder = new ChiseledBlockModelInformationBuilder(accessor, key, level, pos);
			final var metadata = Objects.requireNonNull(builder.describeTerrainSources(snapshot));
			final var boundary = NeighbourBoundary.NONE.withSnapshot(direction, ChiseledRenderSnapshot.of(neighbour));
			final var result = DirectTerrainGeometryBuilder.buildWithoutSealedAir(metadata, snapshot, level, pos, boundary);
			if (!result.supported() || !TerrainLegacyComparison.matchesLegacyOutput(builder.build(), result.fallbackGeometry())) {
				throw new IllegalStateException("Fluid boundary fallback differs from legacy on " + direction);
			}
			if (!TerrainLegacyComparison.matchesLegacyOutput(builder.build(boundary), result.geometry())
					|| !TerrainLegacyComparison.matchesLegacyOutput(builder.build(boundary), result.bakedGeometry())) {
				throw new IllegalStateException("Fluid boundary differs between native and baked paths on " + direction);
			}
			final var faces = result.geometry().parts().stream().flatMap(part -> part.quads().stream())
					.filter(quad -> quad.cullFace() == direction).toList();
			final var original = result.fallbackGeometry().parts().stream().flatMap(part -> part.quads().stream())
					.filter(quad -> quad.cullFace() == direction).findFirst().orElseThrow();
			for (final var face : faces) {
				checkClippedFluidQuad(original, face, direction);
			}
			if (direction == Direction.EAST) {
				checkSlopedTriangleClipping(original);
			}
			for (int u = 4; u < 8; u++) {
				for (int v = 5; v < 9; v++) {
					final float a = (u + 0.5f) / 16;
					final float b = (v + 0.5f) / 16;
					final long covered = faces.stream().filter(quad -> coversBoundary(quad, direction.getAxis(), a, b)).count();
					if (covered != (u < 6 ? 0 : 1)) {
						throw new IllegalStateException("Fluid boundary coverage is wrong on " + direction + " at " + u + "," + v);
					}
				}
			}
			final var different = NeighbourBoundary.NONE.withUniform(direction, info(Blocks.LAVA));
			final var unchanged = DirectTerrainGeometryBuilder.buildWithoutSealedAir(metadata, snapshot, level, pos, different);
			if (!unchanged.supported() || !TerrainLegacyComparison.matchesLegacyOutput(builder.build(), unchanged.geometry())) {
				throw new IllegalStateException("Different-fluid boundary must remain visible on " + direction);
			}
			final var flowing = new BlockInformation(
					Blocks.WATER.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL, 1), Optional.empty());
			final var hidden = DirectTerrainGeometryBuilder.buildWithoutSealedAir(
					metadata, snapshot, level, pos, NeighbourBoundary.NONE.withUniform(direction, flowing));
			if (!hidden.supported() || hidden.geometry().parts().stream().flatMap(part -> part.quads().stream())
					.anyMatch(quad -> quad.cullFace() == direction)
					|| !TerrainLegacyComparison.matchesLegacyOutput(builder.build(), hidden.fallbackGeometry())) {
				throw new IllegalStateException("Flowing water must hide the whole matching boundary on " + direction);
			}
			checkFragmentedFluidBoundary(level, pos, direction);
		}
		LOGGER.info("TERRAIN_MODEL_PARITY_FLUID_BOUNDARY: partial matching fluid clipped on all six sides; fallback and different fluid retained");
	}

	private static void checkFragmentedFluidBoundary(
			final BlockAndTintGetter level, final BlockPos pos, final Direction direction
	) {
		final StateEntryStorage owner = new StateEntryStorage(16);
		final StateEntryStorage neighbour = new StateEntryStorage(16);
		final int ownerLayer = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 15 : 0;
		for (int u = 0; u < 16; u++) {
			for (int v = 0; v < 16; v++) {
				owner.setBlockInformation(NeighbourBoundary.cellX(direction.getAxis(), ownerLayer, u, v),
						NeighbourBoundary.cellY(direction.getAxis(), ownerLayer, u, v),
						NeighbourBoundary.cellZ(direction.getAxis(), ownerLayer, u, v), info(Blocks.WATER));
				if ((u + v) % 2 == 0) {
					final int opposite = 15 - ownerLayer;
					neighbour.setBlockInformation(NeighbourBoundary.cellX(direction.getAxis(), opposite, u, v),
							NeighbourBoundary.cellY(direction.getAxis(), opposite, u, v),
							NeighbourBoundary.cellZ(direction.getAxis(), opposite, u, v), info(Blocks.WATER));
				}
			}
		}
		final var accessor = new SimpleSnapshot(owner);
		final var snapshot = ChiseledRenderSnapshot.of(accessor);
		final var key = ChiseledBlockStateModelManager.getInstance().keyFor(level, pos, accessor, IBlockNeighborhood.EMPTY);
		final var builder = new ChiseledBlockModelInformationBuilder(accessor, key, level, pos);
		final var metadata = Objects.requireNonNull(builder.describeTerrainSources(snapshot));
		final var result = DirectTerrainGeometryBuilder.buildWithoutSealedAir(metadata, snapshot, level, pos,
				NeighbourBoundary.NONE.withSnapshot(direction, ChiseledRenderSnapshot.of(neighbour)));
		if (!result.supported() || !TerrainLegacyComparison.matchesLegacyOutput(builder.build(), result.fallbackGeometry())) {
			throw new IllegalStateException("Fragmented fluid boundary or fallback failed on " + direction);
		}
		final var faces = result.geometry().parts().stream().flatMap(part -> part.quads().stream())
				.filter(quad -> quad.cullFace() == direction).toList();
		final var original = result.fallbackGeometry().parts().stream().flatMap(part -> part.quads().stream())
				.filter(quad -> quad.cullFace() == direction).findFirst().orElseThrow();
		for (final var face : faces) {
			checkClippedFluidQuad(original, face, direction);
		}
		for (int u = 0; u < 16; u++) {
			for (int v = 0; v < 16; v++) {
				final float a = (u + 0.5f) / 16;
				final float b = (v + 0.5f) / 16;
				final long count = faces.stream().filter(face -> coversBoundary(face, direction.getAxis(), a, b)).count();
				if (count != ((u + v) % 2)) {
					throw new IllegalStateException("Fragmented fluid coverage failed on " + direction + " at " + u + "," + v);
				}
			}
		}
	}

	private static void checkClippedFluidQuad(final TerrainQuad original, final TerrainQuad clipped, final Direction direction) {
		if (!original.material().equals(clipped.material()) || original.tintIndex() != clipped.tintIndex()
				|| original.face() != clipped.face() || original.cullFace() != clipped.cullFace()) {
			throw new IllegalStateException("Clipped fluid material changed on " + direction);
		}
		final TerrainVertex origin = original.vertex(0);
		final TerrainVertex edgeA = original.vertex(1);
		final TerrainVertex edgeB = original.vertex(3);
		final double ax = planeU(edgeA, direction.getAxis()) - planeU(origin, direction.getAxis());
		final double ay = planeV(edgeA, direction.getAxis()) - planeV(origin, direction.getAxis());
		final double bx = planeU(edgeB, direction.getAxis()) - planeU(origin, direction.getAxis());
		final double by = planeV(edgeB, direction.getAxis()) - planeV(origin, direction.getAxis());
		final double determinant = ax * by - ay * bx;
		for (int index = 0; index < 4; index++) {
			final TerrainVertex vertex = clipped.vertex(index);
			final double x = planeU(vertex, direction.getAxis()) - planeU(origin, direction.getAxis());
			final double y = planeV(vertex, direction.getAxis()) - planeV(origin, direction.getAxis());
			final double a = (x * by - y * bx) / determinant;
			final double b = (ax * y - ay * x) / determinant;
			final double expectedU = origin.u() + a * (edgeA.u() - origin.u()) + b * (edgeB.u() - origin.u());
			final double expectedV = origin.v() + a * (edgeA.v() - origin.v()) + b * (edgeB.v() - origin.v());
			if (!Double.isFinite(a) || !Double.isFinite(b)
					|| Math.abs(vertex.u() - expectedU) > 0.00001 || Math.abs(vertex.v() - expectedV) > 0.00001) {
				throw new IllegalStateException("Clipped fluid UV changed on " + direction + ": original=" + original
						+ ", clipped=" + clipped + ", expected=" + expectedU + "," + expectedV);
			}
		}
		final TerrainVertex c0 = clipped.vertex(0);
		final TerrainVertex c1 = clipped.vertex(1);
		final TerrainVertex c3 = clipped.vertex(3);
		final double clippedDeterminant = (planeU(c1, direction.getAxis()) - planeU(c0, direction.getAxis()))
				* (planeV(c3, direction.getAxis()) - planeV(c0, direction.getAxis()))
				- (planeV(c1, direction.getAxis()) - planeV(c0, direction.getAxis()))
				* (planeU(c3, direction.getAxis()) - planeU(c0, direction.getAxis()));
		if (clippedDeterminant * determinant <= 0) {
			throw new IllegalStateException("Clipped fluid winding changed on " + direction);
		}
		double triangleArea = 0;
		for (final var triangle : FaceTriangleClipper.clip(original, clipped)) {
			final var a = triangle.v0();
			final var b = triangle.v1();
			final var c = triangle.v2();
			final double signedArea = (planeU(b.position(), direction.getAxis()) - planeU(a.position(), direction.getAxis()))
					* (planeV(c.position(), direction.getAxis()) - planeV(a.position(), direction.getAxis()))
					- (planeV(b.position(), direction.getAxis()) - planeV(a.position(), direction.getAxis()))
					* (planeU(c.position(), direction.getAxis()) - planeU(a.position(), direction.getAxis()));
			if (signedArea * determinant <= 0) {
				throw new IllegalStateException("Clipped triangle winding changed on " + direction);
			}
			if (a.w1() + b.w1() + c.w1() > 0.00001 && a.w3() + b.w3() + c.w3() > 0.00001) {
				throw new IllegalStateException("Clipped triangle crosses the source diagonal on " + direction);
			}
			triangleArea += Math.abs(signedArea);
			for (final var vertex : List.of(a, b, c)) {
				if (Math.abs(vertex.w0() + vertex.w1() + vertex.w2() + vertex.w3() - 1) > 0.00001
						|| vertex.w1() * vertex.w3() > 0.000001
						|| Math.abs(vertex.position().x() - vertex.interpolate(original.v0().x(), original.v1().x(), original.v2().x(), original.v3().x())) > 0.00001
						|| Math.abs(vertex.position().y() - vertex.interpolate(original.v0().y(), original.v1().y(), original.v2().y(), original.v3().y())) > 0.00001
						|| Math.abs(vertex.position().z() - vertex.interpolate(original.v0().z(), original.v1().z(), original.v2().z(), original.v3().z())) > 0.00001
						|| Math.abs(vertex.position().u() - vertex.interpolate(original.v0().u(), original.v1().u(), original.v2().u(), original.v3().u())) > 0.00001
						|| Math.abs(vertex.position().v() - vertex.interpolate(original.v0().v(), original.v1().v(), original.v2().v(), original.v3().v())) > 0.00001) {
					throw new IllegalStateException("Clipped triangle lost source interpolation on " + direction);
				}
			}
		}
		if (Math.abs(triangleArea - Math.abs(clippedDeterminant) * 2) > 0.00001) {
			throw new IllegalStateException("Clipped triangle coverage changed on " + direction);
		}
	}

	private static float planeU(final TerrainVertex vertex, final Direction.Axis axis) {
		return axis == Direction.Axis.X ? vertex.y() : vertex.x();
	}

	private static void checkSlopedTriangleClipping(final TerrainQuad template) {
		final TerrainQuad sloped = new TerrainQuad(
				new TerrainVertex(1, 1, 1, 1, 1), new TerrainVertex(1, 0, 1, 1, 0),
				new TerrainVertex(1, 0, 0, 0, 0), new TerrainVertex(1, 0.5f, 0, 0, 0.5f),
				Direction.EAST, Direction.EAST, template.material(), template.tintIndex());
		final TerrainQuad full = eastBounds(template, 0, 1, 0, 1);
		final double doubledArea = doubledTriangleArea(FaceTriangleClipper.clip(sloped, full));
		final var partial = FaceTriangleClipper.clip(sloped, eastBounds(template, 0.25f, 0.75f, 0.25f, 0.75f));
		if (Math.abs(doubledArea - 1.5) > 0.00001
				|| Math.abs(doubledTriangleArea(partial) - 0.46875) > 0.00001
				|| !FaceTriangleClipper.clip(sloped, eastBounds(template, 1.25f, 1.5f, 0, 1)).isEmpty()) {
			throw new IllegalStateException("Clipped sloped fluid side lost coverage or retained a disjoint region");
		}
	}

	private static double doubledTriangleArea(final List<FaceTriangleClipper.Triangle> triangles) {
		double doubledArea = 0;
		for (final var triangle : triangles) {
			final var a = triangle.v0().position();
			final var b = triangle.v1().position();
			final var c = triangle.v2().position();
			doubledArea += Math.abs((b.y() - a.y()) * (c.z() - a.z()) - (b.z() - a.z()) * (c.y() - a.y()));
		}
		return doubledArea;
	}

	private static TerrainQuad eastBounds(
			final TerrainQuad template, final float minY, final float maxY, final float minZ, final float maxZ
	) {
		return new TerrainQuad(
				new TerrainVertex(1, maxY, maxZ, 0, 0), new TerrainVertex(1, minY, maxZ, 0, 0),
				new TerrainVertex(1, minY, minZ, 0, 0), new TerrainVertex(1, maxY, minZ, 0, 0),
				Direction.EAST, Direction.EAST, template.material(), template.tintIndex());
	}

	private static float planeV(final TerrainVertex vertex, final Direction.Axis axis) {
		return axis == Direction.Axis.Z ? vertex.y() : vertex.z();
	}

	private static boolean coversBoundary(final TerrainQuad quad, final Direction.Axis axis, final float u, final float v) {
		float minU = Float.POSITIVE_INFINITY;
		float maxU = Float.NEGATIVE_INFINITY;
		float minV = Float.POSITIVE_INFINITY;
		float maxV = Float.NEGATIVE_INFINITY;
		for (int index = 0; index < 4; index++) {
			final TerrainVertex vertex = quad.vertex(index);
			final float a = axis == Direction.Axis.X ? vertex.y() : vertex.x();
			final float b = axis == Direction.Axis.Z ? vertex.y() : vertex.z();
			minU = Math.min(minU, a);
			maxU = Math.max(maxU, a);
			minV = Math.min(minV, b);
			maxV = Math.max(maxV, b);
		}
		return u > minU && u < maxU && v > minV && v < maxV;
	}

	private static final int SYNTHETIC_EMISSION = 11;

	/**
	 * No vanilla full-cube block model declares per-quad light emission, so this rewrites the real loader-extracted
	 * sea lantern source quads to a nonzero emission and runs C&B source metadata, direct geometry, the lazy baked
	 * fallback and the legacy builder against the same synthetic source. A touching stone material stays at zero.
	 */
	private static void checkEmissiveSourceQuads(final BlockAndTintGetter level, final BlockPos pos) {
		final FaceManager.SourceTransform emissive = (source, component) -> {
			if (source.blockState().getBlock() != Blocks.SEA_LANTERN) {
				return component;
			}
			final BakedQuad.MaterialInfo material = withEmission(component.material());
			final BakedQuad quad = component.sourceQuad();
			final BakedQuad emissiveQuad = new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(),
					quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(), quad.direction(),
					withEmission(quad.materialInfo()));
			return new com.communi.suggestu.scena.core.client.models.processing.DeconstructedModelPartComponent(
					component.part(), component.vertexData(), component.cullDirection(), emissiveQuad, material);
		};
		for (final int x : new int[]{7, 0}) {
			final StateEntryStorage storage = new StateEntryStorage(16);
			storage.setBlockInformation(x, 7, 7, info(Blocks.SEA_LANTERN));
			storage.setBlockInformation(x, 8, 7, info(Blocks.STONE));
			final var accessor = new SimpleSnapshot(storage);
			final var snapshot = ChiseledRenderSnapshot.of(accessor);
			final ChiseledBlockModelCacheKey key = ChiseledBlockStateModelManager.getInstance()
					.keyFor(level, pos, accessor, IBlockNeighborhood.EMPTY);
			final AtomicReference<ChiseledTerrainMetadata> metadata = new AtomicReference<>();
			final AtomicReference<ChiseledBlockModelInformation> legacy = new AtomicReference<>();
			final AtomicReference<DirectTerrainGeometryBuilder.BuildResult> direct = new AtomicReference<>();
			FaceManager.runWithSourceTransform(emissive, () -> {
				metadata.set(new ChiseledBlockModelInformationBuilder(accessor, key, level, pos).describeTerrainSources(snapshot));
				legacy.set(new ChiseledBlockModelInformationBuilder(accessor, key, level, pos).build());
				if (metadata.get() != null) {
					direct.set(DirectTerrainGeometryBuilder.buildWithoutSealedAir(metadata.get(), snapshot, level, pos));
				}
			});
			final String name = "emissive-source x=" + x;
			if (metadata.get() == null || !direct.get().supported()) {
				throw new IllegalStateException(name + ": nonzero source emission was rejected: "
						+ (direct.get() == null ? "no metadata" : direct.get().rejection()));
			}
			final var result = direct.get();
			final String geometryMismatch = TerrainLegacyComparison.legacyOutputMismatch(legacy.get(), result.geometry());
			final String fallbackGeometryMismatch = TerrainLegacyComparison.legacyOutputMismatch(legacy.get(), result.fallbackGeometry());
			if (geometryMismatch != null || fallbackGeometryMismatch != null) {
				throw new IllegalStateException(name + ": emissive direct/fallback geometry differs from legacy: "
						+ geometryMismatch + " / " + fallbackGeometryMismatch);
			}
			final var publication = new ChiseledTerrainRenderData(metadata.get(), result.geometry(), result.fallbackGeometry());
			final String lazyMismatch = TerrainLegacyComparison.legacyOutputMismatch(publication.fallback(), result.geometry());
			if (lazyMismatch != null) {
				throw new IllegalStateException(name + ": lazy baked fallback differs from direct geometry: " + lazyMismatch);
			}
			final var materials = metadata.get().materials();
			final var quads = result.geometry().parts().stream().flatMap(part -> part.quads().stream()).toList();
			int emissiveQuads = 0;
			for (final var quad : quads) {
				final boolean lantern = materials.get(quad.tintIndex()).blockInformation().blockState().getBlock() == Blocks.SEA_LANTERN;
				if (quad.material().lightEmission() != (lantern ? SYNTHETIC_EMISSION : 0)) {
					throw new IllegalStateException(name + ": per-quad emission leaked or was lost: " + quad.material().lightEmission());
				}
				emissiveQuads += lantern ? 1 : 0;
			}
			if (emissiveQuads != 5 || quads.size() != 10) {
				throw new IllegalStateException(name + ": unexpected emissive/interface quad count " + emissiveQuads + "/" + quads.size());
			}
			for (final var part : publication.fallback().parts()) {
				for (final Direction direction : java.util.stream.Stream.concat(
						java.util.Arrays.stream(Direction.values()), java.util.stream.Stream.of((Direction) null)).toList()) {
					for (final BakedQuad baked : part.getQuads(direction)) {
						final boolean lantern = materials.get(baked.materialInfo().tintIndex())
								.blockInformation().blockState().getBlock() == Blocks.SEA_LANTERN;
						if (baked.materialInfo().lightEmission() != (lantern ? SYNTHETIC_EMISSION : 0)) {
							throw new IllegalStateException(name + ": lazy baked fallback changed per-quad emission");
						}
					}
				}
			}
			// The exact oracle must notice emission: the unmodified source produces a non-matching legacy model.
			if (TerrainLegacyComparison.matchesLegacyOutput(
					new ChiseledBlockModelInformationBuilder(accessor, key, level, pos).build(), result.geometry())) {
				throw new IllegalStateException(name + ": legacy oracle ignored per-quad emission");
			}
		}
		LOGGER.info("TERRAIN_MODEL_PARITY_EMISSIVE_SOURCE nonzero per-quad source emission ({}) matched legacy through metadata, direct geometry and lazy fallback at {}",
				SYNTHETIC_EMISSION, pos);
	}

	private static BakedQuad.MaterialInfo withEmission(final BakedQuad.MaterialInfo material) {
		return new BakedQuad.MaterialInfo(material.sprite(), material.layer(), material.itemRenderType(),
				material.tintIndex(), material.shade(), SYNTHETIC_EMISSION);
	}

	private static void checkHiddenUnsupportedMaterials(final BlockAndTintGetter level, final BlockPos pos) {
		for (final var block : List.of(Blocks.CHEST, Blocks.SHORT_GRASS)) {
			final var storage = new StateEntryStorage(16);
			storage.initializeWith(info(Blocks.OAK_LEAVES));
			storage.setBlockInformation(4, 4, 4, info(block));
			storage.setBlockInformation(8, 8, 8, BlockInformation.AIR);
			final var accessor = new SimpleSnapshot(storage);
			final var snapshot = ChiseledRenderSnapshot.of(accessor);
			final var key = ChiseledBlockStateModelManager.getInstance().keyFor(level, pos, accessor, IBlockNeighborhood.EMPTY);
			final var metadata = new ChiseledBlockModelInformationBuilder(accessor, key, level, pos).describeTerrainSources();
			if (metadata != null && DirectTerrainGeometryBuilder.buildWithoutSealedAir(metadata, snapshot, level, pos).supported()) {
				throw new IllegalStateException("Hidden unsupported material was admitted inside cutout shape: " + block);
			}
		}
	}

	private static void checkMixedCutoutLayoutFallback(final BlockAndTintGetter level, final BlockPos pos) {
		final var storage = new StateEntryStorage(16);
		storage.initializeWith(info(Blocks.STONE));
		storage.setBlockInformation(0, 0, 0, info(Blocks.OAK_LEAVES));
		storage.setBlockInformation(8, 8, 8, BlockInformation.AIR);
		final var accessor = new SimpleSnapshot(storage);
		final var snapshot = ChiseledRenderSnapshot.of(accessor);
		final var key = ChiseledBlockStateModelManager.getInstance().keyFor(level, pos, accessor, IBlockNeighborhood.EMPTY);
		final var builder = new ChiseledBlockModelInformationBuilder(accessor, key, level, pos);
		if (builder.describeTerrainSources(snapshot) != null
				|| !RectangleSubsetPreflight.supportsSourceFaces(builder.build())) {
			throw new IllegalStateException("Mixed cutout/opaque interfaces must retain the legacy path");
		}

		final var fluidMix = new StateEntryStorage(16);
		fluidMix.setBlockInformation(7, 7, 7, info(Blocks.WATER));
		fluidMix.setBlockInformation(8, 7, 7, info(Blocks.OAK_LEAVES));
		final var fluidAccessor = new SimpleSnapshot(fluidMix);
		final var fluidSnapshot = ChiseledRenderSnapshot.of(fluidAccessor);
		final var fluidKey = ChiseledBlockStateModelManager.getInstance().keyFor(level, pos, fluidAccessor, IBlockNeighborhood.EMPTY);
		if (new ChiseledBlockModelInformationBuilder(fluidAccessor, fluidKey, level, pos)
				.describeTerrainSources(fluidSnapshot) != null) {
			throw new IllegalStateException("Fluid/cutout interfaces must retain legacy rendering until both surfaces can be represented");
		}
	}

	private static void checkUnsupportedMaterialFallback(final BlockAndTintGetter level, final BlockPos pos) {
		final StateEntryStorage storage = new StateEntryStorage(16);
		storage.setBlockInformation(7, 7, 7, new BlockInformation(
				Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true),
				Optional.empty()));
		final var accessor = new SimpleSnapshot(storage);
		final var snapshot = ChiseledRenderSnapshot.of(accessor);
		final var key = ChiseledBlockStateModelManager.getInstance().keyFor(level, pos, accessor, IBlockNeighborhood.EMPTY);
		if (new ChiseledBlockModelInformationBuilder(accessor, key, level, pos).describeTerrainSources(snapshot) != null) {
			throw new IllegalStateException("Waterlogged cutout geometry must fall back while the source pipeline emits fluid-only faces");
		}
	}

	private static void checkCutoutFixture(
			final String name, final ChiseledTerrainMetadata metadata,
			final DirectTerrainGeometryBuilder.BuildResult result, final ChiseledRenderSnapshot snapshot,
			final BlockAndTintGetter level, final BlockPos pos
	) {
		final var quads = result.geometry().parts().stream().flatMap(part -> part.quads().stream()).toList();
		if ((!name.contains("buried") && quads.stream().noneMatch(
				quad -> quad.material().layer() == ChunkSectionLayer.CUTOUT))
				|| quads.stream().filter(quad -> quad.material().layer() == ChunkSectionLayer.CUTOUT)
						.anyMatch(quad -> metadata.materials().get(quad.tintIndex()).tintIndex() != quad.material().tintIndex())
				|| RectangleSubsetPreflight.supportsSealedAirOmission(snapshot)
				|| !result.geometry().equals(result.fallbackGeometry())) {
			throw new IllegalStateException(name + ": cutout layer/tint or cavity retention failed");
		}
		try {
			snapshot.greedyRectanglesWithoutSealedAir(NeighbourBoundary.NONE);
			throw new IllegalStateException(name + ": unsafe sealed-air omission accepted");
		} catch (final IllegalArgumentException expected) {
			// The low-level omission entry point must also refuse non-opaque snapshots.
		}
		if (name.contains("mixed") && quads.size() != 10) {
			throw new IllegalStateException(name + ": occupied inter-material interface was emitted");
		}
		if (name.contains("cavity") && quads.stream().noneMatch(quad -> quad.cullFace() == null)) {
			throw new IllegalStateException(name + ": visible-through-cutout cavity faces were omitted");
		}
		final var provider = new ChiseledBlockColorProvider();
		final var colors = new IntArrayList();
		ChiseledTerrainColorScope.run(level, pos, metadata.materials(),
				() -> provider.collect(ModBlocks.CHISELED_BLOCK.get().defaultBlockState(), level, pos, colors));
		for (int index = 0; index < metadata.materials().size(); index++) {
			final var material = metadata.materials().get(index);
			if (material.tintIndex() < 0) {
				continue;
			}
			final var state = material.blockInformation().blockState();
			final var tint = Minecraft.getInstance().getBlockColors().getTintSource(state, material.tintIndex());
			final var sourceView = new SingleBlockBlockAndTintGetter.Builder()
					.withBlockState(state).withPos(pos).withSource(level).createSingleBlockBlockAndTintGetter();
			final var expected = material.blockInformation().blockState().getFluidState().isEmpty()
					? tint == null ? -1 : tint.colorInWorld(state, sourceView, pos)
					: Minecraft.getInstance().getModelManager().getFluidStateModelSet()
							.get(material.blockInformation().blockState().getFluidState()).tintSource()
									.colorInWorld(state, sourceView, pos);
			// The detached parity level has no biome source, so resolved foliage colours may legitimately be
			// white; the requirement is a real source tint whose scoped colour matches exactly.
			if ((state.getFluidState().isEmpty() && tint == null) || colors.getInt(index) != expected) {
				throw new IllegalStateException(name + ": leaf source tint differs from scoped tint: material " + index
						+ " tintIndex=" + material.tintIndex() + " expected=" + Integer.toHexString(expected)
						+ " actual=" + (index < colors.size() ? Integer.toHexString(colors.getInt(index)) : "missing"));
			}
		}
	}

	private static void checkCollidingPartOrder(final BlockAndTintGetter level, final BlockPos pos) {
		final var buckets = new java.util.HashMap<Integer, BlockInformation>();
		for (final var block : List.of(Blocks.STONE, Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE,
				Blocks.COBBLESTONE, Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT,
				Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.BIRCH_PLANKS, Blocks.JUNGLE_PLANKS,
				Blocks.ACACIA_PLANKS, Blocks.DARK_OAK_PLANKS, Blocks.CRIMSON_PLANKS,
				Blocks.WARPED_PLANKS, Blocks.BRICKS, Blocks.NETHER_BRICKS, Blocks.END_STONE)) {
			final var storage = new StateEntryStorage(16);
			storage.initializeWith(info(block));
			final var accessor = new SimpleSnapshot(storage);
			final var key = ChiseledBlockStateModelManager.getInstance().keyFor(level, pos, accessor, IBlockNeighborhood.EMPTY);
			final var metadata = new ChiseledBlockModelInformationBuilder(accessor, key, level, pos).describeTerrainSources();
			if (metadata == null || metadata.parts().size() != 1) {
				continue;
			}
			final int hash = metadata.parts().getFirst().hashCode();
			final int bucket = (hash ^ (hash >>> 16)) & 15;
			final var other = buckets.putIfAbsent(bucket, info(block));
			if (other == null) {
				continue;
			}
			for (int x = 0; x < 8; x++) {
				for (int y = 0; y < 16; y++) {
					for (int z = 0; z < 16; z++) {
						storage.setBlockInformation(x, y, z, other);
					}
				}
			}
			final var mixed = new SimpleSnapshot(storage);
			final var mixedKey = ChiseledBlockStateModelManager.getInstance().keyFor(level, pos, mixed, IBlockNeighborhood.EMPTY);
			final var builder = new ChiseledBlockModelInformationBuilder(mixed, mixedKey, level, pos);
			final var actual = builder.describeTerrainSources();
			final var expected = builder.build().parts().stream().map(part ->
					new mod.chiselsandbits.client.model.parts.ChiseledBlockModelPartKey(
							part.source(), part.appearance(), part.ambientOcclusion(), part.particleMaterial(), part.materialFlags())).toList();
			if (actual == null || !actual.parts().equals(expected)) {
				throw new IllegalStateException("Independent metadata changed legacy ordering for colliding part keys");
			}
			LOGGER.info("TERRAIN_MODEL_PARITY_PART_COLLISION: legacy part order preserved in HashMap bucket {}", bucket);
			return;
		}
		throw new IllegalStateException("Could not construct a colliding supported-material fixture");
	}

	private static void checkSourceLayoutRejectsSplits(
			final ChiseledTerrainMetadata metadata, final GreedyRectangles rectangles,
			final BlockAndTintGetter level, final BlockPos pos
	) {
		for (int index = 0; index < rectangles.size(); index++) {
			final long rectangle = rectangles.rectangle(index);
			final int width = GreedyRectangles.width(rectangle);
			if (width < 2) {
				continue;
			}
			final long[] split = new long[rectangles.size() + 1];
			for (int other = 0; other < rectangles.size(); other++) {
				split[other] = rectangles.rectangle(other);
			}
			split[index] = GreedyRectangles.pack(GreedyRectangles.materialIndex(rectangle), GreedyRectangles.direction(rectangle).ordinal(),
					GreedyRectangles.depth(rectangle), GreedyRectangles.row(rectangle), GreedyRectangles.column(rectangle), 1,
					GreedyRectangles.height(rectangle));
			split[rectangles.size()] = GreedyRectangles.pack(GreedyRectangles.materialIndex(rectangle), GreedyRectangles.direction(rectangle).ordinal(),
					GreedyRectangles.depth(rectangle), GreedyRectangles.row(rectangle), GreedyRectangles.column(rectangle) + 1,
					width - 1, GreedyRectangles.height(rectangle));
			final var candidate = new GreedyRectangles(rectangles.sourceVersion(), rectangles.boundary(), rectangles.materials(), split);
			final DirectTerrainGeometryBuilder.BuildResult rejected = DirectTerrainGeometryBuilder.build(metadata, candidate, level, pos);
			if (candidate.unitFaceCount() != rectangles.unitFaceCount() || rejected.supported()
					|| !rejected.rejection().contains("tessellation")) {
				throw new IllegalStateException("Producer did not independently reject same-coverage/different-tessellation geometry");
			}
			return;
		}
	}

	private static void checkTintScope(final BlockAndTintGetter level, final BlockPos pos) {
		final var grass = new ChiseledBlockModelMaterial(info(Blocks.GRASS_BLOCK), 0);
		final var redstone = new ChiseledBlockModelMaterial(info(Blocks.REDSTONE_WIRE), 0);
		final var provider = new ChiseledBlockColorProvider();
		final var before = new IntArrayList();
		final var after = new IntArrayList();
		final var reversed = new IntArrayList();
		final var state = ModBlocks.CHISELED_BLOCK.get().defaultBlockState();
		final long fullBuilds = ChiseledBlockModelInformationBuilder.fullModelBuildCount();
		ChiseledTerrainColorScope.run(level, pos, List.of(grass, redstone), () -> {
			provider.collect(state, level, pos, before);
			try {
				ChiseledTerrainColorScope.run(level, pos, List.of(redstone, grass), () -> {
					provider.collect(state, level, pos, reversed);
					throw new IllegalStateException("synthetic emission failure");
				});
			} catch (final IllegalStateException expected) {
				if (!"synthetic emission failure".equals(expected.getMessage())) {
					throw expected;
				}
			}
			provider.collect(state, level, pos, after);
			if (CompletableFuture.supplyAsync(() -> ChiseledTerrainColorScope.materials(level, pos),
					task -> Thread.ofPlatform().name("C&B tint parity").start(task)).join() != null
					|| ChiseledTerrainColorScope.materials(level, pos.above()) != null) {
				throw new IllegalStateException("Tint override escaped its thread or position");
			}
		});
		if (before.size() != 2 || reversed.size() != 2 || !before.equals(after)
				|| before.getInt(0) == before.getInt(1)
				|| before.getInt(0) != reversed.getInt(1) || before.getInt(1) != reversed.getInt(0)
				|| ChiseledTerrainColorScope.materials(level, pos) != null
				|| ChiseledBlockModelInformationBuilder.fullModelBuildCount() != fullBuilds) {
			throw new IllegalStateException("Distinct tinted materials were reordered, rebuilt a model, or leaked across emission scopes");
		}
		LOGGER.info("TERRAIN_MODEL_PARITY_DIRECT_SNAPSHOT: independent metadata, lazy fallback and distinct tinted material scopes passed at {}", pos);
	}

	private static void checkCompileSnapshot(
			final ChiseledTerrainMetadata metadata, final DirectTerrainGeometryBuilder.BuildResult result,
			final BlockAndTintGetter level, final BlockPos pos
	) {
		final var grass = new ChiseledBlockModelMaterial(info(Blocks.GRASS_BLOCK), 0);
		final var redstone = new ChiseledBlockModelMaterial(info(Blocks.REDSTONE_WIRE), 0);
		final var table = List.of(grass, redstone);
		final var oldMetadata = new ChiseledTerrainMetadata(metadata.key(), metadata.particleMaterial(),
				metadata.parts(), table, metadata.faceLayout(), metadata.sourceLayers());
		final var newerMetadata = new ChiseledTerrainMetadata(metadata.key(), metadata.particleMaterial(),
				metadata.parts(), List.of(redstone, grass), metadata.faceLayout(), metadata.sourceLayers());
		final var old = new ChiseledTerrainRenderData(oldMetadata, result.geometry(), result.fallbackGeometry());
		final var newer = new ChiseledTerrainRenderData(newerMetadata, result.geometry(), result.fallbackGeometry());
		final IBlockModelData oldData = IModelDataBuilder.create().withInitial(ModModelProperties.TERRAIN_RENDER_DATA, old).build();
		final IBlockModelData newData = IModelDataBuilder.create().withInitial(ModModelProperties.TERRAIN_RENDER_DATA, newer).build();
		final var current = new AtomicReference<>(oldData);
		final BlockAndTintGetter movingView = capturedView(level, current::get);
		final BlockAndTintGetter frozenView = capturedView(level, () -> oldData);
		final var model = new ChiseledBlockStateModel();
		final var state = ModBlocks.CHISELED_BLOCK.get().defaultBlockState();
		final var provider = new ChiseledBlockColorProvider();
		final var expected = new IntArrayList();
		final var directColors = new IntArrayList();
		final var fallbackColors = new IntArrayList();
		ChiseledTerrainColorScope.run(level, pos, table, () -> provider.collect(state, level, pos, expected));
		final long fullBuilds = ChiseledBlockModelInformationBuilder.fullModelBuildCount();
		model.createGeometryKey(frozenView, pos, state, net.minecraft.util.RandomSource.create());
		model.particleMaterial(frozenView, pos, state);
		model.materialFlags(frozenView, pos, state);
		final var nativeContext = new VanillaTerrainGeometryContext(movingView, pos, state,
				baked -> { throw new IllegalStateException("Native emission unexpectedly used fallback"); },
				baked -> null, geometry -> () -> provider.collect(state, movingView, pos, directColors));
		final Object nativePlan = model.preflightTerrainGeometry(nativeContext);
		current.set(newData);
		model.emitTerrainGeometry(nativeContext, nativePlan);
		if (!expected.equals(directColors) || old.isFallbackBuilt() || newer.isFallbackBuilt()
				|| ChiseledBlockModelInformationBuilder.fullModelBuildCount() != fullBuilds) {
			throw new IllegalStateException("In-flight direct plan used newer tints or built a full model");
		}
		current.set(oldData);
		final var fallbackContext = new VanillaTerrainGeometryContext(movingView, pos, state, baked -> {
			final var parts = new ArrayList<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart>();
			baked.collectParts(net.minecraft.util.RandomSource.create(), parts);
			if (parts.isEmpty()) {
				throw new IllegalStateException("Rejected native backend lost fallback geometry");
			}
			provider.collect(state, movingView, pos, fallbackColors);
		});
		final Object fallbackPlan = model.preflightTerrainGeometry(fallbackContext);
		current.set(newData);
		ClientResourceReloadingManager.getInstance().clearCaches();
		model.emitTerrainGeometry(fallbackContext, fallbackPlan);
		final var unhookedParts = new ArrayList<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart>();
		model.collectParts(frozenView, pos, state, net.minecraft.util.RandomSource.create(), unhookedParts);
		final var unhookedColors = new IntArrayList();
		provider.collect(state, frozenView, pos, unhookedColors);
		if (!expected.equals(fallbackColors) || !expected.equals(unhookedColors) || unhookedParts.isEmpty()
				|| ChiseledModelDataAccess.read(frozenView, pos).getData(ModModelProperties.TERRAIN_RENDER_DATA) != old
				|| ChiseledBlockModelInformationBuilder.fullModelBuildCount() != fullBuilds) {
			throw new IllegalStateException("Fallback geometry/colors did not use the same captured loader snapshot");
		}
		checkLiveViewFallback(level, pos, oldData, expected);
		LOGGER.info("TERRAIN_MODEL_PARITY_COMPILE_SNAPSHOT: native rejection and hook-bypass fallback survived table replacement and cache reload");
	}

	private static void checkLiveViewFallback(
			final BlockAndTintGetter source, final BlockPos pos, final IBlockModelData data, final IntArrayList expected
	) {
		final var state = ModBlocks.CHISELED_BLOCK.get().defaultBlockState();
		final var entity = new mod.chiselsandbits.block.entities.ChiseledBlockEntity(pos, state);
		entity.setModelData(data);
		final BlockAndTintGetter view = new BlockAndTintGetter() {
			@Override
			public net.minecraft.world.level.CardinalLighting cardinalLighting() {
				return source.cardinalLighting();
			}

			@Override
			public net.minecraft.world.level.lighting.LevelLightEngine getLightEngine() {
				return source.getLightEngine();
			}

			@Override
			public int getBlockTint(final BlockPos query, final net.minecraft.world.level.ColorResolver resolver) {
				return source.getBlockTint(query, resolver);
			}

			@Override
			public net.minecraft.world.level.block.entity.BlockEntity getBlockEntity(final BlockPos query) {
				return entity;
			}

			@Override
			public net.minecraft.world.level.block.state.BlockState getBlockState(final BlockPos query) {
				return source.getBlockState(query);
			}

			@Override
			public net.minecraft.world.level.material.FluidState getFluidState(final BlockPos query) {
				return source.getFluidState(query);
			}

			@Override
			public int getHeight() {
				return source.getHeight();
			}

			@Override
			public int getMinY() {
				return source.getMinY();
			}
		};
		final var parts = new ArrayList<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart>();
		new ChiseledBlockStateModel().collectParts(view, pos, state, net.minecraft.util.RandomSource.create(), parts);
		final var colors = new IntArrayList();
		new ChiseledBlockColorProvider().collect(state, view, pos, colors);
		if (parts.isEmpty() || !expected.equals(colors)) {
			throw new IllegalStateException("Renderer view without a captured model-data override lost its legacy fallback");
		}
	}

	private static BlockAndTintGetter capturedView(
			final BlockAndTintGetter source, final java.util.function.Supplier<IBlockModelData> snapshot
	) {
		return (BlockAndTintGetter) Proxy.newProxyInstance(BlockAndTintGetter.class.getClassLoader(),
				new Class<?>[]{BlockAndTintGetter.class}, (proxy, method, args) -> switch (method.getName()) {
					case "getBlockEntityRenderData" -> snapshot.get();
					case "getModelData" -> snapshot.get().getClass().getMethod("getDelegate").invoke(snapshot.get());
					case "getBlockEntity" -> throw new AssertionError("Section model/color lookup touched a live block entity");
					default -> method.invoke(source, args);
				});
	}

	private static void checkSealedAirGeometry(final BlockAndTintGetter level, final BlockPos pos) {
		for (final boolean open : new boolean[]{false, true}) {
			final StateEntryStorage storage = new StateEntryStorage(16);
			storage.initializeWith(info(Blocks.STONE));
			if (open) {
				for (int x = 0; x <= 8; x++) {
					storage.setBlockInformation(x, 8, 8, BlockInformation.AIR);
				}
			} else {
				storage.setBlockInformation(8, 8, 8, BlockInformation.AIR);
			}
			final var accessor = new SimpleSnapshot(storage);
			final var legacy = ChiseledBlockStateModelManager.getInstance().get(level, pos, accessor, IBlockNeighborhood.EMPTY);
			final var snapshot = ChiseledRenderSnapshot.of(accessor);
			final var full = TerrainLegacyComparison.build(
					legacy, snapshot.greedyRectangles(FaceCullingPolicy.ANY_OCCUPIED), level, pos
			);
			final var optimized = TerrainLegacyComparison.buildWithoutSealedAir(legacy, snapshot, level, pos);
			if (open && !full.supported() && !optimized.supported()
					&& Objects.equals(full.rejection(), optimized.rejection())) {
				continue;
			}
			if (!full.supported() || !optimized.supported()) {
				throw new IllegalStateException("Cavity geometry rejected: " + full.rejection() + "; " + optimized.rejection());
			}
			final var fullQuads = full.geometry().parts().stream().flatMap(part -> part.quads().stream()).toList();
			final var visibleQuads = optimized.geometry().parts().stream().flatMap(part -> part.quads().stream()).toList();
			if (open ? !full.geometry().equals(optimized.geometry())
					: fullQuads.size() - visibleQuads.size() != 6 || !fullQuads.containsAll(visibleQuads)
							|| visibleQuads.stream().anyMatch(quad -> quad.cullFace() == null)) {
				throw new IllegalStateException("Cavity culling altered visible geometry: open=" + open);
			}
		}
		LOGGER.info("TERRAIN_MODEL_PARITY_CAVITY sealed faces removed; open tunnel retained or safely fell back at {}", pos);
	}

	private static void checkBoundaryNeighbours(final BlockAndTintGetter level, final BlockPos pos) {
		for (final var sourceBlock : List.of(Blocks.STONE, Blocks.OAK_LEAVES)) {
			checkBoundaryNeighbours(level, pos, sourceBlock);
		}
	}

	private static void checkBoundaryNeighbours(
			final BlockAndTintGetter level, final BlockPos pos, final net.minecraft.world.level.block.Block sourceBlock
	) {
		for (final Direction direction : Direction.values()) {
			final StateEntryStorage storage = new StateEntryStorage(16);
			final int x = direction == Direction.WEST ? 0 : direction == Direction.EAST ? 15 : 5;
			final int y = direction == Direction.DOWN ? 0 : direction == Direction.UP ? 15 : 7;
			final int z = direction == Direction.NORTH ? 0 : direction == Direction.SOUTH ? 15 : 9;
			storage.setBlockInformation(x, y, z, info(sourceBlock));
			final var accessor = new SimpleSnapshot(storage);
			final GreedyRectangles rectangles = ChiseledRenderSnapshot.of(accessor)
					.greedyRectangles(FaceCullingPolicy.ANY_OCCUPIED);
			final StateEntryStorage neighbour = new StateEntryStorage(16);
			neighbour.setBlockInformation(direction.getAxis() == Direction.Axis.X ? 15 - x : x,
					direction.getAxis() == Direction.Axis.Y ? 15 - y : y,
					direction.getAxis() == Direction.Axis.Z ? 15 - z : z, info(Blocks.STONE));
			final IBlockNeighborhood chiseled = IBlockNeighborhoodBuilder.getInstance().build(
					d -> d == direction ? info(ModBlocks.CHISELED_BLOCK.get()) : BlockInformation.AIR,
					d -> d == direction ? new SimpleSnapshot(neighbour.createSnapshot()) : null
			);
			final var before = chiseled.getAreaAccessor(direction).createNewShapeIdentifier();
			neighbour.initializeWith(BlockInformation.AIR);
			final IBlockNeighborhood edited = IBlockNeighborhoodBuilder.getInstance().build(
					chiseled::getBlockInformation,
					d -> d == direction ? new SimpleSnapshot(neighbour.createSnapshot()) : null
			);
			if (chiseled.equals(edited)
					|| !before.equals(chiseled.getAreaAccessor(direction).createNewShapeIdentifier())) {
				throw new IllegalStateException(direction + ": neighbour edit mutated a captured snapshot or retained its key");
			}
			for (final IBlockNeighborhood neighborhood : List.of(
					IBlockNeighborhood.EMPTY,
					IBlockNeighborhoodBuilder.getInstance().build(
							d -> d == direction ? info(Blocks.STONE) : BlockInformation.AIR, null),
					chiseled, edited
			)) {
				final var legacy = ChiseledBlockStateModelManager.getInstance().get(level, pos, accessor, neighborhood);
				final var result = TerrainLegacyComparison.build(legacy, rectangles, level, pos);
				if (!result.supported()) {
					throw new IllegalStateException(direction + ": neighbour parity failed: " + result.rejection());
				}
				final long culled = result.geometry().parts().stream().flatMap(part -> part.quads().stream())
						.filter(quad -> quad.cullFace() == direction && quad.face() == direction).count();
				final long unculled = result.geometry().parts().stream().flatMap(part -> part.quads().stream())
						.filter(quad -> quad.cullFace() == null).count();
				if (culled != 1 || unculled != 5) {
					throw new IllegalStateException(direction + ": expected one outer cull face and five unculled faces");
				}
			}
			LOGGER.info("TERRAIN_MODEL_PARITY_BOUNDARY {} {} air/solid/chiseled/edited snapshots passed at {}", sourceBlock, direction, pos);
		}
	}

	private static void assertOutputMutationsFallBack(
			final String fixture,
			final ChiseledBlockModelInformation legacy,
			final TerrainGeometry geometry
	) {
		assertMutationRejected(fixture, legacy, geometry, quad -> {
			final TerrainVertex vertex = quad.v0();
			return new TerrainQuad(new TerrainVertex(Math.nextUp(vertex.x()), vertex.y(), vertex.z(), vertex.u(), vertex.v()),
					quad.v1(), quad.v2(), quad.v3(), quad.face(), quad.cullFace(), quad.material(), quad.tintIndex());
		}, "position differs");
		assertMutationRejected(fixture, legacy, geometry, quad -> {
			final TerrainVertex vertex = quad.v0();
			return new TerrainQuad(new TerrainVertex(vertex.x(), vertex.y(), vertex.z(), Math.nextUp(vertex.u()), vertex.v()),
					quad.v1(), quad.v2(), quad.v3(), quad.face(), quad.cullFace(), quad.material(), quad.tintIndex());
		}, "UV differs");
		assertMutationRejected(fixture, legacy, geometry, quad -> new TerrainQuad(
				quad.v0(), quad.v3(), quad.v2(), quad.v1(), quad.face(), quad.cullFace(), quad.material(), quad.tintIndex()
		), "position differs");
		assertMutationRejected(fixture, legacy, geometry, quad -> new TerrainQuad(
				quad.v0(), quad.v1(), quad.v2(), quad.v3(), quad.face(),
				quad.cullFace() == null ? quad.face() : null, quad.material(), quad.tintIndex()
		), "quad count differs");
		assertMutationRejected(fixture, legacy, geometry, quad -> new TerrainQuad(
				quad.v0(), quad.v1(), quad.v2(), quad.v3(), quad.face(), quad.cullFace(), quad.material(), -1
		), "tint mapping");
		assertMutationRejected(fixture, legacy, geometry, quad -> {
			final BakedQuad.MaterialInfo material = quad.material();
			return new TerrainQuad(quad.v0(), quad.v1(), quad.v2(), quad.v3(), quad.face(), quad.cullFace(),
					new BakedQuad.MaterialInfo(material.sprite(), material.layer(), material.itemRenderType(),
							material.tintIndex(), !material.shade(), material.lightEmission()), quad.tintIndex());
		}, "material differs");

		final List<TerrainGeometryPart> parts = new ArrayList<>(geometry.parts());
		final TerrainGeometryPart first = parts.getFirst();
		parts.set(0, new TerrainGeometryPart(
				first.ambientOcclusion() == TriState.FALSE ? TriState.TRUE : TriState.FALSE,
				first.appearance(), first.materialFlags(), first.quads()
		));
		assertMismatch(fixture, legacy, new TerrainGeometry(parts), "metadata differs");
		LOGGER.info("TERRAIN_MODEL_PARITY_MUTATIONS {} rejected position/UV/winding/cull/tint/material/AO changes", fixture);
	}

	private static void assertMutationRejected(
			final String fixture,
			final ChiseledBlockModelInformation legacy,
			final TerrainGeometry geometry,
			final UnaryOperator<TerrainQuad> mutation,
			final String expectedReason
	) {
		final List<TerrainGeometryPart> parts = new ArrayList<>(geometry.parts());
		final TerrainGeometryPart first = parts.getFirst();
		final List<TerrainQuad> quads = new ArrayList<>(first.quads());
		quads.set(0, mutation.apply(quads.getFirst()));
		parts.set(0, new TerrainGeometryPart(first.ambientOcclusion(), first.appearance(), first.materialFlags(), quads));
		assertMismatch(fixture, legacy, new TerrainGeometry(parts), expectedReason);
	}

	private static void assertMismatch(
			final String fixture,
			final ChiseledBlockModelInformation legacy,
			final TerrainGeometry geometry,
			final String expectedReason
	) {
		final String mismatch = TerrainLegacyComparison.legacyOutputMismatch(legacy, geometry);

		if (mismatch == null || !mismatch.contains(expectedReason)
				|| TerrainLegacyComparison.matchesLegacyOutput(legacy, geometry)) {
			throw new IllegalStateException(fixture + ": expected " + expectedReason + " rejection, got " + mismatch);
		}
	}

	private static void assertDifferingTessellationFallsBack(
				final String fixture,
				final ChiseledBlockModelInformation legacy,
				final TerrainGeometry geometry
		) {
			final TerrainGeometry split = splitFirstQuad(geometry);
			if (!TerrainLegacyComparison.matchesLegacyOutput(legacy, geometry)
					|| TerrainLegacyComparison.matchesLegacyOutput(legacy, split)) {
				throw new IllegalStateException(fixture + ": same-coverage split must fail exact output parity");
			}

			final var plan = ChiseledTerrainModelSelection.prepareNativeGeometry(
					legacy,
					split,
					candidate -> TerrainLegacyComparison.matchesLegacyOutput(legacy, candidate) ? () -> {
					} : null
			);
			if (plan.usesNativeGeometry() || plan.fallbackModel() != legacy) {
				throw new IllegalStateException(fixture + ": differing tessellation did not select the legacy fallback");
			}
			LOGGER.info("TERRAIN_MODEL_PARITY_TESSELLATION {} rejected a same-coverage split", fixture);
		}

	private static TerrainGeometry splitFirstQuad(final TerrainGeometry source) {
			final List<TerrainGeometryPart> parts = new ArrayList<>(source.parts());
			for (int partIndex = 0; partIndex < parts.size(); partIndex++) {
				final TerrainGeometryPart part = parts.get(partIndex);
				if (part.quads().isEmpty()) {
					continue;
				}

				final List<TerrainQuad> quads = new ArrayList<>(part.quads());
				final TerrainQuad original = quads.removeFirst();
				final TerrainVertex middle01 = interpolate(original.v0(), original.v1(), 0.5f);
				final TerrainVertex middle32 = interpolate(original.v3(), original.v2(), 0.5f);
				final TerrainQuad first = new TerrainQuad(
						original.v0(), middle01, middle32, original.v3(),
						original.face(), original.cullFace(), original.material(), original.tintIndex()
				);
				final TerrainQuad second = new TerrainQuad(
						middle01, original.v1(), original.v2(), middle32,
						original.face(), original.cullFace(), original.material(), original.tintIndex()
				);
				if (Math.abs(area(original) - area(first) - area(second)) > 1.0e-6d) {
					throw new IllegalStateException("synthetic split changed face coverage");
				}
				quads.add(0, second);
				quads.add(0, first);
				parts.set(partIndex, new TerrainGeometryPart(
						part.ambientOcclusion(), part.appearance(), part.materialFlags(), quads
				));
				return new TerrainGeometry(parts);
			}
			throw new IllegalStateException("cannot split empty direct geometry");
		}

	private static TerrainVertex interpolate(final TerrainVertex left, final TerrainVertex right, final float amount) {
			return new TerrainVertex(
					left.x() + (right.x() - left.x()) * amount,
					left.y() + (right.y() - left.y()) * amount,
					left.z() + (right.z() - left.z()) * amount,
					left.u() + (right.u() - left.u()) * amount,
					left.v() + (right.v() - left.v()) * amount
			);
		}

	private static double area(final TerrainQuad quad) {
			return triangleArea(quad.v0(), quad.v1(), quad.v2()) + triangleArea(quad.v0(), quad.v2(), quad.v3());
		}

	private static double triangleArea(final TerrainVertex a, final TerrainVertex b, final TerrainVertex c) {
			final double abX = b.x() - a.x();
			final double abY = b.y() - a.y();
			final double abZ = b.z() - a.z();
			final double acX = c.x() - a.x();
			final double acY = c.y() - a.y();
			final double acZ = c.z() - a.z();
			final double crossX = abY * acZ - abZ * acY;
			final double crossY = abZ * acX - abX * acZ;
			final double crossZ = abX * acY - abY * acX;
			return Math.sqrt(crossX * crossX + crossY * crossY + crossZ * crossZ) * 0.5d;
	}

	private static void checkResolvedVertices(
			final String fixture,
			final GreedyRectangles rectangles,
			final BlockAndTintGetter level,
			final BlockPos pos
	) {
		for (int index = 0; index < rectangles.size(); index++) {
			final long rectangle = rectangles.rectangle(index);
			final Direction direction = GreedyRectangles.direction(rectangle);
			final var from = GreedyRectangles.lowerCorner(rectangle);
			final var to = GreedyRectangles.upperCorner(rectangle);
			final var vertices = new ArrayList<QuadGenerationUtils.GeneratedVertices>();
			final var quads = new ArrayList<QuadGenerationUtils.GeneratedQuad>();
			final var material = rectangles.material(rectangle);
			final boolean verticesSupported = QuadGenerationUtils.generateVertices(
					material, direction, level, pos, from, to,
					source -> RectangleSubsetPreflight.supportsSourceFaceGeometry(source, direction), vertices::add
			);
			final boolean quadsSupported = QuadGenerationUtils.generateQuads(
					material, direction, level, pos, from, to,
					source -> RectangleSubsetPreflight.supportsSourceFaceGeometry(source, direction), quads::add
			);
			if (!verticesSupported || !quadsSupported || vertices.size() != quads.size()) {
				throw new IllegalStateException(fixture + ": resolved vertex output did not match generated quads");
			}
			for (int quadIndex = 0; quadIndex < vertices.size(); quadIndex++) {
				final var resolved = vertices.get(quadIndex);
				final var generated = quads.get(quadIndex);
				if (resolved.source() != generated.source() || resolved.vertices().size() != 4) {
					throw new IllegalStateException(fixture + ": source face or vertex count changed");
				}
				for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
					final var vertex = resolved.vertices().get(vertexIndex);
					final var position = generated.quad().position(vertexIndex);
					if (vertex.vertexIndex() != vertexIndex
							|| Float.floatToIntBits(vertex.x()) != Float.floatToIntBits(position.x())
							|| Float.floatToIntBits(vertex.y()) != Float.floatToIntBits(position.y())
							|| Float.floatToIntBits(vertex.z()) != Float.floatToIntBits(position.z())
							|| vertex.uv() != generated.quad().packedUV(vertexIndex)) {
						throw new IllegalStateException(fixture + ": resolved vertex differs from baked output");
					}
				}
			}
		}
	}

	private static List<Fixture> fixtures() {
		final List<Fixture> fixtures = new ArrayList<>(List.of(
				new Fixture("single-oak-leaves", storage -> storage.setBlockInformation(7, 7, 7, info(Blocks.OAK_LEAVES))),
				new Fixture("single-spruce-leaves", storage -> storage.setBlockInformation(7, 7, 7, info(Blocks.SPRUCE_LEAVES))),
				new Fixture("single-birch-leaves", storage -> storage.setBlockInformation(7, 7, 7, info(Blocks.BIRCH_LEAVES))),
				new Fixture("boundary-full-oak-leaves", storage -> storage.initializeWith(info(Blocks.OAK_LEAVES))),
				new Fixture("cavity-oak-leaves", storage -> {
					storage.initializeWith(info(Blocks.OAK_LEAVES));
					storage.setBlockInformation(8, 8, 8, BlockInformation.AIR);
				}),
				new Fixture("single-stone", storage -> storage.setBlockInformation(7, 7, 7, info(Blocks.STONE))),
				new Fixture("terrain-subset-inset-stone", storage -> {
					for (int x = 4; x <= 11; x++) {
						for (int y = 4; y <= 11; y++) {
							for (int z = 4; z <= 11; z++) {
								storage.setBlockInformation(x, y, z, info(Blocks.STONE));
							}
						}
					}
				}),
				new Fixture("stone-cuboid", storage -> {
					for (int x = 6; x < 9; x++) {
						for (int y = 6; y < 9; y++) {
							for (int z = 6; z < 9; z++) {
								storage.setBlockInformation(x, y, z, info(Blocks.STONE));
							}
						}
					}
				}),
				new Fixture("mixed-stone-oak-planks", storage -> {
					storage.setBlockInformation(6, 7, 7, info(Blocks.STONE));
					storage.setBlockInformation(7, 7, 7, info(Blocks.OAK_PLANKS));
					storage.setBlockInformation(8, 7, 7, info(Blocks.STONE));
				})
		));
		fixtures.add(new Fixture("boundary-full-stone", storage -> storage.initializeWith(info(Blocks.STONE))));
		fixtures.add(new Fixture("boundary-stone-slab", storage -> {
			for (int x = 0; x < 16; x++) {
				for (int y = 0; y < 8; y++) {
					for (int z = 0; z < 16; z++) {
						storage.setBlockInformation(x, y, z, info(Blocks.STONE));
					}
				}
			}
		}));
		return fixtures;
	}

	private static BlockInformation info(final net.minecraft.world.level.block.Block block) {
		return new BlockInformation(block.defaultBlockState(), Optional.empty());
	}

	private static StateEntryStorage storage(final Fixture fixture) {
		final StateEntryStorage storage = new StateEntryStorage(16);
		fixture.fill().accept(storage);
		return storage;
	}

	private static OutputSignature signature(final ChiseledBlockModelInformation model) {
		final Map<QuadSignature, Integer> quads = new HashMap<>();
		for (final var part : model.parts()) {
			final PartSignature partSignature = new PartSignature(
					part.source(),
					part.appearance(),
					part.ambientOcclusion(),
					part.particleMaterial(),
					part.materialFlags()
			);
			for (final Direction cullDirection : Direction.values()) {
				for (final BakedQuad quad : part.getQuads(cullDirection)) {
					addQuad(quads, quad, cullDirection.name(), partSignature, model.materials());
				}
			}
			for (final BakedQuad quad : part.getQuads(null)) {
				addQuad(quads, quad, "unculled", partSignature, model.materials());
			}
		}

		return new OutputSignature(model.particleMaterial(), model.materialFlags(),
				model.parts().getFirst().useAmbientOcclusion(), Map.copyOf(quads));
	}

	private static void addQuad(
			final Map<QuadSignature, Integer> quads,
			final BakedQuad quad,
			final String cullDirection,
			final PartSignature part,
			final List<ChiseledBlockModelMaterial> materials
	) {
		final int materialIndex = quad.materialInfo().tintIndex();
		if (materialIndex < 0 || materialIndex >= materials.size()) {
			throw new IllegalStateException("quad references invalid material index " + materialIndex);
		}
		final ChiseledBlockModelMaterial material = materials.get(materialIndex);
		final BakedQuad.MaterialInfo materialInfo = quad.materialInfo();
		final List<VertexSignature> vertices = new ArrayList<>(4);
		for (int index = 0; index < 4; index++) {
			final Vector3fc position = quad.position(index);
			vertices.add(new VertexSignature(
					Float.floatToIntBits(position.x()),
					Float.floatToIntBits(position.y()),
					Float.floatToIntBits(position.z()),
					quad.packedUV(index)
			));
		}
		final QuadSignature signature = new QuadSignature(
				part,
				cullDirection,
				quad.direction(),
				new MaterialSignature(
						materialInfo.sprite(),
						materialInfo.layer(),
						materialInfo.itemRenderType(),
						material.tintIndex(),
						materialInfo.shade(),
						materialInfo.lightEmission()
				),
				material,
				List.copyOf(vertices)
		);
		quads.merge(signature, 1, Integer::sum);
	}

	private static Map<QuadSignature, Integer> difference(
			final Map<QuadSignature, Integer> left,
			final Map<QuadSignature, Integer> right
	) {
		final Map<QuadSignature, Integer> difference = new HashMap<>(left);
		right.forEach((signature, count) -> difference.computeIfPresent(signature, (_, current) ->
				current <= count ? null : current - count));
		return difference;
	}

	private static void finish(final boolean passed, final String message, final Throwable throwable) {
		finished = true;
		if (passed) {
			LOGGER.info("TERRAIN_MODEL_PARITY_PASS: {}", message);
		} else {
			LOGGER.error("TERRAIN_MODEL_PARITY_FAIL: {}", message, throwable);
		}
		Minecraft.getInstance().stop();
	}

	private record Fixture(String name, java.util.function.Consumer<StateEntryStorage> fill) {
		private ISingleBlockAxisAlignedAreaAccessor snapshot() {
			return new SimpleSnapshot(storage(this));
		}
	}

	private record PartSignature(
			BlockInformation source,
			net.minecraft.world.level.block.state.BlockState appearance,
			net.minecraft.util.TriState ambientOcclusion,
			net.minecraft.client.resources.model.sprite.Material.Baked particleMaterial,
			int materialFlags
	) {
	}

	private record VertexSignature(int x, int y, int z, long packedUv) {
	}

	private record QuadSignature(
			PartSignature part,
			String cullDirection,
			Direction direction,
			MaterialSignature material,
			ChiseledBlockModelMaterial materialSource,
			List<VertexSignature> vertices
	) {
	}

	private record MaterialSignature(
			net.minecraft.client.renderer.texture.TextureAtlasSprite sprite,
			net.minecraft.client.renderer.chunk.ChunkSectionLayer layer,
			net.minecraft.client.renderer.rendertype.RenderType itemRenderType,
			int sourceTintIndex,
			boolean shade,
			int lightEmission
	) {
	}

	private record OutputSignature(
			net.minecraft.client.resources.model.sprite.Material.Baked particleMaterial,
			int materialFlags,
			boolean ambientOcclusion,
			Map<QuadSignature, Integer> quads
	) {
		private int quadCount() {
			return quads.values().stream().mapToInt(Integer::intValue).sum();
		}
	}
}

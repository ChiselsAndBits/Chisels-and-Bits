package mod.chiselsandbits.forge.gametest;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

import com.mojang.logging.LogUtils;
import com.communi.suggestu.scena.core.client.models.data.IBlockModelData;
import com.communi.suggestu.scena.core.client.models.data.IModelDataKey;
import com.communi.suggestu.scena.core.client.rendering.terrain.TerrainGeometry;
import mod.chiselsandbits.api.block.storage.StateEntryStorage;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.multistate.StateEntrySize;
import mod.chiselsandbits.api.multistate.accessor.IStateEntryInfo;
import mod.chiselsandbits.api.multistate.accessor.ISingleBlockAxisAlignedAreaAccessor;
import mod.chiselsandbits.api.multistate.accessor.identifier.IAreaShapeIdentifier;
import mod.chiselsandbits.api.neighborhood.IBlockNeighborhood;
import mod.chiselsandbits.client.model.builder.ChiseledBlockModelMaterial;
import mod.chiselsandbits.client.model.meshing.ChiseledRenderSnapshot;
import mod.chiselsandbits.client.model.meshing.FaceCullingPolicy;
import mod.chiselsandbits.client.model.meshing.GreedyMeshBuilder;
import mod.chiselsandbits.client.model.meshing.GreedyMeshFace;
import mod.chiselsandbits.client.model.meshing.GreedyRectangles;
import mod.chiselsandbits.client.model.meshing.NeighbourBoundary;
import mod.chiselsandbits.client.model.meshing.RectangleSubsetPreflight;
import mod.chiselsandbits.client.model.block.ChiseledTerrainModelSelection;
import mod.chiselsandbits.client.model.block.ChiseledTerrainModelDataSnapshot;
import mod.chiselsandbits.client.model.information.ChiseledBlockModelCacheKey;
import mod.chiselsandbits.multistate.snapshot.SimpleSnapshot;
import mod.chiselsandbits.utils.ModelDataGeneration;
import org.joml.Vector3f;
import org.slf4j.Logger;

import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * Game tests for the Phase 1 mesher. They run without a world; the GameTest harness only provides Minecraft bootstrap
 * so that real block states can be used as materials.
 */
final class MesherGameTests {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int GRID = ChiseledRenderSnapshot.GRID;
	private static final int DIRECTIONS = Direction.values().length;
	private static final String BENCHMARK_ENV = "CHISELS_MESHER_BENCHMARK";
	/** Opt-in marker file in the gametest server directory; the run task does not forward env vars reliably. */
	private static final String BENCHMARK_MARKER = "mesher-benchmark.enabled";
	private static final Set<Block> TRANSPARENT = Set.of(Blocks.GLASS, Blocks.STAINED_GLASS.cyan());
	private static final FaceCullingPolicy OPAQUE_POLICY = FaceCullingPolicy.opaqueOrSameMaterial(
			information -> !TRANSPARENT.contains(information.blockState().getBlock())
	);
	private static final List<Block> PALETTE_BLOCKS = List.of(
			Blocks.STONE,
			Blocks.GLASS,
			Blocks.OAK_PLANKS,
			Blocks.QUARTZ_BLOCK,
			Blocks.STAINED_GLASS.cyan()
	);
	private static final List<Block> WIDE_PALETTE_BLOCKS = List.of(
			Blocks.STONE, Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE, Blocks.OAK_PLANKS,
			Blocks.SPRUCE_PLANKS, Blocks.BIRCH_PLANKS, Blocks.QUARTZ_BLOCK, Blocks.GLASS, Blocks.STAINED_GLASS.cyan(),
			Blocks.BRICKS, Blocks.COBBLESTONE, Blocks.SAND, Blocks.GRAVEL, Blocks.DIRT, Blocks.GRASS_BLOCK
	);
	private static final int[] SIZES = {1, 2, 4, 8, 16};

	private MesherGameTests() {
		throw new IllegalStateException("Can not instantiate an instance of: MesherGameTests. This is a utility class");
	}

	static void referenceExpansion(final GameTestHelper helper) {
		final List<BlockInformation> palette = palette(PALETTE_BLOCKS, true);
		int checked = 0;

		for (final int size : SIZES) {
			final List<StateEntryStorage> scenarios = new ArrayList<>();
			scenarios.add(storage(size));
			scenarios.add(filled(size, info(Blocks.STONE)));
			final StateEntryStorage single = storage(size);
			single.setBlockInformation(0, 0, 0, info(Blocks.STONE));
			scenarios.add(single);
			final StateEntryStorage corner = storage(size);
			corner.setBlockInformation(size - 1, size - 1, size - 1, info(Blocks.OAK_PLANKS));
			scenarios.add(corner);
			final StateEntryStorage center = storage(size);
			center.setBlockInformation(size / 2, size / 2, size / 2, info(Blocks.GLASS));
			scenarios.add(center);
			scenarios.add(checkerboard(size));

			for (int seed = 1; seed <= 4; seed++) {
				scenarios.add(randomFill(size, new Random(seed * 31L + size), palette));
			}

			scenarios.add(randomFill(size, new Random(991L + size), List.of(BlockInformation.AIR, info(Blocks.STONE))));

			for (final StateEntryStorage storage : scenarios) {
				for (final FaceCullingPolicy policy : List.of(FaceCullingPolicy.ANY_OCCUPIED, OPAQUE_POLICY)) {
					for (final NeighbourBoundary boundary : List.of(NeighbourBoundary.NONE, randomBoundary(size, checked))) {
						final ChiseledRenderSnapshot snapshot = ChiseledRenderSnapshot.of(storage);
						final GreedyRectangles geometry = snapshot.greedyRectangles(policy, boundary);
						final Map<Long, BlockInformation> expected = referenceFaces(storage, boundary, policy);
						final Map<Long, BlockInformation> actual = new HashMap<>();
						expand(helper, geometry, actual, "mesher size=" + size + " scenario=" + checked);
						assertSameFaces(helper, "reference size=" + size + " scenario=" + checked, expected, actual);
						checked++;
					}
				}
			}
		}

		LOGGER.info("MESHER_TEST reference_expansion checked {} scenarios", checked);
		helper.succeed();
	}

	static void legacyCoverage(final GameTestHelper helper) {
		final int size = StateEntrySize.current().getBitsPerBlockSide();
		final List<BlockInformation> palette = palette(PALETTE_BLOCKS, true);
		final List<StateEntryStorage> scenarios = new ArrayList<>();
		scenarios.add(storage(size));
		scenarios.add(filled(size, info(Blocks.STONE)));
		scenarios.add(checkerboard(size));
		final StateEntryStorage single = storage(size);
		single.setBlockInformation(size - 1, 0, size / 2, info(Blocks.OAK_PLANKS));
		scenarios.add(single);

		for (int seed = 1; seed <= 6; seed++) {
			scenarios.add(randomFill(size, new Random(seed * 17L), palette));
		}

		for (int i = 0; i < scenarios.size(); i++) {
			final StateEntryStorage storage = scenarios.get(i);
			final GreedyMeshFace[] legacy = GreedyMeshBuilder.buildMesh((x, y, z) -> storage.getBlockInformation(x, y, z));
			final Map<Long, BlockInformation> expectedFaces = new HashMap<>();

			for (final GreedyMeshFace face : legacy) {
				expandLegacy(helper, face, expectedFaces, "legacy scenario=" + i);
			}

			final Map<Long, BlockInformation> actualFaces = new HashMap<>();
			final GreedyRectangles geometry = ChiseledRenderSnapshot.of(storage).greedyRectangles(FaceCullingPolicy.ANY_OCCUPIED);
			expand(helper, geometry, actualFaces, "legacy-new scenario=" + i);
			assertSameFaces(helper, "legacy coverage size=" + size + " scenario=" + i, expectedFaces, actualFaces);
		}

		LOGGER.info("MESHER_TEST legacy_coverage size {} compared {} scenarios", size, scenarios.size());
		helper.succeed();
	}

	static void cavityFaces(final GameTestHelper helper) {
		final StateEntryStorage sealed = filled(GRID, info(Blocks.STONE));
		sealed.setBlockInformation(8, 8, 8, BlockInformation.AIR);
		final StateEntryStorage connected = sealed.createSnapshot();
		for (int x = 0; x < 8; x++) {
			connected.setBlockInformation(x, 8, 8, BlockInformation.AIR);
		}
		for (final StateEntryStorage storage : List.of(sealed, connected)) {
			final var geometry = ChiseledRenderSnapshot.of(storage).greedyRectangles(FaceCullingPolicy.ANY_OCCUPIED);
			final Map<Long, BlockInformation> legacy = new HashMap<>();
			for (final var face : GreedyMeshBuilder.buildMesh(storage::getBlockInformation)) {
				expandLegacy(helper, face, legacy, "adjacent-air cavity semantics");
			}
			assertSameFaces(helper, "sealed and connected cavities retain legacy faces", legacy, expandAll(helper, geometry));
			helper.assertTrue(geometry.unitFaceCount() == (storage == sealed ? 1542 : 1572),
					"Adjacent-air meshing must retain six sealed-pocket faces and all narrow-tunnel walls");
			final var exteriorOnly = ChiseledRenderSnapshot.of(storage).greedyRectanglesWithoutSealedAir(NeighbourBoundary.NONE);
			helper.assertTrue(exteriorOnly.unitFaceCount() == (storage == sealed ? 1536 : 1572),
					"Opt-in sealed-air culling must remove only unreachable cavity surfaces");
			if (storage == connected) {
				assertSameFaces(helper, "Open tunnel preserves every reachable internal surface",
						legacy, expandAll(helper, exteriorOnly));
			}
		}
		final StateEntryStorage corner = filled(GRID, info(Blocks.STONE));
		for (int x = 0; x <= 8; x++) {
			corner.setBlockInformation(x, 8, 8, BlockInformation.AIR);
		}
		for (int y = 8; y <= 11; y++) {
			corner.setBlockInformation(8, y, 8, BlockInformation.AIR);
		}
		final var cornerSnapshot = ChiseledRenderSnapshot.of(corner);
		assertSameFaces(helper, "Exterior reachability follows a turning one-bit tunnel",
				expandAll(helper, cornerSnapshot.greedyRectangles(FaceCullingPolicy.ANY_OCCUPIED)),
				expandAll(helper, cornerSnapshot.greedyRectanglesWithoutSealedAir(NeighbourBoundary.NONE)));
		for (final Block unsupported : List.of(Blocks.GLASS, Blocks.OAK_LEAVES, Blocks.WATER)) {
			final StateEntryStorage nonOpaque = sealed.createSnapshot();
			nonOpaque.setBlockInformation(7, 8, 8, info(unsupported));
			helper.assertTrue(rejects(() -> ChiseledRenderSnapshot.of(nonOpaque)
							.greedyRectanglesWithoutSealedAir(NeighbourBoundary.NONE)),
					"Sealed-air culling must reject unsupported material " + unsupported);
		}
		LOGGER.info("MESHER_TEST cavity_faces default sealed=1542/open=1572; opt-in sealed=1536/open=1572; turning tunnel retained; glass/leaves/water rejected");
		helper.succeed();
	}

	static void interiorRectangleSubset(final GameTestHelper helper) {
		final int size = 16;
		final StateEntryStorage interior = storage(size);
		interior.setBlockInformation(7, 7, 7, info(Blocks.STONE));
		final ChiseledRenderSnapshot snapshot = ChiseledRenderSnapshot.of(interior);
		final GreedyRectangles geometry = snapshot.greedyRectangles(FaceCullingPolicy.ANY_OCCUPIED);
		helper.assertTrue(geometry.hasNoBlockBoundaryFaces(), "An isolated interior voxel must qualify for the rectangle subset");

		final Map<Long, BlockInformation> legacyFaces = new HashMap<>();
		for (final GreedyMeshFace face : GreedyMeshBuilder.buildMesh((x, y, z) -> interior.getBlockInformation(x, y, z))) {
			expandLegacy(helper, face, legacyFaces, "interior subset");
		}
		assertSameFaces(helper, "interior subset preserves legacy face coverage", legacyFaces, expandAll(helper, geometry));
		assertSameFaces(helper, "interior subset bounds preserve legacy face coverage", legacyFaces, expandBounds(helper, geometry));

		final StateEntryStorage boundary = storage(size);
		boundary.setBlockInformation(0, 7, 7, info(Blocks.STONE));
		helper.assertTrue(
				!ChiseledRenderSnapshot.of(boundary).greedyRectangles(FaceCullingPolicy.ANY_OCCUPIED).hasNoBlockBoundaryFaces(),
				"A voxel touching the block edge must not be classified as interior"
		);
		helper.assertTrue(
				!ChiseledRenderSnapshot.of(storage(size)).greedyRectangles(FaceCullingPolicy.ANY_OCCUPIED).hasNoBlockBoundaryFaces(),
				"Empty geometry must not qualify"
		);

		final int sourceSize = StateEntrySize.current().getBitsPerBlockSide();
		final int sourceCenter = sourceSize / 2;
		final StateEntryStorage accessorStorage = storage(sourceSize);
		accessorStorage.setBlockInformation(sourceCenter, sourceCenter, sourceCenter, info(Blocks.STONE));
		final SimpleSnapshot accessorSnapshot = new SimpleSnapshot(accessorStorage.createSnapshot());
		final ChiseledRenderSnapshot fromAccessor = ChiseledRenderSnapshot.of(accessorSnapshot);
		final int normalizedCenter = sourceCenter * GRID / sourceSize + (GRID / sourceSize) / 2;
		helper.assertTrue(fromAccessor.materialAt(normalizedCenter, normalizedCenter, normalizedCenter).equals(info(Blocks.STONE)), "Accessor sampling must preserve the source material");
		helper.succeed();
	}

	static void rectangleSubsetPreflight(final GameTestHelper helper) {
		helper.assertTrue(RectangleSubsetPreflight.supportsMaterial(info(Blocks.STONE)), "Opaque solid stone should qualify");
		helper.assertTrue(RectangleSubsetPreflight.supportsMaterial(info(Blocks.GLASS)), "Static translucent glass is eligible for source-face preflight");
		helper.assertTrue(RectangleSubsetPreflight.supportsMaterial(info(Blocks.WATER)), "Static liquid blocks are eligible for fluid-model preflight");
		helper.assertTrue(RectangleSubsetPreflight.supportsMaterial(info(Blocks.OAK_LEAVES)), "Dry leaves are candidates subject to source-face checks");
		helper.assertTrue(RectangleSubsetPreflight.supportsMaterial(new BlockInformation(
				Blocks.OAK_LEAVES.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true),
				Optional.empty())), "Waterlogged states must be rejected only after their actual source pipeline is inspected");
		final ChiseledBlockModelMaterial plainStone = new ChiseledBlockModelMaterial(info(Blocks.STONE), -1);
		final ChiseledBlockModelMaterial plainOak = new ChiseledBlockModelMaterial(info(Blocks.OAK_PLANKS), -1);
		final ChiseledBlockModelMaterial tintedStone = new ChiseledBlockModelMaterial(info(Blocks.STONE), 0);
		helper.assertTrue(
				RectangleSubsetPreflight.hasCompatibleTintIndices(List.of(plainStone, plainOak), List.of(plainOak, plainStone)),
				"Reordering untinted materials must remain eligible"
		);
		helper.assertTrue(
				!RectangleSubsetPreflight.hasCompatibleTintIndices(List.of(tintedStone, plainOak), List.of(plainOak, tintedStone)),
				"Reordering tinted materials must fall back instead of applying another material's tint"
		);
		helper.assertTrue(
				RectangleSubsetPreflight.hasCompatibleTintIndices(List.of(tintedStone, plainOak), List.of(tintedStone, plainOak)),
				"Matching tint indices should remain eligible"
		);
		helper.assertTrue(
				RectangleSubsetPreflight.legacyTintIndex(List.of(plainOak, tintedStone), info(Blocks.STONE), 0) == 1,
				"Direct geometry tint indices must address the exact matching legacy material entry"
		);
		helper.assertTrue(
				RectangleSubsetPreflight.legacyTintIndex(List.of(plainOak), info(Blocks.STONE), 0) == -1,
				"Missing source tint entries must reject the direct geometry candidate"
		);
		helper.assertTrue(
				!RectangleSubsetPreflight.hasCompatibleTintIndices(List.of(plainOak), List.of(plainOak, tintedStone)),
				"A candidate must not reference a missing legacy tint index"
		);
		final SingleVariant simpleVariant = new SingleVariant(null);
		final Object vanillaGeometryKey = simpleVariant.createGeometryKey(null, null, null, null);
		helper.assertTrue(vanillaGeometryKey == simpleVariant,
				"Minecraft 26.2 SingleVariant geometry keys are the stable model instance");
		final ChiseledBlockModelCacheKey staticModels = sourceKey(List.of(info(Blocks.STONE)), Collections.singletonList(vanillaGeometryKey));
		final ChiseledBlockModelCacheKey missingGeometryKey = sourceKey(List.of(info(Blocks.STONE)), Collections.singletonList(null));
		final ChiseledBlockModelCacheKey dynamicModel = sourceKey(List.of(info(Blocks.STONE)), List.of(new Object()));
		final ChiseledBlockModelCacheKey subclassedVariant = sourceKey(List.of(info(Blocks.STONE)),
				List.of(new SingleVariant(null) { }));
		final ChiseledBlockModelCacheKey unalignedKeys = new ChiseledBlockModelCacheKey(
				IAreaShapeIdentifier.DUMMY, BlockInformation.AIR, IBlockNeighborhood.EMPTY, List.of(vanillaGeometryKey));
		final java.util.List<Object> fluidKeys = new java.util.ArrayList<>();
		fluidKeys.add(new Object());
		fluidKeys.add(null);
		final ChiseledBlockModelCacheKey syntheticFluids = sourceKey(List.of(info(Blocks.WATER), info(Blocks.LAVA)), fluidKeys);
		final ChiseledBlockModelCacheKey fluidWithDynamicBlock = sourceKey(
				List.of(info(Blocks.WATER), info(Blocks.STONE)), List.of(vanillaGeometryKey, new Object()));
		final BlockInformation waterlogged = new BlockInformation(Blocks.OAK_LEAVES.defaultBlockState()
				.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), Optional.empty());
		final ChiseledBlockModelCacheKey waterloggedDynamic = sourceKey(List.of(waterlogged), List.of(new Object()));
		helper.assertTrue(RectangleSubsetPreflight.supportsStaticSourceModels(staticModels), "Vanilla simple baked models should qualify as static");
		helper.assertTrue(!RectangleSubsetPreflight.supportsStaticSourceModels(missingGeometryKey), "A null model key is not proof of static geometry");
		helper.assertTrue(
				RectangleSubsetPreflight.sourceModelRejectionReason(missingGeometryKey).contains("no geometry key"),
				"Missing model-key rejection should explain the actual unsupported contract"
		);
		helper.assertTrue(!RectangleSubsetPreflight.supportsStaticSourceModels(dynamicModel),
				"A dynamic geometry key does not prove stable geometry across asynchronous capture and publication");
		helper.assertTrue(RectangleSubsetPreflight.sourceModelRejectionReason(dynamicModel).contains("static single variant"),
				"Dynamic-key rejection should name the static-variant contract");
		helper.assertTrue(!RectangleSubsetPreflight.supportsStaticSourceModels(subclassedVariant),
				"SingleVariant subclasses may override part collection and must fall back");
		helper.assertTrue(!RectangleSubsetPreflight.supportsStaticSourceModels(unalignedKeys),
				"Model keys without their index-aligned source states cannot be attributed and must fall back");
		helper.assertTrue(RectangleSubsetPreflight.supportsStaticSourceModels(syntheticFluids),
				"LiquidBlock fluids use FaceManager's synthetic FluidModel source, independent of the block-state model key");
		helper.assertTrue(!RectangleSubsetPreflight.supportsStaticSourceModels(fluidWithDynamicBlock),
				"The fluid exemption must not extend to a dynamic block model in the same snapshot");
		helper.assertTrue(!RectangleSubsetPreflight.supportsStaticSourceModels(waterloggedDynamic),
				"Waterlogged non-LiquidBlock states keep the static-model requirement");

		helper.assertTrue(RectangleSubsetPreflight.supportsSourceFace(true, false, 0), "Static solid source faces should qualify");
		helper.assertTrue(!RectangleSubsetPreflight.supportsSourceFace(false, false, 0), "Unsupported source layers must fall back");
		helper.assertTrue(RectangleSubsetPreflight.supportsSourceFace(true, true, 0), "Atlas animation is preserved by the source sprite");
		helper.assertTrue(RectangleSubsetPreflight.supportsSourceFace(true, false, 15), "Resolved light emission is preserved by the native sinks");
		helper.assertTrue(RectangleSubsetPreflight.supportsSourceFace(
				ChunkSectionLayer.CUTOUT, false, 0), "Actual static cutout source layers should qualify");
		helper.assertTrue(RectangleSubsetPreflight.supportsSourceFace(
				ChunkSectionLayer.TRANSLUCENT, true, 15), "Animated translucent fluid sources retain layer, sprite and light emission");
		helper.assertTrue(RectangleSubsetPreflight.supportsMaterialFlags(
				net.minecraft.client.resources.model.geometry.BakedQuad.FLAG_ANIMATED
						| net.minecraft.client.resources.model.geometry.BakedQuad.FLAG_TRANSLUCENT),
				"Known animated/translucent material flags are preserved");
		helper.assertTrue(!RectangleSubsetPreflight.supportsMaterialFlags(4), "Unknown material flags must fall back");
		helper.assertTrue(RectangleSubsetPreflight.supportsSourceFaceOutput(true, 1), "A validated emitted source face should qualify");
		helper.assertTrue(!RectangleSubsetPreflight.supportsSourceFaceOutput(true, 0), "Missing source faces must reject the entire candidate");
		helper.assertTrue(!RectangleSubsetPreflight.supportsSourceFaceOutput(false, 1), "An invalid source face must reject the entire candidate");
		helper.assertTrue(
				RectangleSubsetPreflight.supportsSourceFaceGeometry(Direction.UP, new float[][]{
						{0, 1, 0}, {1, 1, 0}, {1, 1, 1}, {0, 1, 1}
				}),
				"Canonical full-face source quads should qualify"
		);
		helper.assertTrue(
				!RectangleSubsetPreflight.supportsSourceFaceGeometry(Direction.UP, new float[][]{
						{0.25f, 1, 0.25f}, {0.75f, 1, 0.25f}, {0.75f, 1, 0.75f}, {0.25f, 1, 0.75f}
				}),
				"Partial source quads must fall back instead of being expanded to a full rectangle"
		);
		helper.assertTrue(
				!RectangleSubsetPreflight.supportsSourceFaceGeometry(Direction.UP, new float[][]{
						{0, 0.9f, 0}, {1, 1, 0}, {1, 1, 1}, {0, 0.9f, 1}
				}),
				"Non-planar source quads must fall back"
		);
		final StateEntryStorage solidInterface = storage(16);
		solidInterface.setBlockInformation(7, 7, 7, info(Blocks.STONE));
		solidInterface.setBlockInformation(8, 7, 7, info(Blocks.OAK_PLANKS));
		final var solidSnapshot = ChiseledRenderSnapshot.of(solidInterface);
		helper.assertTrue(RectangleSubsetPreflight.supportsMaterialInterfaces(solidSnapshot, Map.of(
				info(Blocks.STONE), ChunkSectionLayer.SOLID, info(Blocks.OAK_PLANKS), ChunkSectionLayer.SOLID)),
				"Adjacent opaque solid materials preserve legacy interface culling");
		final StateEntryStorage fluidCutoutInterface = storage(16);
		fluidCutoutInterface.setBlockInformation(7, 7, 7, info(Blocks.WATER));
		fluidCutoutInterface.setBlockInformation(8, 7, 7, info(Blocks.OAK_LEAVES));
		helper.assertTrue(!RectangleSubsetPreflight.supportsMaterialInterfaces(
				ChiseledRenderSnapshot.of(fluidCutoutInterface), Map.of(
						info(Blocks.WATER), ChunkSectionLayer.TRANSLUCENT, info(Blocks.OAK_LEAVES), ChunkSectionLayer.CUTOUT)),
				"Fluid/cutout interfaces use the legacy fallback instead of dropping mutually visible faces");

		final Object legacyModel = new Object();
		final Object rectangleModel = new Object();
		final ChiseledTerrainModelSelection.Selection<Object> selectedRectangle =
				ChiseledTerrainModelSelection.select(legacyModel, rectangleModel);
		helper.assertTrue(selectedRectangle.rectangleSelected() && selectedRectangle.model() == rectangleModel,
				"An available preflighted rectangle model must be selected");
		final ChiseledTerrainModelSelection.Selection<Object> selectedFallback =
				ChiseledTerrainModelSelection.select(legacyModel, null);
		helper.assertTrue(!selectedFallback.rectangleSelected() && selectedFallback.model() == legacyModel,
				"An unavailable rectangle model must select the existing fallback");

		final AtomicInteger nativePreparations = new AtomicInteger();
		final AtomicInteger nativeWrites = new AtomicInteger();
		final var nativePlan = ChiseledTerrainModelSelection.prepare(legacyModel, rectangleModel, model -> {
			helper.assertTrue(model == rectangleModel, "Native preparation must receive the selected snapshot");
			nativePreparations.incrementAndGet();
			return nativeWrites::incrementAndGet;
		});
		helper.assertTrue(nativePreparations.get() == 1 && nativeWrites.get() == 0,
				"Native preparation must not emit geometry");
		helper.assertTrue(nativePlan.nativeEmission() != null, "Supported rectangle model must retain its native plan");
		nativePlan.nativeEmission().run();
		helper.assertTrue(nativeWrites.get() == 1, "The prepared native plan must emit when invoked");
		final var bakedPlan = ChiseledTerrainModelSelection.prepare(legacyModel, rectangleModel, _ -> null);
		helper.assertTrue(bakedPlan.selection().model() == rectangleModel && bakedPlan.nativeEmission() == null,
				"An unsupported native backend must retain the rectangle baked-model path");
		final var legacyPlan = ChiseledTerrainModelSelection.prepare(legacyModel, null, _ -> {
			throw new AssertionError("Legacy models must not be sent to native preparation");
		});
		helper.assertTrue(legacyPlan.selection().model() == legacyModel && legacyPlan.nativeEmission() == null,
				"Legacy selection must retain the existing model path");
		final TerrainGeometry directGeometry = new TerrainGeometry(List.of());
		final AtomicInteger directPreparations = new AtomicInteger();
		final var directPlan = ChiseledTerrainModelSelection.prepareNativeGeometry(legacyModel, directGeometry, geometry -> {
			helper.assertTrue(geometry == directGeometry, "Native preparation must receive the exact resolved geometry snapshot");
			directPreparations.incrementAndGet();
			return nativeWrites::incrementAndGet;
		});
		helper.assertTrue(directPlan.usesNativeGeometry() && directPreparations.get() == 1 && nativeWrites.get() == 1,
				"Resolved geometry preflight must prepare without emitting");
		directPlan.nativeEmission().run();
		helper.assertTrue(nativeWrites.get() == 2, "Prepared resolved geometry must emit only when invoked");
		final var rejectedDirectPlan = ChiseledTerrainModelSelection.prepareNativeGeometry(legacyModel, directGeometry, _ -> null);
		helper.assertTrue(!rejectedDirectPlan.usesNativeGeometry() && rejectedDirectPlan.fallbackModel() == legacyModel,
				"Unsupported native geometry must retain the original legacy model fallback");
		final var absentDirectPlan = ChiseledTerrainModelSelection.prepareNativeGeometry(legacyModel, null, _ -> {
			throw new AssertionError("Missing geometry must not be sent to native preparation");
		});
		helper.assertTrue(!absentDirectPlan.usesNativeGeometry() && absentDirectPlan.fallbackModel() == legacyModel,
				"Missing direct geometry must retain the original legacy model fallback");

		assertTerrainModelDataPublicationRace(helper);
		helper.succeed();
	}

	private static void assertTerrainModelDataPublicationRace(final GameTestHelper helper) {
		final IModelDataKey<Object> legacyKey = new IModelDataKey<>() { };
		final IModelDataKey<Object> rectangleKey = new IModelDataKey<>() { };
		final IModelDataKey<TerrainGeometry> terrainGeometryKey = new IModelDataKey<>() { };
		final Object oldLegacy = new Object();
		final Object oldRectangle = new Object();
		final Object newLegacy = new Object();
		final TerrainGeometry oldGeometry = new TerrainGeometry(List.of());
		final IBlockModelData oldData = modelData(legacyKey, oldLegacy, rectangleKey, oldRectangle, terrainGeometryKey, oldGeometry);
		final IBlockModelData newData = modelData(legacyKey, newLegacy, rectangleKey, null, terrainGeometryKey, null);
		final AtomicInteger reads = new AtomicInteger();

		final ChiseledTerrainModelDataSnapshot<Object> preflight = ChiseledTerrainModelDataSnapshot.read(
				() -> reads.getAndIncrement() == 0 ? oldData : newData,
				legacyKey,
				rectangleKey,
				terrainGeometryKey
		);
		helper.assertTrue(reads.get() == 1, "Preflight must read the published model-data object only once");
		helper.assertTrue(preflight.legacyModel() == oldLegacy && preflight.rectangleModel() == oldRectangle,
				"Both preflight properties must come from the same model-data publication");
		helper.assertTrue(preflight.terrainGeometry() == oldGeometry,
				"Resolved geometry must come from the same model-data publication as its legacy model");

		final AtomicReference<IBlockModelData> published = new AtomicReference<>(oldData);
		final ChiseledTerrainModelDataSnapshot<Object> preparedForEmission =
				ChiseledTerrainModelDataSnapshot.read(published::get, legacyKey, rectangleKey, terrainGeometryKey);
		published.set(newData);
		final Object originalModel = new Object();
		final ChiseledTerrainModelSelection.Selection<Object> inFlightSelection = ChiseledTerrainModelSelection.select(
				preparedForEmission.legacyModel() == null ? originalModel : preparedForEmission.legacyModel(),
				preparedForEmission.rectangleModel()
		);
		helper.assertTrue(inFlightSelection.rectangleSelected() && inFlightSelection.model() == oldRectangle,
				"Emission must use the exact coherent pair accepted during preflight");
		final AtomicReference<TerrainGeometry> preparedGeometry = new AtomicReference<>();
		final ChiseledTerrainModelSelection.NativePrepared<Object> inFlightNativePlan =
				ChiseledTerrainModelSelection.prepareNativeGeometry(
						preparedForEmission.legacyModel(),
						preparedForEmission.terrainGeometry(),
						geometry -> {
							preparedGeometry.set(geometry);
							return () -> { };
						}
				);
		helper.assertTrue(preparedGeometry.get() == oldGeometry,
				"Native preparation must receive resolved geometry from the same immutable publication as legacy data");

		final ChiseledTerrainModelDataSnapshot<Object> nextCompile =
				ChiseledTerrainModelDataSnapshot.read(published::get, legacyKey, rectangleKey, terrainGeometryKey);
		final ChiseledTerrainModelSelection.Selection<Object> nextSelection = ChiseledTerrainModelSelection.select(
				nextCompile.legacyModel() == null ? originalModel : nextCompile.legacyModel(),
				nextCompile.rectangleModel()
		);
		helper.assertTrue(!nextSelection.rectangleSelected() && nextSelection.model() == newLegacy,
				"A later compile must use the newly published coherent fallback pair");
		final ChiseledTerrainModelSelection.NativePrepared<Object> nextNativePlan =
				ChiseledTerrainModelSelection.prepareNativeGeometry(
						nextCompile.legacyModel(),
						nextCompile.terrainGeometry(),
						_ -> {
							throw new AssertionError("A publication without resolved geometry must not be natively prepared");
						}
				);
		helper.assertTrue(!nextNativePlan.usesNativeGeometry() && nextNativePlan.fallbackModel() == newLegacy,
				"A later publication without resolved geometry must fall back with its matching legacy model");

		final IBlockModelData emptyData = modelData(legacyKey, null, rectangleKey, null);
		final ChiseledTerrainModelDataSnapshot<Object> unavailable =
				ChiseledTerrainModelDataSnapshot.read(() -> emptyData, legacyKey, rectangleKey);
		final ChiseledTerrainModelSelection.Selection<Object> originalFallback = ChiseledTerrainModelSelection.select(
				unavailable.legacyModel() == null ? originalModel : unavailable.legacyModel(),
				unavailable.rectangleModel()
		);
		helper.assertTrue(!originalFallback.rectangleSelected() && originalFallback.model() == originalModel,
				"Disappearing block-entity model data must select the original model operation");
	}

	private static IBlockModelData modelData(
			final IModelDataKey<Object> legacyKey,
			final Object legacy,
			final IModelDataKey<Object> rectangleKey,
			final Object rectangle
	) {
		return modelData(legacyKey, legacy, rectangleKey, rectangle, null, null);
	}

	private static IBlockModelData modelData(
			final IModelDataKey<Object> legacyKey,
			final Object legacy,
			final IModelDataKey<Object> rectangleKey,
			final Object rectangle,
			final IModelDataKey<TerrainGeometry> terrainGeometryKey,
			final TerrainGeometry terrainGeometry
	) {
		final Map<IModelDataKey<?>, Object> values = new HashMap<>();
		if (legacy != null) {
			values.put(legacyKey, legacy);
		}
		if (rectangle != null) {
			values.put(rectangleKey, rectangle);
		}
		if (terrainGeometryKey != null && terrainGeometry != null) {
			values.put(terrainGeometryKey, terrainGeometry);
		}

		return new IBlockModelData() {
			@Override
			public boolean hasProperty(final IModelDataKey<?> property) {
				return values.containsKey(property);
			}

			@SuppressWarnings("unchecked")
			@Override
			public <T> T getData(final IModelDataKey<T> property) {
				return (T) values.get(property);
			}
		};
	}

	static void experimentalModelDataGeneration(final GameTestHelper helper) {
		final ModelDataGeneration generations = new ModelDataGeneration();
		final AtomicReference<String> published = new AtomicReference<>();
		final long staleGeneration = generations.begin();
		final CountDownLatch finishStaleBuild = new CountDownLatch(1);
		final CompletableFuture<Boolean> stalePublication = CompletableFuture.supplyAsync(() -> {
			try {
				finishStaleBuild.await();
			} catch (final InterruptedException exception) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(exception);
			}

			return generations.publishIfCurrent(staleGeneration, () -> published.set("stale"));
		});

		final long currentGeneration = generations.begin();
		helper.assertTrue(!generations.isCurrent(staleGeneration), "A newer request must invalidate the older generation");
		helper.assertTrue(generations.publishIfCurrent(currentGeneration, () -> published.set("current")), "The current generation must publish");
		finishStaleBuild.countDown();
		helper.assertTrue(!stalePublication.join(), "A late completion from an older edit must be rejected");
		helper.assertTrue("current".equals(published.get()), "Late stale completion must not replace the current model data");
		helper.succeed();
	}

	static void snapshotInvariants(final GameTestHelper helper) {
		checkPackedStorage(helper);
		final int configuredSize = StateEntrySize.current().getBitsPerBlockSide();
		final StateEntryStorage source = new StateEntryStorage(configuredSize);
		source.setBlockInformation(0, 0, 0, info(Blocks.STONE));
		if (configuredSize > 1) {
			source.setBlockInformation(1, 0, 0, info(Blocks.CAVE_AIR));
			source.setBlockInformation(0, 1, 0, info(Blocks.GLASS));
		}
		final var directAccessor = new SimpleSnapshot(source);
		final AtomicInteger sampled = new AtomicInteger();
		final var genericAccessor = new SimpleSnapshot(source) {
			@Override
			public Optional<IStateEntryInfo> getInAreaTarget(final Vec3 target) {
				sampled.incrementAndGet();
				return super.getInAreaTarget(target);
			}
		};
		final ChiseledRenderSnapshot direct = ChiseledRenderSnapshot.of(directAccessor);
		final ChiseledRenderSnapshot generic = ChiseledRenderSnapshot.of(genericAccessor);
		helper.assertTrue(sampled.get() == configuredSize * configuredSize * configuredSize,
				"Custom snapshot accessors must retain their sampling behavior");
		helper.assertTrue(Arrays.equals(materialsOf(direct), materialsOf(generic)),
				"Direct snapshot storage and generic accessor sampling must produce the same contents");
		helper.assertTrue(Arrays.equals(materialsOf(generic), materialsOf(legacyAccessorSnapshot(genericAccessor))),
				"Generic direct sampling must match reconstruction through temporary storage");
		final IStateEntryInfo customAir = new IStateEntryInfo() {
			@Override public BlockInformation getBlockInformation() { return info(Blocks.CAVE_AIR); }
			@Override public Vec3 getStartPoint() { return Vec3.ZERO; }
			@Override public Vec3 getEndPoint() { return new Vec3(1, 1, 1); }
		};
		final var customAccessor = new SimpleSnapshot(source) {
			@Override public Optional<IStateEntryInfo> getInAreaTarget(final Vec3 target) { return Optional.of(customAir); }
		};
		final var custom = ChiseledRenderSnapshot.of(customAccessor);
		helper.assertTrue(Arrays.equals(materialsOf(custom), materialsOf(legacyAccessorSnapshot(customAccessor)))
						&& custom.materialAt(0, 0, 0).equals(info(Blocks.CAVE_AIR)),
				"Custom accessors must retain explicitly returned non-default air information");
		if (configuredSize > 1) {
			helper.assertTrue(direct.materialAt(GRID / configuredSize, 0, 0).equals(BlockInformation.AIR),
					"Accessor snapshots must normalize non-default air states");
		}
		final StateEntryStorage boundarySource = randomFill(configuredSize, new Random(61L), palette(PALETTE_BLOCKS, true));
		boundarySource.setBlockInformation(0, 0, 0, info(Blocks.CAVE_AIR));
		final var boundaryAccessor = new SimpleSnapshot(boundarySource);
		final AtomicInteger planeSamples = new AtomicInteger();
		final var customBoundaryAccessor = new SimpleSnapshot(boundarySource) {
			@Override
			public Optional<IStateEntryInfo> getInAreaTarget(final Vec3 target) {
				planeSamples.incrementAndGet();
				return super.getInAreaTarget(target);
			}
		};
		final var fullNeighbour = ChiseledRenderSnapshot.of(boundaryAccessor);
		for (final Direction direction : Direction.values()) {
			final var expected = NeighbourBoundary.NONE.withSnapshot(direction, fullNeighbour);
			final var fast = NeighbourBoundary.NONE.withAccessor(direction, boundaryAccessor);
			final var sampledPlane = NeighbourBoundary.NONE.withAccessor(direction, customBoundaryAccessor);
			final var customAirPlane = NeighbourBoundary.NONE.withAccessor(direction, customAccessor);
			for (int u = 0; u < GRID; u++) {
				for (int v = 0; v < GRID; v++) {
					helper.assertTrue(expected.materialAt(direction, u, v).equals(fast.materialAt(direction, u, v))
									&& expected.materialAt(direction, u, v).equals(sampledPlane.materialAt(direction, u, v)),
							"Captured neighbour plane must match the full snapshot on " + direction + " at " + u + "," + v);
					helper.assertTrue(customAirPlane.materialAt(direction, u, v).equals(info(Blocks.CAVE_AIR)),
							"Custom neighbour accessors must retain explicitly returned non-default air");
				}
			}
		}
		helper.assertTrue(planeSamples.get() == Direction.values().length * configuredSize * configuredSize,
				"Custom neighbours must be sampled once per source cell on the touching plane");
		final var captured = NeighbourBoundary.NONE.withAccessor(Direction.EAST, boundaryAccessor);
		final var beforeMutation = captured.materialAt(Direction.EAST, 0, 0);
		boundarySource.setBlockInformation(0, 0, 0, info(Blocks.GOLD_BLOCK));
		helper.assertTrue(captured.materialAt(Direction.EAST, 0, 0).equals(beforeMutation),
				"Captured boundary planes must be independent of later storage mutations");
		final StateEntryStorage mismatchedStorage = new StateEntryStorage(configuredSize + 1);
		mismatchedStorage.setBlockInformation(configuredSize - 1, 0, 0, info(Blocks.STONE));
		final var mismatchedAccessor = new SimpleSnapshot(mismatchedStorage);
		final var mismatchExpected = NeighbourBoundary.NONE.withSnapshot(Direction.WEST,
				ChiseledRenderSnapshot.of(mismatchedAccessor));
		final var mismatchActual = NeighbourBoundary.NONE.withAccessor(Direction.WEST, mismatchedAccessor);
		for (int u = 0; u < GRID; u++) {
			for (int v = 0; v < GRID; v++) {
				helper.assertTrue(mismatchExpected.materialAt(Direction.WEST, u, v)
								.equals(mismatchActual.materialAt(Direction.WEST, u, v)),
						"Mismatched storage sizes must retain accessor sampling semantics");
			}
		}
		source.setBlockInformation(0, 0, 0, info(Blocks.GLASS));
		helper.assertTrue(direct.materialAt(0, 0, 0).equals(info(Blocks.STONE)),
				"Direct render snapshots must remain independent of subsequent storage changes");
		final StateEntryStorage copied = directAccessor.copyStorage();
		copied.setBlockInformation(0, 0, 0, info(Blocks.STONE));
		helper.assertTrue(directAccessor.getInAreaTarget(new Vec3(0, 0, 0)).orElseThrow().getBlockInformation().equals(info(Blocks.GLASS)),
				"Mutating the exported storage copy must not change its source snapshot");
		if (Boolean.getBoolean("chiselsandbits.snapshotBenchmark")) {
			final var baselineAccessor = new SimpleSnapshot(source) { };
			report("runtime_snapshot", "legacy_reconstruction", 1000, () -> legacyAccessorSnapshot(baselineAccessor).version());
			report("runtime_snapshot", "generic_accessor", 1000, () -> ChiseledRenderSnapshot.of(baselineAccessor).version());
			report("runtime_snapshot", "copied_storage", 1000, () -> ChiseledRenderSnapshot.of(directAccessor.copyStorage()).version());
			report("runtime_snapshot", "packed_storage", 1000, () -> ChiseledRenderSnapshot.of(directAccessor).version());
			report("runtime_storage_read", "copied_storage", 1000, () -> readStorage(directAccessor.copyStorage()));
			report("runtime_storage_read", "packed_storage", 1000, () -> readStorage(directAccessor.copyPackedStorage()));
			report("runtime_boundary", "full_snapshot", 1000, () -> NeighbourBoundary.NONE
					.withSnapshot(Direction.EAST, ChiseledRenderSnapshot.of(boundaryAccessor)).version(Direction.EAST));
			report("runtime_boundary", "plane_only", 1000, () -> NeighbourBoundary.NONE
					.withAccessor(Direction.EAST, boundaryAccessor).version(Direction.EAST));
			report("runtime_boundary", "six_planes", 1000, () -> {
				NeighbourBoundary result = NeighbourBoundary.NONE;
				for (final Direction direction : Direction.values()) {
					result = result.withAccessor(direction, boundaryAccessor);
				}
				return result.version(Direction.EAST);
			});
		}

		final StateEntryStorage storage = randomFill(8, new Random(7L), palette(PALETTE_BLOCKS, true));
		final ChiseledRenderSnapshot snapshot = ChiseledRenderSnapshot.of(storage);
		final BlockInformation[] before = materialsOf(snapshot);
		final StateEntryStorage pristine = randomFill(8, new Random(7L), palette(PALETTE_BLOCKS, true));
		helper.assertTrue(Arrays.equals(before, materialsOf(ChiseledRenderSnapshot.of(pristine))), "Reference copy must match the snapshot's source contents");
		final GreedyRectangles intrinsic = snapshot.greedyRectangles(OPAQUE_POLICY);
		final long intrinsicChecksum = intrinsic.checksum();

		for (int x = 0; x < 8; x++) {
			for (int y = 0; y < 8; y++) {
				for (int z = 0; z < 8; z++) {
					storage.setBlockInformation(x, y, z, info(Blocks.GLASS));
				}
			}
		}

		helper.assertTrue(Arrays.equals(before, materialsOf(snapshot)), "Snapshot changed after the source storage was mutated");
		helper.assertTrue(snapshot.greedyRectangles(OPAQUE_POLICY) == intrinsic, "Intrinsic geometry was not cached per policy");
		helper.assertTrue(intrinsic.checksum() == intrinsicChecksum, "Cached geometry changed after source mutation");
		helper.assertTrue(intrinsic.sourceVersion() == snapshot.version(), "Geometry must carry its source version");

		final ChiseledRenderSnapshot rebuilt = ChiseledRenderSnapshot.of(storage);
		helper.assertTrue(rebuilt.version() > snapshot.version(), "Rebuilt snapshot must have a newer version");
		helper.assertTrue(rebuilt.greedyRectangles(OPAQUE_POLICY).checksum() != intrinsicChecksum, "Rebuilt snapshot must reflect the new storage contents");

		final FaceCullingPolicy otherPolicy = FaceCullingPolicy.opaqueOrSameMaterial(information -> true);
		helper.assertTrue(snapshot.greedyRectangles(otherPolicy) != intrinsic, "Different policy instances must not share cached geometry");
		helper.assertTrue(intrinsic.checksum() == intrinsicChecksum, "Evicting the cache must not modify previously returned geometry");

		final StateEntryStorage neighbourStorage = filled(8, info(Blocks.STONE));
		final ChiseledRenderSnapshot neighbour = ChiseledRenderSnapshot.of(neighbourStorage);
		final NeighbourBoundary withNeighbour = NeighbourBoundary.NONE.withSnapshot(Direction.EAST, neighbour);
		final GreedyRectangles boundaryGeometry = snapshot.greedyRectangles(OPAQUE_POLICY, withNeighbour);
		helper.assertTrue(snapshot.greedyRectangles(OPAQUE_POLICY, withNeighbour) != boundaryGeometry, "Boundary geometry must not be cached");
		helper.assertTrue(boundaryGeometry.boundary() == withNeighbour, "Geometry must reference the boundary it was built with");
		helper.assertTrue(withNeighbour.version(Direction.EAST) == neighbour.version(), "Boundary must track its neighbour's version");
		assertSameFaces(helper, "boundary geometry matches reference",
				referenceFaces(pristine, withNeighbour, OPAQUE_POLICY), expandAll(helper, boundaryGeometry));

		final ChiseledRenderSnapshot changedNeighbour = ChiseledRenderSnapshot.of(filled(8, info(Blocks.GLASS)));
		final NeighbourBoundary changed = NeighbourBoundary.NONE.withSnapshot(Direction.EAST, changedNeighbour);
		helper.assertTrue(changed.version(Direction.EAST) != withNeighbour.version(Direction.EAST), "Changed neighbour must change the boundary version");
		helper.assertTrue(changed.materialAt(Direction.EAST, 3, 4).equals(info(Blocks.GLASS)), "Changed boundary must expose the new neighbour material");
		assertSameFaces(helper, "changed boundary geometry matches reference",
				referenceFaces(pristine, changed, OPAQUE_POLICY), expandAll(helper, snapshot.greedyRectangles(OPAQUE_POLICY, changed)));
		helper.assertTrue(boundaryGeometry.boundary() == withNeighbour, "Older boundary geometry must keep its own boundary");

		final NeighbourBoundary uniform = NeighbourBoundary.NONE.withUniform(Direction.UP, info(Blocks.STONE));
		helper.assertTrue(uniform.materialAt(Direction.UP, 3, 4).equals(info(Blocks.STONE)), "Uniform boundary plane must hold the material");
		helper.assertTrue(uniform.materialAt(Direction.DOWN, 3, 4).isAir(), "Other planes must stay empty");
		final NeighbourBoundary extended = uniform.withUniform(Direction.DOWN, info(Blocks.GLASS));
		final NeighbourBoundary replaced = extended.withUniform(Direction.UP, info(Blocks.GOLD_BLOCK));
		helper.assertTrue(uniform.materialAt(Direction.DOWN, 3, 4).isAir()
						&& uniform.materialAt(Direction.UP, 3, 4).equals(info(Blocks.STONE)),
				"Extending or replacing a plane must not mutate an older boundary");
		helper.assertTrue(extended.materialAt(Direction.UP, 3, 4).equals(info(Blocks.STONE))
						&& extended.materialAt(Direction.DOWN, 3, 4).equals(info(Blocks.GLASS))
						&& replaced.materialAt(Direction.UP, 3, 4).equals(info(Blocks.GOLD_BLOCK))
						&& replaced.materialAt(Direction.DOWN, 3, 4).equals(info(Blocks.GLASS))
						&& extended.version(Direction.DOWN) == replaced.version(Direction.DOWN)
						&& extended.version(Direction.UP) != replaced.version(Direction.UP),
				"Replacing a plane must preserve the other plane and its version");

		helper.assertTrue(rejects(() -> ChiseledRenderSnapshot.of(new StateEntryStorage(3))), "Size 3 must be rejected");
		helper.assertTrue(rejects(() -> snapshot.materialAt(16, 0, 0)), "Out-of-range cell must be rejected");
		helper.assertTrue(rejects(() -> uniform.materialAt(Direction.UP, 0, 16)), "Out-of-range boundary coordinate must be rejected");

		helper.succeed();
	}

	private static void checkPackedStorage(final GameTestHelper helper) {
		final List<BlockInformation> materials = palette(WIDE_PALETTE_BLOCKS, true);
		for (final int size : SIZES) {
			for (final int paletteSize : List.of(1, 2, 3, 5, 9, 17)) {
				final StateEntryStorage storage = randomFill(size, new Random(size * 67L + paletteSize),
						materials.subList(0, paletteSize));
				final var packed = storage.copyPackedSnapshot();
				for (int x = 0; x < size; x++) {
					for (int y = 0; y < size; y++) {
						for (int z = 0; z < size; z++) {
							helper.assertTrue(storage.getBlockInformation(x, y, z).equals(packed.getBlockInformation(x, y, z)),
									"Packed snapshot differs from storage for size=" + size + ", palette=" + paletteSize
											+ " at " + x + "," + y + "," + z);
						}

					}
				}
				final BlockInformation before = packed.getBlockInformation(0, 0, 0);
				storage.setBlockInformation(0, 0, 0, info(Blocks.GOLD_BLOCK));
				helper.assertTrue(packed.getBlockInformation(0, 0, 0).equals(before),
						"Packed snapshots must not change after source mutation");
			}
		}
		final StateEntryStorage invalidIndex = storage(4);
		invalidIndex.setBlockInformation(0, 0, 0, info(Blocks.STONE));
		invalidIndex.setBlockInformation(0, 0, 1, info(Blocks.GLASS));
		invalidIndex.getData().set(0, 2);
		helper.assertTrue(invalidIndex.getBlockInformation(0, 0, 0)
						.equals(invalidIndex.copyPackedSnapshot().getBlockInformation(0, 0, 0)),
				"Packed snapshots must preserve invalid-palette-index behavior");
	}

	private static long readStorage(final StateEntryStorage storage) {
		long result = 0;
		for (int x = 0; x < storage.getSize(); x++) {
			for (int y = 0; y < storage.getSize(); y++) {
				for (int z = 0; z < storage.getSize(); z++) {
					result += storage.getBlockInformation(x, y, z).hashCode();
				}
			}
		}
		return result;
	}

	private static long readStorage(final StateEntryStorage.PackedSnapshot storage) {
		long result = 0;
		for (int x = 0; x < storage.getSize(); x++) {
			for (int y = 0; y < storage.getSize(); y++) {
				for (int z = 0; z < storage.getSize(); z++) {
					result += storage.getBlockInformation(x, y, z).hashCode();
				}
			}
		}
		return result;
	}

	private static ChiseledRenderSnapshot legacyAccessorSnapshot(final ISingleBlockAxisAlignedAreaAccessor accessor) {
		final int size = StateEntrySize.current().getBitsPerBlockSide();
		final var storage = new StateEntryStorage(size);
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				for (int z = 0; z < size; z++) {
					final Vec3 center = new Vec3((x + 0.5d) / size, (y + 0.5d) / size, (z + 0.5d) / size);
					storage.setBlockInformation(x, y, z, accessor.getInAreaTarget(center)
							.map(IStateEntryInfo::getBlockInformation).orElse(BlockInformation.AIR));
				}
			}
		}
		return ChiseledRenderSnapshot.of(storage);
	}

	/**
	 * A full stone block next to a neighbour with a single stone cell in the touching plane must lose exactly the face
	 * behind that cell. The expected key is computed here from the documented plane coordinates, independently of the
	 * boundary's own indexing.
	 */
	static void boundaryPlaneIndexing(final GameTestHelper helper) {
		final StateEntryStorage source = filled(GRID, info(Blocks.STONE));
		final ChiseledRenderSnapshot snapshot = ChiseledRenderSnapshot.of(source);
		final int[][] positions = {{0, 0}, {15, 15}, {0, 15}, {15, 0}, {3, 7}, {12, 1}};

		for (final Direction direction : Direction.values()) {
			final boolean positive = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE;

			for (final int[] position : positions) {
				final int neighbourLayer = positive ? 0 : GRID - 1;
				final int sourceLayer = positive ? GRID - 1 : 0;
				final int[] neighbourCell = planeCell(direction, neighbourLayer, position);
				final int[] sourceCell = planeCell(direction, sourceLayer, position);

				final StateEntryStorage neighbour = storage(GRID);
				neighbour.setBlockInformation(neighbourCell[0], neighbourCell[1], neighbourCell[2], info(Blocks.STONE));
				final NeighbourBoundary boundary = NeighbourBoundary.NONE.withSnapshot(direction, ChiseledRenderSnapshot.of(neighbour));
				final Map<Long, BlockInformation> actual = expandAll(helper, snapshot.greedyRectangles(FaceCullingPolicy.ANY_OCCUPIED, boundary));
				final String label = "plane " + direction + " " + Arrays.toString(position);

				helper.assertTrue(actual.size() == 6 * GRID * GRID - 1, label + ": expected exactly one hidden face, got " + actual.size());
				helper.assertTrue(!actual.containsKey(key(sourceCell[0], sourceCell[1], sourceCell[2], direction)), label + ": wrong face hidden");
				assertSameFaces(helper, label + " matches reference",
						referenceFaces(source, boundary, FaceCullingPolicy.ANY_OCCUPIED), actual);
			}
		}

		helper.succeed();
	}

	private static int[] planeCell(final Direction direction, final int layer, final int[] position) {
		return switch (direction.getAxis()) {
		case X -> new int[] {layer, position[0], position[1]};
		case Y -> new int[] {position[0], layer, position[1]};
		case Z -> new int[] {position[0], position[1], layer};
		};
	}

	static void mesherBenchmark(final GameTestHelper helper) {
		if (System.getenv(BENCHMARK_ENV) == null && !Files.exists(Path.of(BENCHMARK_MARKER))) {
			LOGGER.info("MESHER_BENCHMARK skipped, set {}=1 or create {} in the gametest server directory to run", BENCHMARK_ENV, BENCHMARK_MARKER);
			helper.succeed();
			return;
		}

		final int size = StateEntrySize.current().getBitsPerBlockSide();
		final List<BlockInformation> wide = palette(WIDE_PALETTE_BLOCKS, true);
		final Map<String, StateEntryStorage> workloads = new LinkedHashMap<>();
		workloads.put("solid", filled(size, info(Blocks.STONE)));
		workloads.put("checkerboard", checkerboard(size));
		final StateEntryStorage sparse = storage(size);
		sparse.setBlockInformation(size / 4, size / 2, size / 3, info(Blocks.STONE));
		sparse.setBlockInformation(size - 1, 0, 0, info(Blocks.GLASS));
		sparse.setBlockInformation(0, size - 1, size - 1, info(Blocks.OAK_PLANKS));
		workloads.put("sparse_3", sparse);
		workloads.put("multi_material_17", randomFill(size, new Random(4242L), wide));

		final int iterations = 400;
		LOGGER.info("MESHER_BENCHMARK config_size={} iterations={} warmup={} jvm={} comparison=ANY_OCCUPIED,all_air_boundary",
				size, iterations, iterations, System.getProperty("java.version"));
		final FaceCullingPolicy legacyEquivalent = FaceCullingPolicy.ANY_OCCUPIED;

		for (final Map.Entry<String, StateEntryStorage> workload : workloads.entrySet()) {
			final String name = workload.getKey();
			final StateEntryStorage storage = workload.getValue();
			final ChiseledRenderSnapshot primed = ChiseledRenderSnapshot.of(storage);
			final NeighbourBoundary allAir = allAirBoundary();

			final Map<Long, BlockInformation> legacyFaces = new HashMap<>();
			final GreedyMeshFace[] legacy = GreedyMeshBuilder.buildMesh((x, y, z) -> storage.getBlockInformation(x, y, z));

			for (final GreedyMeshFace face : legacy) {
				expandLegacy(helper, face, legacyFaces, "benchmark legacy " + name);
			}

			final GreedyRectangles mesher = primed.greedyRectangles(legacyEquivalent, allAir);
			assertSameFaces(helper, "benchmark equal output " + name, legacyFaces, expandAll(helper, mesher));
			LOGGER.info("MESHER_BENCHMARK workload={} equal_output=true legacy_faces={} rectangles={} unit_faces={} opaque_policy_rectangles={}",
					name, legacy.length, mesher.size(), mesher.unitFaceCount(), primed.greedyRectangles(OPAQUE_POLICY, allAir).size());

			report(name, "legacy_greedy_mesh_builder", iterations,
					() -> GreedyMeshBuilder.buildMesh((x, y, z) -> storage.getBlockInformation(x, y, z)).length);
			report(name, "snapshot_build", iterations, () -> ChiseledRenderSnapshot.of(storage).version());
			report(name, "full_snapshot_and_mesh", iterations,
					() -> ChiseledRenderSnapshot.of(storage).greedyRectangles(legacyEquivalent).size());
			report(name, "mesh_uncached_all_air_boundary", iterations,
					() -> primed.greedyRectangles(legacyEquivalent, allAir).size());
			primed.greedyRectangles(legacyEquivalent);
			report(name, "mesh_cached", iterations, () -> primed.greedyRectangles(legacyEquivalent).size());
			report(name, "mesh_uncached_opaque_policy_production", iterations,
					() -> primed.greedyRectangles(OPAQUE_POLICY, allAir).size());
		}

		helper.succeed();
	}

	private static NeighbourBoundary allAirBoundary() {
		NeighbourBoundary boundary = NeighbourBoundary.NONE;

		for (final Direction direction : Direction.values()) {
			boundary = boundary.withUniform(direction, BlockInformation.AIR);
		}

		return boundary;
	}

	private static void report(final String workload, final String operation, final int iterations, final LongSupplier operationBody) {
		final com.sun.management.ThreadMXBean threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
		long sink = 0;

		for (int i = 0; i < iterations; i++) {
			sink += operationBody.getAsLong();
		}

		final long allocatedBefore = threads.getCurrentThreadAllocatedBytes();
		final long start = System.nanoTime();

		for (int i = 0; i < iterations; i++) {
			sink += operationBody.getAsLong();
		}

		final long elapsed = System.nanoTime() - start;
		final long allocated = threads.getCurrentThreadAllocatedBytes() - allocatedBefore;
		LOGGER.info("MESHER_BENCHMARK workload={} op={} us_per_op={} alloc_bytes_per_op={} checksum={}",
				workload, operation, String.format("%.2f", elapsed / 1000.0 / iterations), allocated / iterations, sink);
	}

	private static boolean rejects(final Runnable action) {
		try {
			action.run();
			return false;
		} catch (final IllegalArgumentException expected) {
			return true;
		}
	}

	private static BlockInformation[] materialsOf(final ChiseledRenderSnapshot snapshot) {
		final BlockInformation[] result = new BlockInformation[GRID * GRID * GRID];

		for (int x = 0; x < GRID; x++) {
			for (int y = 0; y < GRID; y++) {
				for (int z = 0; z < GRID; z++) {
					result[(x * GRID + y) * GRID + z] = snapshot.materialAt(x, y, z);
				}
			}
		}

		return result;
	}

	private static BlockInformation info(final Block block) {
		return new BlockInformation(block.defaultBlockState(), Optional.empty());
	}

	private static List<BlockInformation> palette(final List<Block> blocks, final boolean includeAir) {
		final List<BlockInformation> result = new ArrayList<>();

		if (includeAir) {
			result.add(BlockInformation.AIR);
		}

		for (final Block block : blocks) {
			result.add(info(block));
		}

		return result;
	}

	private static StateEntryStorage storage(final int size) {
		return new StateEntryStorage(size);
	}

	private static StateEntryStorage filled(final int size, final BlockInformation information) {
		final StateEntryStorage storage = storage(size);

		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				for (int z = 0; z < size; z++) {
					storage.setBlockInformation(x, y, z, information);
				}
			}
		}

		return storage;
	}

	private static StateEntryStorage checkerboard(final int size) {
		final StateEntryStorage storage = storage(size);

		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				for (int z = 0; z < size; z++) {
					storage.setBlockInformation(x, y, z, (x + y + z) % 2 == 0 ? info(Blocks.STONE) : BlockInformation.AIR);
				}
			}
		}

		return storage;
	}

	private static StateEntryStorage randomFill(final int size, final Random random, final List<BlockInformation> palette) {
		final StateEntryStorage storage = storage(size);

		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				for (int z = 0; z < size; z++) {
					storage.setBlockInformation(x, y, z, palette.get(random.nextInt(palette.size())));
				}
			}
		}

		return storage;
	}

	private static NeighbourBoundary randomBoundary(final int size, final int seed) {
		final Random random = new Random(seed * 6121L + size);
		NeighbourBoundary boundary = NeighbourBoundary.NONE;

		for (final Direction direction : Direction.values()) {
			switch (random.nextInt(4)) {
			case 0 -> boundary = boundary.withUniform(direction, info(Blocks.STONE));
			case 1 -> boundary = boundary.withUniform(direction, info(Blocks.GLASS));
			case 2 -> {
				final int neighbourSize = SIZES[random.nextInt(SIZES.length)];
				boundary = boundary.withSnapshot(direction, ChiseledRenderSnapshot.of(randomFill(neighbourSize, random, palette(PALETTE_BLOCKS, true))));
			}
			default -> {
			}
			}
		}

		return boundary;
	}

	/**
	 * Naive reference: every non-air normalized cell emits a face in each direction whose outside neighbour is air or is
	 * not hiding it. Outside cells come from the boundary planes, indexed by the source cell's in-plane coordinates.
	 */
	private static Map<Long, BlockInformation> referenceFaces(final StateEntryStorage storage, final NeighbourBoundary boundary, final FaceCullingPolicy policy) {
		final int scale = GRID / storage.getSize();
		final BlockInformation[] grid = new BlockInformation[GRID * GRID * GRID];

		for (int x = 0; x < GRID; x++) {
			for (int y = 0; y < GRID; y++) {
				for (int z = 0; z < GRID; z++) {
					grid[(x * GRID + y) * GRID + z] = storage.getBlockInformation(x / scale, y / scale, z / scale);
				}
			}
		}

		final Map<Long, BlockInformation> faces = new HashMap<>();

		for (int x = 0; x < GRID; x++) {
			for (int y = 0; y < GRID; y++) {
				for (int z = 0; z < GRID; z++) {
					final BlockInformation source = grid[(x * GRID + y) * GRID + z];

					if (source.isAir()) {
						continue;
					}

					for (final Direction direction : Direction.values()) {
						final int nx = x + direction.getStepX();
						final int ny = y + direction.getStepY();
						final int nz = z + direction.getStepZ();
						final BlockInformation neighbour;

						if (nx >= 0 && nx < GRID && ny >= 0 && ny < GRID && nz >= 0 && nz < GRID) {
							neighbour = grid[(nx * GRID + ny) * GRID + nz];
						} else {
							neighbour = switch (direction.getAxis()) {
							case X -> boundary.materialAt(direction, y, z);
							case Y -> boundary.materialAt(direction, x, z);
							case Z -> boundary.materialAt(direction, x, y);
							};
						}

						if (!neighbour.isAir() && policy.hidesFace(source, neighbour, direction)) {
							continue;
						}

						faces.put(key(x, y, z, direction), source);
					}
				}
			}
		}

		return faces;
	}

	/**
	 * Expands packed rectangles back into unit faces using the convention documented on {@link GreedyRectangles}.
	 */
	private static void expand(final GameTestHelper helper, final GreedyRectangles geometry, final Map<Long, BlockInformation> faces, final String label) {
		for (int i = 0; i < geometry.size(); i++) {
			final long rectangle = geometry.rectangle(i);
			final Direction direction = GreedyRectangles.direction(rectangle);
			final int depth = GreedyRectangles.depth(rectangle);
			final int slice = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? depth - 1 : depth;
			final BlockInformation material = geometry.material(rectangle);

			for (int dRow = 0; dRow < GreedyRectangles.height(rectangle); dRow++) {
				for (int dColumn = 0; dColumn < GreedyRectangles.width(rectangle); dColumn++) {
					final int row = GreedyRectangles.row(rectangle) + dRow;
					final int column = GreedyRectangles.column(rectangle) + dColumn;
					final int x;
					final int y;
					final int z;
					switch (direction.getAxis()) {
					case X -> {
						x = slice;
						y = row;
						z = column;
					}
					case Y -> {
						x = row;
						y = slice;
						z = column;
					}
					case Z -> {
						x = column;
						y = row;
						z = slice;
					}
					default -> throw new IllegalStateException();
					}

					helper.assertTrue(faces.put(key(x, y, z, direction), material) == null, label + ": duplicate face at " + x + "," + y + "," + z + " " + direction);
				}
			}
		}
	}

	private static Map<Long, BlockInformation> expandAll(final GameTestHelper helper, final GreedyRectangles geometry) {
		final Map<Long, BlockInformation> faces = new HashMap<>();
		expand(helper, geometry, faces, "expand");
		return faces;
	}

	private static Map<Long, BlockInformation> expandBounds(final GameTestHelper helper, final GreedyRectangles geometry) {
		final Map<Long, BlockInformation> faces = new HashMap<>();

		for (int index = 0; index < geometry.size(); index++) {
			final long rectangle = geometry.rectangle(index);
			final Direction direction = GreedyRectangles.direction(rectangle);
			final Vector3f from = GreedyRectangles.lowerCorner(rectangle).mul(GRID);
			final Vector3f to = GreedyRectangles.upperCorner(rectangle).mul(GRID);
			final int[] start = {Math.round(from.x), Math.round(from.y), Math.round(from.z)};
			final int[] end = {Math.round(to.x), Math.round(to.y), Math.round(to.z)};
			final int axis = direction.getAxis().ordinal();
			final int plane = start[axis];
			final int slice = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? plane - 1 : plane;
			start[axis] = slice;
			end[axis] = slice + 1;

			for (int x = start[0]; x < end[0]; x++) {
				for (int y = start[1]; y < end[1]; y++) {
					for (int z = start[2]; z < end[2]; z++) {
						helper.assertTrue(
								faces.put(key(x, y, z, direction), geometry.material(rectangle)) == null,
								"rectangle bounds produced duplicate face"
						);
					}
				}
			}
		}

		return faces;
	}

	private static void expandLegacy(final GameTestHelper helper, final GreedyMeshFace face, final Map<Long, BlockInformation> faces, final String label) {
		final Direction direction = face.normalDirection();
		final int axis = direction.getAxis().ordinal();
		final Vector3f min = face.minVector();
		final Vector3f max = face.maxVector();
		final int[] start = {Math.round(min.x * GRID), Math.round(min.y * GRID), Math.round(min.z * GRID)};
		final int[] end = {Math.round(max.x * GRID), Math.round(max.y * GRID), Math.round(max.z * GRID)};
		final int plane = start[axis];
		final int slice = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? plane - 1 : plane;
		start[axis] = slice;
		end[axis] = slice + 1;

		for (int x = start[0]; x < end[0]; x++) {
			for (int y = start[1]; y < end[1]; y++) {
				for (int z = start[2]; z < end[2]; z++) {
					helper.assertTrue(faces.put(key(x, y, z, direction), face.faceValue()) == null, label + ": legacy duplicate face at " + x + "," + y + "," + z);
				}
			}
		}
	}

	private static void assertSameFaces(final GameTestHelper helper, final String label, final Map<Long, BlockInformation> expected, final Map<Long, BlockInformation> actual) {
		int missing = 0;
		int extra = 0;
		int mismatched = 0;

		for (final Map.Entry<Long, BlockInformation> entry : expected.entrySet()) {
			final BlockInformation found = actual.get(entry.getKey());

			if (found == null) {
				missing++;
			} else if (!found.equals(entry.getValue())) {
				mismatched++;
			}
		}

		for (final Long key : actual.keySet()) {
			if (!expected.containsKey(key)) {
				extra++;
			}
		}

		helper.assertTrue(missing == 0 && extra == 0 && mismatched == 0,
				label + ": expected " + expected.size() + " faces, got " + actual.size() + " (missing=" + missing + ", extra=" + extra + ", material mismatches=" + mismatched + ")");
	}

	private static long key(final int x, final int y, final int z, final Direction direction) {
		return ((long) (x * GRID + y) * GRID + z) * DIRECTIONS + direction.ordinal();
	}

	private static ChiseledBlockModelCacheKey sourceKey(final List<BlockInformation> states, final List<Object> modelKeys) {
		return new ChiseledBlockModelCacheKey(IAreaShapeIdentifier.DUMMY, BlockInformation.AIR, IBlockNeighborhood.EMPTY,
				modelKeys, states);
	}

}

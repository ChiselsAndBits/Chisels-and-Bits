package mod.chiselsandbits.forge.gametest;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import com.mojang.serialization.JsonOps;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.block.entities.ChiseledBlockEntity;
import mod.chiselsandbits.debug.scenes.ChurnScene;
import mod.chiselsandbits.debug.scenes.SceneGenerators;
import mod.chiselsandbits.debug.scenes.SceneType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class ChiselsAndBitsGameTests {
	private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, Constants.MOD_ID);

	static {
		FUNCTIONS.register("worst_case", () -> ChiselsAndBitsGameTests::worstCase);
		FUNCTIONS.register("sharding", () -> ChiselsAndBitsGameTests::sharding);
		FUNCTIONS.register("terrain_subset", () -> ChiselsAndBitsGameTests::terrainSubset);
		FUNCTIONS.register("builder_detail", () -> ChiselsAndBitsGameTests::builderDetail);
		FUNCTIONS.register("translucency", () -> ChiselsAndBitsGameTests::translucency);
		FUNCTIONS.register("lighting", () -> ChiselsAndBitsGameTests::lighting);
		FUNCTIONS.register("churn", () -> ChiselsAndBitsGameTests::churn);
		FUNCTIONS.register("registry_serialization", () -> ChiselsAndBitsGameTests::registrySerialization);
		FUNCTIONS.register("mesher_reference_expansion", () -> MesherGameTests::referenceExpansion);
		FUNCTIONS.register("mesher_legacy_coverage", () -> MesherGameTests::legacyCoverage);
		FUNCTIONS.register("mesher_cavity_faces", () -> MesherGameTests::cavityFaces);
		FUNCTIONS.register("mesher_interior_rectangle_subset", () -> MesherGameTests::interiorRectangleSubset);
		FUNCTIONS.register("mesher_rectangle_subset_preflight", () -> MesherGameTests::rectangleSubsetPreflight);
		FUNCTIONS.register("experimental_model_data_generation", () -> MesherGameTests::experimentalModelDataGeneration);
		FUNCTIONS.register("mesher_snapshot_invariants", () -> MesherGameTests::snapshotInvariants);
		FUNCTIONS.register("mesher_boundary_plane_indexing", () -> MesherGameTests::boundaryPlaneIndexing);
		FUNCTIONS.register("mesher_benchmark", () -> MesherGameTests::mesherBenchmark);
	}

	private static final String STRUCTURE = "empty_32x32x32";
	private static final String ENVIRONMENT = "default";
	private static final int MAX_TICKS = 400;
	private static final BlockPos ORIGIN = new BlockPos(2, 2, 2);
	private static final int CHURN_TICKS = 40;
	private static final Vec3 CHURN_SAMPLE_POINT = new Vec3(0, 0.5, 0);

	private ChiselsAndBitsGameTests() {
		throw new IllegalStateException("Can not instantiate an instance of: ChiselsAndBitsGameTests. This is a utility class");
	}

	public static void initialize(final IEventBus modBus) {
		FUNCTIONS.register(modBus);
		modBus.addListener((Consumer<RegisterGameTestsEvent>) ChiselsAndBitsGameTests::register);
		NeoForge.EVENT_BUS.addListener((Consumer<ServerStartedEvent>) ChiselsAndBitsGameTests::onServerStarted);
	}

	private static void register(final RegisterGameTestsEvent event) {
		final Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
				id(ENVIRONMENT),
				new TestEnvironmentDefinition.AllOf()
		);

		registerScene(event, environment, "worst_case");
		registerScene(event, environment, "sharding");
		registerScene(event, environment, "terrain_subset");
		registerScene(event, environment, "builder_detail");
		registerScene(event, environment, "translucency");
		registerScene(event, environment, "lighting");
		registerScene(event, environment, "churn");
		registerScene(event, environment, "registry_serialization");
		registerScene(event, environment, "mesher_reference_expansion");
		registerScene(event, environment, "mesher_legacy_coverage");
		registerScene(event, environment, "mesher_cavity_faces");
		registerScene(event, environment, "mesher_interior_rectangle_subset");
		registerScene(event, environment, "mesher_rectangle_subset_preflight");
		registerScene(event, environment, "experimental_model_data_generation");
		registerScene(event, environment, "mesher_snapshot_invariants");
		registerScene(event, environment, "mesher_boundary_plane_indexing");
		registerScene(event, environment, "mesher_benchmark");
	}

	private static void onServerStarted(final ServerStartedEvent event) {
		EmptyStructureTemplate.register(event.getServer(), id(STRUCTURE));
	}

	private static void registerScene(
			final RegisterGameTestsEvent event,
			final Holder<TestEnvironmentDefinition<?>> environment,
			final String name
	) {
		final GameTestInstance instance = new FunctionGameTestInstance(
				ResourceKey.create(Registries.TEST_FUNCTION, id(name)),
				new TestData<>(environment, id(STRUCTURE), MAX_TICKS, 0, true)
		);
		event.registerTest(id(name), instance);
	}

	private static void registrySerialization(final GameTestHelper helper) {
		final var registries = helper.getLevel().registryAccess();
		final var ops = registries.createSerializationContext(JsonOps.INSTANCE);

		for (final var entry : registries.lookupOrThrow(Registries.TEST_INSTANCE).entrySet()) {
			if (!entry.getKey().identifier().getNamespace().equals(Constants.MOD_ID)) {
				continue;
			}

			final var encoded = GameTestInstance.DIRECT_CODEC.encodeStart(ops, entry.getValue()).getOrThrow();
			final GameTestInstance decoded = GameTestInstance.DIRECT_CODEC.parse(ops, encoded).getOrThrow();
			helper.assertTrue(
					GameTestInstance.DIRECT_CODEC.encodeStart(ops, decoded).getOrThrow().equals(encoded),
					"Test instance failed registry serialization round trip: " + entry.getKey()
			);
		}

		helper.succeed();
	}

	private static void worstCase(final GameTestHelper helper) {
		final ServerLevel level = helper.getLevel();
		final BlockPos origin = helper.absolutePos(ORIGIN);

		final int placed = SceneGenerators.generate(SceneType.WORST_CASE, level, origin, 0);
		helper.assertTrue(placed == 4096, "Expected 4096 placed blocks, got " + placed);

		helper.succeedWhen(() -> {
			helper.assertTrue(countChiseled(level, origin, 16, 16, 16) == 4096, "Expected 4096 chiseled blocks in the 16x16x16 area");
			final ChiseledBlockEntity sample = chiseledAt(level, origin.offset(7, 7, 7));
			helper.assertTrue(sample != null, "Missing sampled chiseled block");
			helper.assertTrue(nonAirBits(sample) == 2048, "Expected 2048 non-air bits in the sampled block, got " + nonAirBits(sample));
		});
	}

	private static void builderDetail(final GameTestHelper helper) {
		final ServerLevel level = helper.getLevel();
		final BlockPos origin = helper.absolutePos(ORIGIN);

		final int placed = SceneGenerators.generate(SceneType.BUILDER_DETAIL, level, origin, 0);
		helper.assertTrue(placed == 1000, "Expected 1000 placed blocks, got " + placed);

		helper.succeedWhen(() -> {
			helper.assertTrue(countChiseled(level, origin, 10, 10, 10) == 1000, "Expected 1000 chiseled blocks in the 10x10x10 area");

			final Set<Block> materials = new HashSet<>();

			for (int x = 0; x < 10; x++) {
				for (int y = 0; y < 10; y++) {
					for (int z = 0; z < 10; z++) {
						final ChiseledBlockEntity blockEntity = chiseledAt(level, origin.offset(x, y, z));
						helper.assertTrue(blockEntity != null, "Missing chiseled block at " + x + "," + y + "," + z);
						materials.addAll(materialsOf(blockEntity));
					}

				}
			}

			helper.assertTrue(materials.contains(Blocks.STONE), "Expected stone bits in builder_detail");
			helper.assertTrue(materials.contains(Blocks.OAK_PLANKS), "Expected oak planks bits in builder_detail");
			helper.assertTrue(materials.contains(Blocks.QUARTZ_BLOCK), "Expected quartz bits in builder_detail");
		});
	}

	private static void sharding(final GameTestHelper helper) {
		final ServerLevel level = helper.getLevel();
		final BlockPos origin = helper.absolutePos(ORIGIN);
		final int placed = SceneGenerators.generate(SceneType.SHARDING, level, origin, 0);
		helper.assertTrue(placed == 125, "Expected 125 placed blocks, got " + placed);
		helper.succeedWhen(() -> {
			helper.assertTrue(countChiseled(level, origin, 5, 5, 5) == 125, "Expected 125 chiseled blocks");
			final ChiseledBlockEntity sample = chiseledAt(level, origin.offset(2, 2, 2));
			helper.assertTrue(sample != null, "Missing sampled chiseled block");
			helper.assertTrue(nonAirBits(sample) == 2048, "Expected a stone/air checkerboard");
		});
	}

	private static void terrainSubset(final GameTestHelper helper) {
		final ServerLevel level = helper.getLevel();
		final BlockPos origin = helper.absolutePos(ORIGIN);
		final int placed = SceneGenerators.generate(SceneType.TERRAIN_SUBSET, level, origin, 0);
		helper.assertTrue(placed == 2, "Expected two terrain subset blocks, got " + placed);
		helper.succeedWhen(() -> {
			final ChiseledBlockEntity interior = chiseledAt(level, origin);
			final ChiseledBlockEntity boundary = chiseledAt(level, origin.offset(2, 0, 0));
			helper.assertTrue(interior != null && boundary != null, "Missing terrain subset blocks");
			helper.assertTrue(nonAirBits(interior) == 512, "Expected 512 interior stone bits");
			helper.assertTrue(nonAirBits(boundary) == 2048, "Expected 2048 boundary stone bits");
			helper.assertTrue(isSolid(interior, new Vec3(0.25, 0.25, 0.25)), "Missing interior cube corner");
			helper.assertTrue(!isSolid(interior, Vec3.ZERO), "Interior cube must not touch the boundary");
			helper.assertTrue(isSolid(boundary, Vec3.ZERO), "Fallback shape must touch the boundary");
		});
	}

	private static void translucency(final GameTestHelper helper) {
		final ServerLevel level = helper.getLevel();
		final BlockPos origin = helper.absolutePos(ORIGIN);

		final int placed = SceneGenerators.generate(SceneType.TRANSLUCENCY, level, origin, 0);
		helper.assertTrue(placed == 8, "Expected 8 placed blocks, got " + placed);

		helper.succeedWhen(() -> {
			final Set<Block> materials = new HashSet<>();

			for (int x = 0; x < 8; x++) {
				final ChiseledBlockEntity blockEntity = chiseledAt(level, origin.offset(x, 0, 0));
				helper.assertTrue(blockEntity != null, "Missing chiseled block at x=" + x);
				materials.addAll(materialsOf(blockEntity));
			}

			helper.assertTrue(materials.contains(Blocks.GLASS), "Expected glass bits in translucency");
			helper.assertTrue(materials.contains(Blocks.STAINED_GLASS.cyan()), "Expected stained glass bits in translucency");
			helper.assertTrue(materials.contains(Blocks.WATER), "Expected water bits in translucency");
			helper.assertTrue(level.getBlockState(origin.offset(0, 0, 1)).is(Blocks.WATER), "Expected vanilla water next to the translucent blocks");
		});
	}

	private static void lighting(final GameTestHelper helper) {
		final ServerLevel level = helper.getLevel();
		final BlockPos origin = helper.absolutePos(ORIGIN);

		final int placed = SceneGenerators.generate(SceneType.LIGHTING, level, origin, 0);
		helper.assertTrue(placed == 12, "Expected 12 placed blocks, got " + placed);

		helper.succeedWhen(() -> {
			for (int x = 0; x < 8; x += 2) {
				helper.assertTrue(level.getBlockState(origin.offset(x, 0, 1)).is(Blocks.TORCH), "Expected a torch at x=" + x);
			}

			final ChiseledBlockEntity recess = chiseledAt(level, origin);
			helper.assertTrue(recess != null, "Missing recess chiseled block");
			helper.assertTrue(materialsOf(recess).contains(Blocks.GLOWSTONE), "Expected glowstone bits in the recess block");

			for (int x = 0; x < 8; x++) {
				helper.assertTrue(chiseledAt(level, origin.offset(x, 2, 1)) != null, "Missing overhang block at x=" + x);
			}
		});
	}

	private static void churn(final GameTestHelper helper) {
		final ServerLevel level = helper.getLevel();
		final BlockPos origin = helper.absolutePos(ORIGIN);

		final int placed = SceneGenerators.generate(SceneType.CHURN, level, origin, CHURN_TICKS);
		helper.assertTrue(placed == 16, "Expected 16 placed blocks, got " + placed);

		final Set<Boolean> seen = new HashSet<>();
		helper.succeedWhen(() -> {
			final ChiseledBlockEntity sample = chiseledAt(level, origin);
			helper.assertTrue(sample != null, "Missing churn block");
			seen.add(isSolid(sample, CHURN_SAMPLE_POINT));

			helper.assertTrue(seen.size() == 2, "Churn did not alternate the scene, saw " + seen);
			helper.assertTrue(!ChurnScene.getInstance().isRunning(), "Churn is still running after " + CHURN_TICKS + " ticks");
			helper.assertTrue(chiseledAt(level, origin.offset(3, 0, 3)) != null, "Churn block missing after the run");
		});
	}

	private static Identifier id(final String path) {
		return Identifier.fromNamespaceAndPath(Constants.MOD_ID, path);
	}

	private static ChiseledBlockEntity chiseledAt(final ServerLevel level, final BlockPos pos) {
		return level.getBlockEntity(pos) instanceof ChiseledBlockEntity blockEntity ? blockEntity : null;
	}

	private static int countChiseled(final ServerLevel level, final BlockPos origin, final int sizeX, final int sizeY, final int sizeZ) {
		int count = 0;

		for (int x = 0; x < sizeX; x++) {
			for (int y = 0; y < sizeY; y++) {
				for (int z = 0; z < sizeZ; z++) {
					if (chiseledAt(level, origin.offset(x, y, z)) != null) {
						count++;
					}
				}
			}
		}

		return count;
	}

	private static int nonAirBits(final ChiseledBlockEntity blockEntity) {
		return (int) blockEntity.stream().filter(entry -> !entry.getBlockInformation().isAir()).count();
	}

	private static boolean isSolid(final ChiseledBlockEntity blockEntity, final Vec3 point) {
		return blockEntity.stream()
				.filter(entry -> entry.getStartPoint().equals(point))
				.findFirst()
				.map(entry -> !entry.getBlockInformation().isAir())
				.orElse(false);
	}

	private static Set<Block> materialsOf(final ChiseledBlockEntity blockEntity) {
		final Set<Block> materials = new HashSet<>();
		blockEntity.getStatistics().getStateCounts().keySet().forEach(information -> materials.add(information.blockState().getBlock()));
		return materials;
	}
}

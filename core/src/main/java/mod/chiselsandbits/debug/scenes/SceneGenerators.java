package mod.chiselsandbits.debug.scenes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import mod.chiselsandbits.api.block.storage.StateEntryStorage;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.debug.scenes.SceneBlocks.BitFunction;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Generates the deterministic benchmark scenes used for rendering baselines.
 * See docs/rendering/benchmarks.md for what each scene is meant to stress.
 */
public final class SceneGenerators {
	private static final int SECTION_SIZE = 16;
	private static final int BUILDER_DETAIL_SIDE = 10;
	private static final int BUILDER_DETAIL_PATTERNS = 8;
	private static final long BUILDER_DETAIL_SEED = 1337L;

	private SceneGenerators() {
		throw new IllegalStateException("Can not instantiate an instance of: SceneGenerators. This is a utility class");
	}

	/**
	 * Generates the requested scene at the origin.
	 *
	 * @param type The scene to generate.
	 * @param level The level to generate the scene in.
	 * @param origin The minimum corner of the scene.
	 * @param churnTicks The number of ticks to run the churn scene for, ignored by other scenes.
	 * @return The number of chiseled blocks in the scene.
	 */
	public static int generate(final SceneType type, final ServerLevel level, final BlockPos origin, final int churnTicks) {
		return switch (type) {
		case WORST_CASE -> checkerboardScene(level, origin, SECTION_SIZE);
		case SHARDING -> checkerboardScene(level, origin, 5);
		case TERRAIN_SUBSET -> terrainSubset(level, origin);
		case BUILDER_DETAIL -> builderDetail(level, origin);
		case TRANSLUCENCY -> translucency(level, origin);
		case LIGHTING -> lighting(level, origin);
		case CHURN -> churn(level, origin, churnTicks);
		};
	}

	private static int terrainSubset(final ServerLevel level, final BlockPos origin) {
		final BlockInformation stone = info(Blocks.STONE.defaultBlockState());
		final StateEntryStorage interior = SceneBlocks.storage((x, y, z) ->
				x >= 4 && x <= 11 && y >= 4 && y <= 11 && z >= 4 && z <= 11 ? stone : null);
		final StateEntryStorage boundary = SceneBlocks.storage((x, y, z) -> y < 8 ? stone : null);

		SceneBlocks.clear(level, origin, 3, 1, 1);
		SceneBlocks.floor(level, origin, 3, 1);
		int placed = 0;
		if (SceneBlocks.placeChiseled(level, origin, interior)) {
			placed++;
		}
		if (SceneBlocks.placeChiseled(level, origin.offset(2, 0, 0), boundary)) {
			placed++;
		}
		return placed;
	}

	/**
	 * A cube of chiseled blocks, each with a 3D checkerboard of stone and air.
	 * Every block has exactly 2048 stone bits.
	 */
	private static int checkerboardScene(final ServerLevel level, final BlockPos origin, final int side) {
		final StateEntryStorage template = SceneBlocks.storage(checkerboard(info(Blocks.STONE.defaultBlockState())));

		SceneBlocks.clear(level, origin, side, side, side);
		SceneBlocks.floor(level, origin, side, side);

		int placed = 0;

		for (int x = 0; x < side; x++) {
			for (int y = 0; y < side; y++) {
				for (int z = 0; z < side; z++) {
					if (SceneBlocks.placeChiseled(level, origin.offset(x, y, z), template.createSnapshot())) {
						placed++;
					}
				}
			}
		}

		return placed;
	}

	/**
	 * Roughly a thousand chiseled blocks in a 10x10x10 cube, mixing shapes and materials.
	 * Layout and material choices are seeded, so every run produces the same scene.
	 */
	private static int builderDetail(final ServerLevel level, final BlockPos origin) {
		final List<BlockInformation> materials = new ArrayList<>();
		for (final BlockState state : List.of(
				Blocks.STONE.defaultBlockState(),
				Blocks.COBBLESTONE.defaultBlockState(),
				Blocks.STONE_BRICKS.defaultBlockState(),
				Blocks.MOSSY_STONE_BRICKS.defaultBlockState(),
				Blocks.DEEPSLATE.defaultBlockState(),
				Blocks.POLISHED_ANDESITE.defaultBlockState(),
				Blocks.OAK_PLANKS.defaultBlockState(),
				Blocks.SPRUCE_PLANKS.defaultBlockState(),
				Blocks.DARK_OAK_PLANKS.defaultBlockState(),
				Blocks.QUARTZ_BLOCK.defaultBlockState(),
				Blocks.SANDSTONE.defaultBlockState(),
				Blocks.BRICKS.defaultBlockState())) {
			materials.add(info(state));
		}

		final Random random = new Random(BUILDER_DETAIL_SEED);
		final boolean[] noise = new boolean[SECTION_SIZE * SECTION_SIZE * SECTION_SIZE];

		for (int index = 0; index < noise.length; index++) {
			noise[index] = random.nextInt(100) < 35;
		}

		final Map<Integer, StateEntryStorage> templates = new HashMap<>();
		final int blockCount = BUILDER_DETAIL_SIDE * BUILDER_DETAIL_SIDE * BUILDER_DETAIL_SIDE;

		SceneBlocks.clear(level, origin, BUILDER_DETAIL_SIDE, BUILDER_DETAIL_SIDE, BUILDER_DETAIL_SIDE);
		SceneBlocks.floor(level, origin, BUILDER_DETAIL_SIDE, BUILDER_DETAIL_SIDE);

		int placed = 0;

		for (int index = 0; index < blockCount; index++) {
			final int pattern = random.nextInt(BUILDER_DETAIL_PATTERNS);
			final int primary = random.nextInt(materials.size());
			// A block with a single material is converted to a vanilla block, so the secondary material must differ from the primary.
			final int secondaryRoll = random.nextInt(materials.size() + 1);
			final int secondary = secondaryRoll == primary ? materials.size() : secondaryRoll;
			final int key = (pattern * materials.size() + primary) * (materials.size() + 1) + secondary;

			final StateEntryStorage template = templates.computeIfAbsent(key, ignored -> SceneBlocks.storage(
					(x, y, z) -> isPrimaryPattern(pattern, x, y, z, noise)
					? materials.get(primary)
					: secondary == materials.size() ? null : materials.get(secondary)
			));

			final BlockPos pos = origin.offset(index % BUILDER_DETAIL_SIDE, index / (BUILDER_DETAIL_SIDE * BUILDER_DETAIL_SIDE), (index / BUILDER_DETAIL_SIDE) % BUILDER_DETAIL_SIDE);

			if (SceneBlocks.placeChiseled(level, pos, template.createSnapshot())) {
				placed++;
			}
		}

		return placed;
	}

	private static boolean isPrimaryPattern(final int pattern, final int x, final int y, final int z, final boolean[] noise) {
		return switch (pattern) {
		case 0 -> y < 8;
		case 1 -> y <= x;
		case 2 -> x < 2 || x > 13 || z < 2 || z > 13;
		case 3 -> {
			final int dx = 2 * x - 15;
			final int dy = 2 * y - 15;
			final int dz = 2 * z - 15;
			final int distance = dx * dx + dy * dy + dz * dz;
			yield distance >= 144 && distance <= 256;
		}
		case 4 -> x >= 5 && x <= 10 && z >= 5 && z <= 10;
		case 5 -> x % 4 == 0 || z % 4 == 0;
		case 6 -> noise[x * SECTION_SIZE * SECTION_SIZE + y * SECTION_SIZE + z];
		default -> y < 8 && z < 8;
		};
	}

	/**
	 * Glass, stained glass and water bits, in a row, next to a vanilla water pool.
	 */
	private static int translucency(final ServerLevel level, final BlockPos origin) {
		final int width = 8;
		final List<StateEntryStorage> templates = List.of(
				SceneBlocks.storage((x, y, z) -> y < 8 ? info(Blocks.GLASS.defaultBlockState()) : null),
				SceneBlocks.storage(checkerboard(info(Blocks.STAINED_GLASS.cyan().defaultBlockState()))),
				SceneBlocks.storage((x, y, z) -> y < 8 ? info(Blocks.WATER.defaultBlockState()) : null),
				SceneBlocks.storage((x, y, z) -> isShell(x, y, z)
						? info(Blocks.GLASS.defaultBlockState())
						: y < 8 ? info(Blocks.WATER.defaultBlockState()) : null)
		);

		SceneBlocks.clear(level, origin, width, SECTION_SIZE, 4);
		SceneBlocks.floor(level, origin, width, 4);
		SceneBlocks.fill(level, origin.offset(0, 0, 1), origin.offset(width - 1, 1, 3), Blocks.WATER.defaultBlockState());

		int placed = 0;

		for (int index = 0; index < width; index++) {
			if (SceneBlocks.placeChiseled(level, origin.offset(index, 0, 0), templates.get(index % templates.size()).createSnapshot())) {
				placed++;
			}
		}

		return placed;
	}

	/**
	 * Inset faces next to torches and glowstone bits, with chiseled overhangs above the torches.
	 */
	private static int lighting(final ServerLevel level, final BlockPos origin) {
		final BlockState stone = Blocks.STONE.defaultBlockState();
		final BlockInformation stoneInfo = info(stone);
		final BlockInformation glowstoneInfo = info(Blocks.GLOWSTONE.defaultBlockState());

		final StateEntryStorage recess = SceneBlocks.storage((x, y, z) -> {
			if (x >= 4 && x <= 11 && y >= 4 && y <= 11 && z >= 8) {
				return z == 8 && x >= 5 && x <= 10 && y >= 5 && y <= 10 ? glowstoneInfo : null;
			}

			return stoneInfo;
		});

		final StateEntryStorage overhang = SceneBlocks.storage((x, y, z) ->
				x >= 4 && x <= 11 && y <= 1 && z >= 4 && z <= 11 ? null : stoneInfo);

		SceneBlocks.clear(level, origin, 8, 6, 4);
		SceneBlocks.floor(level, origin, 8, 4);

		int placed = 0;

		for (int x = 0; x < 8; x += 2) {
			SceneBlocks.fill(level, origin.offset(x, 0, 1), origin.offset(x, 0, 1), Blocks.TORCH.defaultBlockState());

			if (SceneBlocks.placeChiseled(level, origin.offset(x, 0, 0), recess.createSnapshot())) {
				placed++;
			}
		}

		for (int x = 0; x < 8; x++) {
			if (SceneBlocks.placeChiseled(level, origin.offset(x, 2, 1), overhang.createSnapshot())) {
				placed++;
			}
		}

		return placed;
	}

	/**
	 * A 4x4 set of chiseled blocks that are re-chiseled every tick until the churn is stopped or the ticks run out.
	 */
	private static int churn(final ServerLevel level, final BlockPos origin, final int ticks) {
		final BlockInformation stoneInfo = info(Blocks.STONE.defaultBlockState());
		final List<StateEntryStorage> templates = List.of(
				SceneBlocks.storage((x, y, z) -> y < 8 ? stoneInfo : null),
				SceneBlocks.storage(checkerboard(stoneInfo))
		);

		SceneBlocks.clear(level, origin, 4, SECTION_SIZE, 4);
		SceneBlocks.floor(level, origin, 4, 4);

		final List<BlockPos> positions = new ArrayList<>();

		for (int x = 0; x < 4; x++) {
			for (int z = 0; z < 4; z++) {
				positions.add(origin.offset(x, 0, z));
			}
		}

		for (int index = 0; index < positions.size(); index++) {
			SceneBlocks.placeChiseled(level, positions.get(index), templates.get(index % templates.size()).createSnapshot());
		}

		ChurnScene.getInstance().start(level, positions, templates, ticks);
		return positions.size();
	}

	private static BitFunction checkerboard(final BlockInformation information) {
		return (x, y, z) -> (x + y + z) % 2 == 0 ? information : null;
	}

	private static boolean isShell(final int x, final int y, final int z) {
		return x == 0 || x == SECTION_SIZE - 1 || y == 0 || y == SECTION_SIZE - 1 || z == 0 || z == SECTION_SIZE - 1;
	}

	private static BlockInformation info(final BlockState state) {
		return SceneBlocks.info(state);
	}
}

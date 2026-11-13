package mod.chiselsandbits.client.model.meshing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mod.chiselsandbits.api.blockinformation.BlockInformation;

import net.minecraft.core.Direction;

/**
 * Bitwise greedy mesher over a {@link ChiseledRenderSnapshot}.
 *
 * <p>For every direction and every non-air material, the visible faces are computed as
 * {@code occupancy & ~(hidden by neighbour materials)} on 16x16 column masks, where each mask holds 16 bits along the
 * third axis. Each visible slice is then covered greedily: a maximal run along the column axis is extended along the
 * row axis while the same run is visible. All scratch state is local to one call.
 */
final class RectangleMesher {
	private static final int GRID = ChiseledRenderSnapshot.GRID;
	private static final int LAYER = GRID * GRID;
	private static final int DIRECTIONS = Direction.values().length;

	private RectangleMesher() {
		throw new IllegalStateException("Cannot instantiate utility class");
	}

	static GreedyRectangles mesh(final ChiseledRenderSnapshot snapshot, final FaceCullingPolicy policy, final NeighbourBoundary boundary) {
		final int sourceMaterialCount = snapshot.materialCount();
		final List<BlockInformation> materials = new ArrayList<>(snapshot.materials());
		final Map<BlockInformation, Integer> indexByMaterial = new HashMap<>();

		for (int i = 0; i < materials.size(); i++) {
			indexByMaterial.put(materials.get(i), i);
		}

		final int[][] outsideIndex = new int[DIRECTIONS][LAYER];

		for (int d = 0; d < DIRECTIONS; d++) {
			for (int i = 0; i < LAYER; i++) {
				final BlockInformation outside = boundary.planeMaterial(d, i);
				outsideIndex[d][i] = indexByMaterial.computeIfAbsent(outside, key -> {
					materials.add(key);
					return materials.size() - 1;
				});
			}
		}

		final int[][][] neighbourMasks = buildNeighbourMasks(snapshot, outsideIndex, materials.size());
		final RectangleBuffer rectangles = new RectangleBuffer();
		final int[] visible = new int[LAYER];
		final int[] rows = new int[GRID];

		for (int d = 0; d < DIRECTIONS; d++) {
			final Direction direction = Direction.values()[d];

			for (int material = 1; material < sourceMaterialCount; material++) {
				final BlockInformation source = materials.get(material);

				for (int column = 0; column < LAYER; column++) {
					visible[column] = snapshot.occupancy(material, column);
				}

				for (int neighbour = 1; neighbour < materials.size(); neighbour++) {
					final int[] hidden = neighbourMasks[d][neighbour];

					if (hidden == null || !policy.hidesFace(source, materials.get(neighbour), direction)) {
						continue;
					}

					for (int column = 0; column < LAYER; column++) {
						visible[column] &= ~hidden[column];
					}
				}

				for (int slice = 0; slice < GRID; slice++) {
					extractRows(visible, direction.getAxis(), slice, rows);
					addGreedyRectangles(rows, material, d, direction, slice, rectangles);
				}
			}
		}

		return new GreedyRectangles(snapshot.version(), boundary, List.copyOf(materials), rectangles.toArray());
	}

	/**
	 * For each direction and neighbour material, builds the 16x16 column masks of cells whose neighbour in that
	 * direction is that material. Columns are indexed {@code x * 16 + y} and bits by {@code z}. In-block neighbours are
	 * taken from the occupancy columns; outer neighbours are then set from the boundary planes.
	 */
	private static int[][][] buildNeighbourMasks(final ChiseledRenderSnapshot snapshot, final int[][] outsideIndex, final int materialCount) {
		final int sourceMaterialCount = snapshot.materialCount();
		final int[][][] masks = new int[DIRECTIONS][materialCount][];

		for (int d = 0; d < DIRECTIONS; d++) {
			final Direction direction = Direction.values()[d];

			for (int material = 1; material < sourceMaterialCount; material++) {
				masks[d][material] = inBlockNeighbourMask(snapshot, direction, material);
			}

			for (int i = 0; i < LAYER; i++) {
				final int outside = outsideIndex[d][i];

				if (outside == 0) {
					continue;
				}

				if (masks[d][outside] == null) {
					masks[d][outside] = new int[LAYER];
				}

				masks[d][outside][boundaryColumn(direction, i)] |= boundaryBit(direction, i);
			}
		}

		return masks;
	}

	/**
	 * Returns, for every column, the bits of cells whose in-block neighbour in {@code direction} is {@code material}.
	 * Cells on the outer face are left clear for {@link #buildNeighbourMasks} to fill from the boundary.
	 */
	private static int[] inBlockNeighbourMask(final ChiseledRenderSnapshot snapshot, final Direction direction, final int material) {
		final int[] mask = new int[LAYER];
		final int stepX = direction.getStepX();
		final int stepY = direction.getStepY();
		final int stepZ = direction.getStepZ();

		for (int x = 0; x < GRID; x++) {
			for (int y = 0; y < GRID; y++) {
				final int column = x * GRID + y;

				if (stepZ != 0) {
					final int occupied = snapshot.occupancy(material, column);
					mask[column] = stepZ > 0 ? occupied >>> 1 : (occupied << 1) & 0xFFFF;
					continue;
				}

				final int nx = x + stepX;
				final int ny = y + stepY;

				if (nx >= 0 && nx < GRID && ny >= 0 && ny < GRID) {
					mask[column] = snapshot.occupancy(material, nx * GRID + ny);
				}
			}
		}

		return mask;
	}

	private static int layer(final Direction direction) {
		return direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? GRID - 1 : 0;
	}

	/**
	 * Maps a boundary plane position to the column of the outer cell that touches it. The plane position {@code i}
	 * is {@code u * 16 + v} with (u, v) = (y, z) for X, (x, z) for Y and (x, y) for Z.
	 */
	private static int boundaryColumn(final Direction direction, final int planeIndex) {
		final int u = planeIndex / GRID;
		final int v = planeIndex % GRID;

		return switch (direction.getAxis()) {
		case X -> layer(direction) * GRID + u;
		case Y -> u * GRID + layer(direction);
		case Z -> u * GRID + v;
		};
	}

	private static int boundaryBit(final Direction direction, final int planeIndex) {
		final int v = planeIndex % GRID;

		return switch (direction.getAxis()) {
		case X, Y -> 1 << v;
		case Z -> 1 << layer(direction);
		};
	}

	/**
	 * Fills {@code rows} with the visible cells of one slice. Rows and columns follow the convention documented on
	 * {@link GreedyRectangles}.
	 */
	private static void extractRows(final int[] visible, final Direction.Axis axis, final int slice, final int[] rows) {
		switch (axis) {
		case X -> {
			for (int row = 0; row < GRID; row++) {
				rows[row] = visible[slice * GRID + row];
			}
		}
		case Y -> {
			for (int row = 0; row < GRID; row++) {
				rows[row] = visible[row * GRID + slice];
			}
		}
		case Z -> {
			for (int row = 0; row < GRID; row++) {
				int bits = 0;

				for (int x = 0; x < GRID; x++) {
					bits |= ((visible[x * GRID + row] >>> slice) & 1) << x;
				}

				rows[row] = bits;
			}
		}
		}
	}

	private static void addGreedyRectangles(
			final int[] rows,
			final int material,
			final int directionOrdinal,
			final Direction direction,
			final int slice,
			final RectangleBuffer output
	) {
		final int depth = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? slice + 1 : slice;

		for (int row = 0; row < GRID; row++) {
			while (rows[row] != 0) {
				final int column = Integer.numberOfTrailingZeros(rows[row]);
				final int width = Integer.numberOfTrailingZeros(~(rows[row] >>> column));
				final int runMask = ((1 << width) - 1) << column;

				int height = 1;

				while (row + height < GRID && (rows[row + height] & runMask) == runMask) {
					height++;
				}

				for (int k = 0; k < height; k++) {
					rows[row + k] &= ~runMask;
				}

				output.add(GreedyRectangles.pack(material, directionOrdinal, depth, row, column, width, height));
			}
		}
	}

	private static final class RectangleBuffer {
		private long[] values = new long[64];
		private int size;

		void add(final long value) {
			if (size == values.length) {
				values = Arrays.copyOf(values, values.length * 2);
			}

			values[size++] = value;
		}

		long[] toArray() {
			return Arrays.copyOf(values, size);
		}
	}
}

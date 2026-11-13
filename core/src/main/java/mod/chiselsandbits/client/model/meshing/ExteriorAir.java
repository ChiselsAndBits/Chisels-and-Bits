package mod.chiselsandbits.client.model.meshing;

import net.minecraft.core.Direction;

/**
 * Air connected to any boundary of a single block. Treating every boundary as open is conservative when
 * neighbouring blocks obstruct it, and avoids making cavity visibility depend on neighbouring snapshots.
 */
final class ExteriorAir {
	private static final int GRID = ChiseledRenderSnapshot.GRID;
	private static final Direction[] DIRECTIONS = Direction.values();

	private ExteriorAir() {
		throw new IllegalStateException("Cannot instantiate utility class");
	}

	static boolean[] find(final ChiseledRenderSnapshot snapshot) {
		final boolean[] reachable = new boolean[GRID * GRID * GRID];
		final int[] pending = new int[reachable.length];
		int tail = 0;

		for (int x = 0; x < GRID; x++) {
			for (int y = 0; y < GRID; y++) {
				for (int z = 0; z < GRID; z++) {
					if (x == 0 || x == GRID - 1 || y == 0 || y == GRID - 1 || z == 0 || z == GRID - 1) {
						tail = addAir(snapshot, reachable, pending, tail, x, y, z);
					}
				}
			}
		}

		for (int head = 0; head < tail; head++) {
			final int index = pending[head];
			final int x = index / (GRID * GRID);
			final int y = index / GRID % GRID;
			final int z = index % GRID;
			for (final Direction direction : DIRECTIONS) {
				tail = addAir(snapshot, reachable, pending, tail, x + direction.getStepX(), y + direction.getStepY(), z + direction.getStepZ());
			}
		}
		return reachable;
	}

	static GreedyRectangles removeSealedFaces(final ChiseledRenderSnapshot snapshot, final GreedyRectangles geometry) {
		final boolean[] reachable = find(snapshot);
		final long[] kept = new long[geometry.size()];
		int count = 0;
		for (int index = 0; index < geometry.size(); index++) {
			final long rectangle = geometry.rectangle(index);
			if (!facesSealedAir(snapshot, reachable, rectangle)) {
				kept[count++] = rectangle;
			}
		}
		return new GreedyRectangles(
				geometry.sourceVersion(), geometry.boundary(), geometry.materials(),
				java.util.Arrays.copyOf(kept, count)
		);
	}

	private static boolean facesSealedAir(
			final ChiseledRenderSnapshot snapshot,
			final boolean[] reachable,
			final long rectangle
	) {
		final Direction direction = GreedyRectangles.direction(rectangle);
		final int depth = GreedyRectangles.depth(rectangle)
				+ (direction.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? -1 : 0);
		if (depth < 0 || depth >= GRID) {
			return false;
		}
		for (int row = GreedyRectangles.row(rectangle); row < GreedyRectangles.row(rectangle) + GreedyRectangles.height(rectangle); row++) {
			for (int column = GreedyRectangles.column(rectangle); column < GreedyRectangles.column(rectangle) + GreedyRectangles.width(rectangle); column++) {
				final int x = switch (direction.getAxis()) {
					case X -> depth;
					case Y -> row;
					case Z -> column;
				};
				final int y = switch (direction.getAxis()) {
					case X, Z -> row;
					case Y -> depth;
				};
				final int z = switch (direction.getAxis()) {
					case X, Y -> column;
					case Z -> depth;
				};
				if (!snapshot.materialAt(x, y, z).isAir()
						|| reachable[ChiseledRenderSnapshot.cellIndex(x, y, z)]) {
					return false;
				}
			}
		}
		return true;
	}

	private static int addAir(
			final ChiseledRenderSnapshot snapshot,
			final boolean[] reachable,
			final int[] pending,
			final int tail,
			final int x,
			final int y,
			final int z
	) {
		if (x < 0 || x >= GRID || y < 0 || y >= GRID || z < 0 || z >= GRID) {
			return tail;
		}

		final int index = ChiseledRenderSnapshot.cellIndex(x, y, z);
		if (!reachable[index] && snapshot.materialAt(x, y, z).isAir()) {
			reachable[index] = true;
			pending[tail] = index;
			return tail + 1;
		}
		return tail;
	}
}

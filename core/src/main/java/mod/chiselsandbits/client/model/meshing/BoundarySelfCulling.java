package mod.chiselsandbits.client.model.meshing;

import java.util.ArrayList;
import java.util.List;

import mod.chiselsandbits.api.blockinformation.BlockInformation;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.material.FluidState;

/** Removes matching self-culled cells from one block-boundary rectangle. */
public final class BoundarySelfCulling {
	private BoundarySelfCulling() {
	}

	static List<Long> visible(final GreedyRectangles rectangles, final long rectangle, final NeighbourBoundary boundary) {
		final Direction direction = GreedyRectangles.cullFace(rectangle);
		final BlockInformation source = rectangles.material(rectangle);
		if (direction == null || (!(source.blockState().getBlock() instanceof LiquidBlock)
				&& !(source.blockState().getBlock() instanceof TransparentBlock))) {
			return List.of(rectangle);
		}

		final int rowStart = GreedyRectangles.row(rectangle);
		final int columnStart = GreedyRectangles.column(rectangle);
		final int height = GreedyRectangles.height(rectangle);
		final int width = GreedyRectangles.width(rectangle);
		final int[] rows = new int[height];
		boolean hidden = false;
		for (int row = 0; row < height; row++) {
			for (int column = 0; column < width; column++) {
				final int r = rowStart + row;
				final int c = columnStart + column;
				final BlockInformation neighbour = direction.getAxis() == Direction.Axis.Z
						? boundary.materialAt(direction, c, r) : boundary.materialAt(direction, r, c);
				if (selfCulls(source, neighbour, direction)) {
					hidden = true;
				} else {
					rows[row] |= 1 << column;
				}
			}
		}
		if (!hidden) {
			return List.of(rectangle);
		}

		final List<Long> visible = new ArrayList<>();
		for (int row = 0; row < height; row++) {
			while (rows[row] != 0) {
				final int column = Integer.numberOfTrailingZeros(rows[row]);
				final int runWidth = Integer.numberOfTrailingZeros(~(rows[row] >>> column));
				final int mask = ((1 << runWidth) - 1) << column;
				int runHeight = 1;
				while (row + runHeight < height && (rows[row + runHeight] & mask) == mask) {
					runHeight++;
				}
				for (int offset = 0; offset < runHeight; offset++) {
					rows[row + offset] &= ~mask;
				}
				visible.add(GreedyRectangles.pack(GreedyRectangles.materialIndex(rectangle), direction.ordinal(),
						GreedyRectangles.depth(rectangle), rowStart + row, columnStart + column, runWidth, runHeight));
			}
		}
		return visible;
	}

	public static List<GreedyMeshFace> visible(final GreedyMeshFace face, final NeighbourBoundary boundary) {
		if (!face.isOnOuterFace() || boundary == NeighbourBoundary.NONE
				|| (!(face.faceValue().blockState().getBlock() instanceof LiquidBlock)
						&& !(face.faceValue().blockState().getBlock() instanceof TransparentBlock))) {
			return List.of(face);
		}
		final Direction direction = face.normalDirection();
		final var min = face.minVector();
		final var max = face.maxVector();
		final int row = Math.round((direction.getAxis() == Direction.Axis.Y ? min.x : min.y) * 16);
		final int column = Math.round((direction.getAxis() == Direction.Axis.Z ? min.x : min.z) * 16);
		final int height = Math.round((direction.getAxis() == Direction.Axis.Y ? max.x - min.x : max.y - min.y) * 16);
		final int width = Math.round((direction.getAxis() == Direction.Axis.Z ? max.x - min.x : max.z - min.z) * 16);
		if (row < 0 || column < 0 || height < 1 || width < 1 || row + height > 16 || column + width > 16) {
			return List.of(face);
		}
		final int depth = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 16 : 0;
		final long original = GreedyRectangles.pack(0, direction.ordinal(), depth, row, column, width, height);
		final var rectangles = new GreedyRectangles(0, boundary, List.of(face.faceValue()), new long[] {original});
		final List<Long> visible = visible(rectangles, original, boundary);
		if (visible.size() == 1 && visible.getFirst() == original) {
			return List.of(face);
		}
		final List<GreedyMeshFace> result = new ArrayList<>();
		for (final long rectangle : visible) {
			final var from = GreedyRectangles.lowerCorner(rectangle);
			final var to = GreedyRectangles.upperCorner(rectangle);
			result.add(new GreedyMeshFace(face.faceValue(), from, to, from, to, direction, true));
		}
		return result;
	}

	private static boolean sameFluid(final BlockInformation source, final FluidState fluid, final BlockInformation neighbour) {
		if (!(neighbour.blockState().getBlock() instanceof LiquidBlock) || !source.variant().equals(neighbour.variant())) {
			return false;
		}
		final FluidState other = neighbour.blockState().getFluidState();
		return !fluid.isEmpty() && !other.isEmpty()
				&& fluid.getType().isSame(other.getType()) && other.getType().isSame(fluid.getType());
	}

	private static boolean selfCulls(final BlockInformation source, final BlockInformation neighbour, final Direction direction) {
		if (!source.variant().equals(neighbour.variant())) {
			return false;
		}
		if (source.blockState().getBlock() instanceof LiquidBlock) {
			return sameFluid(source, source.blockState().getFluidState(), neighbour);
		}
		return source.blockState().getBlock() instanceof TransparentBlock
				&& neighbour.blockState().getBlock() instanceof TransparentBlock
				&& source.blockState().skipRendering(neighbour.blockState(), direction)
				&& neighbour.blockState().skipRendering(source.blockState(), direction.getOpposite());
	}
}

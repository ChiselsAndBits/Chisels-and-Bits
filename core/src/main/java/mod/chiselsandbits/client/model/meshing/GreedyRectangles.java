package mod.chiselsandbits.client.model.meshing;

import java.util.List;

import mod.chiselsandbits.api.blockinformation.BlockInformation;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;

/**
 * Immutable result of greedy meshing: a list of packed rectangles of visible faces.
 *
 * <ul>
 * <li>bits 0-15: material index into {@link #materials()}</li>
 * <li>bits 16-18: face direction ordinal ({@link Direction#values()} order)</li>
 * <li>bits 19-23: face plane depth, 0..16 (positive faces sit at {@code slice + 1}, negative faces at {@code slice})</li>
 * <li>bits 24-27: row, 0..15</li>
 * <li>bits 28-31: column, 0..15</li>
 * <li>bits 32-35: width minus one along the column axis, 0..15</li>
 * <li>bits 36-39: height minus one along the row axis, 0..15</li>
 * </ul>
 *
 * <p>The row and column of a rectangle depend on the axis of its normal: for the X axis the row is y and the column
 * is z; for the Y axis the row is x and the column is z; for the Z axis the row is y and the column is x. The slice
 * index is {@code depth - 1} for positive faces and {@code depth} for negative faces.
 */
public final class GreedyRectangles {
	static final int MAX_MATERIAL_INDEX = 0xFFFF;
	static final int GRID = ChiseledRenderSnapshot.GRID;
	private static final long MASK_4 = 0xF;
	private static final long MASK_5 = 0x1F;
	private static final long MASK_3 = 0x7;
	private static final long MASK_16 = 0xFFFF;

	private final long sourceVersion;
	private final NeighbourBoundary boundary;
	private final List<BlockInformation> materials;
	private final long[] rectangles;

	GreedyRectangles(
			final long sourceVersion,
			final NeighbourBoundary boundary,
			final List<BlockInformation> materials,
			final long[] rectangles
	) {
		this.sourceVersion = sourceVersion;
		this.boundary = boundary;
		this.materials = materials;
		this.rectangles = rectangles;
	}

	/**
	 * @return the version of the snapshot this geometry was built from
	 */
	public long sourceVersion() {
		return sourceVersion;
	}

	/**
	 * @return the neighbour boundary used to cull outer faces
	 */
	public NeighbourBoundary boundary() {
		return boundary;
	}

	/**
	 * @return the immutable material table; index 0 is air
	 */
	public List<BlockInformation> materials() {
		return materials;
	}

	/**
	 * @return the number of rectangles
	 */
	public int size() {
		return rectangles.length;
	}

	/**
	 * @param index rectangle index, from 0 to {@link #size()} exclusive
	 * @return the packed rectangle
	 */
	public long rectangle(final int index) {
		return rectangles[index];
	}

	/**
	 * @param rectangle a packed rectangle from this result
	 * @return the material of the rectangle
	 */
	public BlockInformation material(final long rectangle) {
		return materials.get(materialIndex(rectangle));
	}

	/**
	 * Only faces lying on the owning block boundary participate in renderer neighbour culling.
	 */
	public static @Nullable Direction cullFace(final long rectangle) {
		final int depth = depth(rectangle);
		return depth == 0 || depth == GRID ? direction(rectangle) : null;
	}

	public static Vector3f lowerCorner(final long rectangle) {
		final float depth = depth(rectangle) / (float) GRID;
		final float row = row(rectangle) / (float) GRID;
		final float column = column(rectangle) / (float) GRID;

		return switch (direction(rectangle).getAxis()) {
			case X -> new Vector3f(depth, row, column);
			case Y -> new Vector3f(row, depth, column);
			case Z -> new Vector3f(column, row, depth);
		};
	}

	public static Vector3f upperCorner(final long rectangle) {
		final float depth = depth(rectangle) / (float) GRID;
		final float row = (row(rectangle) + height(rectangle)) / (float) GRID;
		final float column = (column(rectangle) + width(rectangle)) / (float) GRID;

		return switch (direction(rectangle).getAxis()) {
			case X -> new Vector3f(depth, row, column);
			case Y -> new Vector3f(row, depth, column);
			case Z -> new Vector3f(column, row, depth);
		};
	}

	/**
	 * @return the total number of unit faces covered by all rectangles
	 */
	public long unitFaceCount() {
		long count = 0;

		for (final long rectangle : rectangles) {
			count += (long) width(rectangle) * height(rectangle);
		}

		return count;
	}

	/**
	 * @return true when the geometry is non-empty and contains no faces on the owning block's outer boundary
	 */
	public boolean hasNoBlockBoundaryFaces() {
		if (rectangles.length == 0) {
			return false;
		}

		for (final long rectangle : rectangles) {
			final int depth = depth(rectangle);
			if (depth == 0 || depth == GRID) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Computes an order-sensitive checksum over the packed rectangles. Useful to keep benchmarked work observable.
	 *
	 * @return the checksum
	 */
	public long checksum() {
		long hash = rectangles.length;

		for (final long rectangle : rectangles) {
			hash = hash * 31 + rectangle;
		}

		return hash;
	}

	public static int materialIndex(final long rectangle) {
		return (int) (rectangle & MASK_16);
	}

	public static Direction direction(final long rectangle) {
		return Direction.values()[(int) ((rectangle >>> 16) & MASK_3)];
	}

	public static int depth(final long rectangle) {
		return (int) ((rectangle >>> 19) & MASK_5);
	}

	public static int row(final long rectangle) {
		return (int) ((rectangle >>> 24) & MASK_4);
	}

	public static int column(final long rectangle) {
		return (int) ((rectangle >>> 28) & MASK_4);
	}

	public static int width(final long rectangle) {
		return (int) ((rectangle >>> 32) & MASK_4) + 1;
	}

	public static int height(final long rectangle) {
		return (int) ((rectangle >>> 36) & MASK_4) + 1;
	}

	static long pack(
			final int materialIndex,
			final int directionOrdinal,
			final int depth,
			final int row,
			final int column,
			final int width,
			final int height
	) {
		if (materialIndex < 0 || materialIndex > MAX_MATERIAL_INDEX) {
			throw new IllegalArgumentException("Material index out of range: " + materialIndex);
		}

		if (directionOrdinal < 0 || directionOrdinal >= Direction.values().length) {
			throw new IllegalArgumentException("Direction ordinal out of range: " + directionOrdinal);
		}

		if (depth < 0 || depth > GRID) {
			throw new IllegalArgumentException("Depth out of range 0..16: " + depth);
		}

		if (row < 0 || row >= GRID || column < 0 || column >= GRID) {
			throw new IllegalArgumentException("Row/column out of range 0..15: " + row + "," + column);
		}

		if (width < 1 || width > GRID || height < 1 || height > GRID) {
			throw new IllegalArgumentException("Width/height out of range 1..16: " + width + "x" + height);
		}

		return (long) materialIndex
				| ((long) directionOrdinal << 16)
				| ((long) depth << 19)
				| ((long) row << 24)
				| ((long) column << 28)
				| ((long) (width - 1) << 32)
				| ((long) (height - 1) << 36);
	}
}

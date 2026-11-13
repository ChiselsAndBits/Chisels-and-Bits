package mod.chiselsandbits.client.model.meshing;

import java.util.Arrays;

import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.block.storage.StateEntryStorage;
import mod.chiselsandbits.api.multistate.StateEntrySize;
import mod.chiselsandbits.api.multistate.accessor.ISingleBlockAxisAlignedAreaAccessor;
import mod.chiselsandbits.api.multistate.accessor.IStateEntryInfo;
import mod.chiselsandbits.multistate.snapshot.SimpleSnapshot;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Immutable material planes describing the voxels just outside each face of a 16 grid block.
 *
 * <p>Each plane is indexed by the two in-plane coordinates of the source cell: for the X axis {@code (u, v) = (y, z)},
 * for the Y axis {@code (u, v) = (x, z)} and for the Z axis {@code (u, v) = (x, y)}. Planes keep full material
 * identity so that identity-aware culling policies remain correct across chiseled neighbours.
 */
public final class NeighbourBoundary {
	static final int GRID = ChiseledRenderSnapshot.GRID;
	private static final int PLANE_SIZE = GRID * GRID;

	public static final NeighbourBoundary NONE = new NeighbourBoundary(
			emptyPlanes(),
			new long[Direction.values().length]
	);

	private final BlockInformation[][] planes;
	private final long[] versions;

	private NeighbourBoundary(final BlockInformation[][] planes, final long[] versions) {
		this.planes = planes;
		this.versions = versions;
	}

	/**
	 * Returns a boundary where every outside voxel on the given face is {@code material}.
	 *
	 * @param direction the face direction, pointing from the block to the neighbour
	 * @param material the material of the neighbouring full block
	 * @return a new boundary derived from this one
	 */
	public NeighbourBoundary withUniform(final Direction direction, final BlockInformation material) {
		final BlockInformation[] plane = new BlockInformation[PLANE_SIZE];
		Arrays.fill(plane, material);
		return withPlane(direction, plane, ChiseledRenderSnapshot.nextVersion());
	}

	/**
	 * Returns a boundary taken from the face of a neighbouring block that touches this block.
	 *
	 * @param direction the face direction, pointing from the block to the neighbour
	 * @param neighbour the neighbouring snapshot; its opposite face becomes the boundary plane
	 * @return a new boundary derived from this one
	 */
	public NeighbourBoundary withSnapshot(final Direction direction, final ChiseledRenderSnapshot neighbour) {
		final Direction.Axis axis = direction.getAxis();
		final int layer = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 0 : GRID - 1;
		final BlockInformation[] plane = new BlockInformation[PLANE_SIZE];

		for (int u = 0; u < GRID; u++) {
			for (int v = 0; v < GRID; v++) {
				plane[u * GRID + v] = neighbour.materialAt(cellX(axis, layer, u, v), cellY(axis, layer, u, v), cellZ(axis, layer, u, v));
			}
		}

		return withPlane(direction, plane, neighbour.version());
	}

	/**
	 * Captures the touching plane of an immutable single-block accessor without constructing a full render snapshot.
	 */
	public NeighbourBoundary withAccessor(final Direction direction, final ISingleBlockAxisAlignedAreaAccessor neighbour) {
		final int size = StateEntrySize.current().getBitsPerBlockSide();
		final int scale = GRID / size;
		final int layer = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 0 : size - 1;
		final StateEntryStorage.PackedSnapshot copy = neighbour.getClass() == SimpleSnapshot.class
				? ((SimpleSnapshot) neighbour).copyPackedStorage() : null;
		final boolean direct = copy != null && copy.getSize() == size;
		final BlockInformation[] plane = new BlockInformation[PLANE_SIZE];
		for (int u = 0; u < size; u++) {
			for (int v = 0; v < size; v++) {
				final int x = cellX(direction.getAxis(), layer, u, v);
				final int y = cellY(direction.getAxis(), layer, u, v);
				final int z = cellZ(direction.getAxis(), layer, u, v);
				final BlockInformation value;
				if (direct) {
					final BlockInformation stored = copy.getBlockInformation(x, y, z);
					value = stored.isAir() ? BlockInformation.AIR : stored;
				} else {
					value = neighbour.getInAreaTarget(new Vec3(
							(x + 0.5d) / size, (y + 0.5d) / size, (z + 0.5d) / size))
							.map(IStateEntryInfo::getBlockInformation).orElse(BlockInformation.AIR);
				}
				for (int du = 0; du < scale; du++) {
					for (int dv = 0; dv < scale; dv++) {
						plane[(u * scale + du) * GRID + v * scale + dv] = value;
					}
				}
			}
		}
		return withPlane(direction, plane, ChiseledRenderSnapshot.nextVersion());
	}

	/**
	 * Returns the material on the outside of the face at the given in-plane coordinates.
	 *
	 * @param direction the face direction
	 * @param u first in-plane coordinate, see the class documentation
	 * @param v second in-plane coordinate, see the class documentation
	 * @return the outside material
	 */
	public BlockInformation materialAt(final Direction direction, final int u, final int v) {
		checkCoordinate(u);
		checkCoordinate(v);
		return planes[direction.ordinal()][u * GRID + v];
	}

	/**
	 * Returns the identity of the data behind a face, used to tell boundaries apart in results.
	 *
	 * @param direction the face direction
	 * @return a version that changes whenever the plane is produced from different data
	 */
	public long version(final Direction direction) {
		return versions[direction.ordinal()];
	}

	BlockInformation planeMaterial(final int directionOrdinal, final int planeIndex) {
		return planes[directionOrdinal][planeIndex];
	}

	static int cellX(final Direction.Axis axis, final int layer, final int u, final int v) {
		return axis == Direction.Axis.X ? layer : u;
	}

	static int cellY(final Direction.Axis axis, final int layer, final int u, final int v) {
		return axis == Direction.Axis.X ? u : axis == Direction.Axis.Y ? layer : v;
	}

	static int cellZ(final Direction.Axis axis, final int layer, final int u, final int v) {
		return axis == Direction.Axis.Z ? layer : v;
	}

	private NeighbourBoundary withPlane(final Direction direction, final BlockInformation[] plane, final long version) {
		final BlockInformation[][] copiedPlanes = planes.clone();
		copiedPlanes[direction.ordinal()] = plane;

		final long[] copiedVersions = versions.clone();
		copiedVersions[direction.ordinal()] = version;
		return new NeighbourBoundary(copiedPlanes, copiedVersions);
	}

	private static BlockInformation[][] emptyPlanes() {
		final BlockInformation[][] result = new BlockInformation[Direction.values().length][PLANE_SIZE];

		for (final BlockInformation[] plane : result) {
			Arrays.fill(plane, BlockInformation.AIR);
		}

		return result;
	}

	private static void checkCoordinate(final int coordinate) {
		if (coordinate < 0 || coordinate >= GRID) {
			throw new IllegalArgumentException("Boundary coordinate out of range 0.." + (GRID - 1) + ": " + coordinate);
		}
	}
}

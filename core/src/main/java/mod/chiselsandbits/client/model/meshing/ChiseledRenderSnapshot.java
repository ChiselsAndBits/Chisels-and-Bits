package mod.chiselsandbits.client.model.meshing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import mod.chiselsandbits.api.block.storage.StateEntryStorage;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.multistate.StateEntrySize;
import mod.chiselsandbits.api.multistate.accessor.ISingleBlockAxisAlignedAreaAccessor;
import mod.chiselsandbits.api.multistate.accessor.IStateEntryInfo;
import mod.chiselsandbits.multistate.snapshot.SimpleSnapshot;

import net.minecraft.world.phys.Vec3;

/**
 * Immutable, versioned copy of one chiseled block's contents, expanded onto a normalized 16 grid.
 *
 * <p>The snapshot copies all data out of the source storage when it is built, so later mutations of the storage do
 * not affect it. Storage with 1, 2, 4, 8 or 16 entries per side is expanded so every source entry covers
 * {@code 16 / size} cells along each axis. Cell {@code (x, y, z)} is addressed with {@code x, y, z} in {@code 0..15}.
 *
 * <p>Geometry produced without a neighbour boundary is cached on the snapshot for the last policy used. Geometry that
 * depends on a {@link NeighbourBoundary} is never cached, because its outer faces depend on data outside this
 * snapshot.
 */
public final class ChiseledRenderSnapshot {
	public static final int GRID = 16;
	static final int LAYER = GRID * GRID;
	static final int CELLS = GRID * GRID * GRID;

	private static final AtomicLong VERSIONS = new AtomicLong(1);

	private final long version;
	private final List<BlockInformation> materials;
	private final short[] cells;
	private final int[] occupancy;
	private volatile CachedGeometry intrinsicGeometry;

	private ChiseledRenderSnapshot(
			final long version,
			final List<BlockInformation> materials,
			final short[] cells,
			final int[] occupancy
	) {
		this.version = version;
		this.materials = materials;
		this.cells = cells;
		this.occupancy = occupancy;
	}

	/**
	 * Builds a snapshot from storage. Only the storage's current contents are read.
	 *
	 * @param storage the storage to copy
	 * @return the snapshot
	 * @throws IllegalArgumentException when the storage size is not 1, 2, 4, 8 or 16
	 */
	public static ChiseledRenderSnapshot of(final StateEntryStorage storage) {
		Objects.requireNonNull(storage, "storage");
		return build(storage.getSize(), storage::getBlockInformation);
	}

	private static ChiseledRenderSnapshot build(final int sourceSize, final CellReader reader) {
		if (!isSupportedSize(sourceSize)) {
			throw new IllegalArgumentException("Unsupported storage size: " + sourceSize);
		}

		final int scale = GRID / sourceSize;
		final Map<BlockInformation, Integer> indexByMaterial = new HashMap<>();
		final List<BlockInformation> materials = new ArrayList<>();
		indexByMaterial.put(BlockInformation.AIR, 0);
		materials.add(BlockInformation.AIR);

		final short[] cells = new short[CELLS];

		for (int sx = 0; sx < sourceSize; sx++) {
			for (int sy = 0; sy < sourceSize; sy++) {
				for (int sz = 0; sz < sourceSize; sz++) {
					final BlockInformation information = reader.get(sx, sy, sz);
					final int materialIndex = indexByMaterial.computeIfAbsent(information, key -> {
						materials.add(key);
						return materials.size() - 1;
					});

					fillSourceCell(cells, sx, sy, sz, scale, materialIndex);
				}
			}
		}

		final int[] occupancy = new int[materials.size() * LAYER];

		for (int x = 0; x < GRID; x++) {
			for (int y = 0; y < GRID; y++) {
				for (int z = 0; z < GRID; z++) {
					final int materialIndex = cells[cellIndex(x, y, z)];

					if (materialIndex != 0) {
						occupancy[materialIndex * LAYER + x * GRID + y] |= 1 << z;
					}
				}
			}
		}

		return new ChiseledRenderSnapshot(
				VERSIONS.getAndIncrement(),
				List.copyOf(materials),
				cells,
				occupancy
		);
	}

	/**
	 * Builds a snapshot from an immutable single-block area snapshot.
	 *
	 * <p>Sampling each source-cell centre preserves the accessor's configured bit resolution while avoiding any
	 * dependency on the mutable storage owned by a block entity.
	 *
	 * @param accessor an immutable single-block accessor
	 * @return the copied render snapshot
	 */
	public static ChiseledRenderSnapshot of(final ISingleBlockAxisAlignedAreaAccessor accessor) {
		Objects.requireNonNull(accessor, "accessor");
		final int sourceSize = StateEntrySize.current().getBitsPerBlockSide();
		if (accessor.getClass() == SimpleSnapshot.class) {
			final StateEntryStorage.PackedSnapshot storage = ((SimpleSnapshot) accessor).copyPackedStorage();
			if (storage.getSize() == sourceSize) {
				return build(sourceSize, (x, y, z) -> {
					final BlockInformation stored = storage.getBlockInformation(x, y, z);
					return stored.isAir() ? BlockInformation.AIR : stored;
				});
			}
		}
		return build(sourceSize, (x, y, z) -> accessor.getInAreaTarget(new Vec3(
				(x + 0.5d) / sourceSize, (y + 0.5d) / sourceSize, (z + 0.5d) / sourceSize))
				.map(IStateEntryInfo::getBlockInformation).orElse(BlockInformation.AIR));
	}

	/**
	 * @param size the candidate storage size
	 * @return true when the size is one of the configurable sizes 1, 2, 4, 8 or 16
	 */
	static boolean isSupportedSize(final int size) {
		for (final StateEntrySize entrySize : StateEntrySize.values()) {
			if (entrySize.getBitsPerBlockSide() == size) {
				return true;
			}
		}

		return false;
	}

	/**
	 * @return the identity of this snapshot; every built snapshot has a distinct version
	 */
	public long version() {
		return version;
	}

	/**
	 * @param x normalized x coordinate, 0..15
	 * @param y normalized y coordinate, 0..15
	 * @param z normalized z coordinate, 0..15
	 * @return the material at the given normalized cell, air when empty
	 */
	public BlockInformation materialAt(final int x, final int y, final int z) {
		checkCell(x, y, z);
		return materials.get(cells[cellIndex(x, y, z)]);
	}

	/**
	 * Returns geometry for this snapshot with no neighbour data. The result is cached per policy instance on this
	 * snapshot, so repeated calls with the same policy return the same object.
	 *
	 * @param policy the face culling policy
	 * @return immutable greedy rectangles
	 */
	public GreedyRectangles greedyRectangles(final FaceCullingPolicy policy) {
		Objects.requireNonNull(policy, "policy");
		final CachedGeometry cached = intrinsicGeometry;

		if (cached != null && cached.policy() == policy) {
			return cached.geometry();
		}

		final GreedyRectangles geometry = RectangleMesher.mesh(this, policy, NeighbourBoundary.NONE);
		intrinsicGeometry = new CachedGeometry(policy, geometry);
		return geometry;
	}

	/**
	 * Returns geometry for this snapshot with outer faces culled against the given neighbour boundary. This result is
	 * recomputed on every call and is not cached on the snapshot.
	 *
	 * @param policy the face culling policy
	 * @param boundary the material planes outside this block
	 * @return immutable greedy rectangles
	 */
	public GreedyRectangles greedyRectangles(final FaceCullingPolicy policy, final NeighbourBoundary boundary) {
		Objects.requireNonNull(policy, "policy");
		Objects.requireNonNull(boundary, "boundary");

		if (boundary == NeighbourBoundary.NONE) {
			return greedyRectangles(policy);
		}

		return RectangleMesher.mesh(this, policy, boundary);
	}

	/**
	 * Culls air pockets disconnected from every block boundary. Call only when every occupied source cell is known
	 * to render as fully opaque geometry; otherwise a cavity could be visible through its surrounding material.
	 */
	public GreedyRectangles greedyRectanglesWithoutSealedAir(final NeighbourBoundary boundary) {
		Objects.requireNonNull(boundary, "boundary");
		if (!RectangleSubsetPreflight.supportsSealedAirOmission(this)) {
			throw new IllegalArgumentException("Sealed-air culling requires opaque solid source materials");
		}
		return ExteriorAir.removeSealedFaces(this, greedyRectangles(FaceCullingPolicy.ANY_OCCUPIED, boundary));
	}

	static long nextVersion() {
		return VERSIONS.getAndIncrement();
	}

	private static void fillSourceCell(final short[] cells, final int sx, final int sy, final int sz, final int scale, final int materialIndex) {
		for (int x = sx * scale; x < (sx + 1) * scale; x++) {
			for (int y = sy * scale; y < (sy + 1) * scale; y++) {
				for (int z = sz * scale; z < (sz + 1) * scale; z++) {
					cells[cellIndex(x, y, z)] = (short) materialIndex;
				}
			}
		}
	}

	static int cellIndex(final int x, final int y, final int z) {
		return (x * GRID + y) * GRID + z;
	}

	public List<BlockInformation> materials() {
		return materials;
	}

	int occupancy(final int materialIndex, final int column) {
		return occupancy[materialIndex * LAYER + column];
	}

	int materialCount() {
		return materials.size();
	}

	private static void checkCell(final int x, final int y, final int z) {
		if (x < 0 || x >= GRID || y < 0 || y >= GRID || z < 0 || z >= GRID) {
			throw new IllegalArgumentException("Cell out of range 0..15: " + x + "," + y + "," + z);
		}
	}

	private record CachedGeometry(FaceCullingPolicy policy, GreedyRectangles geometry) {
	}

	@FunctionalInterface
	private interface CellReader {
		BlockInformation get(int x, int y, int z);
	}
}

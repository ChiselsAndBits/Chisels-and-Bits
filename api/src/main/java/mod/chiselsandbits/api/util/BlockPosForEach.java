package mod.chiselsandbits.api.util;

import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Iterates over every {@link net.minecraft.core.BlockPos} within an axis-aligned range of integer coordinates.
 */
public class BlockPosForEach {
	private BlockPosForEach() {
		throw new IllegalStateException("Can not instantiate an instance of: BlockPosForEach.This is a utility class");
	}

	/**
	 * Iterates over the cube {@code [0, size)} along each axis from the origin, invoking {@code consumer} for each position.
	 *
	 * @param size the exclusive upper bound for each coordinate
	 * @param consumer receives each position in the range
	 */
	public static void forEachInRange(final int size, final Consumer<BlockPos> consumer) {
		forEachInRange(size, size, size, consumer);
	}

	/**
	 * Iterates over every block position in the {@code xSize} by {@code ySize} by {@code zSize} volume starting at the origin, invoking {@code consumer} for each.
	 *
	 * @param xSize the exclusive upper bound for the x coordinate
	 * @param ySize the exclusive upper bound for the y coordinate
	 * @param zSize the exclusive upper bound for the z coordinate
	 * @param consumer receives each position in the range
	 */
	public static void forEachInRange(final int xSize, final int ySize, final int zSize, final Consumer<BlockPos> consumer) {
		final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

		for (int x = 0; x < xSize; x++) {
			for (int y = 0; y < ySize; y++) {
				for (int z = 0; z < zSize; z++) {
					pos.set(x, y, z);
					consumer.accept(pos);
				}
			}
		}
	}

	/**
	 * Iterates over every block position between the given minimum and maximum coordinates (inclusive), converting them to integer positions via {@link VectorUtils#toBlockPos}.
	 *
	 * @param min minimum corner of the range
	 * @param max maximum corner of the range
	 * @param consumer receives each position in the range
	 */
	public static void forEachInRange(final Vec3 min, final Vec3 max, final Consumer<BlockPos> consumer) {
		final BlockPos minPos = VectorUtils.toBlockPos(min);
		final BlockPos maxPos = VectorUtils.toBlockPos(max);

		forEachInRange(
				minPos.getX(), minPos.getY(), minPos.getZ(),
				maxPos.getX(), maxPos.getY(), maxPos.getZ(),
				consumer
		);
	}

	/**
	 * Iterates over every block position within the inclusive bounds {@code minX..maxX} by {@code minY..maxY} by {@code minZ..maxZ}, invoking {@code consumer} for each.
	 *
	 * @param minX inclusive lower x bound
	 * @param minY inclusive lower y bound
	 * @param minZ inclusive lower z bound
	 * @param maxX inclusive upper x bound
	 * @param maxY inclusive upper y bound
	 * @param maxZ inclusive upper z bound
	 * @param consumer receives each position in the range
	 */
	public static void forEachInRange(final int minX, final int minY, final int minZ, final int maxX, final int maxY, final int maxZ, final Consumer<BlockPos> consumer) {
		if (minX == maxX && minY == maxY && minZ == maxZ) {
			consumer.accept(new BlockPos(minX, minY, minZ));
			return;
		}

		final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					pos.set(x, y, z);
					consumer.accept(pos);
				}
			}
		}
	}
}

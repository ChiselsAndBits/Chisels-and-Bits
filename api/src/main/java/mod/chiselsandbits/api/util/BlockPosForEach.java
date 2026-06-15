package mod.chiselsandbits.api.util;

import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class BlockPosForEach
{
	private BlockPosForEach()

	{
		throw new IllegalStateException("Can not instantiate an instance of: BlockPosForEach. This is a utility class");
	}

	public static void forEachInRange(final int size, final Consumer<BlockPos> consumer)

	{
		forEachInRange(size, size, size, consumer);
	}

	public static void forEachInRange(final int xSize, final int ySize, final int zSize, final Consumer<BlockPos> consumer)

	{
		final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = 0; x < xSize; x++)

		{
			for (int y = 0; y < ySize; y++)

			{
				for (int z = 0; z < zSize; z++)

				{
					pos.set(x, y, z);
					consumer.accept(pos);
				}
			}
		}
	}

	public static void forEachInRange(final Vec3 min, final Vec3 max, final Consumer<BlockPos> consumer) {
		final BlockPos minPos = VectorUtils.toBlockPos(min);
		final BlockPos maxPos = VectorUtils.toBlockPos(max);

		forEachInRange(
		  minPos.getX(), minPos.getY(), minPos.getZ(),
		  maxPos.getX(), maxPos.getY(), maxPos.getZ(),
		  consumer
		);
	}

	public static void forEachInRange(final int minX, final int minY, final int minZ, final int maxX, final int maxY, final int maxZ, final Consumer<BlockPos> consumer)

	{
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

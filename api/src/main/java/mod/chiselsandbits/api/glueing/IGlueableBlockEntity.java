package mod.chiselsandbits.api.glueing;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Supplies block-entity data used to connect adjacent glued blocks into a structure.
 */
public interface IGlueableBlockEntity {
	/**
	 * Returns the block reader used to inspect neighboring block entities.
	 *
	 * @return the block reader used for neighbor lookups
	 */
	BlockGetter blockGetter();

	/**
	 * Returns this entity's world position.
	 *
	 * @return this entity's block position
	 */
	BlockPos blockPos();

	/**
	 * Returns the directions in which this entity is glued to neighbors.
	 *
	 * @return the set of connected directions
	 */
	EnumSet<Direction> gluedSides();

	/**
	 * Creates the item stack dropped when this glued entity is broken.
	 *
	 * @return the created value.
	 */
	ItemStack createGluedDrop();

	/**
	 * Traverses glued neighbors and returns all positions in this connected structure.
	 *
	 * @return all block positions connected by glue, including this entity's position
	 */
	default Set<BlockPos> connectedPositions() {
		final Set<Direction> gluedSides = gluedSides();

		if (gluedSides.isEmpty()) {
			return Set.of(blockPos());
		}

		final Set<BlockPos> walkedPositions = new HashSet<>();
		final Set<BlockPos> connectedPositions = new HashSet<>();
		final LinkedHashSet<BlockPos> toCheck = new LinkedHashSet<>();

		walkedPositions.add(blockPos());
		connectedPositions.add(blockPos());

		for (final Direction direction : gluedSides) {
			final BlockPos offset = blockPos().relative(direction);
			toCheck.add(offset);
		}

		while (!toCheck.isEmpty()) {
			final BlockPos current = toCheck.getFirst();
			toCheck.remove(current);

			if (walkedPositions.contains(current)) {
				continue;
			}

			walkedPositions.add(current);

			final BlockEntity blockEntity = blockGetter().getBlockEntity(current);

			if (blockEntity instanceof IGlueableBlockEntity) {
				connectedPositions.add(current);

				final Set<Direction> neighborGluedSides = ((IGlueableBlockEntity) blockEntity).gluedSides();

				for (final Direction direction : neighborGluedSides) {
					final BlockPos offset = current.relative(direction);

					if (!walkedPositions.contains(offset)) {
						toCheck.add(offset);
					}
				}
			}
		}

		return connectedPositions;
	}
}

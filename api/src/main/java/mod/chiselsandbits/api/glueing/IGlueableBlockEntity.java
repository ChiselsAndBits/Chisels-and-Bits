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

public interface IGlueableBlockEntity {
	BlockGetter blockGetter();

	BlockPos blockPos();

	EnumSet<Direction> gluedSides();

	ItemStack createGluedDrop();

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

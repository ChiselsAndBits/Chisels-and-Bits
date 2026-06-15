package mod.chiselsandbits.utils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import mod.chiselsandbits.api.util.MathUtil;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

public class AABBUtils
{
	private AABBUtils()

	{
		throw new IllegalStateException("Can not instantiate an instance of: AABBUtils. This is a utility class");
	}

	public static boolean areBoxesNeighbors(@NotNull final AABB l, @NotNull final AABB r,@NotNull final Direction direction) {
		final double endOfL = getDirectionalValue(l, direction);
		final double startOfR = getDirectionalValue(r, direction.getOpposite());

		if (!MathUtil.almostEqual(endOfL, startOfR)) {
			return false;
		}

		final List<Direction> remainingDirectionsToCheck = DirectionUtils.getNotOnAxisOf(direction);
		final Map<Direction, Double> valuesOfL = remainingDirectionsToCheck.stream().collect(Collectors.toMap(Function.identity(), d -> getDirectionalValue(l, d)));
		final Map<Direction, Double> valuesOfR = remainingDirectionsToCheck.stream().collect(Collectors.toMap(Function.identity(), d -> getDirectionalValue(r, d)));

		return remainingDirectionsToCheck.stream().allMatch(d -> MathUtil.almostEqual(
		  valuesOfL.get(d),
		  valuesOfR.get(d)
		));
	}

	public static double getDirectionalValue(@NotNull final AABB bb, @NotNull final Direction direction) {
		return direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ?
				 bb.max(direction.getAxis()) :
				 bb.min(direction.getAxis());
	}
}

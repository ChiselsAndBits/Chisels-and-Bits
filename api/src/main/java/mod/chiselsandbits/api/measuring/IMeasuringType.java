package mod.chiselsandbits.api.measuring;

import mod.chiselsandbits.api.item.withmode.group.IToolModeGroup;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public interface IMeasuringType extends IToolModeGroup {
	Vec3 adaptClickedPosition(BlockHitResult blockHitResult);

	Vec3 adaptStartCorner(Vec3 startPosition, Vec3 endPosition, Direction hitFace);

	Vec3 adaptEndCorner(Vec3 startPosition, Vec3 endPosition, Direction hitFace);

	Vec3 getResolution();

	boolean isNeedsNormalization();

	@FunctionalInterface
	interface IPositionAdapter {
		Vec3 adapt(Vec3 startPosition, Vec3 endPosition, Direction hitFace);
	}

	@FunctionalInterface
	interface IClickedPositionAdapter {
		static IClickedPositionAdapter identity() {
			return HitResult::getLocation;
		}

		Vec3 adapt(BlockHitResult startPosition);
	}
}

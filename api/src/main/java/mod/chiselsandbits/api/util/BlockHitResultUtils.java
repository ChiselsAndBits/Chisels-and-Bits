package mod.chiselsandbits.api.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

public class BlockHitResultUtils
{

    private BlockHitResultUtils()
    {
        throw new IllegalStateException("Can not instantiate an instance of: BlockHitResultUtils. This is a utility class");
    }

    public static Vec3 getCenterOfHitObject(final BlockHitResult blockHitResult, final Vec3 objectSize) {
        return getCenterOfHitObject(
          blockHitResult,
          objectSize,
          face -> Vec3.atLowerCornerOf(face.getOpposite().getUnitVec3i())
        );
    }

    private static Vec3 getCenterOfHitObject(
      final BlockHitResult blockHitResult,
      final Vec3 objectSize,
      final Function<Direction, Vec3> placementFacingAdapter
    ) {
        final Vec3 halfSize = objectSize.multiply(0.5, 0.5, 0.5);
        final Vec3 scaleSize = VectorUtils.divide(VectorUtils.ONE, objectSize);

        final Vec3 hitCenterVector = blockHitResult.getLocation().add(
          placementFacingAdapter.apply(blockHitResult.getDirection())
            .multiply(halfSize)
        );

        final Vec3 scaledHitCenterVector = hitCenterVector.multiply(scaleSize);
        final Vec3 scaledHitCenterVectorFloor = Vec3.atLowerCornerOf(VectorUtils.toBlockPos(scaledHitCenterVector));

        final Vec3 lowerLeftCornerOfTargetedObject = scaledHitCenterVectorFloor.multiply(objectSize);
        return lowerLeftCornerOfTargetedObject.add(halfSize);
    }
}

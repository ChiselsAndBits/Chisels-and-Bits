package mod.chiselsandbits.client.model.face;

import mod.chiselsandbits.api.blockinformation.BlockInformation;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class FaceRegion
{
    private final Direction        face;
    private final BlockInformation blockInformation;
    private final boolean          isEdge;
    private float minX;
    private float minY;
    private float minZ;
    private float maxX;
    private float maxY;
    private float maxZ;

    public static FaceRegion createFrom3DObjectWithFacing(
      final Vec3 start,
      final Vec3 end,
      final Direction facing,
      final BlockInformation blockInformation,
      final boolean isEdge
    ) {
        return new FaceRegion(
          facing,
          blockInformation,
          isEdge,
          (float) (facing.getStepX() == 0 ? start.x() : (facing.getStepX() == 1 ? Math.max(start.x(), end.x()) : Math.min(start.x(), end.x()))),
          (float) (facing.getStepY() == 0 ? start.y() : (facing.getStepY() == 1 ? Math.max(start.y(), end.y()) : Math.min(start.y(), end.y()))),
          (float) (facing.getStepZ() == 0 ? start.z() : (facing.getStepZ() == 1 ? Math.max(start.z(), end.z()) : Math.min(start.z(), end.z()))),
          (float) (facing.getStepX() == 0 ? end.x() : (facing.getStepX() == 1 ? Math.max(start.x(), end.x()) : Math.min(start.x(), end.x()))),
          (float) (facing.getStepY() == 0 ? end.y() : (facing.getStepY() == 1 ? Math.max(start.y(), end.y()) : Math.min(start.y(), end.y()))),
          (float) (facing.getStepZ() == 0 ? end.z() : (facing.getStepZ() == 1 ? Math.max(start.z(), end.z()) : Math.min(start.z(), end.z())))
        );
    }

    public FaceRegion(
      final Direction facingDirection,
      final float centerX,
      final float centerY,
      final float centerZ,
      final BlockInformation blockInformation,
      final boolean isEdgeFace)
    {
        face = facingDirection;
        this.blockInformation = blockInformation;
        isEdge = isEdgeFace;
        minX = centerX;
        minY = centerY;
        minZ = centerZ;
        maxX = centerX;
        maxY = centerY;
        maxZ = centerZ;
    }

    public FaceRegion(
      final Direction face,
      final BlockInformation blockInformation,
      final boolean isEdge,
      final float minX,
      final float minY,
      final float minZ,
      final float maxX,
      final float maxY,
      final float maxZ)
    {
        this.face = face;
        this.blockInformation = blockInformation;
        this.isEdge = isEdge;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public BlockInformation getBlockInformation()
    {
        return blockInformation;
    }

    public float getMinX()
    {
        return minX;
    }

    public float getMinY()
    {
        return minY;
    }

    public float getMinZ()
    {
        return minZ;
    }

    public float getMaxX()
    {
        return maxX;
    }

    public float getMaxY()
    {
        return maxY;
    }

    public float getMaxZ()
    {
        return maxZ;
    }

    public boolean isEdge()
    {
        return isEdge;
    }

    public Direction getFace()
    {
        return face;
    }

    public Vector3f maxVector()
    {
        return new Vector3f(maxX, maxY, maxZ);
    }

    public Vector3f minVector()
    {
        return new Vector3f(minX, minY, minZ);
    }

    public boolean extend(
      final FaceRegion faceToExtendTo
    ) {
        if (faceToExtendTo.blockInformation != blockInformation)
        {
            return false;
        }

        switch (face)
        {
            case DOWN:
            case UP:
            {
                final boolean a = maxX == faceToExtendTo.minX && maxZ == faceToExtendTo.maxZ && minZ == faceToExtendTo.minZ;
                final boolean b = minX == faceToExtendTo.maxX && maxZ == faceToExtendTo.maxZ && minZ == faceToExtendTo.minZ;
                final boolean c = maxZ == faceToExtendTo.minZ && maxX == faceToExtendTo.maxX && minX == faceToExtendTo.minX;
                final boolean d = minZ == faceToExtendTo.maxZ && maxX == faceToExtendTo.maxX && minX == faceToExtendTo.minX;

                if (a || b || c || d)
                {
                    minX = Math.min(faceToExtendTo.minX, minX);
                    minY = Math.min(faceToExtendTo.minY, minY);
                    minZ = Math.min(faceToExtendTo.minZ, minZ);
                    maxX = Math.max(faceToExtendTo.maxX, maxX);
                    maxY = Math.max(faceToExtendTo.maxY, maxY);
                    maxZ = Math.max(faceToExtendTo.maxZ, maxZ);
                    return true;
                }

                return false;
            }

            case WEST:
            case EAST:
            {
                final boolean a = maxY == faceToExtendTo.minY && maxZ == faceToExtendTo.maxZ && minZ == faceToExtendTo.minZ;
                final boolean b = minY == faceToExtendTo.maxY && maxZ == faceToExtendTo.maxZ && minZ == faceToExtendTo.minZ;
                final boolean c = maxZ == faceToExtendTo.minZ && maxY == faceToExtendTo.maxY && minY == faceToExtendTo.minY;
                final boolean d = minZ == faceToExtendTo.maxZ && maxY == faceToExtendTo.maxY && minY == faceToExtendTo.minY;

                if (a || b || c || d)
                {
                    minX = Math.min(faceToExtendTo.minX, minX);
                    minY = Math.min(faceToExtendTo.minY, minY);
                    minZ = Math.min(faceToExtendTo.minZ, minZ);
                    maxX = Math.max(faceToExtendTo.maxX, maxX);
                    maxY = Math.max(faceToExtendTo.maxY, maxY);
                    maxZ = Math.max(faceToExtendTo.maxZ, maxZ);
                    return true;
                }

                return false;
            }

            case NORTH:
            case SOUTH:
            {
                final boolean a = maxY == faceToExtendTo.minY && maxX == faceToExtendTo.maxX && minX == faceToExtendTo.minX;
                final boolean b = minY == faceToExtendTo.maxY && maxX == faceToExtendTo.maxX && minX == faceToExtendTo.minX;
                final boolean c = maxX == faceToExtendTo.minX && maxY == faceToExtendTo.maxY && minY == faceToExtendTo.minY;
                final boolean d = minX == faceToExtendTo.maxX && maxY == faceToExtendTo.maxY && minY == faceToExtendTo.minY;

                if (a || b || c || d)
                {
                    minX = Math.min(faceToExtendTo.minX, minX);
                    minY = Math.min(faceToExtendTo.minY, minY);
                    minZ = Math.min(faceToExtendTo.minZ, minZ);
                    maxX = Math.max(faceToExtendTo.maxX, maxX);
                    maxY = Math.max(faceToExtendTo.maxY, maxY);
                    maxZ = Math.max(faceToExtendTo.maxZ, maxZ);
                    return true;
                }

                return false;
            }

            default:
                return false;
        }
    }
}

package mod.chiselsandbits.client.model.meshing;

import mod.chiselsandbits.api.blockinformation.BlockInformation;
import net.minecraft.core.Direction;
import org.joml.Vector3f;

public record GreedyMeshFace(
    BlockInformation faceValue,
    Vector3f lowerLeft,
    Vector3f upperLeft,
    Vector3f lowerRight,
    Vector3f upperRight,
    Direction normalDirection,
    boolean isOnOuterFace
)
{

    public Vector3f minVector()
    {
        return new Vector3f(
            Math.min(lowerLeft.x, Math.min(upperLeft.x, Math.min(lowerRight.x, upperRight.x))),
            Math.min(lowerLeft.y, Math.min(upperLeft.y, Math.min(lowerRight.y, upperRight.y))),
            Math.min(lowerLeft.z, Math.min(upperLeft.z, Math.min(lowerRight.z, upperRight.z)))
        );
    }

    public Vector3f maxVector()
    {
        return new Vector3f(
            Math.max(lowerLeft.x, Math.max(upperLeft.x, Math.max(lowerRight.x, upperRight.x))),
            Math.max(lowerLeft.y, Math.max(upperLeft.y, Math.max(lowerRight.y, upperRight.y))),
            Math.max(lowerLeft.z, Math.max(upperLeft.z, Math.max(lowerRight.z, upperRight.z)))
        );
    }
}

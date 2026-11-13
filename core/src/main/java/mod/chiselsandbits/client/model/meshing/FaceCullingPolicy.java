package mod.chiselsandbits.client.model.meshing;

import java.util.function.Predicate;

import mod.chiselsandbits.api.blockinformation.BlockInformation;

import net.minecraft.core.Direction;

/**
 * Decides whether a face of a source voxel is hidden by the non-air voxel next to it.
 *
 * <p>The mesher never calls this with air as the neighbour. Implementations are evaluated per
 * (source material, neighbour material, direction) triple, so they may be asymmetric and may depend on material
 * identity. Callers that reuse a snapshot's cached geometry must pass the same policy instance each time, because the
 * cache is keyed on instance identity.
 */
@FunctionalInterface
public interface FaceCullingPolicy {
	/**
	 * Legacy policy matching {@link mod.chiselsandbits.client.model.meshing.GreedyMeshBuilder}: any occupied neighbour
	 * hides the face, regardless of material.
	 */
	FaceCullingPolicy ANY_OCCUPIED = (source, neighbour, direction) -> true;

	/**
	 * Creates a policy where a face is hidden when the neighbour is the same material, or when the neighbour is opaque.
	 *
	 * @param opaque predicate deciding whether a neighbouring material fully occludes faces behind it
	 * @return the culling policy
	 */
	static FaceCullingPolicy opaqueOrSameMaterial(final Predicate<BlockInformation> opaque) {
		return (source, neighbour, direction) -> source.equals(neighbour) || opaque.test(neighbour);
	}

	/**
	 * Returns whether the face of {@code source} pointing in {@code direction} is hidden by {@code neighbour}.
	 *
	 * @param source the material owning the face
	 * @param neighbour the non-air material on the other side of the face
	 * @param direction the direction the face points to
	 * @return true when the face must not be emitted
	 */
	boolean hidesFace(BlockInformation source, BlockInformation neighbour, Direction direction);
}

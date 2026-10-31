package mod.chiselsandbits.api.voxelshape;

import java.util.Optional;

import mod.chiselsandbits.api.IChiselsAndBitsAPI;
import mod.chiselsandbits.api.axissize.CollisionType;
import mod.chiselsandbits.api.multistate.accessor.IAreaAccessor;
import mod.chiselsandbits.api.multistate.accessor.identifier.IAreaShapeIdentifier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A manager for dealing with voxelshapes related to chiseled blocks and areas.
 */
public interface IVoxelShapeManager {
	static IVoxelShapeManager getInstance() {
		return IChiselsAndBitsAPI.getInstance().getVoxelShapeManager();
	}

	/**
	 * Returns the shape of the multistate entries which are contained in the given area accessor.
	 * Excludes all air states.
	 *
	 * @param accessor The accessor to get the shape of.
	 * @return The shape of the accessor.
	 */
	default VoxelShape get(
			final IAreaAccessor accessor) {
		return get(accessor, BlockPos.ZERO);
	}

	/**
	 * Returns the shape of the multistate entries which are contained in the given area accessor.
	 * Excludes all air states.
	 *
	 * @param accessor The accessor to get the shape of.
	 * @param offset The offset to apply to the voxelshape during calculation or cache lookup.
	 * @return The shape of the accessor.
	 */
	@SuppressWarnings("deprecation")
	default VoxelShape get(
			final IAreaAccessor accessor,
			final BlockPos offset) {
		return get(accessor, offset, CollisionType.NONE_AIR, true);
	}

	/**
	 * Returns the shape of the multistate entries which are contained in the given area accessor.
	 *
	 * @param accessor The accessor to get the shape of.
	 * @param sizeType The type of the shape to include.
	 * @return The shape of the accessor.
	 */
	default VoxelShape get(final IAreaAccessor accessor,
			final CollisionType sizeType) {
		return get(
				accessor,
				BlockPos.ZERO,
				sizeType,
				true);
	}

	/**
	 * Returns the shape of the multistate entries which are contained in the given area accessor.
	 *
	 * @param accessor The accessor to get the shape of.
	 * @param sizeType The type of the shape to include.
	 * @param simplify Indicates if the returned voxelshape should be simplified.Generally good for performance, but might cause some shapes to not work properly.
	 * @return The shape of the accessor.
	 */
	default VoxelShape get(final IAreaAccessor accessor,
			final CollisionType sizeType,
			final boolean simplify) {
		return get(
				accessor,
				BlockPos.ZERO,
				sizeType,
				simplify);
	}

	/**
	 * Returns the shape of the multistate entries which are contained in the given area accessor.
	 *
	 * @param accessor The accessor to get the shape of.
	 * @param offset The offset to apply to the voxelshape during calculation or cache lookup.
	 * @param sizeType The type to include.
	 * @param simplify Indicates if the returned voxelshape should be simplified.Generally good for performance, but might cause some shapes to not work properly.
	 * @return The shape of the accessor.
	 */
	VoxelShape get(
			IAreaAccessor accessor,
			BlockPos offset,
			CollisionType sizeType,
			boolean simplify);

	/**
	 * Returns the shape that is referenced by a given area shape identifier.
	 * If no shape with the given identifier is known then an empty optional is returned.
	 *
	 * @param identifier The identifier to get the voxel shape for.
	 * @param offset The offset to apply to the voxelshape during cache lookup.
	 * @return The optional, optionally containing the voxel shape.
	 */
	@SuppressWarnings("deprecation")
	default Optional<VoxelShape> getCached(
			final IAreaShapeIdentifier identifier,
			final BlockPos offset) {
		return getCached(identifier, offset, CollisionType.NONE_AIR);
	};

	/**
	 * Returns the shape that is referenced by a given area shape identifier.
	 * If no shape with the given identifier is known then an empty optional is returned.
	 *
	 * @param identifier The identifier to get the voxel shape for.
	 * @param offset The offset to apply to the voxelshape during cache lookup.
	 * @param sizeType The type include.
	 * @return The optional, optionally containing the voxel shape.
	 */
	default Optional<VoxelShape> getCached(
			final IAreaShapeIdentifier identifier,
			final BlockPos offset,
			final CollisionType sizeType) {
		return getCached(
				identifier, offset, sizeType, true
		);
	}

	/**
	 * Returns the shape that is referenced by a given area shape identifier.
	 * If no shape with the given identifier is known then an empty optional is returned.
	 *
	 * @param identifier The identifier to get the voxel shape for.
	 * @param offset The offset to apply to the voxelshape during cache lookup.
	 * @param sizeType The type to include.
	 * @param simplify Indicates if the returned voxelshape should be simplified.Generally good for performance, but might cause some shapes to not work properly.
	 * @return The optional, optionally containing the voxel shape.
	 */
	Optional<VoxelShape> getCached(
			IAreaShapeIdentifier identifier,
			BlockPos offset,
			CollisionType sizeType,
			boolean simplify);
}

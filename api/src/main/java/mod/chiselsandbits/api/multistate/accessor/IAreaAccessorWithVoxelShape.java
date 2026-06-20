package mod.chiselsandbits.api.multistate.accessor;

import mod.chiselsandbits.api.axissize.CollisionType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A special {@link IAreaAccessor} that is capable of determining its own voxelshape.
 */
public interface IAreaAccessorWithVoxelShape extends IAreaAccessor {
	/**
	 * Determines the voxelshape of the {@link IAreaAccessorWithVoxelShape} at the given position.
	 *
	 * @param type The type of the voxel shape that is needed.
	 * @param offset The offset of the {@link IAreaAccessorWithVoxelShape} from the given position.
	 * @param simplify Whether to simplify the voxelshape.
	 * @return The voxelshape of the {@link IAreaAccessorWithVoxelShape} at the given position.
	 */
	VoxelShape provideShape(final CollisionType type, final BlockPos offset, final boolean simplify);
}

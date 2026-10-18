package mod.chiselsandbits.voxelshape;

import mod.chiselsandbits.aabb.AABBManager;
import mod.chiselsandbits.api.axissize.CollisionType;
import mod.chiselsandbits.api.multistate.accessor.IAreaAccessor;
import mod.chiselsandbits.api.multistate.accessor.IAreaAccessorWithVoxelShape;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class VoxelShapeCalculator {
	public static VoxelShape calculate(
			final IAreaAccessor areaAccessor,
			final BlockPos offset,
			final CollisionType sizeType,
			final boolean simplify) {
		if (areaAccessor instanceof IAreaAccessorWithVoxelShape) {
			return ((IAreaAccessorWithVoxelShape) areaAccessor).provideShape(sizeType, offset, simplify);
		}

		final VoxelShape shape =
				AABBManager.getInstance()
					.get(areaAccessor, sizeType)
					.stream()
					.map(aabb -> aabb.move(offset))
					.reduce(
							Shapes.empty(),
							(voxelShape, axisAlignedBB) -> {
								final VoxelShape bbShape = Shapes.create(axisAlignedBB);
								return Shapes.joinUnoptimized(voxelShape, bbShape, BooleanOp.OR);
							},


							(voxelShape, voxelShape2) -> Shapes.joinUnoptimized(voxelShape, voxelShape2, BooleanOp.OR)
		);

		return simplify ? shape.optimize() : shape;
	}
}

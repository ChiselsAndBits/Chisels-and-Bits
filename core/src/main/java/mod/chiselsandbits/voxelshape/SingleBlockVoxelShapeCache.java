package mod.chiselsandbits.voxelshape;

import java.util.EnumMap;

import mod.chiselsandbits.api.axissize.CollisionType;
import mod.chiselsandbits.api.voxelshape.IVoxelShapeManager;
import mod.chiselsandbits.block.entities.ChiseledBlockEntity;

import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SingleBlockVoxelShapeCache {
	private final EnumMap<CollisionType, VoxelShape> shapes = new EnumMap<>(CollisionType.class);

	private final ChiseledBlockEntity blockEntity;

	public SingleBlockVoxelShapeCache(ChiseledBlockEntity blockEntity) {
		this.blockEntity = blockEntity;
	}

	public void reset() {
		shapes.clear();
	}

	public VoxelShape getShape(final CollisionType type) {
		return shapes.computeIfAbsent(type, this::createShape);
	}

	private VoxelShape createShape(final CollisionType type) {
		final VoxelShape shape = IVoxelShapeManager.getInstance().get(blockEntity, type);

		if (type.canBeEmptyWithJustFluids() && shape.isEmpty()) {
			final boolean justFluids = blockEntity.stream().allMatch(stateEntry -> stateEntry.getBlockInformation().isAir() || !stateEntry.getBlockInformation().blockState().getFluidState().isEmpty());
			return justFluids ? shape : Shapes.block();
		}

		return shape.isEmpty() ? Shapes.block() : shape;
	}
}

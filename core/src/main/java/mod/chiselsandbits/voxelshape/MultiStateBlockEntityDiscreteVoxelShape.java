package mod.chiselsandbits.voxelshape;

import java.util.BitSet;

import mod.chiselsandbits.api.multistate.StateEntrySize;
import mod.chiselsandbits.utils.BlockPosUtils;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;

public class MultiStateBlockEntityDiscreteVoxelShape extends BitSetDiscreteVoxelShape
{
	public MultiStateBlockEntityDiscreteVoxelShape(final BitSet source)
	{
		super(
		  StateEntrySize.current().getBitsPerBlockSide(),
		  StateEntrySize.current().getBitsPerBlockSide(),
		  StateEntrySize.current().getBitsPerBlockSide()
		);

		this.storage = source;
		recalculateBounds();
	}

	public MultiStateBlockEntityDiscreteVoxelShape(final BitSet source, int size)
	{
		super(
		  size, size, size
		);

		this.storage = source;
		recalculateBounds();
	}

	private void recalculateBounds()
	{
		this.xMin = this.xSize;
		this.yMin = this.ySize;
		this.zMin = this.zSize;
		this.xMax = 0;
		this.yMax = 0;
		this.zMax = 0;

		for (int x = 0; x < this.xSize; ++x) {
			for (int y = 0; y < this.ySize; ++y) {
				for (int z = 0; z < this.zSize; ++z) {
					if(this.storage.get(BlockPosUtils.getCollisionIndex(x,y,z,this.ySize, this.zSize))) {
						this.xMin = Math.min(this.xMin, x);
						this.yMin = Math.min(this.yMin, y);
						this.zMin = Math.min(this.zMin, z);
						this.xMax = Math.max(this.xMax, x);
						this.yMax = Math.max(this.yMax, y);
						this.zMax = Math.max(this.zMax, z);
					}
				}
			}
		}
	}
}

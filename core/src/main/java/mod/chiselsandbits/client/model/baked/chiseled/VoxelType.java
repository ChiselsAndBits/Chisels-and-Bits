package mod.chiselsandbits.client.model.baked.chiseled;

import java.util.function.Predicate;

import mod.chiselsandbits.api.blockinformation.BlockInformation;

import net.minecraft.world.level.block.state.BlockState;

public enum VoxelType
{
	SOLID(s -> s.getFluidState().isEmpty()),
			FLUID(s -> !s.getFluidState().isEmpty()),
			UNKNOWN(s -> true);

	private final Predicate<BlockState> isValidBlockStateCallback;

	VoxelType(final Predicate<BlockState> isValidBlockStateCallback) {this.isValidBlockStateCallback = isValidBlockStateCallback; }

	public boolean isValidBlockState(final BlockInformation blockState) {
		return this.isValidBlockStateCallback.test(blockState.blockState());
	}

	public boolean isFluid() {
		return this == FLUID;
	}
}

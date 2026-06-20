package mod.chiselsandbits.multistate.mutator;

import static mod.chiselsandbits.block.entities.ChiseledBlockEntity.ONE_THOUSANDS;

import mod.chiselsandbits.api.multistate.StateEntrySize;
import mod.chiselsandbits.api.multistate.accessor.IAccessorFactory;
import mod.chiselsandbits.api.multistate.accessor.IAreaAccessor;
import mod.chiselsandbits.api.multistate.mutator.IGenerallyModifiableAreaMutator;
import mod.chiselsandbits.api.multistate.mutator.IMutatorFactory;
import mod.chiselsandbits.api.multistate.mutator.world.IWorldAreaMutator;
import mod.chiselsandbits.api.util.VectorUtils;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

public class MutatorFactory implements IMutatorFactory, IAccessorFactory {
	private static final MutatorFactory INSTANCE = new MutatorFactory();

	private MutatorFactory()

	{
	}

	public static MutatorFactory getInstance()

	{
		return INSTANCE;
	}

	@Override
	public @NotNull IWorldAreaMutator in(final LevelAccessor world, final BlockPos pos)

	{
		return new ChiselAdaptingWorldMutator(world, pos);
	}

	@Override
	public @NotNull IWorldAreaMutator covering(final LevelAccessor world, final BlockPos from, final BlockPos to)

	{
		return new WorldWrappingMutator(
		  world,
		  Vec3.atLowerCornerOf(from),
		  Vec3.atLowerCornerOf(to).add(new Vec3(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit())
								  .multiply(StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide())
								  .subtract(ONE_THOUSANDS, ONE_THOUSANDS, ONE_THOUSANDS)
		  )
		);
	}

	@Override
	public @NotNull IWorldAreaMutator covering(final LevelAccessor world, final Vec3 from, final Vec3 to)

	{
		return new WorldWrappingMutator(
		  world,
		  Vec3.atLowerCornerOf(VectorUtils.toBlockPos(from.multiply(StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide()))).multiply(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit()).add(ONE_THOUSANDS, ONE_THOUSANDS, ONE_THOUSANDS),
		  Vec3.atLowerCornerOf(VectorUtils.toBlockPos(to.multiply(StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide(), StateEntrySize.current().getBitsPerBlockSide()))).multiply(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit()).add(StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit(), StateEntrySize.current().getSizePerBit()).subtract(ONE_THOUSANDS, ONE_THOUSANDS, ONE_THOUSANDS)
		);
	}

	@Override
	public @NotNull IGenerallyModifiableAreaMutator clonedFromAccessor(final IAreaAccessor source)

	{
		return source.createSnapshot();
	}
}

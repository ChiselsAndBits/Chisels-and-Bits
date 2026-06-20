package mod.chiselsandbits.api.util;

import java.util.function.Predicate;

import net.minecraft.world.level.block.state.BlockState;

public class BlockStatePredicates {
	private BlockStatePredicates()

	{
		throw new IllegalStateException("Can not instantiate an instance of: StateEntryPredicates. This is a utility class");
	}

	public static final Predicate<BlockState> NOT_AIR = new Predicate<>()

	{
		@Override
		public boolean test(final BlockState blockState)

		{
			return !blockState.isAir();
		}

		@Override
		public int hashCode()

		{
			return 0;
		}

		@Override
		public boolean equals(final Object obj)

		{
			return obj == this;
		}
	}

	;

	public static final Predicate<BlockState> ALL = new Predicate<>()

	{
		@Override
		public boolean test(final BlockState blockState)

		{
			return true;
		}

		@Override
		public int hashCode()

		{
			return 1;
		}

		@Override
		public boolean equals(final Object obj)

		{
			return obj == this;
		}
	}

	;

	public static final Predicate<BlockState> COLLIDEABLE_ONLY = new Predicate<>()

	{
		@Override
		public boolean test(final BlockState blockState)

		{
			return blockState.getFluidState().isEmpty() && !blockState.isAir();
		}

		@Override
		public int hashCode()

		{
			return 2;
		}

		@Override
		public boolean equals(final Object obj)

		{
			return obj == this;
		}
	}

	;
}

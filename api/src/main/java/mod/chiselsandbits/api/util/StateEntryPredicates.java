package mod.chiselsandbits.api.util;

import java.util.function.Predicate;

import mod.chiselsandbits.api.multistate.accessor.IStateEntryInfo;

public class StateEntryPredicates
{
	private StateEntryPredicates()

	{
		throw new IllegalStateException("Can not instantiate an instance of: StateEntryPredicates. This is a utility class");
	}

	public static final Predicate<IStateEntryInfo> NOT_AIR = new Predicate<>()

	{
		@Override
		public boolean test(final IStateEntryInfo iStateEntryInfo)

		{
			return BlockStatePredicates.NOT_AIR.test(iStateEntryInfo.getBlockInformation().blockState());
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
	};

	public static final Predicate<IStateEntryInfo> ALL = new Predicate<>()

	{
		@Override
		public boolean test(final IStateEntryInfo iStateEntryInfo)

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
	};

	public static final Predicate<IStateEntryInfo> COLLIDEABLE_ONLY = new Predicate<>()

	{
		@Override
		public boolean test(final IStateEntryInfo iStateEntryInfo)

		{
			return BlockStatePredicates.COLLIDEABLE_ONLY.test(iStateEntryInfo.getBlockInformation().blockState());
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
	};
}

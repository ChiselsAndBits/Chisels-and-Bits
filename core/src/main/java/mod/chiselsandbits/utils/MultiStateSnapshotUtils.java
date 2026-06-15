package mod.chiselsandbits.utils;

import mod.chiselsandbits.api.block.storage.StateEntryStorage;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshot;
import mod.chiselsandbits.multistate.snapshot.SimpleSnapshot;

public class MultiStateSnapshotUtils
{
	private MultiStateSnapshotUtils()

	{
		throw new IllegalStateException("Can not instantiate an instance of: MultiStateSnapshotUtils. This is a utility class");
	}

	public static IMultiStateSnapshot createFromStorage(final StateEntryStorage storage) {
		return new SimpleSnapshot(storage.createSnapshot());
	}
}

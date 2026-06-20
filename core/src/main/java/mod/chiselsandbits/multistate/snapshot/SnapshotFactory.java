package mod.chiselsandbits.multistate.snapshot;

import mod.chiselsandbits.api.block.storage.StateEntryStorage;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshot;
import mod.chiselsandbits.api.multistate.snapshot.ISnapshotFactory;

public final class SnapshotFactory implements ISnapshotFactory {
	private static final SnapshotFactory INSTANCE = new SnapshotFactory();

	public static SnapshotFactory getInstance() {
		return INSTANCE;
	}

	private SnapshotFactory() {
	}

	@Override
	public IMultiStateSnapshot singleBlock() {
		return new SimpleSnapshot(
			new StateEntryStorage()
		);
	}

	@Override
	public IMultiStateSnapshot singleBlock(final BlockInformation blockInformation) {
		return new SimpleSnapshot(blockInformation);
	}
}

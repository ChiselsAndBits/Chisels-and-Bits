package mod.chiselsandbits.registries;

import com.communi.suggestu.scena.core.registries.ICustomRegistry;
import mod.chiselsandbits.api.change.changes.IChangeType;
import mod.chiselsandbits.api.chiseling.mode.IChiselMode;
import mod.chiselsandbits.api.cutting.operation.ICuttingOperation;
import mod.chiselsandbits.api.glueing.operation.IGlueingOperation;
import mod.chiselsandbits.api.modification.operation.IModificationOperation;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshotType;
import mod.chiselsandbits.api.pattern.placement.IPatternPlacementType;
import mod.chiselsandbits.api.registries.IRegistryManager;
import mod.chiselsandbits.registrars.ModChangeTypes;
import mod.chiselsandbits.registrars.ModChiselModes;
import mod.chiselsandbits.registrars.ModCuttingOperation;
import mod.chiselsandbits.registrars.ModGlueingOperation;
import mod.chiselsandbits.registrars.ModModificationOperation;
import mod.chiselsandbits.registrars.ModMultiStateSnapshotTypes;
import mod.chiselsandbits.registrars.ModPatternPlacementTypes;
import org.jetbrains.annotations.NotNull;

public class RegistryManager implements IRegistryManager {
	private static final RegistryManager INSTANCE = new RegistryManager();

	private RegistryManager() {
	}

	public static RegistryManager getInstance() {
		return INSTANCE;
	}

	/**
	 * The registry which controls all available chiseling modes.
	 *
	 * @return The registry.
	 */
	@Override
	public ICustomRegistry<IChiselMode> getChiselModeRegistry() {
		return ModChiselModes.REGISTRY.get();
	}

	@Override
	public ICustomRegistry<IModificationOperation> getModificationOperationRegistry() {
		return ModModificationOperation.REGISTRY_SUPPLIER.get();
	}

	@Override
	public ICustomRegistry<ICuttingOperation> getCuttingOperationRegistry() {
		return ModCuttingOperation.REGISTRY_SUPPLIER.get();
	}

	@Override
	public ICustomRegistry<IGlueingOperation> getGlueingOperationRegistry() {
		return ModGlueingOperation.REGISTRY_SUPPLIER.get();
	}

	@Override
	public ICustomRegistry<IChangeType> getChangeTypeRegistry() {
		return ModChangeTypes.REGISTRY_SUPPLIER.get();
	}

	@Override
	public ICustomRegistry<IMultiStateSnapshotType> getMultiStateSnapshotTypeRegistry() {
		return ModMultiStateSnapshotTypes.REGISTRY_SUPPLIER.get();
	}

	@Override
	public ICustomRegistry<IPatternPlacementType> getPatternPlacementTypeRegistry() {
		return ModPatternPlacementTypes.REGISTRY_SUPPLIER.get();
	}
}

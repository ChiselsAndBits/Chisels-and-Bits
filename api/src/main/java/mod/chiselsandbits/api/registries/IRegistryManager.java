package mod.chiselsandbits.api.registries;

import com.communi.suggestu.scena.core.registries.ICustomRegistry;
import com.communi.suggestu.scena.core.registries.ICustomRegistryEntry;
import mod.chiselsandbits.api.IChiselsAndBitsAPI;
import mod.chiselsandbits.api.change.changes.IChangeType;
import mod.chiselsandbits.api.chiseling.mode.IChiselMode;
import mod.chiselsandbits.api.cutting.operation.ICuttingOperation;
import mod.chiselsandbits.api.glueing.operation.IGlueingOperation;
import mod.chiselsandbits.api.modification.operation.IModificationOperation;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshotType;
import mod.chiselsandbits.api.pattern.placement.IPatternPlacementType;
import org.jetbrains.annotations.NotNull;

/**
 * Manages all registries which are used by Chisels and Bits.
 */
public interface IRegistryManager {
	static IRegistryManager getInstance() {
		return IChiselsAndBitsAPI.getInstance().getRegistryManager();
	}

	/**
	 * The registry which controls all available chiseling modes.
	 *
	 * @return The registry.
	 */
	ICustomRegistry<IChiselMode> getChiselModeRegistry();

	/**
	 * The registry used for modifications of single use patterns.
	 *
	 * @return The modification operation registry.
	 */
		ICustomRegistry<IModificationOperation> getModificationOperationRegistry();

	/**
	 * The registry used for cutting of single use patterns.
	 *
	 * @return The cutting operation registry.
	 */
		ICustomRegistry<ICuttingOperation> getCuttingOperationRegistry();

	/**
	 * The registry used for glueing of single use patterns.
	 *
	 * @return The glueing operation registry.
	 */
		ICustomRegistry<IGlueingOperation> getGlueingOperationRegistry();

	/**
	 * The registry used for change types.
	 *
	 * @return The change type registry.
	 */
		ICustomRegistry<IChangeType> getChangeTypeRegistry();

	/**
	 * The registry used for multi state snapshot types.
	 *
	 * @return The multi state snapshot type registry.
	 */
		ICustomRegistry<IMultiStateSnapshotType> getMultiStateSnapshotTypeRegistry();

	/**
	 * The registry used for pattern placement types.
	 *
	 * @return The pattern placement type registry.
	 */
		ICustomRegistry<IPatternPlacementType> getPatternPlacementTypeRegistry();
}

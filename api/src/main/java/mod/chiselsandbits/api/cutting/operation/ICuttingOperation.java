package mod.chiselsandbits.api.cutting.operation;

import com.communi.suggestu.scena.core.registries.ICustomRegistry;
import com.communi.suggestu.scena.core.registries.ICustomRegistryEntry;
import mod.chiselsandbits.api.IChiselsAndBitsAPI;
import mod.chiselsandbits.api.item.withmode.IToolMode;
import mod.chiselsandbits.api.multistate.accessor.IAreaAccessor;
import mod.chiselsandbits.api.registries.IRegistryManager;

import java.util.Collection;

/**
 * A Cutting operation that can be performed in the Cutting table.
 */
public interface ICuttingOperation extends ICustomRegistryEntry, IToolMode<ICuttingOperationGroup> {
	/**
	 * The default Cutting operation.
	 * @return The default operation.
	 */
	static ICuttingOperation getDefaultMode() {
		return IChiselsAndBitsAPI.getInstance().getDefaultCuttingOperation();
	}

	/**
	 * The underlying registry that contains the different Cutting modes that can be performed.
	 * @return The underlying forge registry.
	 */
	static ICustomRegistry<ICuttingOperation> getRegistry() {
		return IRegistryManager.getInstance().getCuttingOperationRegistry();
	}

	/**
	 * Performs a Cutting on the snapshot.
	 * The returned collection contains the individual pieces that result of this cut.
	 *
	 * In total a maximum 64 pieces can be returned.
	 *
	 * Note: The cutter generates a bundle if more than one unique piece is cut.
	 *
	 * @param source The mutator to modify.
	 * @return A collection of accessors that were created by the operation.
	 */
	Collection<IAreaAccessor> apply(final IAreaAccessor source);
}

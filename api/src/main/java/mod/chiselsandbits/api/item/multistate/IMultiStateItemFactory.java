package mod.chiselsandbits.api.item.multistate;

import mod.chiselsandbits.api.IChiselsAndBitsAPI;
import mod.chiselsandbits.api.exceptions.StateEntryInfoIsToBigException;
import mod.chiselsandbits.api.multistate.accessor.IStateEntryInfo;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshot;
import net.minecraft.world.item.ItemStack;

/**
 * Can create multistate items from a given source.
 */
public interface IMultiStateItemFactory {
	/**
	 * The instance of the manager.
	 * @return The instance.
	 */
	static IMultiStateItemFactory getInstance() {
		return IChiselsAndBitsAPI.getInstance().getMultiStateItemFactory();
	}

	/**
	 * Creates a new multistate itemstack with a single state internally.
	 *
	 * @param stateEntryInfo The state entry info to create an itemstack for.
	 * @return The itemstack containing only the given single state entry.
	 */
	ItemStack createBlockFrom(final IStateEntryInfo stateEntryInfo);
}

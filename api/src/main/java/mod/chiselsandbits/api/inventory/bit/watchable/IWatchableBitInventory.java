package mod.chiselsandbits.api.inventory.bit.watchable;

import mod.chiselsandbits.api.inventory.bit.IBitInventory;

/**
 * Bit inventory variant which can be watched for changes.
 */
public interface IWatchableBitInventory extends IBitInventory {
	/**
     * Invoke to start receiving change callbacks on this given callback.
     *
     * @param onChangeCallback Triggered when the inventory changes.
     *
     * @return The watch object that can be closed to stop listening.
     */
	IWatch startWatching(final Runnable onChangeCallback);
}

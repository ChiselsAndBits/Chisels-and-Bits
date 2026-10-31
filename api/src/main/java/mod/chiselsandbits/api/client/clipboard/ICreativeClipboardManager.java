package mod.chiselsandbits.api.client.clipboard;

import java.util.List;

import mod.chiselsandbits.api.IChiselsAndBitsAPI;
import mod.chiselsandbits.api.item.multistate.IMultiStateItemStack;

import net.minecraft.core.HolderLookup;

/**
 * Manages clipboard operations for creative-mode multistate items.
 */

public interface ICreativeClipboardManager {
	/**
	 * Returns the clipboard manager exposed by the active API instance.
	 *
	 * @return the requested value.
	 */
	static ICreativeClipboardManager getInstance() {
		return IChiselsAndBitsAPI.getInstance().getCreativeClipboardManager();
	}

	/**
	 * The clipboard contents.
	 *
	 * @return The clipboard contents.
	 */
	List<IMultiStateItemStack> getClipboard();

	/**
	 * Adds an entry to the clipboard.
	 *
	 * @param multiStateItemStack The multi-state item stack to add.
	 * @param provider registry lookup used to serialize the stack's contents
	 */
	void addEntry(IMultiStateItemStack multiStateItemStack, HolderLookup.Provider provider);

	/**
	 * Removes an entry from the clipboard.
	 *
	 * @param index The index of the entry to remove.
	 * @param provider registry lookup used to update the serialized clipboard data
	 */
	void removeEntry(int index, HolderLookup.Provider provider);

	/**
	 * Clears the entire clip board.
	 *
	 * @param provider The provider.
	 */
	void clear(HolderLookup.Provider provider);
}

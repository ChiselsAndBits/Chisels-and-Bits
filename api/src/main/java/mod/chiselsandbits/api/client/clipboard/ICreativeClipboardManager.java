package mod.chiselsandbits.api.client.clipboard;

import mod.chiselsandbits.api.IChiselsAndBitsAPI;
import mod.chiselsandbits.api.item.multistate.IMultiStateItemStack;
import net.minecraft.core.HolderLookup;

import java.util.List;

public interface ICreativeClipboardManager
{

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
     * @param provider The provider.
     */
    void addEntry(IMultiStateItemStack multiStateItemStack, HolderLookup.Provider provider);

    /**
     * Removes an entry from the clipboard.
     *
     * @param index The index of the entry to remove.
     */
    void removeEntry(int index, HolderLookup.Provider provider);

    /**
     * Clears the entire clip board.
     *
     * @param provider The provider.
     */
    void clear(HolderLookup.Provider provider);
}

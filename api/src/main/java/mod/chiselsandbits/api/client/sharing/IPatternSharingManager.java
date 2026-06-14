package mod.chiselsandbits.api.client.sharing;

import com.mojang.datafixers.util.Either;
import mod.chiselsandbits.api.IChiselsAndBitsAPI;
import mod.chiselsandbits.api.item.multistate.IMultiStateItemStack;

/**
 * Central manager which handles sharing of patterns.
 * This manager only works on the client side, and does nothing on the server side.
 */
public interface IPatternSharingManager
{
	static IPatternSharingManager getInstance() {
		return IChiselsAndBitsAPI.getInstance().getPatternSharingManager();
	}

	/**
     * Exports the given pattern to disk.
     *
     * @param multiStateItemStack The multistate itemstack to export.
     * @param name The name of the export.
     */
	void exportPattern(IMultiStateItemStack multiStateItemStack, String name);

	/**
     * Imports the pattern from disk.
     *
     * @param name The name of the pattern to import.
     * @return The name of the export.
     */
	Either<IMultiStateItemStack, PatternIOException> importPattern(String name);
}

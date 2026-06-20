package mod.chiselsandbits.api.chiseling.eligibility;

import mod.chiselsandbits.api.IChiselsAndBitsAPI;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * An object which can manage the eligibility of chiseling of blocks, blockstates,
 * or itemstacks (which contain blocks eligible) for chiseling.
 */
public interface IEligibilityManager {
	static IEligibilityManager getInstance() {
		return IChiselsAndBitsAPI.getInstance().getEligibilityManager();
	}

	/**
     * Checks if a given block information can be chiseled or is already chiseled.
     *
     * @param blockInformation The given block information in question.
     *
     * @return True when chiselable or already chiseled.
     */
	default boolean canBeChiseled(@NotNull final BlockInformation blockInformation) {
		return analyse(blockInformation).canBeChiseled() || analyse(blockInformation).isAlreadyChiseled();
	}

	/**
     * Checks if a given {@link ItemStack} can be chiseled or is already chiseled.
     *
     * @param provider The given {@link ItemStack} in question.
     *
     * @return True when chiselable or already chiseled.
     */
	default boolean canBeChiseled(@NotNull final ItemStack provider)

	{
		return analyse(provider).canBeChiseled();
	}

	/**
     * Performs a chiselability analysis on the given block information.
     *
     * @param blockInformation The block information to analyze.
     *
     * @return The analysis result.
     */
	IEligibilityAnalysisResult analyse(@NotNull final BlockInformation blockInformation);

	/**
     * Performs a chiselability analysis on the given {@link ItemStack}.
     *
     * @param provider The {@link ItemStack} to analyze.
     *
     * @return The analysis result.
     */
	IEligibilityAnalysisResult analyse(@NotNull final ItemStack provider);
}

package mod.chiselsandbits.api.chiseling.eligibility;

import net.minecraft.network.chat.MutableComponent;

/**
 * The results of an eligibility analysis.
 */
public interface IEligibilityAnalysisResult {
	/**
     * Indicates if the requested object can be chiseled.
     *
     * @return True for chiselability.
     */
	boolean canBeChiseled();

	/**
     * Indicates if the requested object is already chiseled.
     *
     * @return True when already chiseled.
     */
	boolean isAlreadyChiseled();

	/**
     * The reason why a given eligibility result returned if it can be chiseled or not.
     *
     * @return The displayable reason.
     */
	MutableComponent getReason();
}

package mod.chiselsandbits.api.chiseling.eligibility;

import mod.chiselsandbits.api.IChiselsAndBitsAPI;

/**
 * Determines the additional eligibility options for a given platform.
 */
public interface IEligibilityOptions
{
	/**
     * The eligibility manager that is active for the current platform.
     * Allows for the modification of the eligibility analysis on the given platform.
     * Useful in case the platform defines different default classes for the processing logic.
     *
     * @return The platform's eligibility manager.
     */
	static IEligibilityOptions getInstance() {
		return IChiselsAndBitsAPI.getInstance().getEligibilityOptions();
	}

	/**
     * Indicates if the class that defines the explosion resistance is valid for compatibility.
     * @param explosionDefinitionClass The class that defines the explosion resistance.
     * @return True when the class is valid, false when not.
     */
	boolean isValidExplosionDefinitionClass(final Class<?> explosionDefinitionClass);
}

package mod.chiselsandbits.api.launch;

import mod.chiselsandbits.api.IChiselsAndBitsAPI;
import org.jetbrains.annotations.NotNull;

/**
 * Defines a manager that handles properties related to how the game is launcher
 * Important here is that these properties can effect launch and game behaviour and need to be
 * the same as the properties of the server you are connecting to.
 */
public interface ILaunchPropertyManager {
	/**
     * The current instance of the launch property manager.
     *
     * @return The current instance.
     */
	static ILaunchPropertyManager getInstance() {
		return IChiselsAndBitsAPI.getInstance().getLaunchPropertyManager();
	}

	/**
     * Retrieves the value of the launch property with the given key.
     *
     * @param key The key to get the launch property of.
     * @param defaultValue The default value if not configured.
     * @return The current value of the property.
     */
	@NotNull
	String get(final String key, final String defaultValue);
}

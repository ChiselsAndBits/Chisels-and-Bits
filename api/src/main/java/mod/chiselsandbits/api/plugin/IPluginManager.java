package mod.chiselsandbits.api.plugin;

import com.google.common.collect.ImmutableSet;
import mod.chiselsandbits.api.IChiselsAndBitsAPI;

import java.util.function.Consumer;

/**
 * The manager for plugins.
 */
public interface IPluginManager
{

    /**
     * The instance of the plugin manager.
     *
     * @return The plugin manager.
     */
    static IPluginManager getInstance() {
        return IChiselsAndBitsAPI.getInstance().getPluginManager();
    }

    /**
     * Gets the plugins.
     *
     * @return An immutable set with the plugins.
     */
    ImmutableSet<IChiselsAndBitsPlugin> getPlugins();

    /**
     * Runs a specific task on all available plugins.
     *
     * @param action The name of the action to execute, used in logging.
     * @param callback The task to run for each plugin.
     */
    void run(final String action, final Consumer<IChiselsAndBitsPlugin> callback);
}

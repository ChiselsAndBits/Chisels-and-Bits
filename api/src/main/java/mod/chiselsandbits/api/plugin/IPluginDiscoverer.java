package mod.chiselsandbits.api.plugin;

import com.mojang.logging.LogUtils;
import mod.chiselsandbits.api.IChiselsAndBitsAPI;
import mod.chiselsandbits.api.launch.ILaunchPropertyManager;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Stream;

/**
 * The platform plugin manager which can load the plugins for C{@literal &}B on a given platform.
 */
public interface IPluginDiscoverer
{

    /**
     * Gives access to the platform's plugin manager.
     *
     * @return The platform's plugin manager.
     */
    static IPluginDiscoverer getInstance()
    {
        return IChiselsAndBitsAPI.getInstance().getPluginDiscoverer();
    }

    /**
     * Loads the plugins.
     * Already has performed the instantiation logic and validation.
     *
     * @return The loaded plugins.
     */
    Stream<PluginData<IChiselsAndBitsPlugin>> loadPlugins();

    abstract class AbstractPluginDiscoverer implements IPluginDiscoverer
    {

        private static final Logger LOGGER = LogUtils.getLogger();

        @Override
        public Stream<PluginData<IChiselsAndBitsPlugin>> loadPlugins()
        {
            var serviceLoader = ServiceLoader.load(
                IChiselsAndBitsPlugin.class,
                this.getClass().getClassLoader()
            );

            return serviceLoader.stream()
                .filter(pluginProvider -> canLoad(pluginProvider.type()))
                .map(ServiceLoader.Provider::get)
                .map(PluginData::new);
        }

        private boolean canLoad(Class<? extends IChiselsAndBitsPlugin> potentialClass)
        {
            if (!potentialClass.isAnnotationPresent(ChiselsAndBitsPlugin.class))
            {
                return true;
            }

            var annotation = potentialClass.getAnnotation(ChiselsAndBitsPlugin.class);
            final List<String> requiredMods = Arrays.asList(annotation.requiredMods());
            if (!requiredMods.isEmpty())
            {
                if (requiredMods.stream().anyMatch(this::isModNotLoaded))
                {
                    LOGGER.info("Skipping: {} as plugin, its required mods: {} are not all available!", potentialClass.getSimpleName(), String.join(", ", requiredMods));
                    return false;
                }
            }

            final boolean isExperimental = annotation.isExperimental();
            if (isExperimental && !Boolean.parseBoolean(ILaunchPropertyManager.getInstance().get("plugins.experimental", "false")))
            {
                LOGGER.info("Skipping: {} as plugin, it is marked as experimental and those plugins are disabled by the configuration.", potentialClass.getSimpleName());
                return false;
            }

            return true;
        }

        protected abstract boolean isModNotLoaded(String modId);
    }
}

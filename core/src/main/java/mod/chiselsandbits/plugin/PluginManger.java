package mod.chiselsandbits.plugin;

import com.google.common.collect.ImmutableSet;
import com.sun.jna.platform.win32.WinNT;
import mod.chiselsandbits.api.plugin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class PluginManger implements IPluginManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginManger.class);
    private static final PluginManger INSTANCE = new PluginManger();

    public static PluginManger getInstance() {
        return INSTANCE;
    }

    private ImmutableSet<PluginData<IChiselsAndBitsPlugin>> pluginDatas = ImmutableSet.of();
    private ImmutableSet<IChiselsAndBitsPlugin> plugins = ImmutableSet.of();

    private PluginManger() {
    }

    @Override
    public ImmutableSet<IChiselsAndBitsPlugin> getPlugins() {
        return plugins;
    }

    @Override
    public void run(String action, Consumer<IChiselsAndBitsPlugin> callback) {
        LOGGER.warn("Running plugin action {}...", action);
        for (PluginData<IChiselsAndBitsPlugin> pluginData : pluginDatas) {
            LOGGER.info("  - {}", pluginData.plugin().getId());
            callback.accept(pluginData.plugin());
        }
        LOGGER.warn("Finished running plugin action {}.", action);
    }

    public void detect() {
        this.pluginDatas = ImmutableSet.copyOf(IPluginDiscoverer.getInstance().loadPlugins(
                ChiselsAndBitsPlugin.class,
                ChiselsAndBitsPlugin.Instance.class,
                IChiselsAndBitsPlugin.class,
                IChiselsAndBitsPlugin::getId
        ));
        this.plugins = ImmutableSet.copyOf(this.pluginDatas.stream().map(PluginData::plugin).collect(Collectors.toSet()));
    }
}

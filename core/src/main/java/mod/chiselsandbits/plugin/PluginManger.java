package mod.chiselsandbits.plugin;

import com.google.common.collect.ImmutableSet;
import com.mojang.logging.LogUtils;
import mod.chiselsandbits.api.plugin.*;
import org.slf4j.Logger;

import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class PluginManger implements IPluginManager {

    private static final Logger LOGGER = LogUtils.getLogger();
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
    public void run(Consumer<IChiselsAndBitsPlugin> callback) {
        for (PluginData<IChiselsAndBitsPlugin> pluginData : pluginDatas) {
            callback.accept(pluginData.plugin());
        }
    }

    public void detect() {
        LOGGER.info("Discovering plugins...");
        this.pluginDatas = ImmutableSet.copyOf(IPluginDiscoverer.getInstance().loadPlugins().toList());
        this.plugins = ImmutableSet.copyOf(this.pluginDatas.stream().map(PluginData::plugin).collect(Collectors.toSet()));
        LOGGER.info("Discovered {} plugins: {}", this.pluginDatas.size(), this.pluginDatas.stream().map(p -> p.plugin().getId()).collect(Collectors.joining(", ")));
    }
}

package mod.chiselsandbits.fabric.plugin;

import mod.chiselsandbits.api.plugin.IPluginDiscoverer;
import net.fabricmc.loader.api.FabricLoader;

public final class FabricPluginDiscoverer extends IPluginDiscoverer.AbstractPluginDiscoverer
{
    private static final FabricPluginDiscoverer INSTANCE = new FabricPluginDiscoverer();

    public static FabricPluginDiscoverer getInstance()
    {
        return INSTANCE;
    }

    private FabricPluginDiscoverer()
    {
    }

    @Override
    protected boolean isModNotLoaded(final String modId)
    {
        return !FabricLoader.getInstance().isModLoaded(modId);
    }
}

package mod.chiselsandbits.forge.platform;

import mod.chiselsandbits.api.plugin.IPluginDiscoverer;
import net.neoforged.fml.ModList;

public final class ForgePluginDiscoverer extends IPluginDiscoverer.AbstractPluginDiscoverer {
    private static final ForgePluginDiscoverer INSTANCE = new ForgePluginDiscoverer();

    public static ForgePluginDiscoverer getInstance() {
        return INSTANCE;
    }

    private ForgePluginDiscoverer() {
    }

    @Override
    protected boolean isModNotLoaded(final String modId)
    {
        return !ModList.get().isLoaded(modId);
    }
}

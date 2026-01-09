package mod.chiselsandbits.plugins.wiki.exporter.tags;

import com.communi.suggestu.scena.core.event.IGameEvents;
import com.mojang.logging.LogUtils;
import mod.chiselsandbits.api.plugin.ChiselsAndBitsPlugin;
import mod.chiselsandbits.api.plugin.IChiselsAndBitsPlugin;
import net.minecraft.server.MinecraftServer;
import org.sinytra.wiki.exporter.WikiDataExporter;
import org.slf4j.Logger;

@ChiselsAndBitsPlugin(requiredMods = {"wiki_exporter"})
public class TagExporterEntrypoint implements IChiselsAndBitsPlugin
{
    private static final Logger LOGGER = LogUtils.getLogger();

    private static MinecraftServer server;

    @Override
    public String getId()
    {
        return "tag-exporter";
    }

    @Override
    public void onConstruction()
    {
        LOGGER.info("Registered the Wiki Exporter plugin!");
        IGameEvents.getInstance().getServerStartedEvent()
            .register(minecraftServer -> {
                LOGGER.info("Starting the tag exporter.");
                setServer(minecraftServer);
                WikiDataExporter.runModule(TagExporterFactory.NAME);
            });
    }

    static MinecraftServer getServer()
    {
        if (server == null)
            throw new IllegalStateException("Entrypoint was not invoked properly!");

        return server;
    }

    private static void setServer(final MinecraftServer server)
    {
        TagExporterEntrypoint.server = server;
    }
}

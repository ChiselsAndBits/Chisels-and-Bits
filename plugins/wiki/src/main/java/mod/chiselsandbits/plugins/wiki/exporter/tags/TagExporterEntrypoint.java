package mod.chiselsandbits.plugins.wiki.exporter.tags;

import com.communi.suggestu.scena.core.event.IGameEvents;
import mod.chiselsandbits.api.plugin.ChiselsAndBitsPlugin;
import mod.chiselsandbits.api.plugin.IChiselsAndBitsPlugin;
import net.minecraft.server.MinecraftServer;
import org.sinytra.wiki.exporter.WikiDataExporter;

@ChiselsAndBitsPlugin
public class TagExporterEntrypoint implements IChiselsAndBitsPlugin
{
    private static MinecraftServer server;

    @Override
    public String getId()
    {
        return "tag-exporter";
    }

    @Override
    public void onConstruction()
    {
        IGameEvents.getInstance().getServerStartedEvent()
            .register(minecraftServer -> {
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

package mod.chiselsandbits.client.reloading;

import com.communi.suggestu.scena.core.client.event.IRegisterClientReloadListenersEvent;
import com.google.common.collect.Sets;
import mod.chiselsandbits.api.reloading.ICacheClearingHandler;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.client.besr.BitStorageBESR;
import mod.chiselsandbits.client.besr.ChiseledPrinterBESR;
import mod.chiselsandbits.client.model.block.ChiseledBlockStateModelManager;
import mod.chiselsandbits.client.model.face.FaceManager;
import mod.chiselsandbits.client.model.item.BitBlockBakedModelManager;
import mod.chiselsandbits.reloading.DataReloadingResourceManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class ClientResourceReloadingManager implements ResourceManagerReloadListener
{
    private static final Logger                         LOGGER   = LogManager.getLogger();
    private static final ClientResourceReloadingManager INSTANCE = new ClientResourceReloadingManager();

    public static ClientResourceReloadingManager getInstance()
    {
        return INSTANCE;
    }

    private final Set<ICacheClearingHandler> cacheClearingHandlers = Sets.newConcurrentHashSet();

    private ClientResourceReloadingManager()
    {
    }

    @Override
    public void onResourceManagerReload(final @NotNull ResourceManager manager)
    {
        clearCaches();
    }

    public ClientResourceReloadingManager registerCacheClearer(final ICacheClearingHandler cacheClearingHandler)
    {
        this.cacheClearingHandlers.add(cacheClearingHandler);
        return this;
    }

    public static void setup(final IRegisterClientReloadListenersEvent.Registrar registrar)
    {
        LOGGER.info("Setting up client reloading resource manager.");
        registrar.addListener(
            ResourceLocation.fromNamespaceAndPath(
                Constants.MOD_ID,
                "client_cache_clear"
            ),
            getInstance()
        );

        ClientResourceReloadingManager.getInstance()
            .registerCacheClearer(BitStorageBESR::clearCache)
            .registerCacheClearer(ChiseledPrinterBESR::clearCache)
            .registerCacheClearer(BitBlockBakedModelManager.getInstance()::clearCache)
            .registerCacheClearer(ChiseledBlockStateModelManager.getInstance()::clearCache)
            .registerCacheClearer(FaceManager.getInstance()::clearCache);
    }

    public void clearCaches()
    {
        LOGGER.info("Resetting client caches");
        cacheClearingHandlers.forEach(ICacheClearingHandler::clear);

        DataReloadingResourceManager.getInstance().clearCaches();
    }
}

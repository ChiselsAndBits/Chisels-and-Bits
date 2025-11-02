package mod.chiselsandbits.client.reloading;

import com.google.common.collect.Sets;
import mod.chiselsandbits.api.reloading.ICacheClearingHandler;
import mod.chiselsandbits.client.besr.BitStorageBESR;
import mod.chiselsandbits.client.model.baked.bit.BitBlockBakedModelManager;
import mod.chiselsandbits.client.model.block.ChiseledBlockStateModelManager;
import mod.chiselsandbits.client.model.face.FaceManager;
import mod.chiselsandbits.reloading.DataReloadingResourceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class ClientResourceReloadingManager implements ResourceManagerReloadListener
{
    private static final Logger LOGGER = LogManager.getLogger();
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

    public ClientResourceReloadingManager registerCacheClearer(final ICacheClearingHandler cacheClearingHandler) {
        this.cacheClearingHandlers.add(cacheClearingHandler);
        return this;
    }

    public static void setup() {
        LOGGER.info("Setting up client reloading resource manager.");
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        if (resourceManager instanceof ReloadableResourceManager reloadableResourceManager) {
            reloadableResourceManager.registerReloadListener(ClientResourceReloadingManager.getInstance());
        }

        ClientResourceReloadingManager.getInstance()
          .registerCacheClearer(BitStorageBESR::clearCache)
          .registerCacheClearer(BitBlockBakedModelManager.getInstance()::clearCache)
          .registerCacheClearer(ChiseledBlockStateModelManager.getInstance()::clearCache)
          .registerCacheClearer(FaceManager.getInstance()::clearCache);
    }

    public void clearCaches() {
        LOGGER.info("Resetting client caches");
        cacheClearingHandlers.forEach(ICacheClearingHandler::clear);

        DataReloadingResourceManager.getInstance().clearCaches();
    }
}

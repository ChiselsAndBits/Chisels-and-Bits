package mod.chiselsandbits.client.reloading;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.communi.suggestu.scena.core.client.event.IRegisterClientReloadListenersEvent;
import com.google.common.collect.Sets;
import mod.chiselsandbits.api.reloading.ICacheClearingHandler;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.client.besr.BitStorageBESR;
import mod.chiselsandbits.client.besr.ChiseledPrinterBESR;
import mod.chiselsandbits.client.model.block.ChiseledBlockStateModelManager;
import mod.chiselsandbits.client.model.face.FaceManager;
import mod.chiselsandbits.client.model.data.ChiseledBlockModelDataExecutor;
import mod.chiselsandbits.client.model.item.BitBlockBakedModelManager;
import mod.chiselsandbits.reloading.DataReloadingResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

public class ClientResourceReloadingManager implements ResourceManagerReloadListener {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final ClientResourceReloadingManager INSTANCE = new ClientResourceReloadingManager();
	private static final AtomicLong GENERATION = new AtomicLong();

	public static long generation() {
		return GENERATION.get();
	}

	public static ClientResourceReloadingManager getInstance() {
		return INSTANCE;
	}

	private final Set<ICacheClearingHandler> cacheClearingHandlers = Sets.newConcurrentHashSet();

	private ClientResourceReloadingManager() {
	}

	@Override
	public void onResourceManagerReload(final ResourceManager manager) {
		clearCaches();
		ChiseledBlockModelDataExecutor.requestReloadUpdates();
	}

	public ClientResourceReloadingManager registerCacheClearer(final ICacheClearingHandler cacheClearingHandler) {
		this.cacheClearingHandlers.add(cacheClearingHandler);
		return this;
	}

	public static void setup(final IRegisterClientReloadListenersEvent.Registrar registrar) {
		LOGGER.info("Setting up client reloading resource manager.");
		registrar.addListener(
				Identifier.fromNamespaceAndPath(
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

	public void clearCaches() {
		GENERATION.incrementAndGet();
		LOGGER.info("Resetting client caches");
		cacheClearingHandlers.forEach(ICacheClearingHandler::clear);

		DataReloadingResourceManager.getInstance().clearCaches();
	}
}

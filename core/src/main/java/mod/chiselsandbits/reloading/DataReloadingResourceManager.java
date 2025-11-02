package mod.chiselsandbits.reloading;

import com.google.common.collect.Sets;
import mod.chiselsandbits.aabb.AABBManager;
import mod.chiselsandbits.api.reloading.ICacheClearingHandler;
import mod.chiselsandbits.change.ChangeTrackerManger;
import mod.chiselsandbits.chiseling.LocalChiselingContextCache;
import mod.chiselsandbits.voxelshape.VoxelShapeManager;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class DataReloadingResourceManager implements PreparableReloadListener {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final DataReloadingResourceManager INSTANCE = new DataReloadingResourceManager();

    public static DataReloadingResourceManager getInstance() {
        return INSTANCE;
    }

    private final Set<ICacheClearingHandler> cacheClearingHandlers = Sets.newConcurrentHashSet();

    private DataReloadingResourceManager() {
    }

    public DataReloadingResourceManager registerCacheClearer(final ICacheClearingHandler cacheClearingHandler) {
        this.cacheClearingHandlers.add(cacheClearingHandler);
        return this;
    }

    public void onResourceManagerReload() {
        clearCaches();
    }

    public void setup() {
        this.cacheClearingHandlers.clear();

        registerCacheClearer(AABBManager.getInstance()::clearCache)
                .registerCacheClearer(VoxelShapeManager.getInstance()::clearCache)
                .registerCacheClearer(LocalChiselingContextCache.getInstance()::clearCache)
                .registerCacheClearer(ChangeTrackerManger.getInstance()::clearCache);
    }

    @Override
    public CompletableFuture<Void> reload(
        final SharedState sharedState,
        final Executor exectutor,
        final PreparationBarrier barrier,
        final Executor applyExectutor)
    {
        return barrier.wait(Unit.INSTANCE).thenRunAsync(() -> {
            ProfilerFiller profilerfiller = Profiler.get();
            profilerfiller.startTick();
            profilerfiller.push("C&B Data reload");
            this.onResourceManagerReload();
            profilerfiller.pop();
            profilerfiller.endTick();
        }, applyExectutor);
    }

    public void clearCaches() {
        LOGGER.info("Resetting common caches");
        this.cacheClearingHandlers.forEach(ICacheClearingHandler::clear);
    }
}

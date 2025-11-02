package mod.chiselsandbits.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.MapCodec;
import mod.chiselsandbits.ChiselsAndBits;
import mod.chiselsandbits.api.config.ICommonConfiguration;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

final class MultiThreadAwareStorageEngine<TPayload> implements IMultiThreadedStorageEngine<TPayload>
{
    private static ExecutorService saveService;

    private static synchronized void ensureThreadPoolSetup() {
        if (saveService == null) {
            final ClassLoader classLoader = ChiselsAndBits.class.getClassLoader();
            final AtomicInteger genericThreadCounter = new AtomicInteger();
            saveService = Executors.newFixedThreadPool(
              ICommonConfiguration.getInstance().getBlockSaveThreadCount().get(),
              runnable -> {
                  final Thread thread = new Thread(runnable);
                  thread.setContextClassLoader(classLoader);
                  thread.setName(String.format("Chisels and Bits Block save handler #%s", genericThreadCounter.incrementAndGet()));
                  thread.setDaemon(true);
                  return thread;
              }
            );
        }
    }

    private final Codec<TPayload> internalEngine;

    MultiThreadAwareStorageEngine(final Codec<TPayload> internalEngine) {
        this.internalEngine = internalEngine;
    }

    @SuppressWarnings("deprecation")
    @Override
    public CompletableFuture<Void> encodeAsync(TPayload payload, ValueOutput output) {
        ensureThreadPoolSetup();
        return CompletableFuture.runAsync(() -> {
            output.store(MapCodec.assumeMapUnsafe(internalEngine), payload);
        }, saveService);
    }

    @SuppressWarnings("deprecation")
    @Override
    public CompletableFuture<TPayload> decodeAsync(final ValueInput valueInput) {
        ensureThreadPoolSetup();
        return CompletableFuture.supplyAsync(() -> valueInput.read(MapCodec.assumeMapUnsafe(internalEngine))
            .orElseThrow(() -> new IllegalStateException("Failed to decode payload!")), saveService);
    }
}

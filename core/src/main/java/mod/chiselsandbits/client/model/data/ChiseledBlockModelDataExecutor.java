package mod.chiselsandbits.client.model.data;

import com.communi.suggestu.scena.core.client.models.data.IModelDataBuilder;
import com.communi.suggestu.scena.core.client.models.data.IModelDataManager;
import com.mojang.logging.LogUtils;
import mod.chiselsandbits.ChiselsAndBits;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.config.IClientConfiguration;
import mod.chiselsandbits.api.multistate.accessor.IAreaAccessor;
import mod.chiselsandbits.api.neighborhood.IBlockNeighborhood;
import mod.chiselsandbits.api.neighborhood.IBlockNeighborhoodBuilder;
import mod.chiselsandbits.api.variant.state.IStateVariant;
import mod.chiselsandbits.api.variant.state.IStateVariantManager;
import mod.chiselsandbits.block.entities.ChiseledBlockEntity;
import mod.chiselsandbits.client.model.block.ChiseledBlockStateModelManager;
import mod.chiselsandbits.client.model.information.ChiseledBlockModelInformation;
import mod.chiselsandbits.registrars.ModModelProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ChiseledBlockModelDataExecutor {
    private static ExecutorService recalculationService;

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void updateModelDataCore(final ChiseledBlockEntity tileEntity, final Runnable onCompleteCallback) {
        ensureThreadPoolSetup();

        final IBlockNeighborhood neighborhood = IBlockNeighborhoodBuilder.getInstance().build(
                direction -> {
                    final BlockState state = Objects.requireNonNull(tileEntity.getLevel()).getBlockState(tileEntity.getBlockPos().offset(direction.getUnitVec3i()));
                    final Optional<IStateVariant> additionalStateInfo = IStateVariantManager.getInstance().getStateVariant(
                            state,
                            Optional.ofNullable(tileEntity.getLevel().getBlockEntity(tileEntity.getBlockPos().offset(direction.getUnitVec3i())))
                    );

                    return new BlockInformation(state, additionalStateInfo);
                },
                direction -> {
                    final BlockEntity otherTileEntity = Objects.requireNonNull(tileEntity.getLevel()).getBlockEntity(tileEntity.getBlockPos().offset(direction.getUnitVec3i()));
                    if (otherTileEntity instanceof IAreaAccessor) {
                        return (IAreaAccessor) otherTileEntity;
                    }

                    return null;
                }
        );


        CompletableFuture.supplyAsync(() -> {
                    final ChiseledBlockModelInformation chiseledBlockModelInformation =
                        ChiseledBlockStateModelManager.getInstance().get(
                            tileEntity.getWorld(),
                            tileEntity.blockPos(),
                            tileEntity,
                            neighborhood
                        );

                    return IModelDataBuilder.create()
                            .withInitial(
                                    ModModelProperties.MODEL, chiseledBlockModelInformation
                            )
                            .build();
                }, recalculationService)
                .thenAcceptAsync(tileEntity::setModelData, recalculationService)
                .thenRunAsync(onCompleteCallback, recalculationService)
                .thenRunAsync(() -> {
                    try {
                        IModelDataManager.getInstance().requestModelDataRefresh(tileEntity);
                        if (tileEntity.getLevel() != null) {
                            tileEntity.getLevel().sendBlockUpdated(
                                    tileEntity.getBlockPos(),
                                    tileEntity.getBlockState(),
                                    tileEntity.getBlockState(),
                                    8
                            );
                        }
                    } catch (Exception ignored) {
                    }
                }, Minecraft.getInstance())
                .exceptionally(throwable -> {
                    LOGGER.error("Failed to update model data for chiseled block entity", throwable);
                    return null;
                });
    }

    private static synchronized void ensureThreadPoolSetup() {
        if (recalculationService == null) {
            final ClassLoader classLoader = ChiselsAndBits.class.getClassLoader();
            final AtomicInteger genericThreadCounter = new AtomicInteger();
            recalculationService = Executors.newFixedThreadPool(
                    IClientConfiguration.getInstance().getModelBuildingThreadCount().get(),
                    runnable -> {
                        final Thread thread = new Thread(runnable);
                        thread.setContextClassLoader(classLoader);
                        thread.setName(String.format("Chisels and Bits Model builder #%s", genericThreadCounter.incrementAndGet()));
                        thread.setDaemon(true);
                        return thread;
                    }
            );
        }
    }
}

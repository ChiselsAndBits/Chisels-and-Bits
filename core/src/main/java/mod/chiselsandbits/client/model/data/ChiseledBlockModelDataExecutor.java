package mod.chiselsandbits.client.model.data;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Collections;
import java.util.WeakHashMap;
import java.util.List;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.communi.suggestu.scena.core.client.models.data.IModelDataBuilder;
import com.communi.suggestu.scena.core.client.models.data.IModelDataManager;
import com.communi.suggestu.scena.core.client.rendering.terrain.TerrainGeometryGate;
import com.communi.suggestu.scena.core.util.SingleBlockBlockAndTintGetter;
import com.mojang.logging.LogUtils;
import mod.chiselsandbits.ChiselsAndBits;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.config.IClientConfiguration;
import mod.chiselsandbits.api.multistate.accessor.IAreaAccessor;
import mod.chiselsandbits.api.multistate.accessor.ISingleBlockAxisAlignedAreaAccessor;
import mod.chiselsandbits.api.neighborhood.IBlockNeighborhood;
import mod.chiselsandbits.api.neighborhood.IBlockNeighborhoodBuilder;
import mod.chiselsandbits.api.variant.state.IStateVariant;
import mod.chiselsandbits.api.variant.state.IStateVariantManager;
import mod.chiselsandbits.block.entities.ChiseledBlockEntity;
import mod.chiselsandbits.client.model.block.ChiseledBlockStateModelManager;
import mod.chiselsandbits.client.model.block.ChiseledTerrainRenderData;
import mod.chiselsandbits.client.model.builder.ChiseledBlockModelInformationBuilder;
import mod.chiselsandbits.client.model.information.ChiseledBlockModelInformation;
import mod.chiselsandbits.client.model.information.ChiseledBlockModelCacheKey;
import mod.chiselsandbits.client.model.meshing.ChiseledTerrainMetadata;
import mod.chiselsandbits.client.model.meshing.ChiseledRenderSnapshot;
import mod.chiselsandbits.client.model.meshing.DirectTerrainGeometryBuilder;
import mod.chiselsandbits.client.model.meshing.NeighbourBoundary;
import mod.chiselsandbits.client.model.meshing.RectangleSubsetPreflight;
import mod.chiselsandbits.block.ChiseledBlock;
import mod.chiselsandbits.client.reloading.ClientResourceReloadingManager;
import mod.chiselsandbits.debug.bench.BenchmarkCollector;
import mod.chiselsandbits.registrars.ModModelProperties;
import org.slf4j.Logger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ChunkPos;

public class ChiseledBlockModelDataExecutor {
	private static ExecutorService recalculationService;

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String TERRAIN_DIAGNOSTIC_PROPERTY = "chiselsandbits.debugNativeTerrainGeometry";
	private static final Set<String> REPORTED_SUBSET_REJECTIONS = ConcurrentHashMap.newKeySet();
	private static final Set<ChiseledBlockEntity> EXPERIMENTAL_ENTITIES = Collections.newSetFromMap(new WeakHashMap<>());
	private static final Set<ChunkTransition> CHUNK_TRANSITIONS = new HashSet<>();
	private static volatile boolean reloadUpdatesRequested;

	public static void onChunkTransition(final ClientLevel level, final ChunkPos pos) {
		if (!TerrainGeometryGate.isEnabled()) {
			return;
		}
		final Minecraft minecraft = Minecraft.getInstance();
		if (!minecraft.isSameThread()) {
			minecraft.execute(() -> onChunkTransition(level, pos));
			return;
		}
		CHUNK_TRANSITIONS.add(new ChunkTransition(level, pos));
	}

	public static void requestReloadUpdates() {
		reloadUpdatesRequested = true;
	}

	public static void onClientTick() {
		if (!TerrainGeometryGate.isEnabled()) {
			CHUNK_TRANSITIONS.clear();
			return;
		}
		final boolean reload = reloadUpdatesRequested;
		reloadUpdatesRequested = false;
		final Set<ChunkTransition> transitions = Set.copyOf(CHUNK_TRANSITIONS);
		CHUNK_TRANSITIONS.clear();
		if (!reload && transitions.isEmpty()) {
			return;
		}
		final ClientLevel level = Minecraft.getInstance().level;
		for (final ChiseledBlockEntity entity : List.copyOf(EXPERIMENTAL_ENTITIES)) {
			if (!entity.isRemoved() && entity.getLevel() == level
					&& (reload || bordersTransition(entity.getBlockPos(), level, transitions))) {
				updateModelDataCore(entity, () -> {});
			}
		}
	}

	private static boolean bordersTransition(
			final BlockPos pos, final ClientLevel level, final Set<ChunkTransition> transitions
	) {
		final ChunkPos own = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
		for (final Direction direction : Direction.Plane.HORIZONTAL) {
			final BlockPos neighbour = pos.relative(direction);
			final ChunkPos adjacent = new ChunkPos(neighbour.getX() >> 4, neighbour.getZ() >> 4);
			if (!own.equals(adjacent) && transitions.contains(new ChunkTransition(level, adjacent))) {
				return true;
			}
		}
		return false;
	}

	private record ChunkTransition(ClientLevel level, ChunkPos pos) {
	}

	public static void updateModelDataCore(final ChiseledBlockEntity tileEntity, final Runnable onCompleteCallback) {
		if (!TerrainGeometryGate.isEnabled()) {
			updateLegacyModelData(tileEntity, onCompleteCallback);
			return;
		}

		final long generation = tileEntity.beginExperimentalModelDataUpdate();
		final Minecraft minecraft = Minecraft.getInstance();
		if (!minecraft.isSameThread()) {
			minecraft.execute(() -> updateExperimentalModelData(tileEntity, onCompleteCallback, generation));
			return;
		}

		updateExperimentalModelData(tileEntity, onCompleteCallback, generation);
	}

	private static void updateLegacyModelData(final ChiseledBlockEntity tileEntity, final Runnable onCompleteCallback) {
		updateLegacyModelData(tileEntity, onCompleteCallback, 0L);
	}

	private static void updateLegacyModelData(
			final ChiseledBlockEntity tileEntity,
			final Runnable onCompleteCallback,
			final long experimentalGeneration
	) {
		ensureThreadPoolSetup();
		final long startNanos = System.nanoTime();
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
				});

		final CompletableFuture<com.communi.suggestu.scena.core.client.models.data.IBlockModelData> modelDataFuture = CompletableFuture.supplyAsync(() -> {
			final ChiseledBlockModelInformation chiseledBlockModelInformation =
					ChiseledBlockStateModelManager.getInstance().get(
								new SingleBlockBlockAndTintGetter.Builder()
								.withBlockEntity(() -> tileEntity)
								.withBlockState(tileEntity.getBlockState())
								.withSource(tileEntity.getWorld())
								.withPos(tileEntity.getBlockPos())
								.createSingleBlockBlockAndTintGetter(),
								tileEntity.blockPos(),
								tileEntity,
								neighborhood
					);

			return IModelDataBuilder.create()
					.withInitial(
							ModModelProperties.MODEL, chiseledBlockModelInformation
					)
					.build();
		}, recalculationService);

		if (experimentalGeneration == 0L) {
			modelDataFuture
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

							BenchmarkCollector.getInstance().onChiselUpdateVisible(startNanos);
						} catch (Exception ignored) {
						}
					},
							Minecraft.getInstance())
					.exceptionally(throwable -> {
						LOGGER.error("Failed to update model data for chiseled block entity", throwable);
						return null;
					});
			return;
		}

		final BlockPos pos = tileEntity.getBlockPos().immutable();
		final BlockState blockState = tileEntity.getBlockState();
		modelDataFuture
				.thenAcceptAsync(modelData -> {
					final boolean published = tileEntity.publishExperimentalModelData(experimentalGeneration, modelData);
					runCompletionCallback(onCompleteCallback, pos);
					if (published) {
						refreshExperimentalModelData(tileEntity, pos, blockState, startNanos);
					}
				}, Minecraft.getInstance())
				.exceptionally(throwable -> {
					LOGGER.error("Failed to build generation-guarded fallback model data at {}", pos, throwable);
					runCompletionCallback(onCompleteCallback, pos);
					return null;
				});
	}

	private static void updateExperimentalModelData(
			final ChiseledBlockEntity tileEntity,
			final Runnable onCompleteCallback,
			final long generation
	) {
		final Minecraft minecraft = Minecraft.getInstance();
		if (!tileEntity.isExperimentalModelDataGenerationCurrent(generation)) {
			runCompletionCallback(onCompleteCallback, tileEntity.getBlockPos());
			return;
		}

		if (!minecraft.isSameThread()) {
			minecraft.execute(() -> updateExperimentalModelData(tileEntity, onCompleteCallback, generation));
			return;
		}

		ensureThreadPoolSetup();
		final long startNanos = System.nanoTime();
		final BlockPos pos = tileEntity.getBlockPos().immutable();
		EXPERIMENTAL_ENTITIES.add(tileEntity);
		final long resourceGeneration = ClientResourceReloadingManager.generation();
		final BlockState blockState = tileEntity.getBlockState();
		final BlockAndTintGetter blockAndTintGetter = new SingleBlockBlockAndTintGetter.Builder()
				.withBlockEntity(() -> tileEntity)
				.withBlockState(blockState)
				.withSource(tileEntity.getWorld())
				.withPos(pos)
				.createSingleBlockBlockAndTintGetter();
		final ISingleBlockAxisAlignedAreaAccessor areaSnapshot;
		final IBlockNeighborhood neighborhood;
		try {
			if (!(tileEntity.createSnapshot() instanceof ISingleBlockAxisAlignedAreaAccessor snapshot)) {
				LOGGER.warn("Native terrain snapshot is not a single-block accessor at {}; retaining legacy model data", pos);
				updateLegacyModelData(tileEntity, onCompleteCallback, generation);
				return;
			}

			areaSnapshot = snapshot;
			neighborhood = captureNeighborhood(tileEntity);
		} catch (final RuntimeException exception) {
			LOGGER.error("Failed to capture native terrain snapshot at {}; retaining legacy model data", pos, exception);
			updateLegacyModelData(tileEntity, onCompleteCallback, generation);
			return;
		}

		CompletableFuture.supplyAsync(() -> {
			final ChiseledBlockModelCacheKey key = ChiseledBlockStateModelManager.getInstance()
					.keyFor(blockAndTintGetter, pos, areaSnapshot, neighborhood);
			final IModelDataBuilder modelDataBuilder = IModelDataBuilder.create();
			boolean direct = false;
			final ChiseledRenderSnapshot renderSnapshot;
			final NeighbourBoundary boundary;
			try {
				renderSnapshot = ChiseledRenderSnapshot.of(areaSnapshot);
				boundary = captureSelfCullBoundaries(renderSnapshot, neighborhood);
			} catch (final RuntimeException exception) {
				LOGGER.error("Failed to capture boundary geometry at {}; retaining legacy model data", pos, exception);
				return modelDataBuilder.withInitial(ModModelProperties.MODEL,
						ChiseledBlockStateModelManager.getInstance().get(blockAndTintGetter, pos, areaSnapshot, neighborhood))
						.build();
			}
			final String sourceModelRejection = RectangleSubsetPreflight.sourceModelRejectionReason(key);
			if (sourceModelRejection != null) {
				reportRectangleSubsetRejection(pos, "source-model-key", sourceModelRejection);
			} else {
				try {
					if (!RectangleSubsetPreflight.supportsAllMaterials(renderSnapshot)) {
						reportRectangleSubsetRejection(pos, "material", "one or more source materials are outside the solid/cutout candidates");
					} else {
						final ChiseledTerrainMetadata metadata = new ChiseledBlockModelInformationBuilder(
								areaSnapshot, key, blockAndTintGetter, pos).describeTerrainSources(renderSnapshot);
						if (metadata == null) {
							reportRectangleSubsetRejection(pos, "source-metadata", "source faces do not support independent terrain metadata");
						} else {
							final DirectTerrainGeometryBuilder.BuildResult result = DirectTerrainGeometryBuilder.buildWithoutSealedAir(
									metadata,
									renderSnapshot,
									blockAndTintGetter,
									pos,
									boundary
							);
							if (!result.supported()) {
								reportRectangleSubsetRejection(
										pos,
										"direct-geometry",
										Objects.requireNonNullElse(result.rejection(), "unsupported geometry")
								);
							} else {
								final var publication = new ChiseledTerrainRenderData(
										metadata, Objects.requireNonNull(result.geometry()), Objects.requireNonNull(result.bakedGeometry()));
								modelDataBuilder.withInitial(ModModelProperties.TERRAIN_RENDER_DATA, publication);
								modelDataBuilder.withInitial(ModModelProperties.TERRAIN_GEOMETRY, publication.geometry());
								direct = true;
							}
						}
					}
				} catch (final RuntimeException exception) {
					LOGGER.error("Failed to build experimental rectangle model at {}; retaining legacy model data", pos, exception);
				}
			}

			if (!direct) {
				final ChiseledBlockModelInformation fallback = boundary == NeighbourBoundary.NONE
						? ChiseledBlockStateModelManager.getInstance().get(blockAndTintGetter, pos, areaSnapshot, neighborhood)
						: new ChiseledBlockModelInformationBuilder(areaSnapshot, key, blockAndTintGetter, pos).build(boundary);
				modelDataBuilder.withInitial(ModModelProperties.MODEL, fallback);
			}
			return modelDataBuilder.build();
		}, recalculationService)
				.thenAcceptAsync(modelData -> {
					if (tileEntity.isExperimentalModelDataGenerationCurrent(generation) && !tileEntity.isRemoved()
							&& (resourceGeneration != ClientResourceReloadingManager.generation()
									|| tileEntity.getBlockState() != blockState || !neighborhood.equals(captureNeighborhood(tileEntity)))) {
						updateModelDataCore(tileEntity, onCompleteCallback);
						return;
					}
					final boolean published = tileEntity.publishExperimentalModelData(generation, modelData);
					runCompletionCallback(onCompleteCallback, pos);

					if (!published) {
						return;
					}

					refreshExperimentalModelData(tileEntity, pos, blockState, startNanos);
				}, minecraft)
				.exceptionally(throwable -> {
					LOGGER.error("Failed to update experimental model data for chiseled block entity at {}", pos, throwable);
					runCompletionCallback(onCompleteCallback, pos);
					return null;
				});
	}

	private static IBlockNeighborhood captureNeighborhood(final ChiseledBlockEntity tileEntity) {
		final var level = Objects.requireNonNull(tileEntity.getLevel());
		final BlockPos pos = tileEntity.getBlockPos();
		return IBlockNeighborhoodBuilder.getInstance().build(
				direction -> {
					final BlockPos neighbourPos = pos.relative(direction);
					final BlockState state = level.getBlockState(neighbourPos);
					return new BlockInformation(state, IStateVariantManager.getInstance().getStateVariant(
							state, Optional.ofNullable(level.getBlockEntity(neighbourPos))
					));
				},
				direction -> {
					final BlockEntity neighbour = level.getBlockEntity(pos.relative(direction));
					return neighbour instanceof IAreaAccessor accessor ? accessor.createSnapshot() : null;
				}
		);
	}

	private static NeighbourBoundary captureSelfCullBoundaries(
			final ChiseledRenderSnapshot snapshot, final IBlockNeighborhood neighborhood
	) {
		if (snapshot.materials().stream().noneMatch(material ->
				material.blockState().getBlock() instanceof net.minecraft.world.level.block.LiquidBlock
						|| material.blockState().getBlock() instanceof net.minecraft.world.level.block.TransparentBlock)) {
			return NeighbourBoundary.NONE;
		}
		NeighbourBoundary boundary = NeighbourBoundary.NONE;
		for (final Direction direction : Direction.values()) {
			if (neighborhood.getBlockInformation(direction).blockState().getBlock() instanceof ChiseledBlock
					&& neighborhood.getAreaAccessor(direction) instanceof ISingleBlockAxisAlignedAreaAccessor neighbour) {
				boundary = boundary.withAccessor(direction, neighbour);
			}
		}
		return boundary;
	}

	private static void reportRectangleSubsetRejection(final BlockPos pos, final String category, final String reason) {
		if (!Boolean.getBoolean(TERRAIN_DIAGNOSTIC_PROPERTY) || !REPORTED_SUBSET_REJECTIONS.add(category)) {
			return;
		}

		LOGGER.info("C&B terrain diagnostic: rectangle subset rejected at {}: {}", pos.toShortString(), reason);
	}

	private static void runCompletionCallback(final Runnable callback, final BlockPos pos) {
		try {
			callback.run();
		} catch (final RuntimeException exception) {
			LOGGER.error("Model-data completion callback failed at {}", pos, exception);
		}
	}

	private static void refreshExperimentalModelData(
			final ChiseledBlockEntity tileEntity,
			final BlockPos pos,
			final BlockState blockState,
			final long startNanos
	) {
		try {
			IModelDataManager.getInstance().requestModelDataRefresh(tileEntity);
			if (tileEntity.getLevel() != null) {
				tileEntity.getLevel().sendBlockUpdated(pos, blockState, blockState, 8);
			}

			BenchmarkCollector.getInstance().onChiselUpdateVisible(startNanos);
		} catch (final Exception exception) {
			LOGGER.error("Failed to refresh model data at {}", pos, exception);
		}
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
					});
		}
	}
}

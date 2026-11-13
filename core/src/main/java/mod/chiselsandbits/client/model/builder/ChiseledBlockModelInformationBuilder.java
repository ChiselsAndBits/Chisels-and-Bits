package mod.chiselsandbits.client.model.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicLong;

import com.communi.suggestu.scena.core.client.models.IModelManager;
import com.communi.suggestu.scena.core.client.models.processing.BakedQuadBuilder;
import com.communi.suggestu.scena.core.client.models.processing.DeconstructedModelPartComponent;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.multistate.StateEntrySize;
import mod.chiselsandbits.api.multistate.accessor.IAreaAccessor;
import mod.chiselsandbits.api.multistate.accessor.IStateEntryInfo;
import mod.chiselsandbits.api.multistate.accessor.ISingleBlockAxisAlignedAreaAccessor;
import mod.chiselsandbits.api.profiling.IProfilerSection;
import mod.chiselsandbits.api.util.VectorUtils;
import mod.chiselsandbits.client.model.information.ChiseledBlockModelCacheKey;
import mod.chiselsandbits.client.model.information.ChiseledBlockModelInformation;
import mod.chiselsandbits.client.model.meshing.GreedyMeshBuilder;
import mod.chiselsandbits.client.model.meshing.ChiseledTerrainMetadata;
import mod.chiselsandbits.client.model.meshing.GreedyMeshFace;
import mod.chiselsandbits.client.model.meshing.GreedyRectangles;
import mod.chiselsandbits.client.model.meshing.RectangleSubsetPreflight;
import mod.chiselsandbits.client.model.meshing.ChiseledRenderSnapshot;
import mod.chiselsandbits.client.model.meshing.BoundarySelfCulling;
import mod.chiselsandbits.client.model.meshing.NeighbourBoundary;
import mod.chiselsandbits.client.model.parts.ChiseledBlockModelPart;
import mod.chiselsandbits.client.model.parts.ChiseledBlockModelPartKey;
import mod.chiselsandbits.client.model.face.FaceManager;
import mod.chiselsandbits.client.util.QuadGenerationUtils;
import mod.chiselsandbits.profiling.ProfilingManager;
import org.jspecify.annotations.Nullable;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public record ChiseledBlockModelInformationBuilder(
		IAreaAccessor data,
		ChiseledBlockModelCacheKey key,
		BlockAndTintGetter blockAndTintGetter,
		BlockPos pos
) {
	private static final AtomicLong FULL_MODEL_BUILDS = new AtomicLong();

	public static long fullModelBuildCount() {
		return FULL_MODEL_BUILDS.get();
	}

	public @Nullable ChiseledTerrainMetadata describeTerrainSources() {
		if (!(data() instanceof ISingleBlockAxisAlignedAreaAccessor snapshot)) {
			return null;
		}
		return describeTerrainSources(ChiseledRenderSnapshot.of(snapshot));
	}

	public @Nullable ChiseledTerrainMetadata describeTerrainSources(final ChiseledRenderSnapshot snapshot) {
		if (!RectangleSubsetPreflight.supportsStaticSourceModels(key())) {
			return null;
		}
		final Map<BlockInformation, ChunkSectionLayer> sourceLayers = describeSourceLayers(snapshot);
		if (sourceLayers == null || !RectangleSubsetPreflight.supportsMaterialInterfaces(snapshot, sourceLayers)) {
			return null;
		}
		final List<ChiseledBlockModelMaterial> materials = new ArrayList<>();
		final List<ChiseledTerrainMetadata.Face> faceLayout = new ArrayList<>();
		final Map<ChiseledBlockModelPartKey, Boolean> parts = new HashMap<>();
		for (final GreedyMeshFace face : generateFaces()) {
			faceLayout.add(ChiseledTerrainMetadata.Face.of(
					face.faceValue(), face.normalDirection(), face.lowerLeft(), face.upperRight()));
			final int[] emitted = {0};
			final boolean supported = QuadGenerationUtils.generateVertices(
					face.faceValue(), face.normalDirection(), blockAndTintGetter(), pos(),
					face.lowerLeft(), face.upperRight(),
					source -> source != null && source.part() != null && source.material() != null
							&& source.material().sprite() != null
							&& RectangleSubsetPreflight.supportsMaterial(face.faceValue())
							&& source.part().ambientOcclusion() != null
							&& RectangleSubsetPreflight.supportsMaterialFlags(source.material().flags())
							&& RectangleSubsetPreflight.supportsSourceFace(
									source.material().layer(),
									source.material().sprite().contents().isAnimated(), source.material().lightEmission())
							&& source.material().flags() == ((source.material().layer() == ChunkSectionLayer.TRANSLUCENT
									? net.minecraft.client.resources.model.geometry.BakedQuad.FLAG_TRANSLUCENT : 0)
									| (source.material().sprite().contents().isAnimated()
											? net.minecraft.client.resources.model.geometry.BakedQuad.FLAG_ANIMATED : 0))
							&& RectangleSubsetPreflight.supportsSourceFaceGeometry(source, face.normalDirection()),
					generated -> {
						emitted[0]++;
						final DeconstructedModelPartComponent source = generated.source();
						final var material = new ChiseledBlockModelMaterial(face.faceValue(), source.material().tintIndex());
						if (!materials.contains(material)) {
							materials.add(material);
						}
						// Match PartsBuilder's insertion semantics, including colliding HashMap buckets.
						parts.computeIfAbsent(new ChiseledBlockModelPartKey(
								face.faceValue(), face.faceValue().blockState(), source.part().ambientOcclusion(),
								source.part().particleMaterial(), source.material().flags()), $ -> true);
					});
			if (!supported || emitted[0] == 0) {
				return null;
			}
		}
		if (parts.isEmpty()) {
			return null;
		}
		return new ChiseledTerrainMetadata(key(), IModelManager.getInstance().getParticleMaterial(
				key().primaryState().blockState(), () -> key().primaryState().newBlockEntity(pos()),
				blockAndTintGetter(), pos()), List.copyOf(parts.keySet()), materials, faceLayout, sourceLayers);
	}

	private Map<BlockInformation, ChunkSectionLayer> describeSourceLayers(final ChiseledRenderSnapshot snapshot) {
		final Map<BlockInformation, ChunkSectionLayer> result = new HashMap<>();
		for (final BlockInformation material : snapshot.materials()) {
			if (material.isAir()) {
				continue;
			}
			if (!RectangleSubsetPreflight.supportsMaterial(material)) {
				return null;
			}
			if (!material.blockState().getFluidState().isEmpty()
					&& !(material.blockState().getBlock() instanceof LiquidBlock)) {
				// FaceManager's legacy fluid source path replaces waterlogged block geometry with fluid-only faces.
				return null;
			}

			final ChunkSectionLayer[] layer = {null};
			final boolean[] valid = {true};
			for (final Direction direction : Direction.values()) {
				FaceManager.getInstance().extractQuads(material, direction, blockAndTintGetter(), pos(), source -> {
					if (source.material() == null || source.material().sprite() == null
							|| !RectangleSubsetPreflight.supportsSourceFace(source.material().layer(),
									source.material().sprite().contents().isAnimated(), source.material().lightEmission())
							|| !RectangleSubsetPreflight.supportsMaterialFlags(source.material().flags())) {
						valid[0] = false;
						return;
					}
					if (layer[0] != null && layer[0] != source.material().layer()) {
						valid[0] = false;
						return;
					}
					layer[0] = source.material().layer();
				});
			}

			if (!valid[0] || layer[0] == null) {
				return null;
			}
			result.put(material, layer[0]);
		}
		return result;
	}

	public ChiseledBlockModelInformation build() {
		return build(NeighbourBoundary.NONE);
	}

	public ChiseledBlockModelInformation build(final NeighbourBoundary boundary) {
		FULL_MODEL_BUILDS.incrementAndGet();
		//Handle the case where we have no bits in the system.
		if (key().primaryState().isAir()) {
			return ChiseledBlockModelInformation.EMPTY;
		}

		final PartsBuilder builder = new PartsBuilder();
		final GreedyMeshFace[] faces = generateFaces();

		try (IProfilerSection ignoredQuadGeneration = ProfilingManager.getInstance().withSection("quadGeneration")) {
			for (final GreedyMeshFace original : faces) {
				final List<GreedyMeshFace> visible = BoundarySelfCulling.visible(original, boundary);
				final boolean changed = visible.size() != 1 || visible.getFirst() != original;
				final boolean[] affineUv = {true};
				final boolean canClip = !changed || QuadGenerationUtils.generateVertices(
						original.faceValue(), original.normalDirection(), blockAndTintGetter(), pos(),
						original.lowerLeft(), original.upperRight(),
						source -> source != null && source.sourceQuad() != null
								&& RectangleSubsetPreflight.supportsSourceFaceGeometry(source, original.normalDirection()),
						generated -> {
							if (!visible.isEmpty() && !RectangleSubsetPreflight.supportsClippedUv(generated.vertices())) {
								affineUv[0] = false;
							}
						});
				for (final GreedyMeshFace region : canClip && affineUv[0] ? visible : List.of(original)) {
					final Direction cullDirection = region.normalDirection();

					QuadGenerationUtils.generateQuads(
							region.faceValue(),
							cullDirection,
							blockAndTintGetter(),
							pos(),
							region.lowerLeft(),
							region.upperRight(),
							builder.newQuadProcessor(region, cullDirection)
					);
				}
			}
		}

		return buildInformation(builder, IModelManager.getInstance().getParticleMaterial(
				key().primaryState().blockState(),
				() -> key().primaryState().newBlockEntity(pos()),
				blockAndTintGetter(),
				pos()
		));
	}

	public @Nullable ChiseledBlockModelInformation buildFromRectangles(
			final GreedyRectangles rectangles,
			final Material.Baked particleMaterial
	) {
		final PartsBuilder builder = new PartsBuilder();

		for (int index = 0; index < rectangles.size(); index++) {
			final long rectangle = rectangles.rectangle(index);
			final Direction direction = GreedyRectangles.direction(rectangle);
			final Vector3f from = GreedyRectangles.lowerCorner(rectangle);
			final Vector3f to = GreedyRectangles.upperCorner(rectangle);
			final GreedyMeshFace region = new GreedyMeshFace(
					rectangles.material(rectangle),
					from,
					to,
					from,
					to,
					direction,
					GreedyRectangles.cullFace(rectangle) != null
			);

			final boolean supportedSourceGeometry = QuadGenerationUtils.generateQuads(
					region.faceValue(),
					direction,
					blockAndTintGetter(),
					pos(),
					from,
					to,
					source -> RectangleSubsetPreflight.supportsSourceFaceGeometry(source, direction),
					builder.newQuadProcessor(region, direction)
			);
			if (!supportedSourceGeometry) {
				return null;
			}
		}

		return buildInformation(builder, particleMaterial);
	}

	private ChiseledBlockModelInformation buildInformation(final PartsBuilder builder, final Material.Baked particleMaterial) {
		return new ChiseledBlockModelInformation(particleMaterial, builder.build(), builder.materials(), key());
	}

	private GreedyMeshFace[] generateFaces() {
		final GreedyMeshFace[] faces;

		try (IProfilerSection ignoredFaceProcessing = ProfilingManager.getInstance().withSection("processing")) {
			faces = GreedyMeshBuilder.buildMesh(this::getBlockInformationForOffset);
		}

		return faces;
	}

	private BlockInformation getBlockInformationForOffset(
			int x, int y, int z
	) {
		final Vec3 targetOffset = new Vec3(x, y, z).multiply(StateEntrySize.current().getSizePerBitScalingVector());
		final Vec3 nominalTargetOffset = Vec3.ZERO.add(targetOffset);
		final BlockPos nominalTargetBlockOffset = VectorUtils.toBlockPos(nominalTargetOffset);
		final Vec3 inBlockOffset = nominalTargetOffset.subtract(Vec3.atLowerCornerOf(nominalTargetBlockOffset));
		final Vec3 inBlockOffsetTarget = VectorUtils.makePositive(inBlockOffset);

		final Direction offsetDirection = Direction.getApproximateNearest(
				nominalTargetBlockOffset.getX(),
				nominalTargetBlockOffset.getY(),
				nominalTargetBlockOffset.getZ()
		);

		IAreaAccessor neighborAccessor;
		if (targetOffset.x() >= 0 && targetOffset.x() < 1
				&& targetOffset.y() >= 0 && targetOffset.y() < 1
				&& targetOffset.z() >= 0 && targetOffset.z() < 1
		) {
			neighborAccessor = data();
		} else {
			neighborAccessor = key().neighborhood().getAreaAccessor(offsetDirection);
		}

		if (neighborAccessor != null) {
			return neighborAccessor.getInAreaTarget(inBlockOffsetTarget)
					.map(IStateEntryInfo::getBlockInformation)
					.orElse(BlockInformation.AIR);
		}

		return key().neighborhood().getBlockInformation(offsetDirection);
	}

	private static final class PartsBuilder {
		private final Set<ChiseledBlockModelMaterial> knownMaterials = new HashSet<>();
		private final List<ChiseledBlockModelMaterial> materialRegistrationOrder = new ArrayList<>();
		private final Map<ChiseledBlockModelPartKey, QuadCollection.Builder> partBuilders = new HashMap<>();

		public int material(BlockInformation information, int tintIndex) {
			ChiseledBlockModelMaterial material = new ChiseledBlockModelMaterial(information, tintIndex);

			if (knownMaterials.add(material)) {
				materialRegistrationOrder.add(material);
				return materialRegistrationOrder.size() - 1;
			}

			return materialRegistrationOrder.indexOf(material);
		}

		public Consumer<QuadGenerationUtils.GeneratedQuad> newQuadProcessor(GreedyMeshFace region, @Nullable Direction cullDirection) {
			return quad -> {
				final ChiseledBlockModelPartKey key = new ChiseledBlockModelPartKey(
						region.faceValue(),
						region.faceValue().blockState(),
						quad.source().part().ambientOcclusion(),
						quad.source().part().particleMaterial(),
						quad.quad().materialInfo().flags()
				);

				final QuadCollection.Builder builder = partBuilders.computeIfAbsent(
						key,
						$ -> new QuadCollection.Builder()
				);

				int materialIndex = material(region.faceValue(), quad.quad().materialInfo().tintIndex());
				BakedQuadBuilder quadBuilder = new BakedQuadBuilder(quad.quad().materialInfo());
				quadBuilder.from(quad.quad());
				quadBuilder.tintIndex(materialIndex);
				BakedQuad bakedQuad = quadBuilder.build();

				if (region.isOnOuterFace() && cullDirection != null) {
					builder.addCulledFace(cullDirection, bakedQuad);
				} else {
					builder.addUnculledFace(bakedQuad);
				}
			};
		}

		public List<ChiseledBlockModelPart> build() {
			return partBuilders.entrySet().stream()
					.map(e -> e.getKey().toPart(e.getValue().build()))
					.toList();
		}

		private List<ChiseledBlockModelMaterial> materials() {
			return Collections.unmodifiableList(materialRegistrationOrder);
		}
	}
}

package mod.chiselsandbits.client.model.meshing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.communi.suggestu.scena.core.client.models.processing.DeconstructedModelPartComponent;
import com.communi.suggestu.scena.core.client.models.processing.VertexData;
import com.communi.suggestu.scena.core.client.rendering.terrain.TerrainGeometry;
import com.communi.suggestu.scena.core.client.rendering.terrain.TerrainGeometryPart;
import com.communi.suggestu.scena.core.client.rendering.terrain.TerrainGeometryValidation;
import com.communi.suggestu.scena.core.client.rendering.terrain.TerrainQuad;
import com.communi.suggestu.scena.core.client.rendering.terrain.TerrainVertex;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.client.model.parts.ChiseledBlockModelPartKey;
import mod.chiselsandbits.client.util.QuadGenerationUtils;
import org.jspecify.annotations.Nullable;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Builds resolved rectangles without constructing a chiseled baked model or comparing baked output. */
public final class DirectTerrainGeometryBuilder {
	private DirectTerrainGeometryBuilder() {
	}

	public static BuildResult build(
			final ChiseledTerrainMetadata metadata, final GreedyRectangles rectangles,
			final BlockAndTintGetter level, final BlockPos pos
	) {
		return build(metadata, rectangles, level, pos, null, NeighbourBoundary.NONE);
	}

	public static BuildResult buildWithoutSealedAir(
			final ChiseledTerrainMetadata metadata, final ChiseledRenderSnapshot snapshot,
			final BlockAndTintGetter level, final BlockPos pos
	) {
		return buildWithoutSealedAir(metadata, snapshot, level, pos, NeighbourBoundary.NONE);
	}

	public static BuildResult buildWithoutSealedAir(
			final ChiseledTerrainMetadata metadata, final ChiseledRenderSnapshot snapshot,
			final BlockAndTintGetter level, final BlockPos pos, final NeighbourBoundary boundary
	) {
		if (!RectangleSubsetPreflight.supportsAllMaterials(snapshot)) {
			return BuildResult.rejected("snapshot contains a block-entity material");
		}
		if (!RectangleSubsetPreflight.supportsSealedAirOmission(snapshot)
				|| metadata.sourceLayers().values().stream().anyMatch(layer -> layer != ChunkSectionLayer.SOLID)) {
			return build(metadata, snapshot.greedyRectangles(FaceCullingPolicy.ANY_OCCUPIED), level, pos, null, boundary);
		}
		final Set<Long> retained = new HashSet<>();
		final GreedyRectangles visible = snapshot.greedyRectanglesWithoutSealedAir(NeighbourBoundary.NONE);

		for (int index = 0; index < visible.size(); index++) {
			retained.add(visible.rectangle(index));
		}

		return build(metadata, snapshot.greedyRectangles(FaceCullingPolicy.ANY_OCCUPIED), level, pos, retained, boundary);
	}

	private static BuildResult build(
			final ChiseledTerrainMetadata metadata, final GreedyRectangles rectangles,
			final BlockAndTintGetter level, final BlockPos pos, final @Nullable Set<Long> retained,
			final NeighbourBoundary boundary
	) {
		if (rectangles.size() == 0 || metadata.parts().isEmpty() || metadata.faceLayout().isEmpty()
				|| !RectangleSubsetPreflight.supportsStaticSourceModels(metadata.key())
				|| !RectangleSubsetPreflight.supportsMaterialFlags(metadata.materialFlags())) {
			return BuildResult.rejected("source model is outside the validated static material subset");
		}

		// Different tessellation changes the lighter's interpolation even when face coverage is identical.
		final Map<ChiseledTerrainMetadata.Face, Integer> expected = new HashMap<>();
		metadata.faceLayout().forEach(face -> expected.merge(face, 1, Integer::sum));

		if (!expected.isEmpty()) {
			for (int index = 0; index < rectangles.size(); index++) {
				final long rectangle = rectangles.rectangle(index);
				final ChiseledTerrainMetadata.Face face = ChiseledTerrainMetadata.Face.of(rectangles.material(rectangle),
						GreedyRectangles.direction(rectangle), GreedyRectangles.lowerCorner(rectangle),
						GreedyRectangles.upperCorner(rectangle));
				if (expected.computeIfPresent(face, (key, count) -> count - 1) == null) {
					return BuildResult.rejected("rectangle tessellation does not preserve source face layout");
				}
			}

			if (expected.values().stream().anyMatch(count -> count != 0)) {
				return BuildResult.rejected("rectangle tessellation does not preserve source face layout");
			}
		}

		final List<MutablePart> parts = new ArrayList<>();
		final Map<ChiseledBlockModelPartKey, MutablePart> partByKey = new HashMap<>();

		for (final ChiseledBlockModelPartKey key : metadata.parts()) {
			final var part = new MutablePart(key);

			if (partByKey.putIfAbsent(key, part) != null) {
				return BuildResult.rejected("source part grouping is ambiguous");
			}

			parts.add(part);
		}

		for (int index = 0; index < rectangles.size(); index++) {
			final long rectangle = rectangles.rectangle(index);
			final BlockInformation material = rectangles.material(rectangle);
			final Direction direction = GreedyRectangles.direction(rectangle);

			if (!RectangleSubsetPreflight.supportsMaterial(material)) {
				return BuildResult.rejected("rectangle material cannot be captured as immutable terrain geometry");
			}

			final int[] count = {0};
			final String[] rejection = {null};
			final boolean retain = retained == null || retained.contains(rectangle);
			final List<Long> fallbackRectangles = BoundarySelfCulling.visible(rectangles, rectangle, boundary);
			final boolean unchanged = fallbackRectangles.size() == 1 && fallbackRectangles.getFirst() == rectangle;
			final boolean supported = QuadGenerationUtils.generateVertices(
					material, direction, level, pos, GreedyRectangles.lowerCorner(rectangle),
					GreedyRectangles.upperCorner(rectangle), source -> supportsSource(source, direction),
					generated -> {
						count[0]++;

						if (rejection[0] == null) {
							if (!unchanged && !fallbackRectangles.isEmpty()
									&& !RectangleSubsetPreflight.supportsClippedUv(generated.vertices())) {
								rejection[0] = "boundary fragment has non-affine UV coordinates";
							} else {
								rejection[0] = append(metadata, partByKey, material, GreedyRectangles.cullFace(rectangle),
										true, retain && unchanged, unchanged, generated);
							}
						}
					});
			if (!supported || count[0] == 0 || rejection[0] != null) {
				return BuildResult.rejected(rejection[0] == null ? "source face is missing or unsupported" : rejection[0]);
			}
			if (!unchanged) {
				for (final long visibleRectangle : fallbackRectangles) {
					count[0] = 0;
					final boolean fragmentSupported = QuadGenerationUtils.generateVertices(
							material, direction, level, pos, GreedyRectangles.lowerCorner(visibleRectangle),
							GreedyRectangles.upperCorner(visibleRectangle), source -> supportsSource(source, direction),
							generated -> {
								count[0]++;
								if (rejection[0] == null) {
									rejection[0] = append(metadata, partByKey, material,
											GreedyRectangles.cullFace(visibleRectangle), false, retain, true, generated);
								}
							});
					if (!fragmentSupported || count[0] == 0 || rejection[0] != null) {
						return BuildResult.rejected(rejection[0] == null ? "boundary fragment is unsupported" : rejection[0]);
					}
				}
			}
		}

		final List<TerrainGeometryPart> full = new ArrayList<>();
		final List<TerrainGeometryPart> visible = new ArrayList<>();
		final List<TerrainGeometryPart> fallback = new ArrayList<>();

		for (final MutablePart part : parts) {
			if (part.quads.isEmpty()) {
				return BuildResult.rejected("rectangle output omitted a source model part");
			}

			full.add(new TerrainGeometryPart(part.key.ambientOcclusion(), part.key.appearance(), part.key.materialFlags(), part.quads));
			visible.add(new TerrainGeometryPart(part.key.ambientOcclusion(), part.key.appearance(), part.key.materialFlags(), part.visible));
			fallback.add(new TerrainGeometryPart(part.key.ambientOcclusion(), part.key.appearance(), part.key.materialFlags(), part.fallback));
		}

		final var fullGeometry = new TerrainGeometry(full);
		final TerrainGeometry geometry = new TerrainGeometry(visible);
		final TerrainGeometry bakedGeometry = new TerrainGeometry(fallback);

		final String invalid = invalidGeometry(fullGeometry);
		if (invalid != null || !TerrainGeometryValidation.supports(geometry) || !TerrainGeometryValidation.supports(bakedGeometry)) {
			return BuildResult.rejected(invalid == null
					? "visible geometry failed native terrain validation" : invalid);
		}

		return new BuildResult(geometry, fullGeometry, bakedGeometry, null);
	}

	private static @Nullable String invalidGeometry(final TerrainGeometry geometry) {
		for (int partIndex = 0; partIndex < geometry.parts().size(); partIndex++) {
			final TerrainGeometryPart part = geometry.parts().get(partIndex);
			if (part.ambientOcclusion() == null) {
				return "terrain part has no ambient occlusion metadata";
			}
			for (final TerrainQuad quad : part.quads()) {
				if (!TerrainGeometryValidation.supports(quad)) {
					return "terrain source quad failed validation: face=" + quad.face() + ", layer=" + quad.material().layer()
							+ ", tint=" + quad.tintIndex() + ", emission=" + quad.material().lightEmission()
							+ ", vertices=" + quad.v0() + " " + quad.v1() + " " + quad.v2() + " " + quad.v3();
				}
				int expectedFlags = quad.material().layer() == ChunkSectionLayer.TRANSLUCENT
						? BakedQuad.FLAG_TRANSLUCENT : 0;
				if (quad.material().sprite().contents().isAnimated()) {
					expectedFlags |= BakedQuad.FLAG_ANIMATED;
				}
				if (part.materialFlags() != expectedFlags) {
					return "terrain part flags differ from resolved materials: part=" + partIndex
							+ ", flags=" + part.materialFlags() + ", expected=" + expectedFlags;
				}
			}
		}
		return null;
	}

	private static boolean supportsSource(final DeconstructedModelPartComponent source, final Direction direction) {
		return source != null && source.part() != null && source.part().ambientOcclusion() != null
				&& source.cullDirection() == direction && source.sourceQuad() != null
				&& source.sourceQuad().direction() == direction && source.material() != null
				&& source.material().sprite() != null
				&& RectangleSubsetPreflight.supportsMaterialFlags(source.material().flags())
				&& RectangleSubsetPreflight.supportsSourceFace(source.material().layer(),
						source.material().sprite().contents().isAnimated(), source.material().lightEmission())
				&& RectangleSubsetPreflight.supportsSourceFaceGeometry(source, direction);
	}

	private static @Nullable String append(
			final ChiseledTerrainMetadata metadata, final Map<ChiseledBlockModelPartKey, MutablePart> parts,
			final BlockInformation material, final @Nullable Direction cull, final boolean full, final boolean visible,
			final boolean fallback,
			final QuadGenerationUtils.GeneratedVertices generated
	) {
		final DeconstructedModelPartComponent source = generated.source();
		final List<VertexData> vertices = generated.vertices();

		if (vertices.size() != 4) {
			return "source clipping did not produce four ordered vertices";
		}

		for (int index = 0; index < 4; index++) {
			if (vertices.get(index) == null || vertices.get(index).vertexIndex() != index) {
				return "source clipping did not preserve vertex winding";
			}
		}

		final int tint = RectangleSubsetPreflight.legacyTintIndex(metadata.materials(), material, source.material().tintIndex());

		if (tint < 0) {
			return "source tint has no unique compile-state material entry";
		}

		final MutablePart part = parts.get(new ChiseledBlockModelPartKey(material, material.blockState(),
				source.part().ambientOcclusion(), source.part().particleMaterial(), source.material().flags()));
		if (part == null) {
			return "source quad has no matching ordered part";
		}

		// Match the material normalization performed by the legacy tint-index adapter on both loaders.
		final BakedQuad.MaterialInfo sourceMaterial = source.material();
		final var resolvedMaterial = new BakedQuad.MaterialInfo(sourceMaterial.sprite(), sourceMaterial.layer(),
				sourceMaterial.itemRenderType(), sourceMaterial.tintIndex(), sourceMaterial.shade(), sourceMaterial.lightEmission());
		// Preserve the host renderer's directional cull group for every layer.
		final var quad = new TerrainQuad(vertex(vertices.get(0)), vertex(vertices.get(1)), vertex(vertices.get(2)),
				vertex(vertices.get(3)), source.sourceQuad().direction(), cull, resolvedMaterial, tint);
		if (full) {
			part.quads.add(quad);
		}

		if (visible) {
			part.visible.add(quad);
		}

		if (fallback) {
			part.fallback.add(quad);
		}

		return null;
	}

	private static TerrainVertex vertex(final VertexData vertex) {
		return new TerrainVertex(vertex.x(), vertex.y(), vertex.z(), vertex.u(), vertex.v());
	}

	public record BuildResult(
			@Nullable TerrainGeometry geometry, @Nullable TerrainGeometry fallbackGeometry,
			@Nullable TerrainGeometry bakedGeometry, @Nullable String rejection
	) {
		private static BuildResult rejected(final String rejection) {
			return new BuildResult(null, null, null, rejection);
		}

		public boolean supported() {
			return geometry != null;
		}
	}

	private static final class MutablePart {
		private final ChiseledBlockModelPartKey key;
		private final List<TerrainQuad> quads = new ArrayList<>();
		private final List<TerrainQuad> visible = new ArrayList<>();
		private final List<TerrainQuad> fallback = new ArrayList<>();

		private MutablePart(final ChiseledBlockModelPartKey key) {
			this.key = key;
		}
	}
}

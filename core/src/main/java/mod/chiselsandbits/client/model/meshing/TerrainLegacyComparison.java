package mod.chiselsandbits.client.model.meshing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.communi.suggestu.scena.core.client.rendering.terrain.TerrainGeometry;
import com.communi.suggestu.scena.core.client.rendering.terrain.TerrainGeometryPart;
import com.communi.suggestu.scena.core.client.rendering.terrain.TerrainQuad;
import com.communi.suggestu.scena.core.client.rendering.terrain.TerrainVertex;
import mod.chiselsandbits.client.model.builder.ChiseledBlockModelMaterial;
import mod.chiselsandbits.client.model.information.ChiseledBlockModelInformation;
import mod.chiselsandbits.client.model.parts.ChiseledBlockModelPart;
import mod.chiselsandbits.client.model.parts.ChiseledBlockModelPartKey;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Exact baked-output oracle used only by the explicitly enabled, isolated client test harnesses. */
public final class TerrainLegacyComparison {
	private TerrainLegacyComparison() {
	}

	public static DirectTerrainGeometryBuilder.BuildResult build(
			final ChiseledBlockModelInformation legacy, final GreedyRectangles rectangles,
			final BlockAndTintGetter level, final BlockPos pos
	) {
		return DirectTerrainGeometryBuilder.build(metadata(legacy, rectangles), rectangles, level, pos);
	}

	public static DirectTerrainGeometryBuilder.BuildResult buildWithoutSealedAir(
			final ChiseledBlockModelInformation legacy, final ChiseledRenderSnapshot snapshot,
			final BlockAndTintGetter level, final BlockPos pos
	) {
		return DirectTerrainGeometryBuilder.buildWithoutSealedAir(
				metadata(legacy, snapshot.greedyRectangles(FaceCullingPolicy.ANY_OCCUPIED)), snapshot, level, pos);
	}

	private static ChiseledTerrainMetadata metadata(final ChiseledBlockModelInformation legacy, final GreedyRectangles rectangles) {
		final List<ChiseledTerrainMetadata.Face> layout = new ArrayList<>();
		final Map<mod.chiselsandbits.api.blockinformation.BlockInformation, ChunkSectionLayer> sourceLayers = new HashMap<>();
		for (final var part : legacy.parts()) {
			for (final Direction direction : Direction.values()) {
				for (final BakedQuad quad : part.getQuads(direction)) {
					final var source = legacy.materials().get(quad.materialInfo().tintIndex()).blockInformation();
					sourceLayers.putIfAbsent(source, quad.materialInfo().layer());
				}
			}
			for (final BakedQuad quad : part.getQuads(null)) {
				final var source = legacy.materials().get(quad.materialInfo().tintIndex()).blockInformation();
				sourceLayers.putIfAbsent(source, quad.materialInfo().layer());
			}
		}

		for (int index = 0; index < rectangles.size(); index++) {
			final long rectangle = rectangles.rectangle(index);
			layout.add(ChiseledTerrainMetadata.Face.of(rectangles.material(rectangle), GreedyRectangles.direction(rectangle),
					GreedyRectangles.lowerCorner(rectangle), GreedyRectangles.upperCorner(rectangle)));
		}

		return new ChiseledTerrainMetadata(legacy.key(), legacy.particleMaterial(), legacy.parts().stream()
				.map(part -> new ChiseledBlockModelPartKey(part.source(), part.appearance(), part.ambientOcclusion(),
						part.particleMaterial(), part.materialFlags())).toList(), legacy.materials(), layout, sourceLayers);
	}

	public static boolean matchesLegacyOutput(final ChiseledBlockModelInformation legacy, final TerrainGeometry geometry) {
		return legacyOutputMismatch(legacy, geometry) == null;
	}

	public static @Nullable String legacyOutputMismatch(final ChiseledBlockModelInformation legacy, final TerrainGeometry geometry) {
		if (legacy.parts().size() != geometry.parts().size()) {
			return "part count differs";
		}

		int flags = 0;

		for (int index = 0; index < legacy.parts().size(); index++) {
			final ChiseledBlockModelPart expected = legacy.parts().get(index);
			final TerrainGeometryPart actual = geometry.parts().get(index);
			if (expected.ambientOcclusion() != actual.ambientOcclusion()
					|| !Objects.equals(expected.appearance(), actual.appearance())
					|| expected.materialFlags() != actual.materialFlags()) {
				return "part " + index + " metadata differs";
			}

			flags |= actual.materialFlags();

			for (int group = 0; group < 7; group++) {
				final Direction cull = group == 6 ? null : Direction.values()[group];
				final String mismatch = groupMismatch(legacy, expected, actual, cull);

				if (mismatch != null) {
					return "part " + index + ", cull " + cull + ": " + mismatch;
				}
			}
		}

		return flags == legacy.materialFlags() ? null : "aggregate material flags differ";
	}

	private static @Nullable String groupMismatch(
			final ChiseledBlockModelInformation legacy, final ChiseledBlockModelPart part,
			final TerrainGeometryPart direct, final @Nullable Direction cull
	) {
		final List<BakedQuad> expected = part.getQuads(cull);
		final List<TerrainQuad> actual = direct.quads().stream().filter(quad -> quad.cullFace() == cull).toList();
		return quadListMismatch(legacy, expected, actual);
	}

	private static @Nullable String quadListMismatch(
			final ChiseledBlockModelInformation legacy, final List<BakedQuad> expected, final List<TerrainQuad> actual
	) {
		if (expected.size() != actual.size()) {
			return "quad count differs";
		}

		final Map<Signature, Integer> a = new HashMap<>();
		final Map<Signature, Integer> b = new HashMap<>();

		for (final BakedQuad quad : expected) {
			final int tint = quad.materialInfo().tintIndex();

			if (tint < 0 || tint >= legacy.materials().size()) {
				return "invalid legacy tint index";
			}

			final List<Vertex> vertices = new ArrayList<>();

			for (int vertex = 0; vertex < 4; vertex++) {
				final Vector3fc position = quad.position(vertex);
				vertices.add(new Vertex(Float.floatToIntBits(position.x()), Float.floatToIntBits(position.y()),
						Float.floatToIntBits(position.z()), quad.packedUV(vertex)));
			}

			a.merge(new Signature(quad.direction(), legacy.materials().get(tint), quad.materialInfo().sprite(),
					quad.materialInfo().layer(), quad.materialInfo().itemRenderType(), tint,
					quad.materialInfo().shade(), quad.materialInfo().lightEmission(), vertices), 1, Integer::sum);
		}

		for (final TerrainQuad quad : actual) {
			final int tint = quad.tintIndex();
			if (tint < 0 || tint >= legacy.materials().size()
					|| legacy.materials().get(tint).tintIndex() != quad.material().tintIndex()) {
				return "invalid direct tint mapping";
			}

			final List<Vertex> vertices = new ArrayList<>();

			for (int vertex = 0; vertex < 4; vertex++) {
				final TerrainVertex position = quad.vertex(vertex);
				vertices.add(new Vertex(Float.floatToIntBits(position.x()), Float.floatToIntBits(position.y()),
						Float.floatToIntBits(position.z()), UVPair.pack(position.u(), position.v())));
			}

			b.merge(new Signature(quad.face(), legacy.materials().get(tint), quad.material().sprite(),
					quad.material().layer(), quad.material().itemRenderType(), tint,
					quad.material().shade(), quad.material().lightEmission(), vertices), 1, Integer::sum);
		}

		if (a.equals(b)) {
			return null;
		}

		final Signature first = a.keySet().stream().filter(key -> !Objects.equals(a.get(key), b.get(key))).findFirst().orElseThrow();
		final Signature second = b.keySet().stream().filter(key -> !Objects.equals(b.get(key), a.get(key)))
				.filter(key -> key.face() == first.face()).findFirst()
				.orElseGet(() -> b.keySet().stream().filter(key -> !Objects.equals(b.get(key), a.get(key))).findFirst().orElseThrow());
		if (!Objects.equals(first.source(), second.source()) || first.tint() != second.tint()) {
			return "tint differs";
		}

		if (!Objects.equals(first.sprite(), second.sprite()) || first.layer() != second.layer()
				|| !Objects.equals(first.itemRenderType(), second.itemRenderType()) || first.shade() != second.shade()
				|| first.emission() != second.emission()) {
			return "material differs";
		}

		for (int vertex = 0; vertex < 4; vertex++) {
			final Vertex x = first.vertices().get(vertex);
			final Vertex y = second.vertices().get(vertex);

			if (x.x() != y.x() || x.y() != y.y() || x.z() != y.z()) {
				return "ordered vertex " + vertex + " position differs";
			}

			if (x.uv() != y.uv()) {
				return "ordered vertex " + vertex + " UV differs";
			}
		}

		return "quad multiplicity differs";
	}

	private record Vertex(int x, int y, int z, long uv) {
	}

	private record Signature(
			Direction face, ChiseledBlockModelMaterial source, Object sprite, Object layer, Object itemRenderType,
			int tint, boolean shade, int emission, List<Vertex> vertices
	) {
	}
}

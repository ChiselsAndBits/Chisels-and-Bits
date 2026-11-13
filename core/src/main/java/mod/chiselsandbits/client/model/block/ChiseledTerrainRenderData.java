package mod.chiselsandbits.client.model.block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.communi.suggestu.scena.core.client.rendering.terrain.TerrainGeometry;
import com.communi.suggestu.scena.core.client.rendering.terrain.TerrainQuad;
import com.communi.suggestu.scena.core.client.rendering.terrain.TerrainVertex;
import com.google.common.base.Suppliers;
import mod.chiselsandbits.client.model.information.ChiseledBlockModelInformation;
import mod.chiselsandbits.client.model.meshing.ChiseledTerrainMetadata;
import mod.chiselsandbits.client.model.parts.ChiseledBlockModelPart;
import org.joml.Vector3f;

import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;

/**
 * One immutable publication, including all inputs to the lazy baked fallback. The fallback never consults a
 * live accessor, model manager, atlas or face cache, even when requested after an edit or resource reload.
 */
public final class ChiseledTerrainRenderData {
	private final ChiseledTerrainMetadata metadata;
	private final TerrainGeometry geometry;
	private final Supplier<ChiseledBlockModelInformation> fallback;
	private volatile boolean fallbackBuilt;

	public ChiseledTerrainRenderData(
			final ChiseledTerrainMetadata metadata,
			final TerrainGeometry geometry,
			final TerrainGeometry fallbackGeometry
	) {
		this.metadata = metadata;
		this.geometry = geometry;
		this.fallback = Suppliers.memoize(() -> {
			final List<ChiseledBlockModelPart> parts = new ArrayList<>();

			for (int index = 0; index < fallbackGeometry.parts().size(); index++) {
				final QuadCollection.Builder quads = new QuadCollection.Builder();

				for (final TerrainQuad quad : fallbackGeometry.parts().get(index).quads()) {
					final BakedQuad baked = bake(quad);

					if (quad.cullFace() == null) {
						quads.addUnculledFace(baked);
					} else {
						quads.addCulledFace(quad.cullFace(), baked);
					}
				}

				parts.add(metadata.parts().get(index).toPart(quads.build()));
			}

			final var model = new ChiseledBlockModelInformation(
					metadata.particleMaterial(), List.copyOf(parts), metadata.materials(), metadata.key());
			fallbackBuilt = true;
			return model;
		});
	}

	public ChiseledTerrainMetadata metadata() {
		return metadata;
	}

	public TerrainGeometry geometry() {
		return geometry;
	}

	public ChiseledBlockModelInformation fallback() {
		return fallback.get();
	}

	public boolean isFallbackBuilt() {
		return fallbackBuilt;
	}

	private static BakedQuad bake(final TerrainQuad quad) {
		final BakedQuad.MaterialInfo source = quad.material();
		final BakedQuad.MaterialInfo material = new BakedQuad.MaterialInfo(
				source.sprite(), source.layer(), source.itemRenderType(), quad.tintIndex(),
				source.shade(), source.lightEmission());
		return new BakedQuad(position(quad.v0()), position(quad.v1()), position(quad.v2()), position(quad.v3()),
				uv(quad.v0()), uv(quad.v1()), uv(quad.v2()), uv(quad.v3()), quad.face(), material);
	}

	private static Vector3f position(final TerrainVertex vertex) {
		return new Vector3f(vertex.x(), vertex.y(), vertex.z());
	}

	private static long uv(final TerrainVertex vertex) {
		return UVPair.pack(vertex.u(), vertex.v());
	}
}

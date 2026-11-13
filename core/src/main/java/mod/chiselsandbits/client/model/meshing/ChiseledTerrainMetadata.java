package mod.chiselsandbits.client.model.meshing;

import java.util.List;
import java.util.Map;

import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.client.model.builder.ChiseledBlockModelMaterial;
import mod.chiselsandbits.client.model.information.ChiseledBlockModelCacheKey;
import mod.chiselsandbits.client.model.parts.ChiseledBlockModelPartKey;
import org.joml.Vector3f;

import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.Direction;

public record ChiseledTerrainMetadata(
		ChiseledBlockModelCacheKey key,
		Material.Baked particleMaterial,
		List<ChiseledBlockModelPartKey> parts,
		List<ChiseledBlockModelMaterial> materials,
		List<Face> faceLayout,
		Map<BlockInformation, ChunkSectionLayer> sourceLayers
) {
	public ChiseledTerrainMetadata {
		parts = List.copyOf(parts);
		materials = List.copyOf(materials);
		faceLayout = List.copyOf(faceLayout);
		sourceLayers = Map.copyOf(sourceLayers);
	}

	public int materialFlags() {
		return parts.stream().mapToInt(ChiseledBlockModelPartKey::materialFlags).reduce(0, (a, b) -> a | b);
	}

	public record Face(BlockInformation material, Direction direction, int x0, int y0, int z0, int x1, int y1, int z1) {
		public static Face of(final BlockInformation material, final Direction direction, final Vector3f from, final Vector3f to) {
			return new Face(material, direction, Float.floatToIntBits(from.x()), Float.floatToIntBits(from.y()),
					Float.floatToIntBits(from.z()), Float.floatToIntBits(to.x()), Float.floatToIntBits(to.y()),
					Float.floatToIntBits(to.z()));
		}
	}
}

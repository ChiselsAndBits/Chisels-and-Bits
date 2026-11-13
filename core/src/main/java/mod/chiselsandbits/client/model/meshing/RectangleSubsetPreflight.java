package mod.chiselsandbits.client.model.meshing;

import java.util.List;
import java.util.Map;

import com.communi.suggestu.scena.core.client.models.processing.DeconstructedModelPartComponent;
import com.communi.suggestu.scena.core.client.models.processing.VertexData;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.client.model.builder.ChiseledBlockModelMaterial;
import mod.chiselsandbits.client.model.information.ChiseledBlockModelCacheKey;
import mod.chiselsandbits.client.model.information.ChiseledBlockModelInformation;
import mod.chiselsandbits.client.model.parts.ChiseledBlockModelPartKey;
import org.joml.Vector3fc;

import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.LiquidBlock;

public final class RectangleSubsetPreflight {
	private static final Direction[] POSITIVE_DIRECTIONS = {Direction.EAST, Direction.UP, Direction.SOUTH};

	private RectangleSubsetPreflight() {
		throw new IllegalStateException("Cannot instantiate utility class");
	}

	/** Candidate filter only; complete static source geometry and its actual layer still require validation. */
	public static boolean supportsMaterial(final BlockInformation material) {
		return !material.isAir()
				&& !material.blockState().hasBlockEntity();
	}

	public static boolean isOpaque(final BlockInformation material) {
		return material.blockState().canOcclude() && material.blockState().isSolidRender();
	}

	public static boolean supportsSourceFace(
			final ChunkSectionLayer layer, final boolean animated, final int lightEmission
	) {
		return (layer == ChunkSectionLayer.SOLID || layer == ChunkSectionLayer.CUTOUT
				|| layer == ChunkSectionLayer.TRANSLUCENT)
				&& lightEmission >= 0 && lightEmission <= 15;
	}

	public static boolean supportsSourceFace(
			final boolean supportedLayer,
			final boolean animated,
			final int lightEmission
	) {
		return supportedLayer && lightEmission >= 0 && lightEmission <= 15;
	}

	public static boolean supportsMaterialFlags(final int flags) {
		return (flags & ~(BakedQuad.FLAG_ANIMATED | BakedQuad.FLAG_TRANSLUCENT)) == 0;
	}

	public static boolean supportsPart(final ChiseledBlockModelPartKey part) {
		return supportsMaterialFlags(part.materialFlags());
	}

	public static boolean supportsSourceFaceOutput(final boolean preflightPassed, final int emittedQuadCount) {
		return preflightPassed && emittedQuadCount > 0;
	}

	public static boolean supportsSourceFaces(final ChiseledBlockModelInformation model) {
		if (model.parts().isEmpty() || !supportsMaterialFlags(model.materialFlags())) {
			return false;
		}

		boolean hasQuads = false;
		for (final var part : model.parts()) {
			for (final var quad : part.quads().getAll()) {
				hasQuads = true;
				final var material = quad.materialInfo();
				if (!supportsSourceFace(
						material.layer(),
						material.sprite().contents().isAnimated(),
						material.lightEmission()
				) || !supportsMaterialFlags(material.flags())) {
					return false;
				}
			}
		}

		return hasQuads;
	}

	public static boolean hasCompatibleTintIndices(
			final List<ChiseledBlockModelMaterial> legacy,
			final List<ChiseledBlockModelMaterial> candidate
	) {
		for (int index = 0; index < candidate.size(); index++) {
			if (index >= legacy.size()) {
				return false;
			}
			final ChiseledBlockModelMaterial oldMaterial = legacy.get(index);
			final ChiseledBlockModelMaterial newMaterial = candidate.get(index);
			if (oldMaterial.tintIndex() == -1 && newMaterial.tintIndex() == -1) {
				continue;
			}
			if (!oldMaterial.equals(newMaterial)) {
				return false;
			}
		}
		return true;
	}

	public static int legacyTintIndex(
			final List<ChiseledBlockModelMaterial> legacyMaterials,
			final BlockInformation source,
			final int sourceTintIndex
	) {
		final ChiseledBlockModelMaterial expected = new ChiseledBlockModelMaterial(source, sourceTintIndex);
		int result = -1;
		for (int index = 0; index < legacyMaterials.size(); index++) {
			if (expected.equals(legacyMaterials.get(index))) {
				if (result >= 0) {
					return -1;
				}
				result = index;
			}
		}
		return result;
	}

	public static boolean supportsSourceFaceGeometry(
			final DeconstructedModelPartComponent source,
			final Direction expectedDirection
	) {
		return source.cullDirection() == expectedDirection
				&& supportsSourceFaceGeometry(expectedDirection, source.sourceQuad());
	}

	public static boolean supportsClippedUv(final List<VertexData> vertices) {
		if (vertices.size() != 4) {
			return false;
		}
		return Math.abs(vertices.get(0).u() + vertices.get(2).u() - vertices.get(1).u() - vertices.get(3).u()) < 0.000001f
				&& Math.abs(vertices.get(0).v() + vertices.get(2).v() - vertices.get(1).v() - vertices.get(3).v()) < 0.000001f;
	}

	public static boolean supportsSourceFaceGeometry(
			final Direction direction,
			final BakedQuad quad
	) {
		final float[][] vertices = new float[4][3];
		for (int index = 0; index < vertices.length; index++) {
			final Vector3fc position = quad.position(index);
			vertices[index][0] = position.x();
			vertices[index][1] = position.y();
			vertices[index][2] = position.z();
		}

		return supportsSourceFaceGeometry(direction, vertices);
	}

	public static boolean supportsSourceFaceGeometry(final Direction direction, final float[][] vertices) {
		if (vertices.length != 4) {
			return false;
		}

		final int normalAxis = switch (direction.getAxis()) {
			case X -> 0;
			case Y -> 1;
			case Z -> 2;
		};
		final int firstAxis = (normalAxis + 1) % 3;
		final int secondAxis = (normalAxis + 2) % 3;
		final float expectedPlane = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1f : 0f;
		int corners = 0;

		for (final float[] vertex : vertices) {
			if (vertex.length != 3 || !near(vertex[normalAxis], expectedPlane)) {
				return false;
			}

			final int firstCorner = unitCorner(vertex[firstAxis]);
			final int secondCorner = unitCorner(vertex[secondAxis]);
			if (firstCorner < 0 || secondCorner < 0) {
				return false;
			}

			final int corner = 1 << (firstCorner * 2 + secondCorner);
			if ((corners & corner) != 0) {
				return false;
			}
			corners |= corner;
		}

		return corners == 0b1111;
	}

	public static boolean supportsStaticSourceModels(final ChiseledBlockModelCacheKey key) {
		return sourceModelRejectionReason(key) == null;
	}

	/**
	 * Native publication is asynchronous, so a source key must prove that its geometry cannot change after capture.
	 * Only an exact {@link SingleVariant} key qualifies: both loaders return the immutable variant itself (weighted
	 * models return their selected variant). {@link LiquidBlock} fluids are exempt because {@code FaceManager} builds
	 * them from the reload-scoped {@code FluidModel} and never consults the block-state model.
	 */
	public static String sourceModelRejectionReason(final ChiseledBlockModelCacheKey key) {
		final List<BlockInformation> sourceStates = key.sourceStates();
		final List<Object> modelCacheKeys = new java.util.ArrayList<>(key.modelCacheKeys());
		if (modelCacheKeys.isEmpty()) {
			return "source model cache keys are missing";
		}
		if (modelCacheKeys.size() != sourceStates.size()) {
			return "source model cache keys do not match the captured source states";
		}

		for (int i = 0; i < modelCacheKeys.size(); i++) {
			final BlockInformation state = sourceStates.get(i);
			if (state.isAir() || isSyntheticFluidSource(state)) {
				continue;
			}
			final Object modelCacheKey = modelCacheKeys.get(i);
			if (modelCacheKey == null) {
				return "source model returned no geometry key";
			}
			if (modelCacheKey.getClass() != SingleVariant.class) {
				return "source model geometry key is not a static single variant: " + modelCacheKey.getClass().getName();
			}
		}

		return null;
	}

	public static boolean isSyntheticFluidSource(final BlockInformation state) {
		return !state.blockState().getFluidState().isEmpty() && state.blockState().getBlock() instanceof LiquidBlock;
	}

	public static boolean supportsAllMaterials(final ChiseledRenderSnapshot snapshot) {
		for (final BlockInformation material : snapshot.materials()) {
			if (!material.isAir() && !supportsMaterial(material)) {
				return false;
			}
		}

		return true;
	}

	/** Necessary state-level proof; the producer additionally requires every emitted source quad to be SOLID. */
	public static boolean supportsSealedAirOmission(final ChiseledRenderSnapshot snapshot) {
		for (final BlockInformation material : snapshot.materials()) {
			if (!material.isAir() && (!supportsMaterial(material) || !isOpaque(material))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * The legacy greedy mesher removes every occupied-to-occupied interface. That remains correct only
	 * when both touching materials have fully opaque SOLID source geometry; otherwise a cutout/translucent
	 * surface can expose a missing face. Such mixed interfaces use the legacy baked path.
	 */
	public static boolean supportsMaterialInterfaces(
			final ChiseledRenderSnapshot snapshot,
			final Map<BlockInformation, ChunkSectionLayer> sourceLayers
	) {
		for (int x = 0; x < ChiseledRenderSnapshot.GRID; x++) {
			for (int y = 0; y < ChiseledRenderSnapshot.GRID; y++) {
				for (int z = 0; z < ChiseledRenderSnapshot.GRID; z++) {
					final BlockInformation source = snapshot.materialAt(x, y, z);
					if (source.isAir()) {
						continue;
					}

					for (final Direction direction : POSITIVE_DIRECTIONS) {
						final int nx = x + direction.getStepX();
						final int ny = y + direction.getStepY();
						final int nz = z + direction.getStepZ();
						if (nx >= ChiseledRenderSnapshot.GRID || ny >= ChiseledRenderSnapshot.GRID || nz >= ChiseledRenderSnapshot.GRID) {
							continue;
						}

						final BlockInformation neighbor = snapshot.materialAt(nx, ny, nz);
						if (neighbor.isAir() || source.equals(neighbor)) {
							continue;
						}

						if (!isOpaque(source) || !isOpaque(neighbor)
								|| sourceLayers.get(source) != ChunkSectionLayer.SOLID
								|| sourceLayers.get(neighbor) != ChunkSectionLayer.SOLID) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	private static int unitCorner(final float coordinate) {
		if (near(coordinate, 0f)) {
			return 0;
		}

		return near(coordinate, 1f) ? 1 : -1;
	}

	private static boolean near(final float left, final float right) {
		return Math.abs(left - right) <= 1.0e-5f;
	}
}

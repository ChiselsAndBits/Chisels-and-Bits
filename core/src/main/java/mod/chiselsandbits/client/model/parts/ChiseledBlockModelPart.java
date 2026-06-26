package mod.chiselsandbits.client.model.parts;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import com.communi.suggestu.scena.core.client.rendering.ExtendedBlockStateModelPart;
import com.google.common.base.Suppliers;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.util.TriState;
import net.minecraft.world.level.block.state.BlockState;

public record ChiseledBlockModelPart(
		BlockInformation source,
		BlockState appearance,
		QuadCollection quads,
		TriState ambientOcclusion,
		Material.Baked particleMaterial,
		int materialFlags,
		Supplier<Vector3fc[]> extendsCalculator
) implements BlockStateModelPart, ExtendedBlockStateModelPart {
	public ChiseledBlockModelPart(
			final BlockInformation source,
			final BlockState appearance,
			final QuadCollection quads,
			final TriState ambientOcclusion,
			final Material.Baked particleMaterial,
			final int materialFlags) {
		this(
				source,
				appearance,
				quads,
				ambientOcclusion,
				particleMaterial,
				materialFlags,
				Suppliers.memoize(
						() -> {
							Set<Vector3fc> set = new HashSet<>();

							for (BakedQuad bakedquad : quads.getAll()) {
								for (int i = 0; i < 4; i++) {
					set.add(bakedquad.position(i));
				}
							}

							return set.toArray(Vector3fc[]::new);
						})
		);
	}

	@Override
	public BlockState getBlockAppearance() {
		return appearance();
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable final Direction direction) {
		return quads.getQuads(direction);
	}

	@Override
	public boolean useAmbientOcclusion() {
		return ambientOcclusion().toBoolean(true);
	}
}

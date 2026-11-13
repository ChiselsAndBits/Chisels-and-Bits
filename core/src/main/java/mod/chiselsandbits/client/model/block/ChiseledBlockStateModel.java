package mod.chiselsandbits.client.model.block;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.communi.suggestu.scena.core.client.rendering.DataAwareBlockStateModel;
import com.communi.suggestu.scena.core.client.models.data.IBlockModelData;
import com.communi.suggestu.scena.core.client.rendering.terrain.ITerrainGeometryContext;
import com.communi.suggestu.scena.core.client.rendering.terrain.TerrainBackend;
import com.communi.suggestu.scena.core.client.rendering.terrain.TerrainGeometryBlockStateModel;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import mod.chiselsandbits.client.model.information.ChiseledBlockModelInformation;
import mod.chiselsandbits.registrars.ModModelProperties;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public class ChiseledBlockStateModel implements BlockStateModel, DataAwareBlockStateModel, TerrainGeometryBlockStateModel {
	private static final String TERRAIN_DIAGNOSTIC_PROPERTY = "chiselsandbits.debugNativeTerrainGeometry";
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final AtomicBoolean REPORTED_LEGACY_SELECTION = new AtomicBoolean();
	private static final AtomicBoolean REPORTED_NATIVE_EMISSION = new AtomicBoolean();
	private static final AtomicBoolean REPORTED_NATIVE_BOUNDARY_EMISSION = new AtomicBoolean();

	@Override
	public void collectParts(final RandomSource random, final List<BlockStateModelPart> output) {
		//Noop
	}

	@Override
	public Material.@NonNull Baked particleMaterial() {
		return new Material.Baked(
				Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(MissingTextureAtlasSprite.getLocation()),
				false
		);
	}

	@Override
	public @BakedQuad.MaterialFlags int materialFlags() {
		return 0;
	}

	protected @Nullable ChiseledBlockModelInformation getInformation(final BlockAndTintGetter blockAndTintGetter, final BlockPos blockPos) {
		final IBlockModelData modelData = ChiseledModelDataAccess.read(blockAndTintGetter, blockPos);
		if (modelData == null) {
			return null;
		}
		final ChiseledTerrainRenderData direct = modelData.getData(ModModelProperties.TERRAIN_RENDER_DATA);
		return direct == null ? modelData.getData(ModModelProperties.MODEL) : direct.fallback();
	}

	private @Nullable ChiseledTerrainModelDataSnapshot<ChiseledBlockModelInformation> getTerrainModelData(
			final BlockAndTintGetter blockAndTintGetter,
			final BlockPos blockPos
	) {
		final IBlockModelData modelData = ChiseledModelDataAccess.read(blockAndTintGetter, blockPos);
		if (modelData == null) {
			return null;
		}

		return ChiseledTerrainModelDataSnapshot.read(
				() -> modelData,
				ModModelProperties.MODEL,
				ModModelProperties.RECTANGLE_MODEL,
				ModModelProperties.TERRAIN_GEOMETRY
		);
	}

	@Override
	public @Nullable Object createGeometryKey(
			final BlockAndTintGetter blockAndTintGetter,
			final BlockPos blockPos,
			final BlockState blockState,
			final RandomSource randomSource) {
		final IBlockModelData modelData = ChiseledModelDataAccess.read(blockAndTintGetter, blockPos);
		final ChiseledTerrainRenderData direct = modelData == null ? null : modelData.getData(ModModelProperties.TERRAIN_RENDER_DATA);
		if (direct != null) {
			return direct.metadata().key();
		}
		final ChiseledBlockModelInformation modelInformation =
				modelData == null ? null : modelData.getData(ModModelProperties.MODEL);

		if (modelInformation == null) {
			return null;
		}

		return modelInformation.key();
	}

	@Override
	public boolean canEmitTerrainGeometry(final ITerrainGeometryContext context) {
		return preflightTerrainGeometry(context) != null;
	}

	@Override
	public @Nullable Object preflightTerrainGeometry(final ITerrainGeometryContext context) {
		if (context.backend() != TerrainBackend.VANILLA) {
			return null;
		}
		final IBlockModelData publication = ChiseledModelDataAccess.read(context.region(), context.pos());
		final ChiseledTerrainRenderData direct = publication == null ? null : publication.getData(ModModelProperties.TERRAIN_RENDER_DATA);
		if (direct != null) {
			return new DirectPlan(direct, context.prepareNativeGeometry(direct.geometry()));
		}

		final ChiseledTerrainModelDataSnapshot<ChiseledBlockModelInformation> modelData =
				getTerrainModelData(context.region(), context.pos());
		if (modelData == null || modelData.legacyModel() == null) {
			return null;
		}

		return ChiseledTerrainModelSelection.prepareNativeGeometry(
				modelData.legacyModel(),
				modelData.terrainGeometry(),
				context::prepareNativeGeometry
		);
	}

	@Override
	public void emitTerrainGeometry(final ITerrainGeometryContext context) {
		if (context.backend() != TerrainBackend.VANILLA) {
			throw new IllegalStateException("Chiseled geometry requires a supported terrain backend");
		}

		final Object preflight = preflightTerrainGeometry(context);
		if (preflight == null) {
			context.emitBakedModel(this);
			return;
		}

		emitTerrainGeometry(context, preflight);
	}

	@Override
	public void emitTerrainGeometry(final ITerrainGeometryContext context, final Object preflight) {
		if (context.backend() != TerrainBackend.VANILLA) {
			throw new IllegalStateException("Chiseled geometry requires a supported terrain backend");
		}
		if (preflight instanceof DirectPlan plan) {
			ChiseledTerrainColorScope.run(context.region(), context.pos(), plan.data().metadata().materials(), () -> {
				if (plan.emission() == null) {
					context.emitBakedModel(plan.data().fallback());
					reportTerrainSelection(context.pos(), false);
				} else {
					plan.emission().run();
					mod.chiselsandbits.debug.bench.TerrainWorldMeshCapture.onNativeEmission(context.pos());
					reportTerrainSelection(context.pos(), true);
					if (Boolean.getBoolean(TERRAIN_DIAGNOSTIC_PROPERTY)
							&& !REPORTED_NATIVE_BOUNDARY_EMISSION.get()
							&& plan.data().geometry().parts().stream().flatMap(part -> part.quads().stream())
									.anyMatch(quad -> quad.cullFace() != null)
							&& REPORTED_NATIVE_BOUNDARY_EMISSION.compareAndSet(false, true)) {
						LOGGER.info("C&B terrain diagnostic: native boundary rectangle geometry emitted at {}", context.pos().toShortString());
					}
				}
			});
			return;
		}

		if (!(preflight instanceof ChiseledTerrainModelSelection.NativePrepared<?> rawPlan)
				|| !(rawPlan.fallbackModel() instanceof ChiseledBlockModelInformation model)) {
			context.emitBakedModel(this);
			return;
		}

		if (rawPlan.usesNativeGeometry()) {
			rawPlan.nativeEmission().run();
			reportTerrainSelection(context.pos(), true);
			if (Boolean.getBoolean(TERRAIN_DIAGNOSTIC_PROPERTY)
					&& !REPORTED_NATIVE_BOUNDARY_EMISSION.get()
					&& model.parts().stream().anyMatch(part ->
							Arrays.stream(Direction.values()).anyMatch(direction -> !part.getQuads(direction).isEmpty()))
					&& REPORTED_NATIVE_BOUNDARY_EMISSION.compareAndSet(false, true)) {
				LOGGER.info("C&B terrain diagnostic: native boundary rectangle geometry emitted at {}", context.pos().toShortString());
			}
		} else {
			reportTerrainSelection(context.pos(), false);
			context.emitBakedModel(model);
		}
	}

	private static void reportTerrainSelection(final BlockPos pos, final boolean nativeGeometryEmitted) {
		if (!Boolean.getBoolean(TERRAIN_DIAGNOSTIC_PROPERTY)) {
			return;
		}

		final AtomicBoolean reported = nativeGeometryEmitted ? REPORTED_NATIVE_EMISSION : REPORTED_LEGACY_SELECTION;
		if (!reported.compareAndSet(false, true)) {
			return;
		}

		final String path = nativeGeometryEmitted ? "native rectangle geometry emitted" : "legacy baked model fallback";
		final Component message = Component.literal("C&B terrain diagnostic: selected " + path + " at " + pos.toShortString());
		LOGGER.info("{}", message.getString());
		Minecraft.getInstance().execute(() -> {
			final Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player != null) {
				minecraft.gui.hud.getChat().addClientSystemMessage(message);
			}
		});
	}

	@Override
	public void collectParts(
			final BlockAndTintGetter blockAndTintGetter,
			final BlockPos blockPos,
			final BlockState blockState,
			final RandomSource randomSource,
			final List<BlockStateModelPart> list) {
		final ChiseledBlockModelInformation modelInformation = getInformation(blockAndTintGetter, blockPos);

		if (modelInformation == null) {
			return;
		}

		modelInformation.collectParts(randomSource, list);
	}

	@Override
	public Material.Baked particleMaterial(final BlockAndTintGetter blockAndTintGetter, final BlockPos blockPos, final BlockState blockState) {
		final IBlockModelData modelData = ChiseledModelDataAccess.read(blockAndTintGetter, blockPos);
		final ChiseledTerrainRenderData direct = modelData == null ? null : modelData.getData(ModModelProperties.TERRAIN_RENDER_DATA);
		if (direct != null) {
			return direct.metadata().particleMaterial();
		}
		final ChiseledBlockModelInformation modelInformation = getInformation(blockAndTintGetter, blockPos);

		if (modelInformation == null) {
			return particleMaterial();
		}

		return modelInformation.particleMaterial();
	}

	@Override
	public @BakedQuad.MaterialFlags int materialFlags(final BlockAndTintGetter blockAndTintGetter, final BlockPos blockPos, final BlockState blockState) {
		final IBlockModelData modelData = ChiseledModelDataAccess.read(blockAndTintGetter, blockPos);
		final ChiseledTerrainRenderData direct = modelData == null ? null : modelData.getData(ModModelProperties.TERRAIN_RENDER_DATA);
		if (direct != null) {
			return direct.metadata().materialFlags();
		}
		ChiseledBlockModelInformation information = getInformation(blockAndTintGetter, blockPos);

		if (information == null) {
			return materialFlags();
		}

		return information.materialFlags();
	}

	private record DirectPlan(ChiseledTerrainRenderData data, @Nullable Runnable emission) {
	}

	public record Unbaked() implements BlockStateModel.Unbaked {
		public static final MapCodec<Unbaked> CODEC = MapCodec.unit(new Unbaked());

		@Override
		public void resolveDependencies(final Resolver resolver) {
		}

		@Override
		public BlockStateModel bake(final ModelBaker baker) {
			return new ChiseledBlockStateModel();
		}
	}
}

package mod.chiselsandbits.client.colors;

import java.util.List;

import com.communi.suggestu.scena.core.client.rendering.IColorManager;
import com.communi.suggestu.scena.core.client.models.data.IBlockModelData;
import com.communi.suggestu.scena.core.util.SingleBlockBlockAndTintGetter;
import it.unimi.dsi.fastutil.ints.IntList;
import mod.chiselsandbits.api.multistate.accessor.ISingleBlockAxisAlignedAreaAccessor;
import mod.chiselsandbits.api.neighborhood.IBlockNeighborhood;
import mod.chiselsandbits.client.model.block.ChiseledBlockStateModelManager;
import mod.chiselsandbits.client.model.block.ChiseledModelDataAccess;
import mod.chiselsandbits.client.model.block.ChiseledTerrainColorScope;
import mod.chiselsandbits.client.model.block.ChiseledTerrainRenderData;
import mod.chiselsandbits.client.model.information.ChiseledBlockModelInformation;
import mod.chiselsandbits.registrars.ModModelProperties;
import mod.chiselsandbits.client.model.builder.ChiseledBlockModelMaterial;
import mod.chiselsandbits.client.model.face.FaceManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ChiseledBlockColorProvider implements IColorManager.IDynamicColorProvider {
	@Override
	public void collect(final BlockState blockState, final BlockAndTintGetter blockAndTintGetter, final BlockPos blockPos, final IntList intList) {
		List<ChiseledBlockModelMaterial> materials = ChiseledTerrainColorScope.materials(blockAndTintGetter, blockPos);
		if (materials == null) {
			final IBlockModelData modelData = ChiseledModelDataAccess.read(blockAndTintGetter, blockPos);
			final ChiseledTerrainRenderData direct = modelData == null ? null : modelData.getData(ModModelProperties.TERRAIN_RENDER_DATA);
			final ChiseledBlockModelInformation legacy = modelData == null ? null : modelData.getData(ModModelProperties.MODEL);
			if (direct != null) {
				materials = direct.metadata().materials();
			} else if (legacy != null) {
				materials = legacy.materials();
			} else {
				// Retain the legacy path for non-C&B accessors and callers without published model data.
				final BlockEntity entity = blockAndTintGetter.getBlockEntity(blockPos);
				if (!(entity instanceof ISingleBlockAxisAlignedAreaAccessor accessor)) {
					return;
				}
				materials = ChiseledBlockStateModelManager.getInstance()
						.get(blockAndTintGetter, blockPos, accessor, IBlockNeighborhood.around(entity)).materials();
			}
		}
		intList.clear();
		intList.size(materials.size());

		for (int i = 0; i < materials.size(); i++) {
			final ChiseledBlockModelMaterial material = materials.get(i);

			if (material.tintIndex() == -1) {
				intList.set(i, -1);
				continue;
			}

			final BlockState materialState = material.blockInformation().blockState();
			final BlockTintSource tintSource;
			if (!materialState.getFluidState().isEmpty() && material.tintIndex() == FaceManager.FLUID_TINT_INDEX) {
				final FluidModel fluidModel = Minecraft.getInstance().getModelManager().getFluidStateModelSet()
						.get(materialState.getFluidState());
				tintSource = fluidModel.tintSource();
			} else {
				tintSource = Minecraft.getInstance().getBlockColors().getTintSource(materialState, material.tintIndex());
			}

			if (tintSource == null) {
				intList.set(i, -1);
				continue;
			}

			SingleBlockBlockAndTintGetter simulatedEnvironment = new SingleBlockBlockAndTintGetter.Builder()
					.withBlockState(material.blockInformation().blockState())
					.withPos(blockPos)
					.withSource(blockAndTintGetter)
					.withBlockEntity(() -> material.blockInformation().newBlockEntity(blockPos))
					.createSingleBlockBlockAndTintGetter();

			int tint = tintSource.colorInWorld(
					materialState,
					simulatedEnvironment,
					blockPos
			);

			intList.set(i, tint);
		}
	}
}

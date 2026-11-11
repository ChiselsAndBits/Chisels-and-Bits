package mod.chiselsandbits.client.chiseling.preview.render;

import java.util.List;

import com.communi.suggestu.scena.core.client.event.ISubmitCustomGeometryEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import mod.chiselsandbits.api.axissize.CollisionType;
import mod.chiselsandbits.api.chiseling.ChiselingOperation;
import mod.chiselsandbits.api.chiseling.IChiselingContext;
import mod.chiselsandbits.api.client.render.preview.chiseling.IChiselContextPreviewRenderer;
import mod.chiselsandbits.api.config.IClientConfiguration;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.client.render.ModRenderTypes;
import mod.chiselsandbits.voxelshape.VoxelShapeManager;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class ConfigurableColoredVoxelShapeChiselContextPreviewRenderer implements IChiselContextPreviewRenderer<ConfigurableColoredVoxelShapeChiselContextPreviewRenderer.RenderState> {

	static Identifier ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "default");

	public record RenderState(VoxelShape previewShape, @Nullable MutatorRenderState mutator, int inMaterialColor, int outsideMaterialColor, Vec3 position) {}
	public record MutatorRenderState(VoxelShape mutatorShape, int mutatorColor) {}

	@Override
	public Identifier getId() {
		return ID;
	}

	@Override
	public RenderState extractState(IChiselingContext context, LevelRenderState levelRenderState, ClientLevel clientLevel, Camera camera, DeltaTracker deltaTracker) {
		if (context.getMutator().isEmpty()) {
            throw new IllegalStateException("Cannot extract state for a context without a mutator.");
        }

		final BlockPos inWorldStartPos = context.getMutator().get().getInWorldStartBlockPoint();
		final Vec3 position = new Vec3(inWorldStartPos.getX(), inWorldStartPos.getY(), inWorldStartPos.getZ())
				.subtract(camera.position());

		final VoxelShape mutatorShape = VoxelShapeManager.getInstance()
				.get(context.getMutator().get(), context.getModeOfOperandus().isChiseling() ? CollisionType.NONE_AIR : CollisionType.ALL, false);

		final VoxelShape previewShape = context.getMode().getShape(context);


		final List<? extends Float> color = context.getModeOfOperandus() == ChiselingOperation.CHISELING
				? IClientConfiguration.getInstance().getPreviewChiselingColor().get()
				: IClientConfiguration.getInstance().getPreviewPlacementColor().get();

		final List<? extends Float> mutatorColor = context.getModeOfOperandus() == ChiselingOperation.CHISELING
				? IClientConfiguration.getInstance().getMutatorPreviewChiselingColor().get()
				: IClientConfiguration.getInstance().getMutatorPreviewPlacementColor().get();

		final int inMaterialColor = ARGB.colorFromFloat(
				getColorValue(color, 3, 1f) * 0.3f,
				getColorValue(color, 0, 0f) * 0.3f,
				getColorValue(color, 1, 0f) * 0.3f,
				getColorValue(color, 2, 0f) * 0.3f
		);
		final int outsideMaterialColor = ARGB.colorFromFloat(
				getColorValue(color, 3, 1f),
				getColorValue(color, 0, 0f),
				getColorValue(color, 1, 0f),
				getColorValue(color, 2, 0f)
		);
		final int mutatorMaterialColor = ARGB.colorFromFloat(
				getColorValue(mutatorColor, 3, 1f),
				getColorValue(mutatorColor, 0, 0f),
				getColorValue(mutatorColor, 1, 0f),
				getColorValue(mutatorColor, 2, 0f)
		);

		return new RenderState(
				previewShape,
				IClientConfiguration.getInstance().getMutatorPreviewDebug().get() ? new MutatorRenderState(mutatorShape, mutatorMaterialColor) : null,
				inMaterialColor,
				outsideMaterialColor,
				position
		);
	}

	@Override
	public void submit(RenderState renderState, SubmitNodeCollector submitNodeCollector, PoseStack poseStack, LevelRenderState levelRenderState, ISubmitCustomGeometryEvent.IRenderStateStore store) {
		poseStack.pushPose();
		poseStack.translate(renderState.position());
		submitNodeCollector.submitShapeOutline(
				poseStack,
				renderState.previewShape(),
				ModRenderTypes.CHISEL_PREVIEW_INSIDE_BLOCKS.get(),
				renderState.inMaterialColor(),
				2.5f,
				true
		);
		submitNodeCollector.submitShapeOutline(
				poseStack,
				renderState.previewShape(),
				ModRenderTypes.CHISEL_PREVIEW_OUTSIDE_BLOCKS.get(),
				renderState.outsideMaterialColor(),
				2.5f,
				true
		);
		if (renderState.mutator() != null) {
			submitNodeCollector.submitShapeOutline(
					poseStack,
					renderState.mutator().mutatorShape(),
					ModRenderTypes.CHISEL_PREVIEW_INSIDE_BLOCKS.get(),
					renderState.mutator().mutatorColor(),
					2.5f,
					true
			);
		}
		poseStack.popPose();
	}

	private static float getColorValue(final List<? extends Float> values, final int index, final float defaultValue) {
		if (values.size() <= index || index < 0) {
			return defaultValue;
		}

		final Number value = values.get(index);

		if (0 <= value.floatValue() && value.floatValue() <= 1f) {
			return value.floatValue();
		}

		return defaultValue;
	}
}

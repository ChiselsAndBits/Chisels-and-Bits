package mod.chiselsandbits.client.logic;

import static mod.chiselsandbits.block.entities.ChiseledBlockEntity.ONE_THOUSANDS;

import com.communi.suggestu.scena.core.client.event.IExtractLevelRenderStateEvent;
import com.communi.suggestu.scena.core.client.event.ISubmitCustomGeometryEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import mod.chiselsandbits.api.client.render.preview.placement.PlacementPreviewRenderMode;
import mod.chiselsandbits.api.config.IClientConfiguration;
import mod.chiselsandbits.api.item.multistate.IMultiStateItemStack;
import mod.chiselsandbits.api.item.pattern.IPatternItem;
import mod.chiselsandbits.api.item.wireframe.IWireframeProvidingItem;
import mod.chiselsandbits.api.multistate.StateEntrySize;
import mod.chiselsandbits.api.placement.IPlacementPreviewProvidingItem;
import mod.chiselsandbits.api.placement.PlacementResult;
import mod.chiselsandbits.client.render.ChiseledBlockPreviewRenderer;
import mod.chiselsandbits.utils.ItemStackUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.joml.Vector4f;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MultiStateBlockPreviewRenderHandler {

	public static void extractMultiStateBlockPreviewRenderState(LevelRenderState levelRenderState, ClientLevel clientLevel, Camera camera, DeltaTracker deltaTracker, IExtractLevelRenderStateEvent.IRenderStateStore store) {
		final HitResult rayTraceResult = Minecraft.getInstance().hitResult;

		if (!(rayTraceResult instanceof final BlockHitResult blockRayTraceResult) || blockRayTraceResult.getType() == HitResult.Type.MISS) {
			return;
		}

		final Player playerEntity = Minecraft.getInstance().player;

		if (playerEntity == null || playerEntity.isSpectator()) {
			return;
		}

		final ItemStack heldStack = ItemStackUtils.getMultiStateItemStackFromPlayer(playerEntity);

		if (!(heldStack.getItem() instanceof IWireframeProvidingItem wireframeItem)) {
			return;
		}
		Vec3 targetedRenderPos = wireframeItem.getTargetedPosition(heldStack, playerEntity, blockRayTraceResult).add(ONE_THOUSANDS, ONE_THOUSANDS, ONE_THOUSANDS);
		// Snap to bit grid
		final float bitSize = StateEntrySize.current().getSizePerBit();
		targetedRenderPos = targetedRenderPos.subtract(
				targetedRenderPos.x % bitSize + (targetedRenderPos.x < 0 ? bitSize : 0),
				targetedRenderPos.y % bitSize + (targetedRenderPos.y < 0 ? bitSize : 0),
				targetedRenderPos.z % bitSize + (targetedRenderPos.z < 0 ? bitSize : 0)
		);

		final PlacementResult placementResult;
		final boolean ignoreDepth, forceWireframe;

		if (heldStack.getItem() instanceof IPlacementPreviewProvidingItem placementPreviewItem) {
			placementResult = placementPreviewItem.getPlacementResult(heldStack, playerEntity, blockRayTraceResult);
			ignoreDepth = placementPreviewItem.ignoreDepthForPlacement(heldStack, placementResult);
			forceWireframe = false;
		} else {
			placementResult = PlacementResult.failure(wireframeItem.getWireFrameColor(heldStack, playerEntity, blockRayTraceResult));
			ignoreDepth = wireframeItem.ignoreDepth(heldStack);
			forceWireframe = true;
		}

		final IClientConfiguration clientConfig = IClientConfiguration.getInstance();
		final PlacementPreviewRenderMode success = clientConfig.getSuccessfulPlacementRenderMode().get();
		final PlacementPreviewRenderMode failure = clientConfig.getFailedPlacementRenderMode().get();

		final boolean asWireframe = forceWireframe
				|| (placementResult.isSuccess() && success.isWireframe())
				|| (!placementResult.isSuccess() && failure.isWireframe())
				|| (heldStack.getItem() instanceof IPatternItem patternItem && patternItem.createItemStack(heldStack).toBlockStack().isEmpty());

		ChiseledBlockPreviewRenderer.getInstance().extract(
				camera,
				store,
				targetedRenderPos,
				heldStack,
				placementResult,
				success,
				failure,
				ignoreDepth,
				asWireframe
		);
	}

	public static void submitMultiStateBlockPreviewRenderGeometry(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, LevelRenderState levelRenderState, ISubmitCustomGeometryEvent.IRenderStateStore iRenderStateStore) {
		ChiseledBlockPreviewRenderer.getInstance().submit(submitNodeCollector, poseStack, iRenderStateStore);
	}
}

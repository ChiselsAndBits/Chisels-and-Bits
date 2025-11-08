package mod.chiselsandbits.client.logic;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.chiselsandbits.api.client.render.preview.placement.PlacementPreviewRenderMode;
import mod.chiselsandbits.api.config.IClientConfiguration;
import mod.chiselsandbits.api.item.multistate.IMultiStateItemStack;
import mod.chiselsandbits.api.item.pattern.IPatternItem;
import mod.chiselsandbits.api.item.wireframe.IWireframeProvidingItem;
import mod.chiselsandbits.api.multistate.StateEntrySize;
import mod.chiselsandbits.api.placement.IPlacementPreviewProvidingItem;
import mod.chiselsandbits.api.placement.PlacementResult;
import mod.chiselsandbits.client.render.ChiseledBlockGhostRenderer;
import mod.chiselsandbits.client.render.ChiseledBlockWireframeRenderer;
import mod.chiselsandbits.utils.ItemStackUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector4f;

import static mod.chiselsandbits.block.entities.ChiseledBlockEntity.ONE_THOUSANDS;

public class MultiStateBlockPreviewRenderHandler
{

    public static void renderMultiStateBlockPreview(
        final PoseStack poseStack,
        final MultiBufferSource.BufferSource bufferSource)
    {
        final HitResult rayTraceResult = Minecraft.getInstance().hitResult;
        if (!(rayTraceResult instanceof final BlockHitResult blockRayTraceResult) || blockRayTraceResult.getType() == HitResult.Type.MISS)
        {
            return;
        }

        final Player playerEntity = Minecraft.getInstance().player;
        if (playerEntity == null || playerEntity.isSpectator())
        {
            return;
        }

        final ItemStack heldStack = ItemStackUtils.getMultiStateItemStackFromPlayer(playerEntity);
        if (!(heldStack.getItem() instanceof IWireframeProvidingItem wireframeItem))
        {
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
        if (heldStack.getItem() instanceof IPlacementPreviewProvidingItem placementPreviewItem)
        {
            placementResult = placementPreviewItem.getPlacementResult(heldStack, playerEntity, blockRayTraceResult);
            ignoreDepth = placementPreviewItem.ignoreDepthForPlacement(heldStack, placementResult);
            forceWireframe = false;
        }
        else
        {
            placementResult = PlacementResult.failure(wireframeItem.getWireFrameColor(heldStack, playerEntity, blockRayTraceResult));
            ignoreDepth = wireframeItem.ignoreDepth(heldStack);
            forceWireframe = true;
        }

        final IClientConfiguration clientConfig = IClientConfiguration.getInstance();
        final PlacementPreviewRenderMode success = clientConfig.getSuccessfulPlacementRenderMode().get();
        final PlacementPreviewRenderMode failure = clientConfig.getFailedPlacementRenderMode().get();
        if (forceWireframe
            || (placementResult.isSuccess() && success.isWireframe())
            || (!placementResult.isSuccess() && failure.isWireframe())
            || !renderGhost(poseStack, bufferSource, heldStack, targetedRenderPos, placementResult, success, failure, ignoreDepth))
        {
            renderWireFrame(poseStack, bufferSource, playerEntity, heldStack, wireframeItem, blockRayTraceResult, targetedRenderPos, placementResult.getColor(), ignoreDepth);
        }
    }

    private static void renderWireFrame(
        final PoseStack poseStack,
        final MultiBufferSource.BufferSource bufferSource,
        final Player playerEntity,
        final ItemStack heldStack,
        final IWireframeProvidingItem wireframeItem,
        final BlockHitResult blockRayTraceResult,
        final Vec3 targetedRenderPos,
        final Vector4f color,
        final boolean ignoreDepth)
    {
        final VoxelShape wireFrame = wireframeItem.getWireFrame(heldStack, playerEntity, blockRayTraceResult);

        ChiseledBlockWireframeRenderer.getInstance().renderShape(
            poseStack,
            wireFrame,
            targetedRenderPos,
            color,
            ignoreDepth
        );
    }

    private static boolean renderGhost(
        final PoseStack poseStack,
        final MultiBufferSource.BufferSource bufferSource,
        final ItemStack heldStack,
        final Vec3 targetedRenderPos,
        final PlacementResult placementResult,
        final PlacementPreviewRenderMode success,
        final PlacementPreviewRenderMode failure,
        final boolean ignoreDepth)
    {
        final ItemStack renderStack;
        if (heldStack.getItem() instanceof final IPatternItem patternItem)
        {
            final IMultiStateItemStack multiSate = patternItem.createItemStack(heldStack);
            renderStack = multiSate.toBlockStack();
            if (renderStack.isEmpty())
            {
                return false;
            }
        }
        else
        {
            renderStack = heldStack;
        }

        ChiseledBlockGhostRenderer.getInstance().renderGhost(
            poseStack,
            bufferSource,
            renderStack,
            targetedRenderPos,
            placementResult,
            success,
            failure,
            ignoreDepth
        );
        return true;
    }
}

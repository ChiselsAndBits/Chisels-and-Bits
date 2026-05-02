package mod.chiselsandbits.client.logic;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.chiselsandbits.api.item.withhighlight.IWithHighlightItem;
import mod.chiselsandbits.utils.ItemStackUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class SelectedObjectRenderHandler
{
    public static void renderCustomWorldHighlight(
        final LevelRenderer levelRenderer,
        final PoseStack poseStack,
        final MultiBufferSource.BufferSource bufferSource,
        final LevelRenderState levelRenderState,
        final float partialTicks
    )
    {
        final Player playerEntity = Minecraft.getInstance().player;
        if (playerEntity == null || playerEntity.isSpectator())
        {
            return;
        }

        final ItemStack heldStack = ItemStackUtils.getHighlightItemStackFromPlayer(playerEntity);
        if (heldStack.isEmpty())
        {
            return;
        }

        final Item heldItem = heldStack.getItem();
        if (!(heldItem instanceof final IWithHighlightItem withHighlightItem))
        {
            return;
        }

        if (withHighlightItem.shouldDrawDefaultHighlight(playerEntity))
        {
            return;
        }

        withHighlightItem.renderHighlight(
            playerEntity,
            levelRenderer,
            poseStack,
            bufferSource,
            levelRenderState,
            partialTicks
        );
    }
}

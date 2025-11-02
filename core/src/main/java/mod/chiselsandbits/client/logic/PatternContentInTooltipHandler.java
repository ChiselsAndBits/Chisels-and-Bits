package mod.chiselsandbits.client.logic;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.chiselsandbits.api.item.pattern.IPatternItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

public class PatternContentInTooltipHandler
{
    public static void doRenderContent(final ItemStack stack, final PoseStack poseStack, final int mouseX, final int mouseY)
    {
        if (!(stack.getItem() instanceof final IPatternItem patternItem))
            return;

        if (!Minecraft.getInstance().hasShiftDown())
            return;

        final ItemStack renderTarget = patternItem.createItemStack(stack).toBlockStack();

        //Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(poseStack, renderTarget, mouseX + 4, mouseY + Minecraft.getInstance().font.lineHeight * 2 + 4);
    }
}

package mod.chiselsandbits.client.tool.mode.icon;

import com.mojang.blaze3d.systems.RenderSystem;
import mod.chiselsandbits.api.client.tool.mode.icon.ISelectedToolModeIconRenderer;
import mod.chiselsandbits.api.item.withmode.IRenderableMode;
import mod.chiselsandbits.api.item.withmode.IToolMode;
import mod.chiselsandbits.api.item.withmode.IWithModeItem;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.client.icon.IconManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;

public class RootGroupTopLeftSelectedToolModeIconRenderer implements ISelectedToolModeIconRenderer
{
    static ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "group");

    @Override
    public ResourceLocation getId()
    {
        return ID;
    }

    @Override
    public void render(final GuiGraphics guiGraphics, final ItemStack stack)
    {
        if (!(stack.getItem() instanceof final IWithModeItem<?> modeItem))
            return;

        final IToolMode<?> mode = modeItem.getMode(stack);
        final IRenderableMode renderableMode = getRootRenderableMode(mode);

        final Vec2 positionVector = renderableMode.getPositionVector();
        final Vec2 scaleVector = renderableMode.getScaleVector();

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(positionVector.x, positionVector.y);
        guiGraphics.pose().scale(scaleVector.x, scaleVector.y);
        guiGraphics.pose().pushMatrix();

        TextureAtlasSprite sprite = IconManager.getInstance().getIcon(renderableMode.getIcon());

        guiGraphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            sprite,
            0,
            0,
            16,16,
            ARGB.colorFromFloat(
                (float) mode.getAlphaChannel(),
                (float) mode.getColorVector().x(),
                (float) mode.getColorVector().y(),
                (float) mode.getColorVector().z()
            ));

        guiGraphics.pose().popMatrix();
        guiGraphics.pose().popMatrix();
    }

    private IRenderableMode getRootRenderableMode(final IRenderableMode mode) {
        if (mode instanceof IToolMode && ((IToolMode<?>) mode).getGroup().isPresent())
        {
            return ((IToolMode<?>) mode).getGroup().get();
        }

        return mode;
    }
}

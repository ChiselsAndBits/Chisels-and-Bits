package mod.chiselsandbits.client.screens.widgets;

import com.communi.suggestu.scena.core.client.rendering.IExtendedGuiGraphics;
import com.communi.suggestu.scena.core.util.TransformationUtils;
import com.mojang.blaze3d.platform.Window;
import mod.chiselsandbits.api.client.screen.widget.AbstractChiselsAndBitsWidget;
import mod.chiselsandbits.api.item.multistate.IMultiStateItem;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshot;
import mod.chiselsandbits.api.util.ColorUtils;
import mod.chiselsandbits.client.screens.pips.RotatableItemRenderer;
import mod.chiselsandbits.multistate.snapshot.EmptySnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;
public class MultiStateSnapshotWidget extends AbstractChiselsAndBitsWidget
{

    private ItemStack snapshotBlockStack = ItemStack.EMPTY;
    private TrackingItemStackRenderState renderState = new TrackingItemStackRenderState();

    private Vec3 facingVector = Vec3.ZERO;
    private float scaleFactor = 1f;

    public MultiStateSnapshotWidget(final int x, final int y, final int width, final int height, final Component title)
    {
        super(x, y, width, height, title);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void renderWidget(final @NotNull GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks)
    {
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, ColorUtils.pack(139));
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width - 1, this.getY() + this.height - 1, ColorUtils.pack(55));
        graphics.fill(this.getX() + 1, this.getY() + 1, this.getX() + this.width, this.getY() + this.height, ColorUtils.pack(ColorUtils.FULL_CHANNEL));
        graphics.fill(this.getX() + 1, this.getY() + 1, this.getX() + this.width - 1, this.getY() + this.height - 1, ColorUtils.pack(ColorUtils.EMPTY_CHANNEL));

        scissorStart();

        if (!snapshotBlockStack.isEmpty()) {
            graphics.pose().pushMatrix();
            renderRotatableItemAndEffectIntoGui(graphics);
            graphics.pose().popMatrix();
        }

        scissorEnd();
    }

    @SuppressWarnings({"deprecation", "ConstantConditions"})
    public void renderRotatableItemAndEffectIntoGui(@NotNull GuiGraphics graphics) {
        final IExtendedGuiGraphics extendedGuiGraphics = extendGraphics(graphics);
        extendedGuiGraphics.submitPip(
            new RotatableItemRenderer.RenderState(
                renderState,
                TransformationUtils.quatFromXYZ(this.facingVector.toVector3f(), false),
                this.getX(),
                this.getY(),
                this.getX() + this.width,
                this.getY() + this.height,
                scaleFactor,
                extendedGuiGraphics.currentScissorArea()
            )
        );
    }

    public IMultiStateSnapshot getSnapshot()
    {
        if (!(snapshotBlockStack.getItem() instanceof IMultiStateItem))
            return EmptySnapshot.INSTANCE;

        return ((IMultiStateItem) snapshotBlockStack.getItem()).createItemStack(snapshotBlockStack).createSnapshot();
    }

    public void setSnapshot(final IMultiStateSnapshot snapshot)
    {
        this.snapshotBlockStack = snapshot.toItemStack().toBlockStack();
    }

    @Override
    protected void onDrag(final MouseButtonEvent event, final double mouseX, final double mouseY)
    {
        this.facingVector = this.facingVector.add(-mouseY * 10, mouseX * 10, 0);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double deltaX, final double deltaY)
    {
        this.scaleFactor += (float) (deltaY * 0.25);
        return true;
    }

    private void scissorStart()
    {
        Window mw = Minecraft.getInstance().getWindow();
        double sf = mw.getGuiScale();
        GL11.glScissor((int)((this.getX() + 1) * mw.getGuiScale()), (int)(mw.getGuiScaledHeight() * sf - height * sf - (getY() - 1) * sf), (int)((width - 2) * sf), (int)((height - 1) * sf));
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
    }

    protected void scissorEnd() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}

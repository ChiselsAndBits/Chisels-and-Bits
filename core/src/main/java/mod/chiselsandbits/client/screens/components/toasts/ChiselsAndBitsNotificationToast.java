package mod.chiselsandbits.client.screens.components.toasts;

import mod.chiselsandbits.api.util.IWithColor;
import mod.chiselsandbits.api.util.IWithIcon;
import mod.chiselsandbits.api.util.IWithText;
import mod.chiselsandbits.client.icon.IconManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class ChiselsAndBitsNotificationToast<T extends IWithColor & IWithIcon & IWithText> implements Toast
{
    private static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("toast/advancement");
    private final        T          contents;
    private Toast.Visibility wantedVisibility = Toast.Visibility.HIDE;

    public static <G extends IWithColor & IWithIcon & IWithText> void notifyOf(G contents) {
        Minecraft.getInstance().getToastManager().addToast(new ChiselsAndBitsNotificationToast<>(contents));
    }

    private ChiselsAndBitsNotificationToast(final T contents) {this.contents = contents;}

    @Override
    public @NonNull Visibility getWantedVisibility()
    {
        return this.wantedVisibility;
    }

    @Override
    public void update(final ToastManager toastManager, final long visibilityTime)
    {
        this.wantedVisibility = visibilityTime >= 5000.0 * toastManager.getNotificationDisplayTimeMultiplier() ? Visibility.HIDE : Visibility.SHOW;
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor guiGraphics, final @NonNull Font font, final long time)
    {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, 0, 0, this.width(), this.height());

        List<FormattedCharSequence> list = Minecraft.getInstance().font.split(contents.getText(), 125);
        if (list.size() == 1)
        {
            guiGraphics.text(Minecraft.getInstance().font, contents.getText(), 30, 12, -1);
        }
        else
        {
            int fontColor = Mth.floor(Mth.clamp((float) (time) / 40.0F, 0.0F, 1.0F) * 252.0F) << 24 | 67108864;
            int verticalOffset = this.height() / 2 - list.size() * 9 / 2;

            for (FormattedCharSequence formattedcharsequence : list)
            {
                guiGraphics.text(Minecraft.getInstance().font, formattedcharsequence, 30, verticalOffset, 16777215 | fontColor);
                verticalOffset += 9;
            }
        }

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(8,8);
        guiGraphics.pose().pushMatrix();

        final int color = ARGB.colorFromFloat(
            (float) contents.getAlphaChannel(),
            (float) contents.getColorVector().x(),
            (float) contents.getColorVector().y(),
            (float) contents.getColorVector().z()
        );
        guiGraphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            IconManager.getInstance().getIcon(contents.getIcon()),
            0,
            0,
            16,16,
            color);

        guiGraphics.pose().popMatrix();
        guiGraphics.pose().popMatrix();
    }
}

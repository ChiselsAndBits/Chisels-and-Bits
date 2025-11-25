package mod.chiselsandbits.client.screens.widgets;

import com.communi.suggestu.scena.core.client.rendering.IExtendedGuiGraphics;
import mod.chiselsandbits.api.client.screen.widget.AbstractChiselsAndBitsWidget;
import mod.chiselsandbits.api.config.IClientConfiguration;
import mod.chiselsandbits.api.item.withmode.IRenderableMode;
import mod.chiselsandbits.api.item.withmode.group.IToolModeGroup;
import mod.chiselsandbits.client.icon.IconManager;
import mod.chiselsandbits.client.screens.pips.Torus;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class RadialSelectionWidget extends AbstractChiselsAndBitsWidget
{

    private final Supplier<IRenderableMode>       currentlySelectedModeSupplier;
    private final Consumer<IRenderableMode>       currentlyHoveredModeCallback;
    private final Consumer<IRenderableMode>       currentlyClickedModeCallback;
    private final List<? extends IRenderableMode> modes;
    private final float                           sectionArcAngle;
    private final float                           sectionStartAngle;
    private final float                           innerSelectionRadius;
    private final float                           outerSelectionRadius;
    private final boolean                         keepSelectionWhenBeyondOuterSelectionRadius;
    private final float                           innerRadius;
    private final float                           outerRadius;
    private final float                           iconSize;
    private final float                           iconScaleFactor;
    private final int                             iconTextSpacer;
    private final Font                            fontRenderer;

    private final float centerX;
    private final float centerY;

    public RadialSelectionWidget(
        final Screen screen,
        final int width,
        final int height,
        final Component message,
        final Supplier<IRenderableMode> currentlySelectedModeSupplier,
        final Consumer<IRenderableMode> currentlyHoveredModeCallback,
        final Consumer<IRenderableMode> currentlyClickedModeCallback,
        final List<IRenderableMode> modes,
        final float sectionArcAngle,
        final float sectionStartAngle,
        final boolean hideInactiveIcons,
        final float innerSelectionRadius,
        final float outerSelectionRadius,
        final boolean keepSelectionWhenBeyondOuterSelectionRadius,
        final float innerRadius,
        final float outerRadius,
        final float iconSize,
        final float iconScaleFactor,
        final int iconTextSpacer,
        final Font fontRenderer)
    {
        this(
            (int) (screen.width / 2f - (width / 2f)),
            (int) (screen.height / 2f - (height / 2f)),
            width,
            height,
            message,
            currentlySelectedModeSupplier,
            currentlyHoveredModeCallback,
            currentlyClickedModeCallback,
            modes,
            sectionArcAngle,
            sectionStartAngle,
            hideInactiveIcons, innerSelectionRadius,
            outerSelectionRadius,
            keepSelectionWhenBeyondOuterSelectionRadius,
            innerRadius,
            outerRadius,
            iconSize,
            iconScaleFactor,
            iconTextSpacer,
            fontRenderer
        );
    }

    public RadialSelectionWidget(
        final int x,
        final int y,
        final int width,
        final int height,
        final Component message,
        final Supplier<IRenderableMode> currentlySelectedModeSupplier,
        final Consumer<IRenderableMode> currentlyHoveredModeCallback,
        final Consumer<IRenderableMode> currentlyClickedModeCallback,
        final List<? extends IRenderableMode> modes,
        final float sectionArcAngle,
        final float sectionStartAngle,
        final boolean hideInactiveIcons,
        final float innerSelectionRadius,
        final float outerSelectionRadius,
        final boolean keepSelectionWhenBeyondOuterSelectionRadius,
        final float innerRadius,
        final float outerRadius,
        final float iconSize,
        final float iconScaleFactor,
        final int iconTextSpacer,
        final Font fontRenderer
    )
    {
        super(x, y, width, height, message);
        this.currentlySelectedModeSupplier = currentlySelectedModeSupplier;
        this.currentlyHoveredModeCallback = currentlyHoveredModeCallback;
        this.currentlyClickedModeCallback = currentlyClickedModeCallback;
        this.keepSelectionWhenBeyondOuterSelectionRadius = keepSelectionWhenBeyondOuterSelectionRadius;
        this.modes = modes.stream().filter(mode -> !hideInactiveIcons || mode.isActive()).collect(Collectors.toList());
        this.sectionArcAngle = sectionArcAngle;
        this.sectionStartAngle = sectionStartAngle;
        this.innerSelectionRadius = innerSelectionRadius;
        this.outerSelectionRadius = outerSelectionRadius;
        this.innerRadius = innerRadius;
        this.outerRadius = outerRadius;
        this.iconSize = iconSize;
        this.iconScaleFactor = iconScaleFactor;
        this.iconTextSpacer = iconTextSpacer;
        this.fontRenderer = fontRenderer;

        this.centerX = x + (width / 2f);
        this.centerY = y + (height / 2f);
    }

    public <G extends IToolModeGroup> RadialSelectionWidget(
        final AbstractChiselsAndBitsWidget widget,
        final int width,
        final int height,
        final Component message,
        final Supplier<IRenderableMode> currentlySelectedModeSupplier,
        final Consumer<IRenderableMode> currentlyHoveredModeCallback,
        final Consumer<IRenderableMode> currentlyClickedModeCallback,
        final List<? extends IRenderableMode> modes,
        final float sectionArcAngle,
        final float sectionStartAngle,
        final boolean hideInactiveIcons,
        final float innerSelectionRadius,
        final float outerSelectionRadius,
        final boolean keepSelectionWhenBeyondOuterSelectionRadius,
        final float innerRadius,
        final float outerRadius,
        final float iconSize,
        final float iconScaleFactor,
        final int iconTextSpacer,
        final Font fontRenderer)
    {
        this(
            (int) (widget.getX() + (widget.getWidth() / 2f) - (width / 2f)),
            (int) (widget.getY() + (widget.getHeight() / 2f) - (height / 2f)),
            width,
            height,
            message,
            currentlySelectedModeSupplier,
            currentlyHoveredModeCallback,
            currentlyClickedModeCallback,
            modes,
            sectionArcAngle,
            sectionStartAngle,
            hideInactiveIcons,
            innerSelectionRadius,
            outerSelectionRadius,
            keepSelectionWhenBeyondOuterSelectionRadius,
            innerRadius,
            outerRadius,
            iconSize,
            iconScaleFactor,
            iconTextSpacer,
            fontRenderer
        );
    }

    @Override
    public void renderWidget(final @NotNull GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks)
    {
        final IRenderableMode current = currentlySelectedModeSupplier.get();

        final int selectableItemCount = modes.size();
        if (selectableItemCount == 0)
        {
            return;
        }

        // center of screen
        int centerX = (int) (this.getX() + (this.width / 2f));
        int centerY = (int) (this.getY() + (this.height / 2f));

        graphics.pose().pushMatrix();

        final float itemArcAngle = sectionArcAngle / selectableItemCount;

        final float mouseAngle = calculateMouseAngle(mouseX, mouseY, centerX, centerY);
        final float mouseRadius = calculateMouseRadius(mouseX, mouseY, centerX, centerY);

        final float inSectionMouseAngle = mouseAngle - sectionStartAngle;
        final boolean mouseIsInSectionArc = inSectionMouseAngle >= 0 && inSectionMouseAngle <= sectionArcAngle;
        boolean isMouseInSection = mouseIsInSectionArc && mouseRadius >= innerSelectionRadius && mouseRadius <= outerSelectionRadius;

        int hoveredItemIndex = !isMouseInSection ? -1 : (int) (inSectionMouseAngle / itemArcAngle);
        if (!isMouseInSection && current != null && modes.contains(current) && keepSelectionWhenBeyondOuterSelectionRadius)
        {
            if (mouseIsInSectionArc && mouseRadius >= innerSelectionRadius)
            {
                //We are at least in our arc bundle
                //Lets just assume we are in a sub menu of hours and keep the current.
                isMouseInSection = true;
                hoveredItemIndex = modes.indexOf(current);
            }
        }

        final int renderableHoveredItemIndex = hoveredItemIndex;
        modes.forEach(mode -> {
            final int modeIndex = modes.indexOf(mode);
            final boolean isSelected = current != null && mode == current;
            final boolean isHovered = renderableHoveredItemIndex == modeIndex;

            final float itemTargetAngle = ((modeIndex + 0.5f) * itemArcAngle) + sectionStartAngle;

            if (mode.isActive())
            {
                drawSelectableSection(
                    graphics,
                    sectionArcAngle,
                    innerRadius,
                    outerRadius,
                    centerX,
                    centerY,
                    selectableItemCount,
                    itemTargetAngle,
                    isSelected,
                    isHovered
                );
            }
            else
            {
                drawDeactivatedSection(
                    graphics,
                    sectionArcAngle,
                    innerRadius,
                    outerRadius,
                    centerX,
                    centerY,
                    selectableItemCount,
                    itemTargetAngle
                );
            }
        });

        if (isMouseInSection && hoveredItemIndex >= 0 && hoveredItemIndex < modes.size() && modes.get(hoveredItemIndex).isActive())
        {
            if (IClientConfiguration.getInstance().getEnableMouseIndicatorInRadialMenu().get())
            {
                float startOfMouseArcAngle = mouseAngle - (itemArcAngle / 2);
                float mouseArcAngle = itemArcAngle;

                if (sectionArcAngle != 360)
                {
                    if (startOfMouseArcAngle < sectionStartAngle)
                    {
                        mouseArcAngle -= (sectionStartAngle - startOfMouseArcAngle);
                        startOfMouseArcAngle = sectionStartAngle;
                    }

                    if ((startOfMouseArcAngle + mouseArcAngle) > (sectionStartAngle + sectionArcAngle))
                    {
                        mouseArcAngle = (sectionStartAngle + sectionArcAngle) - startOfMouseArcAngle;
                    }
                }

                drawTorus(graphics,
                    startOfMouseArcAngle - 90,
                    mouseArcAngle,
                    innerRadius,
                    outerRadius,
                    centerX,
                    centerY,
                    ARGB.colorFromFloat(0.3F, 0.8F, 0.8F, 0.8F)
                );
            }

            if (hoveredItemIndex >= 0 && modes.get(hoveredItemIndex) != current)
            {
                currentlyHoveredModeCallback.accept(modes.get(hoveredItemIndex));
            }
        }
        else if (current != null)
        {
            currentlyHoveredModeCallback.accept(null);
        }


        graphics.pose().translate(centerX, centerY);

        if (modes.size() > 1 || this.innerRadius != 0f)
        {
            modes.forEach(mode -> {
                if (mode.isActive())
                {
                    final int modeIndex = modes.indexOf(mode);
                    final float itemTargetAngle = ((modeIndex + 0.5f) * itemArcAngle) + sectionStartAngle;
                    renderModeIconAtAngle(graphics, innerRadius, outerRadius, itemTargetAngle, iconScaleFactor, iconTextSpacer, mode, fontRenderer);
                }
            });
        } else if (modes.size() == 1) {
            renderModeIconCentered(graphics, iconScaleFactor, iconTextSpacer, modes.getFirst(), fontRenderer);
        }

        graphics.pose().popMatrix();
    }

    private static float calculateMouseAngle(final float mouseX, final float mouseY, final float centerX, final float centerY)
    {
        final float xDiff = mouseX - centerX;
        final float yDiff = mouseY - centerY;

        float mouseAngle = (float) Math.toDegrees(Math.atan2(yDiff, xDiff)) - 270f;
        while (mouseAngle < 0)
        {
            mouseAngle += 360;
        }
        return mouseAngle;
    }

    private static float calculateMouseRadius(final float mouseX, final float mouseY, final float centerX, final float centerY)
    {
        final float xDiff = mouseX - centerX;
        final float yDiff = mouseY - centerY;

        return (float) Math.sqrt(xDiff * xDiff + yDiff * yDiff);
    }

    @SuppressWarnings("deprecation")
    private static void drawSelectableSection(
        @NotNull final GuiGraphics graphics,
        final float sectionArcAngle,
        final float innerRadius,
        final float outerRadius,
        final int centerX, final int centerY,
        final int itemCountInSection,
        final float itemTargetAngle,
        final boolean isSelected,
        final boolean isHovered
    )
    {
        final float itemArcAngle = sectionArcAngle / itemCountInSection;
        final float itemRenderAngle = itemTargetAngle - 90F;

        final float sectionStartAngle = itemRenderAngle - (itemArcAngle / 2);

        drawTorus(
            graphics,
            sectionStartAngle,
            itemArcAngle,
            innerRadius,
            outerRadius,
            centerX,
            centerY,
            ARGB.colorFromFloat(0.3f, 0.3f, 0.3f, 0.3f)
        );

        if (isSelected)
        {
            drawTorus(
                graphics,
                sectionStartAngle,
                itemArcAngle,
                innerRadius,
                outerRadius,
                centerX,
                centerY,
                ARGB.colorFromFloat(0.7F, 0.4F, 0.4F, 0.4F)
            );
        }

        if (isHovered)
        {
            drawTorus(
                graphics,
                sectionStartAngle,
                itemArcAngle,
                innerRadius,
                outerRadius,
                centerX,
                centerY,
                ARGB.colorFromFloat(0.7F, 0.7F, 0.7F, 0.7F)
            );
        }
    }

    @SuppressWarnings("deprecation")
    private static void drawDeactivatedSection(
        @NotNull final GuiGraphics graphics,
        final float sectionArcAngle,
        final float innerRadius,
        final float outerRadius,
        final int centerX, final int centerY,
        final int itemCountInSection,
        final float itemTargetAngle
    )
    {
        final float itemArcAngle = sectionArcAngle / itemCountInSection;
        final float itemRenderAngle = itemTargetAngle - 90F;

        final float sectionStartAngle = itemRenderAngle - (itemArcAngle / 2);

        drawTorus(
            graphics,
            sectionStartAngle,
            itemArcAngle,
            innerRadius,
            outerRadius,
            centerX,
            centerY,
            ARGB.colorFromFloat(0.1f, 0.1f, 0.1f, 0.1f)
        );
    }

    private static void drawTorus(GuiGraphics graphics, float startAngle, float sizeAngle, float inner, float outer, final int centerX, final int centerY, int color)
    {
        final IExtendedGuiGraphics extendedGuiGraphics = extendGraphics(graphics);

        extendedGuiGraphics.submitPip(
            new Torus.RenderState(
                startAngle, sizeAngle,
                inner, outer, color,
                centerX, centerY,
                extendedGuiGraphics.currentScissorArea()
            )
        );
    }

    private void renderModeIconAtAngle(
        final @NotNull GuiGraphics graphics,
        final float innerRadius,
        final float outerRadius,
        final float itemTargetAngle,
        final float iconScaleFactor,
        final int iconTextSpacer,
        final @NotNull IRenderableMode mode,
        final Font fontRenderer)
    {
        float workingAngle = itemTargetAngle - 90;
        while (workingAngle < 0)
        {
            workingAngle += 360;
        }

        final float itemCenterX = (float) Math.cos(Math.toRadians(workingAngle)) * (innerRadius + outerRadius) / 2F;
        final float itemCenterY = (float) Math.sin(Math.toRadians(workingAngle)) * (innerRadius + outerRadius) / 2F;

        final Component name = mode.getMultiLineDisplayName();
        final List<FormattedCharSequence> lines = fontRenderer.split(name, 75);

        final int itemHeight = mode.shouldRenderDisplayNameInMenu() ?
            (int) ((iconSize * iconScaleFactor) + iconTextSpacer + (fontRenderer.lineHeight * lines.size()))
            : (int) (iconSize * iconScaleFactor);

        final int iconStartX = (int) (itemCenterX - ((iconSize * iconScaleFactor) / 2f));
        final int iconStartY = (int) (itemCenterY - (itemHeight / 2f));

        renderModeIconAt(graphics, iconScaleFactor, iconTextSpacer, mode, fontRenderer, iconStartX, iconStartY, itemCenterX, itemCenterY, lines);
    }

    private void renderModeIconCentered(
        final @NotNull GuiGraphics graphics,
        final float iconScaleFactor,
        final int iconTextSpacer,
        final @NotNull IRenderableMode mode,
        final Font fontRenderer
    ) {
        final float itemCenterX = 0f;
        final float itemCenterY = 0f;

        final Component name = mode.getMultiLineDisplayName();
        final List<FormattedCharSequence> lines = fontRenderer.split(name, 75);

        final int itemHeight = mode.shouldRenderDisplayNameInMenu() ?
            (int) ((iconSize * iconScaleFactor) + iconTextSpacer + (fontRenderer.lineHeight * lines.size()))
            : (int) (iconSize * iconScaleFactor);

        final int iconStartX = (int) (itemCenterX - ((iconSize * iconScaleFactor) / 2f));
        final int iconStartY = (int) (itemCenterY - (itemHeight / 2f));

        renderModeIconAt(graphics, iconScaleFactor, iconTextSpacer, mode, fontRenderer, iconStartX, iconStartY, itemCenterX, itemCenterY, lines);
    }

    private static void renderModeIconAt(
        final @NotNull GuiGraphics graphics,
        final float iconScaleFactor,
        final int iconTextSpacer,
        final @NotNull IRenderableMode mode,
        final Font fontRenderer,
        final int iconStartX,
        final int iconStartY,
        final float itemCenterX,
        final float itemCenterY,
        final List<FormattedCharSequence> lines)
    {
        graphics.pose().pushMatrix();
        graphics.pose().scale(iconScaleFactor);
        graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            IconManager.getInstance().getIcon(mode.getIcon()),
            (int) (iconStartX * 1f / iconScaleFactor),
            (int) (iconStartY * 1f / iconScaleFactor),
            16, 16,
            ARGB.colorFromFloat(
                (float) mode.getAlphaChannel(),
                (float) mode.getColorVector().x(),
                (float) mode.getColorVector().y(),
                (float) mode.getColorVector().z()
            ));
        graphics.pose().popMatrix();

        graphics.pose().pushMatrix();
        if (mode.shouldRenderDisplayNameInMenu())
        {
            graphics.pose().translate(itemCenterX, itemCenterY);
            graphics.pose().scale(0.6F * iconScaleFactor, 0.6F * iconScaleFactor);

            int offset = 0;
            for (final FormattedCharSequence line : lines)
            {
                graphics.drawString(fontRenderer, line, (int) (fontRenderer.width(line) / -2f), iconTextSpacer + offset, 0xCCFFFFFF);
                offset += fontRenderer.lineHeight;
            }
        }

        graphics.pose().popMatrix();
    }

    @Override
    public void onClick(final @NotNull MouseButtonEvent event, final boolean isDoubleClick)
    {
        if (!this.active || !this.visible)
        {
            return;
        }

        final int selectableItemCount = modes.size();
        if (selectableItemCount == 0)
        {
            return;
        }

        final float itemArcAngle = sectionArcAngle / selectableItemCount;

        final float mouseAngle = calculateMouseAngle((float) event.x(), (float) event.y(), centerX, centerY);
        final float mouseRadius = calculateMouseRadius((float) event.x(), (float) event.y(), centerX, centerY);

        final float inSectionMouseAngle = mouseAngle - sectionStartAngle;
        final boolean mouseIsInSectionArc = inSectionMouseAngle >= 0 && inSectionMouseAngle <= sectionArcAngle;
        final boolean isMouseInSection = mouseIsInSectionArc && mouseRadius >= innerSelectionRadius && mouseRadius <= outerSelectionRadius;

        final int hoveredItemIndex = !isMouseInSection ? -1 : (int) (inSectionMouseAngle / itemArcAngle);
        if (hoveredItemIndex == -1)
        {
            return;
        }

        this.currentlyClickedModeCallback.accept(modes.get(hoveredItemIndex));
    }
}

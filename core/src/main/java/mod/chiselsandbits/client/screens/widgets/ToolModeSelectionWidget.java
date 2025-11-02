package mod.chiselsandbits.client.screens.widgets;

import com.google.common.collect.Lists;
import mod.chiselsandbits.ChiselsAndBits;
import mod.chiselsandbits.api.client.screen.AbstractChiselsAndBitsScreen;
import mod.chiselsandbits.api.client.screen.widget.AbstractChiselsAndBitsWidget;
import mod.chiselsandbits.api.item.withmode.IRenderableMode;
import mod.chiselsandbits.api.item.withmode.IToolMode;
import mod.chiselsandbits.api.item.withmode.IWithModeItem;
import mod.chiselsandbits.api.item.withmode.group.IToolModeGroup;
import mod.chiselsandbits.api.util.LocalStrings;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.client.icon.IconManager;
import mod.chiselsandbits.network.packets.HeldToolModeChangedPacket;
import mod.chiselsandbits.utils.ItemStackUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ToolModeSelectionWidget<M extends IToolMode<G>, G extends IToolModeGroup> extends AbstractChiselsAndBitsWidget
{
    private static final int PAGE_SIZE = 6;

    private static final int     PAGING_WIDGET_SIZE                = 35;
    private static final float   FULL_ROTATION_IN_DEGREES          = 360f;
    private static final float   HALF_ROTATION_IN_DEGREES          = 180f;
    private static final float   NO_ROTATION_IN_DEGREES            = 0;
    private static final float   PAGING_INNER_SELECTION_RADIUS     = 0;
    private static final float   PAGING_OUTER_SELECTION_RADIUS     = PAGING_WIDGET_SIZE;
    private static final float   PAGING_INNER_RENDERING_RADIUS     = 0;
    private static final float   PAGING_OUTER_RENDERING_RADIUS     = PAGING_WIDGET_SIZE;
    private static final boolean DISCARD_SELECTION_OUTSIDE_OF_AREA = false;
    private static final float   DEFAULT_ICON_SIZE                 = 16;
    private static final float   NO_ICON_SCALING_FACTOR            = 1f;
    private static final int     DEFAULT_ICON_NAME_SPACER          = 6;

    private static final int     MAIN_WIDGET_SIZE               = 80;
    private static final float   MAIN_TORUS_INNER               = 40;
    private static final float   MAIN_TORUS_OUTER               = MAIN_WIDGET_SIZE;
    private static final boolean KEEP_SELECTION_OUTSIDE_OF_AREA = true;

    private static final int   OUTER_WIDGET_SIZE              = 120;
    private static final float OUTER_TORUS_INNER              = 85;
    private static final float OUTER_TORUS_OUTER              = OUTER_WIDGET_SIZE;
    private static final float OUTER_TORUS_ICON_SCALE_FACTOR  = 0.8f;
    private static final int   OUTER_DEFAULT_ICON_TEXT_SPACER = 2;

    private static final boolean SHOW_INACTIVE_MODES = false;
    private static final boolean HIDE_INACTIVE_MODES = true;

    private final AbstractChiselsAndBitsScreen owner;
    private final IWithModeItem<M>             toolModeItem;
    private final ItemStack                    sourceStack;

    private final List<M>                           modes            = new ArrayList<>();
    private final LinkedList<List<IRenderableMode>> pages            = new LinkedList<>();
    private       int                               currentPageIndex = 0;

    private final List<M>         noneGroupedModes = new ArrayList<>();
    private final Map<G, List<M>> groupings        = new HashMap<>();

    private       RadialSelectionWidget pageSelectionWidget = null;
    private final PageSelectionMode     previousMode        = new PageSelectionMode(true);
    private final PageSelectionMode     nextMode            = new PageSelectionMode(false);
    private final List<IRenderableMode> pagingModes         = Lists.newArrayList(nextMode, previousMode); //Needs to be in reverse order since we render clockwise from the top.

    private RadialSelectionWidget currentPageSelectionWidget = null;
    private List<IRenderableMode> page                       = new LinkedList<>();
    private IRenderableMode       currentHoveredPage         = null;

    private       RadialSelectionWidget currentGroupSelectionWidget = null;
    private final List<M>               group                       = new LinkedList<>();
    private       IRenderableMode       lastFrameMainSelectedToolMode;
    private       IRenderableMode       mainSelectedToolMode;
    private       M                     selectedOuterToolMode;

    /**
     * Creates a new tool mode selection widget centered on the given screen.
     * Loads the tool modes from the given item and stack.
     *
     * @param screen       The screen to center the widget on.
     * @param toolModeItem The item supporting tool modes in question.
     * @param sourceStack  The source stack of the item.
     */
    public ToolModeSelectionWidget(
        final AbstractChiselsAndBitsScreen screen,
        final IWithModeItem<M> toolModeItem,
        final ItemStack sourceStack)
    {
        super(
            screen.width / 2 - (Math.min(screen.width, screen.height) / 2),
            screen.height / 2 - (Math.min(screen.width, screen.height) / 2),
            Math.min(screen.width, screen.height),
            Math.min(screen.width, screen.height),
            LocalStrings.ToolMenuSelectorName.getText(sourceStack.getDisplayName())
        );

        this.owner = screen;
        this.toolModeItem = toolModeItem;
        this.sourceStack = sourceStack;
        this.visible = false;
    }

    @SuppressWarnings({"SuspiciousMethodCalls", "unchecked"})
    @Override
    public void init()
    {
        super.init();

        modes.clear();
        modes.addAll(this.toolModeItem.getPossibleModes());

        modes.stream()
            .filter(mode -> mode.getGroup().isPresent())
            .forEach(mode -> groupings.computeIfAbsent(mode.getGroup().get(), (group) -> Lists.newArrayList()).add(mode));
        modes.stream()
            .filter(mode -> mode.getGroup().isEmpty())
            .forEach(noneGroupedModes::add);

        final List<IRenderableMode> allPagesCombined = new ArrayList<>();
        allPagesCombined.addAll(groupings.keySet());
        allPagesCombined.addAll(noneGroupedModes);
        allPagesCombined.sort(Comparator.comparingInt(renderableMode -> {
            if (noneGroupedModes.contains(renderableMode))
            {
                return modes.indexOf(renderableMode);
            }

            return modes.indexOf(groupings.get((G) renderableMode).iterator().next());
        }));

        if (allPagesCombined.isEmpty())
        {
            return;
        }

        while (!allPagesCombined.isEmpty())
        {
            final List<IRenderableMode> page = new LinkedList<>();
            for (int i = 0; i < PAGE_SIZE; i++)
            {
                if (!allPagesCombined.isEmpty())
                {
                    page.add(allPagesCombined.remove(0));
                }
            }
            pages.addLast(page);
        }

        rebuildPagingControl();

        rebuildPageControl();
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics p_268228_, int p_268034_, int p_268009_, float p_268085_)
    {

    }

    private void nextPage()
    {
        if (!nextMode.isActive())
        {
            return;
        }

        currentPageIndex++;
        rebuildPagingControl();
        rebuildPageControl();
    }

    private void previousPage()
    {
        if (!previousMode.isActive())
        {
            return;
        }

        currentPageIndex--;
        rebuildPagingControl();
        rebuildPageControl();
    }

    private void rebuildPagingControl()
    {
        removePagingControl();

        this.pageSelectionWidget = owner.addRenderableWidget(new RadialSelectionWidget(
            this,
            PAGING_WIDGET_SIZE * 2,
            PAGING_WIDGET_SIZE * 2,
            LocalStrings.ToolMenuPageSelectorName.getText(),
            () -> this.currentHoveredPage,
            this::onPageSelectorHover,
            this::onPageSelected,
            pagingModes,
            FULL_ROTATION_IN_DEGREES,
            NO_ROTATION_IN_DEGREES,
            HIDE_INACTIVE_MODES,
            PAGING_INNER_SELECTION_RADIUS,
            PAGING_OUTER_SELECTION_RADIUS,
            DISCARD_SELECTION_OUTSIDE_OF_AREA,
            PAGING_INNER_RENDERING_RADIUS,
            PAGING_OUTER_RENDERING_RADIUS,
            DEFAULT_ICON_SIZE,
            NO_ICON_SCALING_FACTOR,
            DEFAULT_ICON_NAME_SPACER,
            getFont()
        ));

        if (pages.size() == 1)
        {
            this.pageSelectionWidget.visible = false;
            this.pageSelectionWidget.active = false;
        }
    }

    private void rebuildPageControl()
    {
        removePageControl();

        this.mainSelectedToolMode = null;
        this.lastFrameMainSelectedToolMode = null;
        this.selectedOuterToolMode = null;

        this.page = this.pages.get(this.currentPageIndex);

        this.currentPageSelectionWidget = owner.addRenderableWidget(new RadialSelectionWidget(
            this,
            MAIN_WIDGET_SIZE * 2,
            MAIN_WIDGET_SIZE * 2,
            LocalStrings.ToolMenuGroupSelectorName.getText(),
            () -> this.mainSelectedToolMode,
            this::onMainToolModeHover,
            this::onMainToolModeClick,
            page,
            FULL_ROTATION_IN_DEGREES,
            NO_ROTATION_IN_DEGREES,
            SHOW_INACTIVE_MODES,
            MAIN_TORUS_INNER,
            MAIN_TORUS_OUTER,
            KEEP_SELECTION_OUTSIDE_OF_AREA,
            MAIN_TORUS_INNER,
            MAIN_TORUS_OUTER,
            DEFAULT_ICON_SIZE,
            NO_ICON_SCALING_FACTOR,
            DEFAULT_ICON_NAME_SPACER,
            getFont()
        ));
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    private void rebuildGroupControl()
    {
        removeGroupWidget();

        if (this.selectedOuterToolMode != null && this.mainSelectedToolMode instanceof IToolModeGroup)
        {
            this.currentGroupSelectionWidget = owner.addRenderableWidget(new RadialSelectionWidget(
                this,
                OUTER_WIDGET_SIZE * 2,
                OUTER_WIDGET_SIZE * 2,
                LocalStrings.ToolMenuModeSelectorName.getText(),
                () -> this.selectedOuterToolMode,
                this::onGroupedToolModeHover,
                this::onGroupedToolModeClick,
                this.groupings.get(this.mainSelectedToolMode),
                HALF_ROTATION_IN_DEGREES,
                this.page.indexOf(this.mainSelectedToolMode) < Math.min(PAGE_SIZE, this.page.size()) / 2 ? NO_ROTATION_IN_DEGREES : HALF_ROTATION_IN_DEGREES,
                SHOW_INACTIVE_MODES, OUTER_TORUS_INNER,
                OUTER_TORUS_OUTER,
                DISCARD_SELECTION_OUTSIDE_OF_AREA,
                OUTER_TORUS_INNER,
                OUTER_TORUS_OUTER,
                DEFAULT_ICON_SIZE,
                OUTER_TORUS_ICON_SCALE_FACTOR,
                OUTER_DEFAULT_ICON_TEXT_SPACER,
                getFont()
            ));
        }
    }

    private void removePagingControl()
    {
        if (this.pageSelectionWidget == null)
        {
            return;
        }

        removeWidget(this.pageSelectionWidget);
    }

    private void removePageControl()
    {
        if (currentPageSelectionWidget == null)
        {
            return;
        }

        removeWidget(currentPageSelectionWidget);
    }

    private void removeGroupWidget()
    {
        if (currentGroupSelectionWidget == null)
        {
            return;
        }

        removeWidget(currentGroupSelectionWidget);
    }

    private void removeWidget(final GuiEventListener widget)
    {
        owner.removeWidget(widget);
    }

    private void onPageSelected(IRenderableMode iRenderableMode)
    {
        if (iRenderableMode == nextMode)
        {
            nextPage();
        }
        else
        {
            previousPage();
        }
    }

    private void onPageSelectorHover(final IRenderableMode mode)
    {
        this.currentHoveredPage = mode;
    }

    @SuppressWarnings("unchecked")
    private void onMainToolModeHover(final IRenderableMode mode)
    {
        this.mainSelectedToolMode = mode;

        if (this.lastFrameMainSelectedToolMode == null && this.mainSelectedToolMode == null)
        {
            rebuildGroupControl();
            return;
        }

        if (this.lastFrameMainSelectedToolMode != null && this.mainSelectedToolMode == null)
        {
            this.lastFrameMainSelectedToolMode = null;
            this.selectedOuterToolMode = null;
            rebuildGroupControl();
            return;
        }

        if ((this.lastFrameMainSelectedToolMode == null || this.lastFrameMainSelectedToolMode != this.mainSelectedToolMode) && this.mainSelectedToolMode instanceof IToolModeGroup)
        {
            this.lastFrameMainSelectedToolMode = this.mainSelectedToolMode;
            this.selectedOuterToolMode = this.groupings.get((G) this.mainSelectedToolMode).get(0);
            rebuildGroupControl();
            return;
        }

        if (this.mainSelectedToolMode instanceof IToolMode)
        {
            this.lastFrameMainSelectedToolMode = this.mainSelectedToolMode;
            this.selectedOuterToolMode = null;
            rebuildGroupControl();
        }
    }

    private void onMainToolModeClick(final IRenderableMode mode)
    {
        this.onMainToolModeHover(mode);
        if (!(mode instanceof IToolModeGroup))
        {
            this.mainSelectedToolMode = mode;
            updateSelection();
            this.owner.onClose();
        }
    }

    @SuppressWarnings("unchecked")
    private void onGroupedToolModeHover(final IRenderableMode mode)
    {
        this.selectedOuterToolMode = (M) mode;
    }

    private void onGroupedToolModeClick(final IRenderableMode mode)
    {
        this.mainSelectedToolMode = mode;
        updateSelection();
        this.owner.onClose();
    }

    @Override
    public void removed()
    {
        if (toolModeItem.requiresUpdateOnClosure())
        {
            updateSelection();
        }
    }

    @SuppressWarnings("unchecked")
    public void updateSelection()
    {
        if (this.mainSelectedToolMode != null)
        {
            final M mainToolModeSelection =
                this.mainSelectedToolMode instanceof IToolModeGroup ? this.selectedOuterToolMode : (this.mainSelectedToolMode instanceof IToolMode) ? (M) this.mainSelectedToolMode
                    : toolModeItem.getMode(this.sourceStack);

            toolModeItem.setMode(
                ItemStackUtils.getModeItemStackFromPlayer(Minecraft.getInstance().player),
                mainToolModeSelection
            );

            ChiselsAndBits.getInstance().getNetworkChannel().sendToServer(new HeldToolModeChangedPacket(modes.indexOf(mainToolModeSelection)));
        }
    }

    private final class PageSelectionMode implements IRenderableMode
    {

        private final boolean isPrevious;

        private PageSelectionMode(final boolean isPrevious) {this.isPrevious = isPrevious;}

        @Override
        public TextureAtlasSprite getIcon()
        {
            return IconManager.getInstance().getIcon(
                isPrevious ? ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "undo") :
                    ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "redo")
            );
        }

        @Override
        public Component getDisplayName()
        {
            return isPrevious ? LocalStrings.ToolMenuPreviousPageName.getText() :
                LocalStrings.ToolMenuNextPageName.getText();
        }

        @Override
        public boolean isActive()
        {
            return isPrevious ? currentPageIndex != 0 : currentPageIndex != pages.size() - 1;
        }
    }
}

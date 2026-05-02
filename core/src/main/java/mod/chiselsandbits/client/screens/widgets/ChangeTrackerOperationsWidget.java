package mod.chiselsandbits.client.screens.widgets;

import mod.chiselsandbits.ChiselsAndBits;
import mod.chiselsandbits.api.change.IChangeTracker;
import mod.chiselsandbits.api.change.IChangeTrackerManager;
import mod.chiselsandbits.api.client.screen.AbstractChiselsAndBitsScreen;
import mod.chiselsandbits.api.client.screen.widget.AbstractChiselsAndBitsWidget;
import mod.chiselsandbits.api.util.LocalStrings;
import mod.chiselsandbits.client.icon.IconManager;
import mod.chiselsandbits.network.packets.ClearChangeTrackerPacket;
import mod.chiselsandbits.network.packets.RequestChangeTrackerOperationPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import org.jspecify.annotations.NonNull;

public class ChangeTrackerOperationsWidget extends AbstractChiselsAndBitsWidget
{
    public static final int BUTTON_COUNT      = 3;
    public static final int WIDTH             = 18;
    public static final int ELEMENT_SEPARATOR = 3;
    public static final int HEIGHT            = WIDTH * BUTTON_COUNT + ELEMENT_SEPARATOR * (BUTTON_COUNT - 1);

    private final AbstractChiselsAndBitsScreen owner;

    private GuiIconButton undoButton = null;
    private GuiIconButton redoButton = null;
    private GuiIconButton clearButton = null;

    /**
     * Creates a new widget.
     *
     * @param x     The x position.
     * @param y     The y position.
     * @param owner The screen to display this operator on.
     */
    public ChangeTrackerOperationsWidget(
      final int x,
      final int y,
      final AbstractChiselsAndBitsScreen owner)
    {
        super(
          x,
          y,
          WIDTH,
          HEIGHT,
          LocalStrings.ChangeTrackerOperations.getText()
        );
        this.owner = owner;
    }

    @Override
    public void init()
    {
        super.init();
        this.undoButton = owner.addRenderableWidget(
          new GuiIconButton(
            this.getX(),
            this.getY(),
            LocalStrings.ChangeTrackerOperationsButtonUndoName.getText(),
            IconManager.getInstance().getUndoIcon(),
            button -> ChiselsAndBits.getInstance().getNetworkChannel().sendToServer(new RequestChangeTrackerOperationPacket(false)),
            Tooltip.create(LocalStrings.ChangeTrackerOperationsButtonUndoName.getText())
          )
        );

        this.redoButton = owner.addRenderableWidget(
          new GuiIconButton(
            this.getX(),
            this.getY() + WIDTH + ELEMENT_SEPARATOR,
            LocalStrings.ChangeTrackerOperationsButtonRedoName.getText(),
            IconManager.getInstance().getRedoIcon(),
            button -> ChiselsAndBits.getInstance().getNetworkChannel().sendToServer(new RequestChangeTrackerOperationPacket(true)),
            Tooltip.create(LocalStrings.ChangeTrackerOperationsButtonRedoName.getText())
          )
        );

        this.clearButton = owner.addRenderableWidget(
          new GuiIconButton(
            this.getX(),
            this.getY() + WIDTH + ELEMENT_SEPARATOR + WIDTH + ELEMENT_SEPARATOR,
            LocalStrings.ChangeTrackerOperationsButtonClearName.getText(),
            IconManager.getInstance().getTrashIcon(),
            button -> ChiselsAndBits.getInstance().getNetworkChannel().sendToServer(new ClearChangeTrackerPacket()),
            Tooltip.create(LocalStrings.ChangeTrackerOperationsButtonClearName.getText())
          )
        );

        updateState();
    }

    @Override
    protected void extractWidgetRenderState(final @NonNull GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a)
    {

    }

    public void updateState() {
        final IChangeTracker changeTracker = IChangeTrackerManager.getInstance().getChangeTracker(Minecraft.getInstance().player);

        if (this.undoButton != null)
            this.undoButton.active = changeTracker.canUndo(Minecraft.getInstance().player);

        if (this.redoButton != null)
            this.redoButton.active = changeTracker.canRedo(Minecraft.getInstance().player);

        if (this.clearButton != null)
            this.clearButton.active = !changeTracker.getChanges().isEmpty();
    }
}

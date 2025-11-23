package mod.chiselsandbits.client.screens;

import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshot;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.client.screens.widgets.MultiStateSnapshotWidget;
import mod.chiselsandbits.container.ModificationTableContainer;
import mod.chiselsandbits.multistate.snapshot.EmptySnapshot;
import mod.chiselsandbits.recipe.modificationtable.ModificationTableRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ModificationTableScreen extends AbstractContainerScreen<ModificationTableContainer>
{
    private static final ResourceLocation RECIPE_SELECTED_SPRITE = ResourceLocation.withDefaultNamespace("container/stonecutter/recipe_selected");
    private static final ResourceLocation RECIPE_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("container/stonecutter/recipe_highlighted");
    private static final ResourceLocation RECIPE_SPRITE = ResourceLocation.withDefaultNamespace("container/stonecutter/recipe");
    private static final ResourceLocation BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/container/modification_table.png");
    private               float            sliderProgress;
    /** Is {@code true} if the player clicked on the scroll wheel in the GUI. */
    private               boolean          clickedOnSroll;
    /**
     * The index of the primary recipe to display.
     * The number of recipes displayed at any time is 12 (4 recipes per row, and 3 rows). If the player scrolled down one
     * row, this value would be 4 (representing the index of the primary slot on the fallback row).
     */
    private               int              recipeIndexOffset;
    private boolean hasItemsInInputSlot;

    private MultiStateSnapshotWidget snapshotWidget;
    private int                      lastRenderedSelectedRecipeIndex = -1;

    public ModificationTableScreen(ModificationTableContainer containerIn, Inventory playerInv, Component titleIn) {
        super(containerIn, playerInv, titleIn);
        containerIn.setInventoryUpdateListener(this::onInventoryUpdate);
        this.imageHeight = 196;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init()
    {
        super.init();
        this.snapshotWidget = this.addRenderableWidget(new MultiStateSnapshotWidget(this.leftPos + 51,this.topPos + 71, 66,28, Component.translatable(Constants.MOD_ID + ".screen.widgets.multistate.preview")));
    }

    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    protected void renderBg(@NotNull GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND_TEXTURE, left, top, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        int sliderOffset = (int)(41.0F * this.sliderProgress);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND_TEXTURE, left + 119, top + 15 + sliderOffset, this.imageWidth + (this.canScroll() ? 0 : 12), 0, 12, 15, 256, 256);

        int recipesLeft = this.leftPos + 52;
        int recipesTop = this.topPos + 14;
        int recipeIndexOffsetMax = this.recipeIndexOffset + 12;
        this.renderButtons(graphics, mouseX, mouseY, recipesLeft, recipesTop, recipeIndexOffsetMax);
        this.drawRecipesItems(graphics, recipesLeft, recipesTop, recipeIndexOffsetMax);

        if (this.lastRenderedSelectedRecipeIndex != this.menu.getSelectedRecipe() && this.hasItemsInInputSlot) {
            this.lastRenderedSelectedRecipeIndex = this.menu.getSelectedRecipe();

            final CraftingInput input = CraftingInput.of(1,1, List.of(this.menu.inputInventory.getItem(0)));

            final IMultiStateSnapshot snapshot = this.menu.getRecipeList().get(this.lastRenderedSelectedRecipeIndex).value().getAppliedSnapshot(input);
            this.snapshotWidget.setSnapshot(snapshot);
        }
    }

    protected void renderTooltip(@NotNull GuiGraphics graphics, int x, int y) {
        super.renderTooltip(graphics, x, y);
        if (this.hasItemsInInputSlot) {
            int i = this.leftPos + 52;
            int j = this.topPos + 14;
            int k = this.recipeIndexOffset + 12;
            List<RecipeHolder<ModificationTableRecipe>> list = this.menu.getRecipeList();

            for(int l = this.recipeIndexOffset; l < k && l < this.menu.getRecipeListSize(); ++l) {
                int i1 = l - this.recipeIndexOffset;
                int j1 = i + i1 % 4 * 16;
                int k1 = j + i1 / 4 * 18 + 2;
                if (x >= j1 && x < j1 + 16 && y >= k1 && y < k1 + 18) {
                    graphics.renderTooltip(font,
                        List.of(ClientTooltipComponent.create(list.get(l).value().getDisplayName().getVisualOrderText())),
                        x,
                        y,
                        DefaultTooltipPositioner.INSTANCE,
                        null);
                }
            }
        }

    }

    private void renderButtons(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int lastVisibleElementIndex) {
        for(int index = this.recipeIndexOffset; index < lastVisibleElementIndex && index < this.menu.getRecipeListSize(); ++index) {
            int displayedIndex = index - this.recipeIndexOffset;
            int drawX = x + displayedIndex % 4 * 16;
            int rowIndex = displayedIndex / 4;
            int drawY = y + rowIndex * 18 + 2;
            ResourceLocation resourcelocation;
            if (index == this.menu.getSelectedRecipe()) {
                resourcelocation = RECIPE_SELECTED_SPRITE;
            } else if (mouseX >= drawX && mouseY >= drawY && mouseX < drawX + 16 && mouseY < drawY + 18) {
                resourcelocation = RECIPE_HIGHLIGHTED_SPRITE;
            } else {
                resourcelocation = RECIPE_SPRITE;
            }

            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, resourcelocation, drawX, drawY - 1, 16, 18);
        }
    }

    private void drawRecipesItems(final GuiGraphics graphics, int recipesLeft, int recipesTop, int recipeIndexOffsetMax) {
        List<RecipeHolder<ModificationTableRecipe>> list = this.menu.getRecipeList();

        for(int offset = this.recipeIndexOffset; offset < recipeIndexOffsetMax && offset < this.menu.getRecipeListSize(); ++offset) {
            int itemIndex = offset - this.recipeIndexOffset;
            int itemX = recipesLeft + itemIndex % 4 * 16;
            int rowIndex = itemIndex / 4;
            int itemY = recipesTop + rowIndex * 18 + 2;
            if (this.minecraft != null)
            {
                final CraftingInput input = CraftingInput.of(1,1, List.of(this.menu.inputInventory.getItem(0)));
                graphics.pose().pushMatrix();
                graphics.pose().translate(itemX + 0.75f, itemY + 0.75f);
                graphics.pose().scale(0.9f, 0.9f);
                graphics.renderItem(list.get(offset).value().getCraftingBlockResult(input), 0, 0);
                graphics.pose().popMatrix();
            }
        }

    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean repeat)
    {
        final int mouseX = (int) event.x();
        final int mouseY = (int) event.y();

        this.clickedOnSroll = false;
        if (this.hasItemsInInputSlot) {
            int i = this.leftPos + 52;
            int j = this.topPos + 14;
            int k = this.recipeIndexOffset + 12;

            for(int l = this.recipeIndexOffset; l < k; ++l) {
                int i1 = l - this.recipeIndexOffset;
                double d0 = mouseX - (double)(i + i1 % 4 * 16);
                double d1 = mouseY - (double)(j + i1 / 4 * 18);
                if (this.minecraft != null && this.minecraft.player != null && d0 >= 0.0D && d1 >= 0.0D && d0 < 16.0D && d1 < 18.0D
                      && this.menu.clickMenuButton(this.minecraft.player, l))
                {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
                    if (this.minecraft.gameMode != null)
                    {
                        this.minecraft.gameMode.handleInventoryButtonClick((this.menu).containerId, l);
                    }
                    return true;
                }
            }

            i = this.leftPos + 119;
            j = this.topPos + 9;
            if (mouseX >= (double)i && mouseX < (double)(i + 12) && mouseY >= (double)j && mouseY < (double)(j + 54)) {
                this.clickedOnSroll = true;
            }
        }

        return super.mouseClicked(event, repeat);
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY)
    {
        final int mouseX = (int) event.x();
        final int mouseY = (int) event.y();

        if (this.clickedOnSroll && this.canScroll()) {
            int i = this.topPos + 14;
            int j = i + 54;
            this.sliderProgress = ((float)mouseY - (float)i - 7.5F) / ((float)(j - i) - 15.0F);
            this.sliderProgress = Mth.clamp(this.sliderProgress, 0.0F, 1.0F);
            this.recipeIndexOffset = (int)((double)(this.sliderProgress * (float)this.getHiddenRows()) + 0.5D) * 4;
            return true;
        } else {
            if (this.snapshotWidget.isMouseOver(mouseX, mouseY)) {
                return this.snapshotWidget.mouseDragged(event, dragX, dragY);
            }
            return super.mouseDragged(event, dragX, dragY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (this.snapshotWidget.isMouseOver(mouseX, mouseY)) {
            return this.snapshotWidget.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
        }

        if (this.canScroll()) {
            int i = this.getHiddenRows();
            this.sliderProgress = (float)((double)this.sliderProgress - deltaY / (double)i);
            this.sliderProgress = Mth.clamp(this.sliderProgress, 0.0F, 1.0F);
            this.recipeIndexOffset = (int)((double)(this.sliderProgress * (float)i) + 0.5D) * 4;
        }

        return true;
    }

    private boolean canScroll() {
        return this.hasItemsInInputSlot && this.menu.getRecipeListSize() > 12;
    }

    protected int getHiddenRows() {
        return (this.menu.getRecipeListSize() + 4 - 1) / 4 - 3;
    }

    /**
     * Called every time this screen's container is changed (is marked as dirty).
     */
    private void onInventoryUpdate() {
        this.hasItemsInInputSlot = this.menu.hasItemsInInputSlot();
        if (!this.hasItemsInInputSlot) {
            this.sliderProgress = 0.0F;
            this.recipeIndexOffset = 0;
            this.snapshotWidget.setSnapshot(EmptySnapshot.INSTANCE);
            this.lastRenderedSelectedRecipeIndex = -1;
        }
    }
}

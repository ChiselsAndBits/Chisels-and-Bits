package mod.chiselsandbits.client.screens;

import mod.chiselsandbits.ChiselsAndBits;
import mod.chiselsandbits.api.util.LocalStrings;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.client.icon.IconManager;
import mod.chiselsandbits.client.screens.widgets.GuiIconButton;
import mod.chiselsandbits.container.BagContainer;
import mod.chiselsandbits.item.BitBagItem;
import mod.chiselsandbits.network.packets.ClearBagGuiPacket;
import mod.chiselsandbits.network.packets.ConvertBagGuiPacket;
import mod.chiselsandbits.network.packets.SortBagGuiPacket;
import mod.chiselsandbits.network.packets.UpdateBagModesPacket;
import mod.chiselsandbits.registrars.ModItems;
import mod.chiselsandbits.slots.BitSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class BitBagScreen extends AbstractContainerScreen<BagContainer>
{

    private static final ResourceLocation BAG_GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/container/bitbag.png");

    boolean requireConfirm = true;
    boolean dontThrow      = false;
    private GuiIconButton trashBtn;
    private GuiIconButton sortBtn;
    private GuiIconButton convertBtn;
    private GuiIconButton preferedBtn;
    private GuiIconButton filteredBtn;

    public BitBagScreen(
        final BagContainer container,
        final Inventory playerInventory,
        final Component title
    )
    {
        super(container, playerInventory, title);
        imageHeight = 240;
        inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init()
    {
        super.init();

        trashBtn = addRenderableWidget(new GuiIconButton(leftPos - 22, topPos + 3, LocalStrings.Trash.getText(), IconManager.getInstance().getTrashIcon(),
            button -> {
                if (requireConfirm)
                {
                    dontThrow = true;
                    if (isValidBitItem())
                    {
                        requireConfirm = false;
                    }
                }
                else
                {
                    requireConfirm = true;
                    // server side!
                    final ClearBagGuiPacket packet = new ClearBagGuiPacket(getInHandItem());
                    ChiselsAndBits.getInstance().getNetworkChannel().sendToServer(packet);
                    assert Minecraft.getInstance().player != null;
                    packet.execute(Minecraft.getInstance().player);
                }
            }));

        sortBtn = addRenderableWidget(new GuiIconButton(leftPos - 22, topPos + 25, LocalStrings.Sort.getText(), IconManager.getInstance().getSortIcon(),
            button -> {
                final SortBagGuiPacket packet = new SortBagGuiPacket();
                ChiselsAndBits.getInstance().getNetworkChannel().sendToServer(packet);
                assert Minecraft.getInstance().player != null;
                packet.execute(Minecraft.getInstance().player);
            },
            Tooltip.create(LocalStrings.Sort.getText())));

        convertBtn = addRenderableWidget(new GuiIconButton(leftPos - 22, topPos + 47, LocalStrings.Convert.getText(), IconManager.getInstance().getPlaceIcon(),
            button -> {
                final ConvertBagGuiPacket packet = new ConvertBagGuiPacket();
                ChiselsAndBits.getInstance().getNetworkChannel().sendToServer(packet);
                assert Minecraft.getInstance().player != null;
                packet.execute(Minecraft.getInstance().player);
            },
            Tooltip.create(LocalStrings.Convert.getText())
        ));

        preferedBtn = addRenderableWidget(new GuiIconButton(leftPos - 22, topPos + 89, LocalStrings.NonPreferredPickup.getText(), IconManager.getInstance().getBagPicksUpSecondIcon(),
            button -> {
                final UpdateBagModesPacket packet = new UpdateBagModesPacket(
                    isCurrentlyFiltered(), !isCurrentlyPreferred()
                );
                ChiselsAndBits.getInstance().getNetworkChannel().sendToServer(packet);
                assert Minecraft.getInstance().player != null;
                packet.execute(Minecraft.getInstance().player);
                updatePreferredButtonTooltip();
            },
            Tooltip.create(LocalStrings.NonPreferredPickup.getText())) {
            @Override
            protected TextureAtlasSprite getDefaultIcon()
            {
                if (isCurrentlyPreferred()) {
                    return IconManager.getInstance().getBagPicksUpFirstIcon();
                }

                return IconManager.getInstance().getBagPicksUpSecondIcon();
            }
        });

        filteredBtn = addRenderableWidget(new GuiIconButton(leftPos - 22, topPos + 111, LocalStrings.NonFilteredPickup.getText(), IconManager.getInstance().getNormalBagModeIcon(),
            button -> {
                final UpdateBagModesPacket packet = new UpdateBagModesPacket(
                    !isCurrentlyFiltered(), isCurrentlyPreferred()
                );
                ChiselsAndBits.getInstance().getNetworkChannel().sendToServer(packet);
                assert Minecraft.getInstance().player != null;
                packet.execute(Minecraft.getInstance().player);
                updateFilteredButtonTooltip();
            },
            Tooltip.create(LocalStrings.FilteredPickup.getText())) {
            @Override
            protected TextureAtlasSprite getDefaultIcon()
            {
                if (isCurrentlyFiltered()) {
                    return IconManager.getInstance().getFilterBagModeIcon();
                }

                return IconManager.getInstance().getNormalBagModeIcon();
            }
        });

        updateFilteredButtonTooltip();
        updatePreferredButtonTooltip();
    }

    private BagContainer getBagContainer()
    {
        return menu;
    }

    private void updatePreferredButtonTooltip() {
        final LocalStrings newTooltip = isCurrentlyPreferred() ?
            LocalStrings.PreferredPickup : LocalStrings.NonPreferredPickup;

        preferedBtn.setTooltip(Tooltip.create(newTooltip.getText(
            Minecraft.getInstance().options.keyAttack.getTranslatedKeyMessage()
        )));
    }

    private void updateFilteredButtonTooltip() {
        final LocalStrings newTooltip = isCurrentlyFiltered() ?
            LocalStrings.FilteredPickup : LocalStrings.NonFilteredPickup;

        filteredBtn.setTooltip(Tooltip.create(newTooltip.getText(
            Minecraft.getInstance().options.keyAttack.getTranslatedKeyMessage()
        )));
    }

    @Override
    protected boolean hasClickedOutside(final double x, final double y, final int left, final int top)
    {
        final boolean doThrow = !dontThrow;
        if (requireConfirm && dontThrow)
        {
            dontThrow = false;
        }
        return doThrow && super.hasClickedOutside(x, y, left, top);
    }

    @Override
    public void render(
        final @NotNull GuiGraphics guiGraphics,
        final int mouseX,
        final int mouseY,
        final float partialTicks)
    {
        if (trashBtn.isMouseOver(mouseX, mouseY))
        {
            if (isValidBitItem())
            {
                final Component msgNotConfirm =
                    !getInHandItem().isEmpty() ? LocalStrings.TrashItem.getText(getInHandItem().getHoverName().getString()) : LocalStrings.Trash.getText();
                final Component msgConfirm =
                    !getInHandItem().isEmpty() ? LocalStrings.ReallyTrashItem.getText(getInHandItem().getHoverName().getString()) : LocalStrings.ReallyTrash.getText();

                this.trashBtn.setTooltip(Tooltip.create(requireConfirm ? msgNotConfirm : msgConfirm));
            }
            else
            {
                this.trashBtn.setTooltip(Tooltip.create(LocalStrings.TrashInvalidItem.getText(getInHandItem().getHoverName().getString())));
            }
        }
        else
        {
            requireConfirm = true;
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void renderSlot(final GuiGraphics guiGraphics, final Slot slot)
    {
        if (!(slot instanceof BitSlot bitSlot))
        {
            super.renderSlot(guiGraphics, slot);
            return;
        }

        final ItemStack currentContents = bitSlot.getItem();
        bitSlot.set(currentContents.copyWithCount(1));
        super.renderSlot(guiGraphics, bitSlot);
        bitSlot.set(currentContents);

        if (currentContents.getCount() != 0) {
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().scale(0.5f, 0.5f);

            String countString = String.valueOf(currentContents.getCount());
            guiGraphics.drawString(font,
                countString,
                (slot.x + 19 - 3) * 2 - font.width(countString),
                (slot.y + 9 + 3) * 2,
                -1,
                true);

            guiGraphics.pose().popMatrix();
        }
    }

    @Override
    protected void renderBg(
        final @NotNull GuiGraphics guiGraphics,
        final float partialTicks,
        final int mouseX,
        final int mouseY)
    {
        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED,
            BAG_GUI_TEXTURE,
            this.leftPos,
            this.topPos,
            0.0F,
            0.0F,
            this.imageWidth,
            this.imageHeight,
            256,
            256
        );
    }

    private ItemStack getInHandItem()
    {
        return getBagContainer().getCarried();
    }

    private boolean isValidBitItem()
    {
        return getInHandItem().isEmpty() || getInHandItem().getItem() == ModItems.ITEM_BLOCK_BIT.get();
    }

    private boolean isCurrentlyPreferred() {
        return getBagContainer().getBagStack().getItem() instanceof BitBagItem bitBagItem && bitBagItem.isPreferredPickupInventory(getBagContainer().getBagStack());
    }

    private boolean isCurrentlyFiltered() {
        return getBagContainer().getBagStack().getItem() instanceof BitBagItem bitBagItem && bitBagItem.isFilteredPickupInventory(getBagContainer().getBagStack());
    }
}

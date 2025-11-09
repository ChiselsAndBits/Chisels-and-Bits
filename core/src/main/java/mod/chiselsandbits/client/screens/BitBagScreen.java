package mod.chiselsandbits.client.screens;

import mod.chiselsandbits.ChiselsAndBits;
import mod.chiselsandbits.api.util.LocalStrings;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.client.icon.IconManager;
import mod.chiselsandbits.client.screens.widgets.GuiIconButton;
import mod.chiselsandbits.container.BagContainer;
import mod.chiselsandbits.network.packets.ClearBagGuiPacket;
import mod.chiselsandbits.network.packets.ConvertBagGuiPacket;
import mod.chiselsandbits.network.packets.SortBagGuiPacket;
import mod.chiselsandbits.registrars.ModItems;
import mod.chiselsandbits.slots.BitSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.locale.Language;
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

    public BitBagScreen(
        final BagContainer container,
        final Inventory playerInventory,
        final Component title
    )
    {
        super(container, playerInventory, title);
        imageHeight = 239;
    }

    @Override
    protected void init()
    {
        super.init();

        trashBtn = addRenderableWidget(new GuiIconButton(leftPos - 20, topPos, LocalStrings.Trash.getText(), IconManager.getInstance().getTrashIcon(),
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

        addRenderableWidget(new GuiIconButton(leftPos - 20, topPos + 22, LocalStrings.Sort.getText(), IconManager.getInstance().getSortIcon(),
            button -> {
                final SortBagGuiPacket packet = new SortBagGuiPacket();
                ChiselsAndBits.getInstance().getNetworkChannel().sendToServer(packet);
                assert Minecraft.getInstance().player != null;
                packet.execute(Minecraft.getInstance().player);
            },
            Tooltip.create(LocalStrings.Sort.getText())));

        addRenderableWidget(new GuiIconButton(leftPos - 20, topPos + 42, LocalStrings.Convert.getText(), IconManager.getInstance().getPlaceIcon(),
            button -> {
                final ConvertBagGuiPacket packet = new ConvertBagGuiPacket();
                ChiselsAndBits.getInstance().getNetworkChannel().sendToServer(packet);
                assert Minecraft.getInstance().player != null;
                packet.execute(Minecraft.getInstance().player);
            },
            Tooltip.create(LocalStrings.Convert.getText())
        ));
    }

    BagContainer getBagContainer()
    {
        return menu;
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
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
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

        this.getBagContainer().bitSlots.forEach(slot -> slot.setActive(false));
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.getBagContainer().bitSlots.forEach(slot -> slot.setActive(true));
    }

    @Override
    public void renderSlot(final GuiGraphics guiGraphics, final Slot slot)
    {
        if (!(slot instanceof BitSlot bitSlot))
            return;

        final ItemStack currentContents = bitSlot.getItem();
        bitSlot.setActive(true);
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
                16777215,
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
        return Minecraft.getInstance().player == null ? ItemStack.EMPTY : Minecraft.getInstance().player.containerMenu.getCarried();
    }

    private boolean isValidBitItem()
    {
        return getInHandItem().isEmpty() || getInHandItem().getItem() == ModItems.ITEM_BLOCK_BIT.get();
    }

    @Override
    protected void renderLabels(final @NotNull GuiGraphics graphics, final int x, final int y)
    {
        graphics.drawString(font, Language.getInstance().getVisualOrder(ModItems.ITEM_BIT_BAG.get().getName(ItemStack.EMPTY)), 8, 6, 0x404040);
        graphics.drawString(font, I18n.get("container.inventory"), 8, imageHeight - 93, 0x404040);
    }
}

package mod.chiselsandbits.container;

import mod.chiselsandbits.ChiselsAndBits;
import mod.chiselsandbits.api.inventory.bit.IBitInventoryItem;
import mod.chiselsandbits.api.inventory.bit.IBitInventoryItemStack;
import mod.chiselsandbits.api.item.bit.IBitItem;
import mod.chiselsandbits.container.helper.MergeSupportingHelperContainer;
import mod.chiselsandbits.inventory.wrapping.WrappingInventory;
import mod.chiselsandbits.network.packets.BagGuiStackPacket;
import mod.chiselsandbits.registrars.ModContainerTypes;
import mod.chiselsandbits.slots.BitSlot;
import mod.chiselsandbits.slots.ReadonlySlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class BagContainer extends AbstractContainerMenu
{
    static final int OUTER_SLOT_SIZE = 18;
    final public List<Slot>      customSlots      = new ArrayList<>();
    final public List<ItemStack> customSlotsItems = new ArrayList<>();
    final Player      thePlayer;
    final WrappingInventory visibleInventory = new WrappingInventory();

    IBitInventoryItemStack bagInv;
    ReadonlySlot           bagSlot;

    public BagContainer(final int id, final Inventory playerInventory)
    {
        super(ModContainerTypes.BIT_BAG.get(), id);
        thePlayer = playerInventory.player;

        final int playerInventoryOffset = (7 - 4) * OUTER_SLOT_SIZE;

        final ItemStack is = thePlayer.getMainHandItem();
        setBag(is);

        for (int yOffset = 0; yOffset < 7; ++yOffset)
        {
            for (int xOffset = 0; xOffset < 9; ++xOffset)
            {
                addCustomSlot(new BitSlot(visibleInventory, xOffset + yOffset * 9, 8 + xOffset * OUTER_SLOT_SIZE, 18 + yOffset * OUTER_SLOT_SIZE));
            }
        }

        for (int xPlayerInventory = 0; xPlayerInventory < 3; ++xPlayerInventory)
        {
            for (int yPlayerInventory = 0; yPlayerInventory < 9; ++yPlayerInventory)
            {
                addSlot(new Slot(thePlayer.getInventory(),
                  yPlayerInventory + xPlayerInventory * 9 + 9,
                  8 + yPlayerInventory * OUTER_SLOT_SIZE,
                  104 + xPlayerInventory * OUTER_SLOT_SIZE + playerInventoryOffset));
            }
        }

        for (int xToolbar = 0; xToolbar < 9; ++xToolbar)
        {
            if (thePlayer.getInventory().selected == xToolbar)
            {
                addSlot(bagSlot = new ReadonlySlot(thePlayer.getInventory(), xToolbar, 8 + xToolbar * OUTER_SLOT_SIZE, 162 + playerInventoryOffset));
            }
            else
            {
                addSlot(new Slot(thePlayer.getInventory(), xToolbar, 8 + xToolbar * OUTER_SLOT_SIZE, 162 + playerInventoryOffset));
            }
        }
    }

    private void setBag(
      final ItemStack bagItem)
    {
        if (!bagItem.isEmpty() && bagItem.getItem() instanceof final IBitInventoryItem bitInventoryItem)
        {
            bagInv = bitInventoryItem.create(bagItem);
            visibleInventory.setWrapped(bagInv);
        }
        else
        {
            bagInv = null;
        }
    }

    private void addCustomSlot(
      final BitSlot newSlot)
    {
        newSlot.index = customSlots.size();
        customSlots.add(newSlot);
        customSlotsItems.add(ItemStack.EMPTY);
    }

    public void handleCustomSlotAction(
      final int slotNumber,
      final int mouseButton,
      final boolean duplicateButton,
      final boolean holdingShift)
    {
        final Slot slot = customSlots.get(slotNumber);
        final ItemStack held = getCarried();
        final ItemStack slotStack = slot.getItem();

        if (duplicateButton && thePlayer.isCreative())
        {
            if (slot.hasItem() && held.isEmpty())
            {
                final ItemStack is = slot.getItem().copy();
                is.setCount(is.getMaxStackSize());
                setCarried(is);
            }
        }
        else if (holdingShift)
        {
            if (!slotStack.isEmpty())
            {
                transferStack(slotNumber, false);
            }
        }
        else if (mouseButton == 0 && !duplicateButton)
        {
            if (held.isEmpty() && slot.hasItem())
            {
                final ItemStack pulled = slotStack.copy();
                pulled.setCount(Math.min(pulled.getMaxStackSize(), pulled.getCount()));

                final ItemStack newStackSlot = slotStack.copy();
                newStackSlot.setCount(pulled.getCount() >= slotStack.getCount() ? 0 : slotStack.getCount() - pulled.getCount());

                slot.set(newStackSlot.getCount() <= 0 ? ItemStack.EMPTY : newStackSlot);
                setCarried(pulled);
            }
            else if (!held.isEmpty() && slot.hasItem() && slot.mayPlace(held))
            {
                if (held.getItem() == slotStack.getItem() && held.getDamageValue() == slotStack.getDamageValue() && ItemStack.isSameItemSameTags(held, slotStack))
                {
                    final ItemStack newStackSlot = slotStack.copy();
                    newStackSlot.setCount(newStackSlot.getCount() + held.getCount());
                    int held_stackSize = 0;

                    if (newStackSlot.getCount() > slot.getMaxStackSize())
                    {
                        held_stackSize = newStackSlot.getCount() - slot.getMaxStackSize();
                        newStackSlot.setCount(newStackSlot.getCount() - held_stackSize);
                    }

                    slot.set(newStackSlot);
                    held.setCount(held_stackSize);
                    setCarried(held);
                }
                else
                {
                    if (!held.isEmpty() && slot.hasItem() && slotStack.getCount() <= slotStack.getMaxStackSize())
                    {
                        slot.set(held);
                        setCarried(slotStack);
                    }
                }
            }
            else if (!held.isEmpty() && !slot.hasItem() && slot.mayPlace(held))
            {
                slot.set(held);
                setCarried(ItemStack.EMPTY);
            }
        }
        else if (mouseButton == 1 && !duplicateButton)
        {
            if (held.isEmpty() && slot.hasItem())
            {
                final ItemStack pulled = slotStack.copy();
                pulled.setCount(Math.max(1, (Math.min(pulled.getMaxStackSize(), pulled.getCount()) + 1) / 2));

                final ItemStack newStackSlot = slotStack.copy();
                newStackSlot.setCount(pulled.getCount() >= slotStack.getCount() ? 0 : slotStack.getCount() - pulled.getCount());

                slot.set(newStackSlot.getCount() <= 0 ? ItemStack.EMPTY : newStackSlot);
                setCarried(pulled);
            }
            else if (!held.isEmpty() && slot.hasItem() && slot.mayPlace(held))
            {
                if (held.getItem() == slotStack.getItem() && held.getDamageValue() == slotStack.getDamageValue() && ItemStack.isSameItemSameTags(held, slotStack))
                {
                    final ItemStack newStackSlot = slotStack.copy();
                    newStackSlot.setCount(newStackSlot.getCount() + 1);
                    int held_quantity = held.getCount() - 1;

                    if (newStackSlot.getCount() > slot.getMaxStackSize())
                    {
                        final int diff = newStackSlot.getCount() - slot.getMaxStackSize();
                        held_quantity += diff;
                        newStackSlot.setCount(newStackSlot.getCount() - diff);
                    }

                    slot.set(newStackSlot);
                    held.setCount(held_quantity);
                    setCarried(!held.isEmpty() ? held : ItemStack.EMPTY);
                }
            }
            else if (!held.isEmpty() && !slot.hasItem() && slot.mayPlace(held))
            {
                final ItemStack newStackSlot = held.copy();
                newStackSlot.setCount(newStackSlot.getCount() + 1);
                held.setCount(held.getCount() - 1);

                slot.set(newStackSlot);
                setCarried(!held.isEmpty() ? held : ItemStack.EMPTY);
            }
        }
    }

    @Override
    public void broadcastChanges()
    {
        super.broadcastChanges();

        for (int slotIdx = 0; slotIdx < customSlots.size(); ++slotIdx)
        {
            final ItemStack realStack = customSlots.get(slotIdx).getItem();
            ItemStack clientStack = customSlotsItems.get(slotIdx);

            if (!ItemStack.matches(clientStack, realStack))
            {
                clientStack = realStack.isEmpty() ? ItemStack.EMPTY : realStack.copy();
                customSlotsItems.set(slotIdx, clientStack);

                for (final ContainerListener cl : containerListeners)
                {
                    if (cl instanceof ServerPlayer)
                    {
                        final BagGuiStackPacket packet = new BagGuiStackPacket(slotIdx, clientStack);
                        ChiselsAndBits.getInstance().getNetworkChannel().sendToPlayer(packet, (ServerPlayer) cl);
                    }
                }
            }
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(
      final @NotNull Player playerIn,
      final int index)
    {
        return transferStack(index, true);
    }

    @Override
    public boolean stillValid(
      @NotNull final Player playerIn)
    {
        return bagInv != null && playerIn == thePlayer && hasBagInHand(thePlayer);
    }

    private boolean hasBagInHand(
      final Player player)
    {
        if (bagInv.toItemStack() != player.getMainHandItem())
        {
            setBag(player.getMainHandItem());
        }

        return bagInv != null && bagInv.toItemStack().getItem() instanceof IBitInventoryItem;
    }

    private ItemStack transferStack(
      final int index,
      final boolean normalToBag)
    {
        ItemStack someReturnValue = ItemStack.EMPTY;
        boolean reverse = true;

        final MergeSupportingHelperContainer helper = new MergeSupportingHelperContainer(1);

        if (!normalToBag)
        {
            helper.slots.clear();
            helper.slots.addAll(customSlots);
        }
        else
        {
            helper.slots.clear();
            helper.slots.addAll(slots);
            reverse = false;
        }

        final Slot slot = helper.slots.get(index);

        if (slot.hasItem())
        {
            final ItemStack transferStack = slot.getItem();
            someReturnValue = transferStack.copy();

            int extraItems = 0;
            if (transferStack.getCount() > transferStack.getMaxStackSize())
            {
                extraItems = transferStack.getCount() - transferStack.getMaxStackSize();
                transferStack.setCount(transferStack.getMaxStackSize());
            }

            if (normalToBag)
            {
                helper.slots.clear();
                helper.slots.addAll(customSlots);
            }
            else
            {
                helper.slots.clear();
                helper.slots.addAll(slots);
            }

            final Item transferItem = transferStack.getItem();
            if (transferItem instanceof final IBitItem bitItem)
            {
                bitItem.onMergeOperationWithBagBeginning();
            }
            try
            {
                if (!helper.doMergeItemStack(transferStack, 0, helper.slots.size(), reverse))
                {
                    return ItemStack.EMPTY;
                }
            }
            finally
            {
                // add the extra items back on...
                transferStack.setCount(transferStack.getCount() + extraItems);
                if (transferItem instanceof final IBitItem bitItem)
                {
                    bitItem.onMergeOperationWithBagEnding();
                }
            }

            if (transferStack.isEmpty())
            {
                slot.set(ItemStack.EMPTY);
            }
            else
            {
                slot.setChanged();
            }
        }

        return someReturnValue;
    }

    public void clear(
      final ItemStack stack)
    {
        if (!stack.isEmpty() && stack.getItem() instanceof final IBitItem bitItem)
        {
            bagInv.clear(bitItem.getBlockInformation(stack));
        }
        else {
            bagInv.clearContent();
        }

        setCarried(ItemStack.EMPTY);

        transferState(this);
    }

    public void sort()
    {
        bagInv.sort();
        transferState(this);
    }

    public void convert(Player player)
    {
        bagInv.convert(player);
        transferState(this);
    }
}

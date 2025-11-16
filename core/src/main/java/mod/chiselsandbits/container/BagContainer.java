package mod.chiselsandbits.container;

import mod.chiselsandbits.api.inventory.bit.IBitInventoryItem;
import mod.chiselsandbits.api.inventory.bit.IBitInventoryItemStack;
import mod.chiselsandbits.api.item.bit.IBitItem;
import mod.chiselsandbits.inventory.wrapping.WrappingInventory;
import mod.chiselsandbits.item.BitBagItem;
import mod.chiselsandbits.registrars.ModContainerTypes;
import mod.chiselsandbits.registrars.ModItems;
import mod.chiselsandbits.slots.BitSlot;
import mod.chiselsandbits.slots.ReadonlySlot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class BagContainer extends AbstractContainerMenu
{
    private static final int OUTER_SLOT_SIZE = 18;
    private final List<BitSlot> bitSlots = new ArrayList<>();
    private final Player      thePlayer;
    private final WrappingInventory visibleInventory = new WrappingInventory();


    private IBitInventoryItemStack bagInv;
    private ReadonlySlot           bagSlot;

    public BagContainer(final int id, final Inventory playerInventory)
    {
        super(ModContainerTypes.BIT_BAG.get(), id);
        thePlayer = playerInventory.player;

        final int playerInventoryOffset = (7 - 4) * OUTER_SLOT_SIZE;

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
            if (thePlayer.getInventory().getSelectedSlot() == xToolbar)
            {
                addSlot(bagSlot = new ReadonlySlot(thePlayer.getInventory(), xToolbar, 8 + xToolbar * OUTER_SLOT_SIZE, 162 + playerInventoryOffset));
            }
            else
            {
                addSlot(new Slot(thePlayer.getInventory(), xToolbar, 8 + xToolbar * OUTER_SLOT_SIZE, 162 + playerInventoryOffset));
            }
        }

        setBag(bagSlot.getItem());
    }

    private void setBag(
      final ItemStack bagItem)
    {
        if (!bagItem.isEmpty() && bagItem.getItem() instanceof final IBitInventoryItem bitInventoryItem)
        {
            bagInv = bitInventoryItem.create(bagItem);
            visibleInventory.setWrapped(bagInv);
            bagSlot.set(bagItem);
        }
        else
        {
            bagInv = null;
            visibleInventory.setWrapped(null);
            bagSlot.set(ItemStack.EMPTY);
        }
    }

    private void addCustomSlot(
      final BitSlot newSlot)
    {
        newSlot.index = bitSlots.size();
        bitSlots.add(newSlot);

        addSlot(newSlot);
    }

    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            ItemStack original = slotStack;
            if (slot instanceof BitSlot) {
                slotStack = slotStack.split(ModItems.ITEM_BLOCK_BIT.get().getDefaultMaxStackSize());
            }
            itemstack = slotStack.copy();
            //Source is the bag:
            if (index < this.bitSlots.size()) {
                if (!this.moveItemStackTo(slotStack, this.bitSlots.size(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            //Source is the player inventory.
            } else if (
                    !this.moveItemStackTo(slotStack, 0, this.bitSlots.size(), false))
            {
                return ItemStack.EMPTY;
            }

            if (slotStack.isEmpty()) {
                slot.setByPlayer(original);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
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
        final ItemStack bagStack = bagInv.toItemStack();
        if (bagStack.getItem() == player.getMainHandItem().getItem())
        {
            setBag(bagStack);
            player.setItemInHand(InteractionHand.MAIN_HAND, bagStack);
        }

        return bagInv != null && bagInv.toItemStack().getItem() instanceof IBitInventoryItem;
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
        broadcastChanges();
    }

    public void sort()
    {
        bagInv.sort();
        broadcastChanges();
    }

    public void convert(Player player)
    {
        bagInv.convert(player);
        broadcastChanges();
    }

    public void setBagProperties(final boolean filtered, final boolean preferred)
    {
        if (getBagStack().getItem() instanceof BitBagItem bitBagItem) {
            bitBagItem.setFilteredPickupInventory(thePlayer.getMainHandItem(), filtered);
            bitBagItem.setPreferredPickupInventory(thePlayer.getMainHandItem(), preferred);
            broadcastChanges();
            thePlayer.inventoryMenu.broadcastChanges();
        }
    }

    public List<BitSlot> getBitSlots()
    {
        return bitSlots;
    }

    public ItemStack getBagStack()
    {
        return bagSlot.getItem();
    }
}

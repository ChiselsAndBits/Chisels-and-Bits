package mod.chiselsandbits.container;

import mod.chiselsandbits.api.item.chisel.IChiselItem;
import mod.chiselsandbits.api.item.pattern.IPatternItem;
import mod.chiselsandbits.registrars.ModContainerTypes;
import mod.chiselsandbits.utils.container.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ChiseledPrinterContainer extends AbstractContainerMenu
{
    public static final int PLAYER_INVENTORY_ROWS = 3;
    public static final int PLAYER_INVENTORY_COLUMNS = 9;
    public static final int PLAYER_INVENTORY_X_OFFSET = 8;
    public static final int PLAYER_INVENTORY_Y_OFFSET = 84;
    public static final int SLOT_SIZE = 18;
    public static final int PLAYER_TOOLBAR_Y_OFFSET = 142;

    private final ContainerData stationData;

    private final SimpleContainer toolHandler;

    public ChiseledPrinterContainer(
      final int id,
      final Inventory playerInventory)
    {
        this(id, playerInventory, new SimpleContainer(1), new SimpleContainer(1), new SimpleContainer(1), new SimpleContainerData(1));
    }

    public ChiseledPrinterContainer(
      final int id,
      final Inventory playerInventory,
      final SimpleContainer patternHandler,
      final SimpleContainer toolHandler,
      final SimpleContainer resultHandler,
      final ContainerData stationData)
    {
        super(ModContainerTypes.CHISELED_PRINTER_CONTAINER.get(), id);
        this.stationData = stationData;
        this.toolHandler = toolHandler;

        this.addSlot(
          new Slot(
            patternHandler,
            0,
            50,
            47
          ) {
              @Override
              public boolean mayPlace(@NotNull final ItemStack stack)
              {
                  return stack.isEmpty() || stack.getItem() instanceof IPatternItem;
              }
          }
        );

        this.addSlot(
          new Slot(
            toolHandler,
            0,
            81,
            21
          ) {
              @Override
              public boolean mayPlace(@NotNull final ItemStack stack)
              {
                  return stack.getItem() instanceof IChiselItem;
              }
          }
        );

        this.addSlot(
          new Slot(
            resultHandler,
            0,
            116,
            47
          ) {
              @Override
              public boolean mayPlace(@NotNull final ItemStack stack)
              {
                  return false;
              }
          }
        );

        for(int rowIndex = 0; rowIndex < PLAYER_INVENTORY_ROWS; ++rowIndex) {
            for(int columnIndex = 0; columnIndex < PLAYER_INVENTORY_COLUMNS; ++columnIndex) {
                this.addSlot(new Slot(
                    playerInventory,
                    columnIndex + rowIndex * PLAYER_INVENTORY_COLUMNS + PLAYER_INVENTORY_COLUMNS,
                    PLAYER_INVENTORY_X_OFFSET + columnIndex * SLOT_SIZE,
                    PLAYER_INVENTORY_Y_OFFSET + rowIndex * SLOT_SIZE
                  )
                );
            }
        }

        for(int toolbarIndex = 0; toolbarIndex < PLAYER_INVENTORY_COLUMNS; ++toolbarIndex) {
            this.addSlot(new Slot(playerInventory, toolbarIndex, PLAYER_INVENTORY_X_OFFSET + toolbarIndex * SLOT_SIZE, PLAYER_TOOLBAR_Y_OFFSET));
        }

        this.addDataSlots(this.stationData);
    }

    @Override
    public boolean stillValid(final Player playerIn)
    {
        return true;
    }

    public int getChiselProgressionScaled()
    {
        int progress = this.stationData.get(0);
        return progress != 0 ? progress * 16 / 100 : 0;
    }

    public ItemStack getToolStack()
    {
        return toolHandler.getItem(0);
    }

    @NotNull
    @Override
    public ItemStack quickMoveStack(final Player playerIn, final int index)
    {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index == 2) {
                if (!this.moveItemStackTo(itemstack1, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemstack1, itemstack);
            } else if (index != 1 && index != 0) {
                if (itemstack1.getItem() instanceof IPatternItem) {
                    if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (itemstack1.getItem() instanceof IChiselItem) {
                    if (!this.moveItemStackTo(itemstack1, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index < 30) {
                    if (!this.moveItemStackTo(itemstack1, 30, 39, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index < 39 && !this.moveItemStackTo(itemstack1, 3, 30, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 3, 39, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, itemstack1);
        }

        return itemstack;
    }

}

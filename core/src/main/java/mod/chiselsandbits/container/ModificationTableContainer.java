package mod.chiselsandbits.container;

import com.google.common.collect.Lists;
import mod.chiselsandbits.ChiselsAndBits;
import mod.chiselsandbits.network.packets.ModificationRecipesUpdated;
import mod.chiselsandbits.recipe.modificationtable.ModificationTableRecipe;
import mod.chiselsandbits.registrars.ModBlocks;
import mod.chiselsandbits.registrars.ModContainerTypes;
import mod.chiselsandbits.registrars.ModRecipeTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ModificationTableContainer extends AbstractContainerMenu
{
    private final ContainerLevelAccess                        worldPosCallable;
    private final DataSlot                                    selectedRecipe          = DataSlot.standalone();
    private final Level                                       world;
    private final Inventory                                   playerInventory;
    private       List<RecipeHolder<ModificationTableRecipe>> recipes                 = Lists.newArrayList();
    private       ItemStack                                   inputItemStack          = ItemStack.EMPTY;
    private       long                                        lastOnTake;
    final         Slot                                        inputInventorySlot;
    final         Slot                                        outputInventorySlot;
    private       Runnable                                    inventoryUpdateListener = () -> {};
    public final  Container                                   inputInventory          = new SimpleContainer(1)
    {
        public void setChanged()
        {
            super.setChanged();
            ModificationTableContainer.this.slotsChanged(this);
            ModificationTableContainer.this.inventoryUpdateListener.run();
        }
    };
    private final ResultContainer                             inventory               = new ResultContainer();

    public ModificationTableContainer(int windowIdIn, Inventory playerInventoryIn)
    {
        this(windowIdIn, playerInventoryIn, ContainerLevelAccess.NULL);
    }

    public ModificationTableContainer(int windowIdIn, Inventory playerInventoryIn, final ContainerLevelAccess worldPosCallableIn)
    {
        super(ModContainerTypes.MODIFICATION_TABLE.get(), windowIdIn);
        this.worldPosCallable = worldPosCallableIn;
        this.world = playerInventoryIn.player.level();
        this.inputInventorySlot = this.addSlot(new Slot(this.inputInventory, 0, 20, 33));
        this.outputInventorySlot = this.addSlot(new Slot(this.inventory, 1, 143, 33)
        {
            /**
             * Check if the stack is allowed to be placed in this slot, used for armor slots as well as furnace fuel.
             */
            public boolean mayPlace(@NotNull ItemStack stack)
            {
                return false;
            }

            public void onTake(@NotNull Player thePlayer, @NotNull ItemStack stack)
            {
                stack.onCraftedBy(thePlayer, stack.getCount());
                ModificationTableContainer.this.inventory.awardUsedRecipes(thePlayer, List.of(ModificationTableContainer.this.inputInventorySlot.getItem()));
                ItemStack itemstack = ModificationTableContainer.this.inputInventorySlot.remove(1);
                if (!itemstack.isEmpty())
                {
                    ModificationTableContainer.this.updateRecipeResultSlot();
                }

                worldPosCallableIn.execute((p_216954_1_, p_216954_2_) -> {
                    long l = p_216954_1_.getGameTime();
                    if (ModificationTableContainer.this.lastOnTake != l)
                    {
                        p_216954_1_.playSound(null, p_216954_2_, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.0F);
                        ModificationTableContainer.this.lastOnTake = l;
                    }
                });
                super.onTake(thePlayer, stack);
            }
        });

        for (int i = 0; i < 3; ++i)
        {
            for (int j = 0; j < 9; ++j)
            {
                this.addSlot(new Slot(playerInventoryIn, j + i * 9 + 9, 8 + j * 18, 115 + i * 18));
            }
        }

        for (int k = 0; k < 9; ++k)
        {
            this.addSlot(new Slot(playerInventoryIn, k, 8 + k * 18, 173));
        }

        this.addDataSlot(this.selectedRecipe);
        this.playerInventory = playerInventoryIn;
    }

    /**
     * Returns the index of the selected recipe.
     */
    public int getSelectedRecipe()
    {
        return this.selectedRecipe.get();
    }

    public List<RecipeHolder<ModificationTableRecipe>> getRecipeList()
    {
        return this.recipes;
    }

    public int getRecipeListSize()
    {
        return this.recipes.size();
    }

    public boolean hasItemsInInputSlot()
    {
        return this.inputInventorySlot.hasItem() && !this.recipes.isEmpty();
    }

    /**
     * Determines whether supplied player can use this container
     */
    public boolean stillValid(@NotNull Player playerIn)
    {
        return stillValid(this.worldPosCallable, playerIn, ModBlocks.MODIFICATION_TABLE.get());
    }

    /**
     * Handles the given Button-click on the server, currently only used by enchanting. Name is for legacy.
     */
    public boolean clickMenuButton(@NotNull Player playerIn, int id)
    {
        if (this.isValidRecipeIndex(id))
        {
            this.selectedRecipe.set(id);
            this.updateRecipeResultSlot();
        }

        return true;
    }

    private boolean isValidRecipeIndex(int recipeIndex)
    {
        return recipeIndex >= 0 && recipeIndex < this.recipes.size();
    }

    /**
     * Callback for when the crafting matrix is changed.
     */
    public void slotsChanged(@NotNull Container inventoryIn)
    {
        ItemStack itemstack = this.inputInventorySlot.getItem();
        if (itemstack.getItem() != this.inputItemStack.getItem())
        {
            this.inputItemStack = itemstack.copy();
            this.updateAvailableRecipes(inventoryIn, itemstack);
        }
    }

    private void updateAvailableRecipes(Container inventoryIn, ItemStack stack)
    {
        this.recipes.clear();
        this.selectedRecipe.set(-1);
        this.outputInventorySlot.set(ItemStack.EMPTY);
        if (!stack.isEmpty())
        {
            final CraftingInput input = CraftingInput.of(1, 1, List.of(stack));
            this.recipes =
                this.world.getServer() != null ?
                    this.world.getServer().getRecipeManager().recipes.getRecipesFor(ModRecipeTypes.MODIFICATION_TABLE.get(), input, this.world).collect(Collectors.toList()) :
                    new ArrayList<>();
            this.recipes.sort(Comparator.comparing(modificationTableRecipe -> Objects.requireNonNull(modificationTableRecipe.value().operation().getRegistryName()).toString()));

            if (!this.world.isClientSide() && this.playerInventory.player instanceof ServerPlayer serverPlayer) {
                ChiselsAndBits.getInstance().getNetworkChannel()
                    .sendToPlayer(new ModificationRecipesUpdated(this.recipes), serverPlayer);
            }
        }
    }

    private void updateRecipeResultSlot()
    {
        if (!this.recipes.isEmpty() && this.isValidRecipeIndex(this.selectedRecipe.get()))
        {
            RecipeHolder<ModificationTableRecipe> modificationTableRecipe = this.recipes.get(this.selectedRecipe.get());
            this.inventory.setRecipeUsed(modificationTableRecipe);
            final CraftingInput input = CraftingInput.of(1, 1, List.of(this.inputInventory.getItem(0)));
            this.outputInventorySlot.set(modificationTableRecipe.value().assemble(input));
        }
        else
        {
            this.outputInventorySlot.set(ItemStack.EMPTY);
        }

        this.broadcastChanges();
    }

    public @NotNull MenuType<?> getType()
    {
        return ModContainerTypes.MODIFICATION_TABLE.get();
    }

    public void setInventoryUpdateListener(Runnable listenerIn)
    {
        this.inventoryUpdateListener = listenerIn;
    }

    /**
     * Called to determine if the current slot is valid for the stack merging (double-click) code. The stack passed in is
     * null for the initial slot that was double-clicked.
     */
    public boolean canTakeItemForPickAll(@NotNull ItemStack stack, Slot slotIn)
    {
        return slotIn.container != this.inventory && super.canTakeItemForPickAll(stack, slotIn);
    }

    /**
     * Handle when the stack in slot {@code index} is shift-clicked. Normally this moves the stack between the player
     * inventory and the other inventory(s).
     */
    public @NotNull ItemStack quickMoveStack(@NotNull Player playerIn, int index)
    {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem())
        {
            ItemStack itemstack1 = slot.getItem();
            Item item = itemstack1.getItem();
            itemstack = itemstack1.copy();
            if (index == 1)
            {
                item.onCraftedBy(itemstack1, playerIn);
                if (!this.moveItemStackTo(itemstack1, 2, 38, true))
                {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemstack1, itemstack);
            }
            else if (index == 0)
            {
                if (!this.moveItemStackTo(itemstack1, 2, 38, false))
                {
                    return ItemStack.EMPTY;
                }
            }
            else if (this.world.getServer() != null && this.world.getServer()
                .getRecipeManager()
                .getRecipeFor(ModRecipeTypes.MODIFICATION_TABLE.get(), CraftingInput.of(1, 1, List.of(inputItemStack)), this.world)
                .isPresent())
            {
                if (!this.moveItemStackTo(itemstack1, 0, 1, false))
                {
                    return ItemStack.EMPTY;
                }
            }
            else if (index >= 2 && index < 29)
            {
                if (!this.moveItemStackTo(itemstack1, 29, 38, false))
                {
                    return ItemStack.EMPTY;
                }
            }
            else if (index >= 29 && index < 38 && !this.moveItemStackTo(itemstack1, 2, 29, false))
            {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty())
            {
                slot.set(ItemStack.EMPTY);
            }

            slot.setChanged();
            if (itemstack1.getCount() == itemstack.getCount())
            {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, itemstack1);
            this.broadcastChanges();
        }

        return itemstack;
    }

    /**
     * Called when the container is closed.
     */
    public void removed(@NotNull Player playerIn)
    {
        super.removed(playerIn);
        this.inventory.removeItemNoUpdate(1);
        this.worldPosCallable.execute((p_217079_2_, p_217079_3_) -> this.clearContainer(playerIn, this.inputInventory));
    }

    public void setRecipes(final List<RecipeHolder<ModificationTableRecipe>> recipes)
    {
        this.recipes.clear();
        this.recipes.addAll(recipes);
        ModificationTableContainer.this.inventoryUpdateListener.run();
    }
}

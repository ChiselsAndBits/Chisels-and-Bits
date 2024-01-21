package mod.chiselsandbits.inventory.bit;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import mod.chiselsandbits.api.blockinformation.IBlockInformation;
import mod.chiselsandbits.blockinformation.BlockInformation;
import mod.chiselsandbits.api.config.IServerConfiguration;
import mod.chiselsandbits.api.inventory.bit.IBitInventoryItemStack;
import mod.chiselsandbits.api.item.bit.IBitItem;
import mod.chiselsandbits.api.item.bit.IBitItemManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SlottedBitInventoryItemStack extends SlottedBitInventory implements IBitInventoryItemStack
{
    private final Function<CompoundTag, ItemStack> saveBuilder;

    public SlottedBitInventoryItemStack(final int size, final Function<CompoundTag, ItemStack> saveBuilder)
    {
        super(size);

        this.saveBuilder = saveBuilder;
    }

    @Override
    public ItemStack toItemStack()
    {
        final CompoundTag compoundNBT = this.serializeNBT();
        return this.saveBuilder.apply(compoundNBT);
    }

    @Override
    public List<Component> listContents()
    {
        return getContents().stream()
          .sorted(Comparator.comparingInt(BitSlot::getCount).reversed())
          .map(slot -> Component.translatable("chiselsandbits.bitbag.contents.enum.entry", slot.getCount(), slot.getBlockInformation().getBlockState().getBlock().getName()))
          .collect(Collectors.toList());
    }

    @Override
    public double getFilledRatio()
    {
        return this.slotMap.keySet().size() / (double) this.size;
    }

    @Override
    public void clear(final IBlockInformation state)
    {
        final Int2ObjectMap<BitSlot> slots = new Int2ObjectArrayMap<>(this.slotMap);

        this.slotMap.clear();
        int slotIndex = 0;
        for (BitSlot bitSlot : slots.values())
        {
            if (bitSlot.getBlockInformation() != state) {
                this.slotMap.put(slotIndex, bitSlot);
                slotIndex++;
            }
        }
    }
    @Override
    public void convert(Player player) {
        // Get counts of all the bits present in the bag and clear it.
        final Map<IBlockInformation, Integer> contentMap = Maps.newHashMap();
        this.slotMap.values().forEach(bitSlot -> {
                    contentMap.putIfAbsent(bitSlot.getBlockInformation(), 0);
                    contentMap.compute(bitSlot.getBlockInformation(), (s, c) -> (c == null ? 0 : c) + bitSlot.getCount());
                }
        );
        this.slotMap.clear();



        List<Map.Entry<IBlockInformation, Integer>> toSort = new ArrayList<>(contentMap.entrySet());
        toSort.sort(Map.Entry.<IBlockInformation, Integer>comparingByValue().reversed());

        int slotIndex = 0;
        for (Map.Entry<IBlockInformation, Integer> e : toSort)
        {
            int count = e.getValue();
            if (count == 0) {
                continue;
            }

            // Give player items for each 4096 bits they have (full block) 16^3
            while (count >= IServerConfiguration.getInstance().getBitSize().get().getBitsPerBlock()) {
                // Give the block to the player
                ItemStack block = new ItemStack(e.getKey().getBlockState().getBlock().asItem());
                if (player.getInventory().add(block)) {
                    count -= IServerConfiguration.getInstance().getBitSize().get().getBitsPerBlock();
                } else {
                    // The player has run out of space!
                    break;
                }
            }
            // Sort the remaining bits into stacks.
            while (count > IServerConfiguration.getInstance().getBagStackSize().get() && count > 0) {
                this.slotMap.put(slotIndex, new BitSlot(e.getKey(), IServerConfiguration.getInstance().getBagStackSize().get()));
                slotIndex++;
                count -= IServerConfiguration.getInstance().getBagStackSize().get();
            }

            if (count > 0) {
                this.slotMap.put(slotIndex, new BitSlot(e.getKey(), count));
                slotIndex++;
            }
        }

    }

    @Override
    public void sort()
    {
        final Map<IBlockInformation, Integer> contentMap = Maps.newHashMap();
        this.slotMap.values().forEach(bitSlot -> {
            contentMap.putIfAbsent(bitSlot.getBlockInformation(), 0);
            contentMap.compute(bitSlot.getBlockInformation(), (s, c) -> (c == null ? 0 : c) + bitSlot.getCount());
          }
        );

        this.slotMap.clear();

        List<Map.Entry<IBlockInformation, Integer>> toSort = new ArrayList<>(contentMap.entrySet());
        toSort.sort(Map.Entry.<IBlockInformation, Integer>comparingByValue().reversed());

        int slotIndex = 0;
        for (Map.Entry<IBlockInformation, Integer> e : toSort)
        {
            int count = e.getValue();
            if (count == 0)
                continue;

            while (count > IServerConfiguration.getInstance().getBagStackSize().get() && count > 0) {
                this.slotMap.put(slotIndex, new BitSlot(e.getKey(), IServerConfiguration.getInstance().getBagStackSize().get()));
                slotIndex++;
                count -= IServerConfiguration.getInstance().getBagStackSize().get();
            }

            if (count > 0) {
                this.slotMap.put(slotIndex, new BitSlot(e.getKey(), count));
                slotIndex++;
            }
        }
    }

    @Override
    public int getContainerSize()
    {
        return this.size;
    }

    @Override
    public @NotNull ItemStack getItem(final int index)
    {
        return super.getItem(index);
    }

    @Override
    public @NotNull ItemStack removeItem(final int index, final int count)
    {
        if (!this.slotMap.containsKey(index))
            return ItemStack.EMPTY;

        final BitSlot bitSlot = this.slotMap.get(index);
        final int containedCount = bitSlot.getCount();
        bitSlot.setCount(containedCount - count);
        if (bitSlot.getCount() <= 0)
            this.slotMap.remove(index);

        return IBitItemManager.getInstance().create(bitSlot.getBlockInformation(), Math.min(containedCount, count));
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(final int index)
    {
        return removeItem(index, Integer.MAX_VALUE);
    }

    @Override
    public void setItem(final int index, final ItemStack stack)
    {
        if (stack.isEmpty()) {
            this.slotMap.remove(index);
            return;
        }

        if (!(stack.getItem() instanceof final IBitItem bitItem)) {
            return;
        }

        final IBlockInformation state = bitItem.getBlockInformation(stack);

        final BitSlot bitSlot = this.slotMap.getOrDefault(index, new BitSlot());
        bitSlot.setBlockInformation(state);
        bitSlot.setCount(stack.getCount());

        this.slotMap.put(index, bitSlot);
    }

    @Override
    public void setChanged()
    {
        onChange();
    }

    @Override
    public boolean stillValid(final @NotNull Player player)
    {
        return true;
    }

    @Override
    public void clearContent()
    {
        this.slotMap.clear();
        onChange();
    }

    @Override
    protected void onChange()
    {
        super.onChange();
        //We invoke to itemStack for sure to guarantee the saving.
        toItemStack();
    }

    @Override
    protected int getMaxBitsForSlot()
    {
        return IServerConfiguration.getInstance().getBagStackSize().get();
    }

    @Override
    public int getMaxStackSize()
    {
        return getMaxBitsForSlot();
    }

    //The following methods are needed to handle the obfuscation tree.
    @Override
    public boolean isEmpty()
    {
        return super.isEmpty();
    }
}

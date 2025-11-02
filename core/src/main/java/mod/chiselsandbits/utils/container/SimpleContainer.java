package mod.chiselsandbits.utils.container;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mod.chiselsandbits.api.serialization.Serializable;
import mod.chiselsandbits.api.util.constants.NbtConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SimpleContainer implements Container, Serializable.Registry<SimpleContainer>
{
    public static final Codec<SimpleContainer> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            SlotData.CODEC.listOf().fieldOf(NbtConstants.SLOTS).forGetter(SimpleContainer::slots)
        ).apply(instance, SimpleContainer::new)
    );
    public static final MapCodec<SimpleContainer> MAP_CODEC = SlotData.CODEC.listOf().fieldOf(NbtConstants.SLOTS).xmap(SimpleContainer::new, SimpleContainer::slots);
    public static final StreamCodec<RegistryFriendlyByteBuf, SimpleContainer> STREAM_CODEC = SlotData.STREAM_CODEC.apply(ByteBufCodecs.list()).map(SimpleContainer::new, SimpleContainer::slots);

    private int                     size;
    private NonNullList<ItemStack>  items;
    private List<ContainerListener> listeners;

    public SimpleContainer(int size) {
        this.size = size;
        this.items = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    public SimpleContainer(ItemStack... content) {
        this.size = content.length;
        this.items = NonNullList.of(ItemStack.EMPTY, content);
    }

    private SimpleContainer(List<SlotData> slotData) {
        this.size = slotData.size();
        this.items = NonNullList.withSize(size, ItemStack.EMPTY);
        slotData.forEach(data -> this.items.set(data.index(), data.stack()));
    }

    public void addListener(ContainerListener param0) {
        if (this.listeners == null) {
            this.listeners = Lists.newArrayList();
        }

        this.listeners.add(param0);
    }

    public void removeListener(ContainerListener param0) {
        this.listeners.remove(param0);
    }

    public @NotNull ItemStack getItem(int index) {
        return index >= 0 && index < this.items.size() ? this.items.get(index) : ItemStack.EMPTY;
    }

    public List<ItemStack> removeAllItems() {
        List<ItemStack> list = this.items.stream().filter((stack) -> !stack.isEmpty()).collect(Collectors.toList());
        this.clearContent();
        return list;
    }

    public @NotNull ItemStack removeItem(int index, int count) {
        ItemStack removeResult = ContainerHelper.removeItem(this.items, index, count);
        if (!removeResult.isEmpty()) {
            this.setChanged();
        }

        return removeResult;
    }

    public ItemStack removeItemType(Item item, int toExtract) {
        ItemStack nullStack = new ItemStack(item, 0);

        for(int reversedIndex = this.size - 1; reversedIndex >= 0; --reversedIndex) {
            ItemStack stack = this.getItem(reversedIndex);
            if (stack.getItem().equals(item)) {
                int remaining = toExtract - nullStack.getCount();
                ItemStack splitResult = stack.split(remaining);
                nullStack.grow(splitResult.getCount());
                if (nullStack.getCount() == toExtract) {
                    break;
                }
            }
        }

        if (!nullStack.isEmpty()) {
            this.setChanged();
        }

        return nullStack;
    }

    public ItemStack addItem(ItemStack input) {
        ItemStack var0 = input.copy();
        this.moveItemToOccupiedSlotsWithSameType(var0);
        if (var0.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            this.moveItemToEmptySlots(var0);
            return var0.isEmpty() ? ItemStack.EMPTY : var0;
        }
    }

    public boolean canAddItem(ItemStack input) {
        boolean foundEmptyOrMatching = false;

        for (final ItemStack stack : this.items)
        {
            if (stack.isEmpty() || ItemStack.isSameItemSameComponents(stack, input) && stack.getCount() < stack.getMaxStackSize())
            {
                foundEmptyOrMatching = true;
                break;
            }
        }

        return foundEmptyOrMatching;
    }

    public @NotNull ItemStack removeItemNoUpdate(int index) {
        ItemStack stack = this.items.get(index);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            this.items.set(index, ItemStack.EMPTY);
            return stack;
        }
    }

    public void setItem(int index, @NotNull ItemStack stack) {
        this.items.set(index, stack);
        if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }

        this.setChanged();
    }

    public int getContainerSize() {
        return this.size;
    }

    public boolean isEmpty() {
        return this.items.stream().allMatch(ItemStack::isEmpty);
    }

    public void setChanged() {
        if (this.listeners != null) {
            this.listeners.forEach(listener -> listener.containerChanged(this));
        }
    }

    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    public void clearContent() {
        this.items.clear();
        this.setChanged();
    }

    public String toString() {
        return this.items.stream().filter((stack) -> !stack.isEmpty()).toList().toString();
    }

    private void moveItemToEmptySlots(ItemStack input) {
        for(int index = 0; index < this.size; ++index) {
            ItemStack stack = this.getItem(index);
            if (stack.isEmpty()) {
                this.setItem(index, input.copy());
                input.setCount(0);
                return;
            }
        }

    }

    private void moveItemToOccupiedSlotsWithSameType(ItemStack input) {
        for(int index = 0; index < this.size; ++index) {
            ItemStack stack = this.getItem(index);
            if (ItemStack.isSameItemSameComponents(stack, input)) {
                this.moveItemsBetweenStacks(input, stack);
                if (input.isEmpty()) {
                    return;
                }
            }
        }

    }

    private void moveItemsBetweenStacks(ItemStack leftInput, ItemStack rightInput) {
        int maxTransfer = Math.min(this.getMaxStackSize(), rightInput.getMaxStackSize());
        int toTransfer = Math.min(leftInput.getCount(), maxTransfer - rightInput.getCount());
        if (toTransfer > 0) {
            rightInput.grow(toTransfer);
            leftInput.shrink(toTransfer);
            this.setChanged();
        }
    }

    public void setSize(int size)
    {
        this.size = size;
        this.items = NonNullList.withSize(size, ItemStack.EMPTY);
        setChanged();
    }

    private List<SlotData> slots() {
        return IntStream.range(0, this.size)
            .mapToObj(index -> new SlotData(index, this.items.get(index)))
            .collect(Collectors.toList());
    }

    @Override
    public Codec<SimpleContainer> codec() {
        return CODEC;
    }

    @Override
    public MapCodec<SimpleContainer> mapCodec() {
        return MAP_CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, SimpleContainer> streamCodec() {
        return STREAM_CODEC;
    }

    private record SlotData(int index, ItemStack stack) {

        static Codec<SlotData> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                Codec.INT.fieldOf("index").forGetter(SlotData::index),
                ItemStack.OPTIONAL_CODEC.fieldOf("stack").forGetter(SlotData::stack)
            ).apply(instance, SlotData::new)
        );

        static StreamCodec<RegistryFriendlyByteBuf, SlotData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT,
                SlotData::index,
                ItemStack.OPTIONAL_STREAM_CODEC,
                SlotData::stack,
                SlotData::new
        );
    }
}

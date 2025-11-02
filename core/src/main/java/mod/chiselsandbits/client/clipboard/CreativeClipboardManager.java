package mod.chiselsandbits.client.clipboard;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Dynamic;
import mod.chiselsandbits.api.client.clipboard.ICreativeClipboardManager;
import mod.chiselsandbits.api.config.IClientConfiguration;
import mod.chiselsandbits.api.item.multistate.IMultiStateItemStack;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.item.multistate.SingleBlockMultiStateItemStack;
import mod.chiselsandbits.registrars.ModCreativeTabs;
import mod.chiselsandbits.utils.SimpleMaxSizedList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;

public final class CreativeClipboardManager implements ICreativeClipboardManager
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final CreativeClipboardManager INSTANCE = new CreativeClipboardManager();

    public static CreativeClipboardManager getInstance()
    {
        return INSTANCE;
    }

    private final SimpleMaxSizedList<IMultiStateItemStack> cache = new SimpleMaxSizedList<>(
      IClientConfiguration.getInstance().getClipboardSize()
    );

    private CreativeClipboardManager()
    {
    }

    public void load(HolderLookup.Provider provider) {
        final File file = new File(Constants.MOD_ID + "/clipboard.dat");
        if (!file.exists()) {
            return;
        }

        try
        {
            final CompoundTag data = NbtIo.readCompressed(file.toPath(), NbtAccounter.unlimitedHeap());
            final ListTag tags = data.getList("clipboard").orElse(new ListTag());
            tags.stream()
              .filter(CompoundTag.class::isInstance)
              .map(CompoundTag.class::cast)
              .map(tag -> {
                  final RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, provider);
                  return ItemStack.OPTIONAL_CODEC.parse(ops, tag).getOrThrow();
              })
              .map(SingleBlockMultiStateItemStack::new)
              .forEach(cache::add);
        }
        catch (IOException e)
        {
            LOGGER.fatal("Failed to read a clipboard file!", e);
        }
    }

    private void writeContentsToDisk(HolderLookup.Provider provider) {
        final CompoundTag data = new CompoundTag();
        final ListTag tags = new ListTag();

        cache.stream()
          .map(IMultiStateItemStack::toBlockStack)
          .map(stack -> {
              final RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, provider);
              return ItemStack.OPTIONAL_CODEC.encode(stack, ops, new CompoundTag()).getOrThrow();
          })
          .forEach(tags::add);

        data.put("clipboard", tags);

        final File file = new File(Constants.MOD_ID + "/clipboard.dat");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }
        else
        {
            file.delete();
        }

        try
        {
            file.createNewFile();
            NbtIo.writeCompressed(data, file.toPath());
        }
        catch (IOException e)
        {
            LOGGER.fatal("Failed to create a clipboard file!", e);
        }
    }

    @Override
    public List<IMultiStateItemStack> getClipboard()
    {
        return ImmutableList.copyOf(cache);
    }

    @Override
    public void addEntry(final IMultiStateItemStack multiStateItemStack, HolderLookup.Provider provider)
    {
        synchronized (cache) {
            cache.add(multiStateItemStack);
            writeContentsToDisk(provider);

            updateCreativeTab();
        }
    }

    private void updateCreativeTab() {
        ModCreativeTabs.CLIPBOARD.get()
                .displayItems = ItemStackLinkedSet.createTypeAndComponentsSet();
        ModCreativeTabs.CLIPBOARD.get()
                .displayItemsSearchTab = ItemStackLinkedSet.createTypeAndComponentsSet();

        cache.forEach(stack -> {
            ModCreativeTabs.CLIPBOARD.get()
                    .displayItems.add(stack.toBlockStack());
        });

        if (Minecraft.getInstance().screen instanceof CreativeModeInventoryScreen screen) {
            screen.refreshCurrentTabContents(this.cache.stream().map(IMultiStateItemStack::toBlockStack).toList());
        }
    }

    @Override
    public void removeEntry(int index, HolderLookup.Provider provider) {
        synchronized (cache) {
            if (index < 0 || index >= cache.size()) {
                return;
            }

            cache.remove(index);
            writeContentsToDisk(provider);

            updateCreativeTab();
        }
    }
}

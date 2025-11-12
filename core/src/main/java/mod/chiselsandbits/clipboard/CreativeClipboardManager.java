package mod.chiselsandbits.clipboard;

import com.google.common.collect.ImmutableList;
import mod.chiselsandbits.api.client.clipboard.ICreativeClipboardManager;
import mod.chiselsandbits.api.config.IClientConfiguration;
import mod.chiselsandbits.api.item.multistate.IMultiStateItemStack;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.item.multistate.SingleBlockMultiStateItemStack;
import mod.chiselsandbits.registrars.ModCreativeTabs;
import mod.chiselsandbits.utils.SimpleMaxSizedList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
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

    public void load() {
        final File file = new File(Constants.MOD_ID + "/clipboard.dat");
        if (!file.exists()) {
            return;
        }

        cache.clear();

        try
        {
            final CompoundTag data = NbtIo.readCompressed(file);
            final ListTag tags = data.getList("clipboard", Tag.TAG_COMPOUND);
            tags.stream()
              .filter(CompoundTag.class::isInstance)
              .map(CompoundTag.class::cast)
              .map(ItemStack::of)
              .map(SingleBlockMultiStateItemStack::new)
              .forEach(cache::add);
        }
        catch (IOException e)
        {
            LOGGER.fatal("Failed to read a clipboard file!", e);
        }
    }

    private void writeContentsToDisk() {
        final CompoundTag data = new CompoundTag();
        final ListTag tags = new ListTag();

        cache.stream()
          .map(IMultiStateItemStack::toBlockStack)
          .map(stack -> stack.save(new CompoundTag()))
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
            NbtIo.writeCompressed(data, file);
        }
        catch (IOException e)
        {
            LOGGER.fatal("Failed to create a clipboard file!", e);
        }
    }

    private void updateCreativeTab() {
        try {
            ModCreativeTabs.CLIPBOARD.get().displayItems = ItemStackLinkedSet.createTypeAndTagSet();

            cache.forEach(stack -> {
                ModCreativeTabs.CLIPBOARD.get().displayItems.add(stack.toBlockStack());
            });

            if (Minecraft.getInstance().screen instanceof CreativeModeInventoryScreen screen) {
                screen.refreshCurrentTabContents(this.cache.stream().map(IMultiStateItemStack::toBlockStack).toList());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to update creative tab", e);
        }
    }


    @Override
    public List<IMultiStateItemStack> getClipboard()
    {
        return ImmutableList.copyOf(cache);
    }

    @Override
    public void addEntry(final IMultiStateItemStack multiStateItemStack)
    {
        synchronized (cache) {
            if (cache.contains(multiStateItemStack))
                return;

            cache.add(multiStateItemStack);
            writeContentsToDisk();

            updateCreativeTab();
        }
    }

    @Override
    public void removeEntry(int index) {
        synchronized (cache) {
            if (index < 0 || index >= cache.size()) {
                return;
            }

            cache.remove(index);
            writeContentsToDisk();

            updateCreativeTab();
        }
    }
}

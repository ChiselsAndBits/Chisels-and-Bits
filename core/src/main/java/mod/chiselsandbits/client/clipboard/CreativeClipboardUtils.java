package mod.chiselsandbits.client.clipboard;

import mod.chiselsandbits.api.client.clipboard.ICreativeClipboardManager;
import mod.chiselsandbits.api.config.IClientConfiguration;
import mod.chiselsandbits.api.item.multistate.IMultiStateItemStack;
import mod.chiselsandbits.registrars.ModCreativeTabs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.HolderLookup;

public final class CreativeClipboardUtils
{

    private CreativeClipboardUtils()
    {
        throw new IllegalStateException("Can not instantiate an instance of: CreativeClipboardUtils. This is a utility class");
    }

    public static void addPickedBlock(final IMultiStateItemStack multiStateItemStack, HolderLookup.Provider provider) {
        if (IClientConfiguration.getInstance().getShouldPickedBlocksBeAddedToClipboard().get()) {
            ICreativeClipboardManager.getInstance().addEntry(multiStateItemStack, provider);
        }
    }

    public static void addBrokenBlock(final IMultiStateItemStack multiStateItemStack, HolderLookup.Provider provider) {
        if (IClientConfiguration.getInstance().getShouldBrokenBlocksBeAddedToClipboard().get()) {
            ICreativeClipboardManager.getInstance().addEntry(multiStateItemStack, provider);
        }
    }

    public static void deleteHoveredClipboardEntry(final Minecraft minecraft, final CreativeModeInventoryScreen screen) {
        if (CreativeModeInventoryScreen.selectedTab != ModCreativeTabs.CLIPBOARD.get())
            return;

        if (screen.hoveredSlot == null)
            return;

        if (minecraft.level == null)
            return;

        if (screen.isCreativeSlot(screen.hoveredSlot)) {
            ICreativeClipboardManager.getInstance().removeEntry(screen.hoveredSlot.index, minecraft.level.registryAccess());
        }
    }

    public static void clearClipboard(final Minecraft minecraft) {
        if (CreativeModeInventoryScreen.selectedTab != ModCreativeTabs.CLIPBOARD.get())
            return;

        if (minecraft.level == null)
            return;

        ICreativeClipboardManager.getInstance().clear(minecraft.level.registryAccess());
    }
}

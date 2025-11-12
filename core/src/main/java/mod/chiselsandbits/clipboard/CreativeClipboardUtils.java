package mod.chiselsandbits.clipboard;

import mod.chiselsandbits.api.client.clipboard.ICreativeClipboardManager;
import mod.chiselsandbits.api.config.IClientConfiguration;
import mod.chiselsandbits.api.item.multistate.IMultiStateItemStack;
import mod.chiselsandbits.registrars.ModCreativeTabs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class CreativeClipboardUtils
{

    private static final Logger LOGGER = LogManager.getLogger();

    private CreativeClipboardUtils()
    {
        throw new IllegalStateException("Can not instantiate an instance of: CreativeClipboardUtils. This is a utility class");
    }

    public static void addPickedBlock(final IMultiStateItemStack multiStateItemStack) {
        if (IClientConfiguration.getInstance().getShouldPickedBlocksBeAddedToClipboard().get()) {
            ICreativeClipboardManager.getInstance().addEntry(multiStateItemStack);
        }
    }

    public static void addBrokenBlock(final IMultiStateItemStack multiStateItemStack) {
        if (IClientConfiguration.getInstance().getShouldBrokenBlocksBeAddedToClipboard().get()) {
            ICreativeClipboardManager.getInstance().addEntry(multiStateItemStack);
        }
    }

    public static void deleteHoveredClipboardEntry(final Minecraft minecraft, final CreativeModeInventoryScreen screen) {
        if (CreativeModeInventoryScreen.selectedTab != ModCreativeTabs.CLIPBOARD.get())
            return;

        if (screen.hoveredSlot == null)
            return;

        if (minecraft.level == null)
            return;

        try {
            if (screen.isCreativeSlot(screen.hoveredSlot)) {
                    ICreativeClipboardManager.getInstance().removeEntry(screen.hoveredSlot.index);
            }
        } catch (final Exception e) {
            LOGGER.error("Failed to delete hovered clipboard entry.", e);
        }
    }

}

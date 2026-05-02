package mod.chiselsandbits.client.tool.mode.icon;

import mod.chiselsandbits.api.client.tool.mode.icon.ISelectedToolModeIconRenderer;
import mod.chiselsandbits.api.util.constants.Constants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public class NoopSelectedToolModeIconRenderer implements ISelectedToolModeIconRenderer
{
    static Identifier ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "noop");

    @Override
    public Identifier getId()
    {
        return ID;
    }

    @Override
    public void extractGraphics(final GuiGraphicsExtractor guiGraphics, final ItemStack stack)
    {
        //Noop
    }
}

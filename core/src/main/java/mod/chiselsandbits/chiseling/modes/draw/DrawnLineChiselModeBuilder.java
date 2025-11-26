package mod.chiselsandbits.chiseling.modes.draw;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

public class DrawnLineChiselModeBuilder
{
    private MutableComponent displayName;
    private MutableComponent multiLineDisplayName;
    private Identifier       iconName;

    public DrawnLineChiselModeBuilder setDisplayName(final MutableComponent displayName)
    {
        this.displayName = displayName;
        return this;
    }

    public DrawnLineChiselModeBuilder setMultiLineDisplayName(final MutableComponent multiLineDisplayName)
    {
        this.multiLineDisplayName = multiLineDisplayName;
        return this;
    }

    public DrawnLineChiselModeBuilder setIconName(final Identifier iconName)
    {
        this.iconName = iconName;
        return this;
    }

    public DrawnLineChiselMode createDrawnLineChiselMode()
    {
        return new DrawnLineChiselMode(displayName, multiLineDisplayName, iconName);
    }
}
package mod.chiselsandbits.chiseling.modes.draw;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

public class DrawnWallChiselModeBuilder
{
    private MutableComponent displayName;
    private MutableComponent multiLineDisplayName;
    private Identifier       iconName;
    private int              width = 1;

    public DrawnWallChiselModeBuilder setDisplayName(final MutableComponent displayName)
    {
        this.displayName = displayName;
        return this;
    }

    public DrawnWallChiselModeBuilder setMultiLineDisplayName(final MutableComponent multiLineDisplayName)
    {
        this.multiLineDisplayName = multiLineDisplayName;
        return this;
    }

    public DrawnWallChiselModeBuilder setIconName(final Identifier iconName)
    {
        this.iconName = iconName;
        return this;
    }

    public DrawnWallChiselModeBuilder setWidth(final int width)
    {
        this.width = width;
        return this;
    }

    public DrawnWallChiselMode createDrawnWallChiselMode()
    {
        return new DrawnWallChiselMode(displayName, multiLineDisplayName, iconName, width);
    }
}
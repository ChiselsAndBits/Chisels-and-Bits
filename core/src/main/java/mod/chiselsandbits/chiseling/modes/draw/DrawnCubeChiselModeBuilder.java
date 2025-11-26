package mod.chiselsandbits.chiseling.modes.draw;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

public class DrawnCubeChiselModeBuilder
{
    private MutableComponent displayName;
    private MutableComponent multiLineDisplayName;
    private Identifier       iconName;

    public DrawnCubeChiselModeBuilder setDisplayName(final MutableComponent displayName)
    {
        this.displayName = displayName;
        return this;
    }

    public DrawnCubeChiselModeBuilder setMultiLineDisplayName(final MutableComponent multiLineDisplayName)
    {
        this.multiLineDisplayName = multiLineDisplayName;
        return this;
    }

    public DrawnCubeChiselModeBuilder setIconName(final Identifier iconName)
    {
        this.iconName = iconName;
        return this;
    }

    public DrawnCubeChiselMode createDrawnCubeChiselMode()
    {
        return new DrawnCubeChiselMode(displayName, multiLineDisplayName, iconName);
    }
}
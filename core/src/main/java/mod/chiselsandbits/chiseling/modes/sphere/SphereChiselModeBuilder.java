package mod.chiselsandbits.chiseling.modes.sphere;

import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.MutableComponent;

public class SphereChiselModeBuilder {
    private int                       diameter;
    private MutableComponent displayName;
    private MutableComponent multiLineDisplayName;
    private Identifier       iconName;

    public SphereChiselModeBuilder setDiameter(final int diameter)
    {
        this.diameter = diameter;
        return this;
    }

    public SphereChiselModeBuilder setDisplayName(final MutableComponent displayName)
    {
        this.displayName = displayName;
        return this;
    }

    public SphereChiselModeBuilder setMultiLineDisplayName(final MutableComponent multiLineDisplayName)
    {
        this.multiLineDisplayName = multiLineDisplayName;
        return this;
    }

    public SphereChiselModeBuilder setIconName(final Identifier iconName)
    {
        this.iconName = iconName;
        return this;
    }

    public SphereChiselMode createSphereChiselMode()
    {
        return new SphereChiselMode(diameter, displayName, multiLineDisplayName, iconName);
    }
}
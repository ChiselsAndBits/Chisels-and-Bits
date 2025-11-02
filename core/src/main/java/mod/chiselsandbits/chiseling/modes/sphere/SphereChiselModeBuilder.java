package mod.chiselsandbits.chiseling.modes.sphere;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.MutableComponent;

public class SphereChiselModeBuilder {
    private int                       diameter;
    private MutableComponent displayName;
    private MutableComponent multiLineDisplayName;
    private ResourceLocation          iconName;

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

    public SphereChiselModeBuilder setIconName(final ResourceLocation iconName)
    {
        this.iconName = iconName;
        return this;
    }

    public SphereChiselMode createSphereChiselMode()
    {
        return new SphereChiselMode(diameter, displayName, multiLineDisplayName, iconName);
    }
}
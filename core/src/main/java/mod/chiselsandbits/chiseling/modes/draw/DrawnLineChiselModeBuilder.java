package mod.chiselsandbits.chiseling.modes.draw;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

public class DrawnLineChiselModeBuilder
{
    private MutableComponent displayName;
    private MutableComponent   multiLineDisplayName;
    private ResourceLocation iconName;

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

    public DrawnLineChiselModeBuilder setIconName(final ResourceLocation iconName)
    {
        this.iconName = iconName;
        return this;
    }

    public DrawnLineChiselMode createDrawnLineChiselMode()
    {
        return new DrawnLineChiselMode(displayName, multiLineDisplayName, iconName);
    }
}
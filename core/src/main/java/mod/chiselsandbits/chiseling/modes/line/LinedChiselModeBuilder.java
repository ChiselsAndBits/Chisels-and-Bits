package mod.chiselsandbits.chiseling.modes.line;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.MutableComponent;

public class LinedChiselModeBuilder
{
    private int                       bitsPerSide;
    private MutableComponent displayName;
    private MutableComponent   multiLineDisplayName;
    private ResourceLocation iconName;

    public LinedChiselModeBuilder setBitsPerSide(final int bitsPerSide)
    {
        this.bitsPerSide = bitsPerSide;
        return this;
    }

    public LinedChiselModeBuilder setDisplayName(final MutableComponent displayName)
    {
        this.displayName = displayName;
        return this;
    }

    public LinedChiselModeBuilder setMultiLineDisplayName(final MutableComponent multiLineDisplayName)
    {
        this.multiLineDisplayName = multiLineDisplayName;
        return this;
    }

    public LinedChiselModeBuilder setIconName(final ResourceLocation iconName)
    {
        this.iconName = iconName;
        return this;
    }

    public LinedChiselMode createLinedChiselMode()
    {
        return new LinedChiselMode(bitsPerSide, displayName, multiLineDisplayName, iconName);
    }
}
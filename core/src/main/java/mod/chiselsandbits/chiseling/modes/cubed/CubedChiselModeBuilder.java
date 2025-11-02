package mod.chiselsandbits.chiseling.modes.cubed;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.MutableComponent;

public class CubedChiselModeBuilder {
    private int                       bitsPerSide;
    private boolean                   aligned;
    private MutableComponent displayName;
    private MutableComponent multiLineDisplayName;
    private ResourceLocation          iconName;

    public CubedChiselModeBuilder setBitsPerSide(final int bitsPerSide)
    {
        this.bitsPerSide = bitsPerSide;
        return this;
    }

    public CubedChiselModeBuilder setAligned(final boolean aligned)
    {
        this.aligned = aligned;
        return this;
    }

    public CubedChiselModeBuilder setDisplayName(final MutableComponent displayName)
    {
        this.displayName = displayName;
        return this;
    }

    public CubedChiselModeBuilder setMultiLineDisplayName(final MutableComponent multiLineDisplayName)
    {
        this.multiLineDisplayName = multiLineDisplayName;
        return this;
    }

    public CubedChiselModeBuilder setIconName(final ResourceLocation iconName)
    {
        this.iconName = iconName;
        return this;
    }

    public CubedChiselMode createCubedChiselMode()
    {
        return new CubedChiselMode(bitsPerSide, aligned, displayName, multiLineDisplayName, iconName);
    }
}
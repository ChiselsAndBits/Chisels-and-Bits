package mod.chiselsandbits.chiseling.modes.replace;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

public class ReplaceChiselingModeBuilder {
    private MutableComponent          displayName;
    private MutableComponent   multiLineDisplayName;
    private ResourceLocation iconName;

    public ReplaceChiselingModeBuilder setDisplayName(final MutableComponent displayName)
    {
        this.displayName = displayName;
        return this;
    }

    public ReplaceChiselingModeBuilder setMultiLineDisplayName(final MutableComponent multiLineDisplayName)
    {
        this.multiLineDisplayName = multiLineDisplayName;
        return this;
    }

    public ReplaceChiselingModeBuilder setIconName(final ResourceLocation iconName)
    {
        this.iconName = iconName;
        return this;
    }

    public ReplaceChiselingMode createReplaceChiselingMode()
    {
        return new ReplaceChiselingMode(displayName, multiLineDisplayName, iconName);
    }
}
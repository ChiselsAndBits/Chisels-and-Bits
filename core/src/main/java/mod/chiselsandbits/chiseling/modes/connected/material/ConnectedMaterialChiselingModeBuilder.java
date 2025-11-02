package mod.chiselsandbits.chiseling.modes.connected.material;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

public class ConnectedMaterialChiselingModeBuilder
{
    private int                       depth;
    private MutableComponent          displayName;
    private MutableComponent   multiLineDisplayName;
    private ResourceLocation iconName;

    public ConnectedMaterialChiselingModeBuilder setDepth(final int depth)
    {
        this.depth = depth;
        return this;
    }

    public ConnectedMaterialChiselingModeBuilder setDisplayName(final MutableComponent displayName)
    {
        this.displayName = displayName;
        return this;
    }

    public ConnectedMaterialChiselingModeBuilder setMultiLineDisplayName(final MutableComponent multiLineDisplayName)
    {
        this.multiLineDisplayName = multiLineDisplayName;
        return this;
    }

    public ConnectedMaterialChiselingModeBuilder setIconName(final ResourceLocation iconName)
    {
        this.iconName = iconName;
        return this;
    }

    public ConnectedMaterialChiselingMode createConnectedMaterialChiselingMode()
    {
        return new ConnectedMaterialChiselingMode(depth, displayName, multiLineDisplayName, iconName);
    }
}
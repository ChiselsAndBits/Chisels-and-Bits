package mod.chiselsandbits.chiseling.modes.connected.plane;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

public class ConnectedPlaneChiselingModeBuilder {
    private int                       depth;
    private MutableComponent          displayName;
    private MutableComponent   multiLineDisplayName;
    private ResourceLocation iconName;

    public ConnectedPlaneChiselingModeBuilder setDepth(final int depth)
    {
        this.depth = depth;
        return this;
    }

    public ConnectedPlaneChiselingModeBuilder setDisplayName(final MutableComponent displayName)
    {
        this.displayName = displayName;
        return this;
    }

    public ConnectedPlaneChiselingModeBuilder setMultiLineDisplayName(final MutableComponent multiLineDisplayName)
    {
        this.multiLineDisplayName = multiLineDisplayName;
        return this;
    }

    public ConnectedPlaneChiselingModeBuilder setIconName(final ResourceLocation iconName)
    {
        this.iconName = iconName;
        return this;
    }

    public ConnectedPlaneChiselingMode createConnectedPlaneChiselingMode()
    {
        return new ConnectedPlaneChiselingMode(depth, displayName, multiLineDisplayName, iconName);
    }
}
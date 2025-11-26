package mod.chiselsandbits.chiseling.modes.replace;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

public class ReplaceChiselingModeBuilder {
    private MutableComponent          displayName;
    private MutableComponent multiLineDisplayName;
    private Identifier       iconName;

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

    public ReplaceChiselingModeBuilder setIconName(final Identifier iconName)
    {
        this.iconName = iconName;
        return this;
    }

    public ReplaceChiselingMode createReplaceChiselingMode()
    {
        return new ReplaceChiselingMode(displayName, multiLineDisplayName, iconName);
    }
}
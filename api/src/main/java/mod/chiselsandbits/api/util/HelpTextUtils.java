package mod.chiselsandbits.api.util;

import mod.chiselsandbits.api.config.ICommonConfiguration;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * Utility class for processing help texts,
 * most notably used in tooltips of items and blocks.
 */
public class HelpTextUtils
{

    private HelpTextUtils()
    {
        throw new IllegalStateException("Can not instantiate an instance of: HelpTextUtils. This is a utility class");
    }

    /**
     * Builds a new help tooltip if this is enabled by the player.
     *
     * @param helpText The help tooltip.
     * @param tooltip The tooltip lines to append to.
     * @param variables The variables to inject.
     */
    public static void build(final LocalStrings helpText, final List<Component> tooltip, final Object... variables)
    {
        build(helpText, tooltip::add, variables);
    }

    /**
     * Builds a new help tooltip if this is enabled by the player.
     *
     * @param helpText The help tooltip.
     * @param tooltip The tooltip lines to append to.
     * @param variables The variables to inject.
     */
    public static void build(final LocalStrings helpText, final Consumer<Component> tooltip, final Object... variables)
    {
        if ( ICommonConfiguration.getInstance().getEnableHelp().get() )
        {
            helpText.getTextLines(variables).forEach(tooltip);
        }
    }
}

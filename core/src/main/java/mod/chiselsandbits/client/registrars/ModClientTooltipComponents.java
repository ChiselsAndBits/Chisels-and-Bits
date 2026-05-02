package mod.chiselsandbits.client.registrars;

import mod.chiselsandbits.client.tooltip.ChiseledBlockTooltipHandler;

public class ModClientTooltipComponents
{

    public static void onClientConstruction() {
        ChiseledBlockTooltipHandler.configure();
    }
}

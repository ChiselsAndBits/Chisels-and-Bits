package mod.chiselsandbits.client;

import mod.chiselsandbits.api.plugin.IChiselsAndBitsPlugin;
import mod.chiselsandbits.api.plugin.IPluginManager;
import mod.chiselsandbits.client.registrars.*;
import mod.chiselsandbits.keys.KeyBindingManager;

public class ChiselsAndBitsClient {

    public ChiselsAndBitsClient() {
        BlockEntityRenderers.onClientConstruction();
        ItemColors.onClientConstruction();
        BlockColors.onClientConstruction();
        ItemBlockRenderTypes.onClientConstruction();
        EventHandlers.onClientConstruction();
        KeyBindingManager.getInstance().onModInitialization();
        Screens.onClientConstruction();
        ItemProperties.onClientConstruction();
        ClientTooltipComponents.onClientConstruction();

        IPluginManager.getInstance().run(IChiselsAndBitsPlugin::onClientConstruction);
    }
}

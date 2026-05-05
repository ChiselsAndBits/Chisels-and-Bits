package mod.chiselsandbits.client;

import mod.chiselsandbits.api.plugin.IChiselsAndBitsPlugin;
import mod.chiselsandbits.api.plugin.IPluginManager;
import mod.chiselsandbits.client.registrars.*;
import mod.chiselsandbits.keys.KeyBindingManager;

public class ChiselsAndBitsClient {

    public ChiselsAndBitsClient() {
        ModBlockEntityRenderers.onClientConstruction();
        ModBlockColors.onClientConstruction();
        ModClientEventHandlers.onClientConstruction();
        KeyBindingManager.getInstance().onModInitialization();
        ModScreens.onClientConstruction();
        ModItemProperties.onClientConstruction();
        ModClientTooltipComponents.onClientConstruction();
        ModEffectHandlers.onClientConstruction();

        IPluginManager.getInstance().run("client construction", IChiselsAndBitsPlugin::onClientConstruction);
    }
}

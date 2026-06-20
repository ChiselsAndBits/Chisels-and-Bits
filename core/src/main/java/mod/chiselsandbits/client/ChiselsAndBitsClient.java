package mod.chiselsandbits.client;

import mod.chiselsandbits.api.plugin.IChiselsAndBitsPlugin;
import mod.chiselsandbits.api.plugin.IPluginManager;
import mod.chiselsandbits.client.registrars.ModBlockColors;
import mod.chiselsandbits.client.registrars.ModBlockEntityRenderers;
import mod.chiselsandbits.client.registrars.ModClientEventHandlers;
import mod.chiselsandbits.client.registrars.ModClientTooltipComponents;
import mod.chiselsandbits.client.registrars.ModEffectHandlers;
import mod.chiselsandbits.client.registrars.ModItemProperties;
import mod.chiselsandbits.client.registrars.ModScreens;
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

package mod.chiselsandbits;

import mod.chiselsandbits.api.IChiselsAndBitsAPI;
import mod.chiselsandbits.api.chiseling.eligibility.IEligibilityOptions;
import mod.chiselsandbits.api.config.IChiselsAndBitsConfiguration;
import mod.chiselsandbits.api.inventory.bit.IAdaptingBitInventoryManager;
import mod.chiselsandbits.api.plugin.IChiselsAndBitsPlugin;
import mod.chiselsandbits.api.plugin.IPluginDiscoverer;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.apiipml.ChiselsAndBitsAPI;
import mod.chiselsandbits.config.ChiselsAndBitsConfiguration;
import mod.chiselsandbits.network.NetworkChannel;
import mod.chiselsandbits.plugin.PluginManger;
import mod.chiselsandbits.registrars.ModBlockEntityTypes;
import mod.chiselsandbits.registrars.ModBlocks;
import mod.chiselsandbits.registrars.ModChangeTypes;
import mod.chiselsandbits.registrars.ModChiselModeGroups;
import mod.chiselsandbits.registrars.ModChiselModes;
import mod.chiselsandbits.registrars.ModContainerTypes;
import mod.chiselsandbits.registrars.ModCreativeTabs;
import mod.chiselsandbits.registrars.ModDataComponentTypes;
import mod.chiselsandbits.registrars.ModEventHandler;
import mod.chiselsandbits.registrars.ModItems;
import mod.chiselsandbits.registrars.ModMetadataKeys;
import mod.chiselsandbits.registrars.ModModelProperties;
import mod.chiselsandbits.registrars.ModModificationOperation;
import mod.chiselsandbits.registrars.ModModificationOperationGroups;
import mod.chiselsandbits.registrars.ModMultiStateSnapshotTypes;
import mod.chiselsandbits.registrars.ModPatternPlacementTypes;
import mod.chiselsandbits.registrars.ModRecipeSerializers;
import mod.chiselsandbits.registrars.ModRecipeTypes;
import mod.chiselsandbits.registrars.ModTags;

public class ChiselsAndBits {
	private static ChiselsAndBits instance;
	private final NetworkChannel networkChannel = new NetworkChannel(Constants.MOD_ID);

	private final IChiselsAndBitsConfiguration configuration;

	public ChiselsAndBits(
			IEligibilityOptions eligibilityOptions,
			IAdaptingBitInventoryManager adaptingBitInventoryManager,
			IPluginDiscoverer pluginDiscoverer) {
		instance = this;

		this.configuration = new ChiselsAndBitsConfiguration();
		IChiselsAndBitsAPI.Holder.setInstance(new ChiselsAndBitsAPI(
				eligibilityOptions,
				adaptingBitInventoryManager,
				pluginDiscoverer
		));

		ModBlockEntityTypes.onModConstruction();
		ModBlocks.onModConstruction();
		ModChiselModeGroups.onModConstruction();
		ModChiselModes.onModConstruction();
		ModContainerTypes.onModConstruction();
		ModCreativeTabs.onModConstruction();
		ModItems.onModConstruction();
		ModMetadataKeys.onModConstruction();
		ModModelProperties.onModConstruction();
		ModModificationOperation.onModConstruction();
		ModModificationOperationGroups.onModConstruction();
		ModPatternPlacementTypes.onModConstruction();
		ModRecipeSerializers.onModConstruction();
		ModTags.onModConstruction();
		ModRecipeTypes.onModConstruction();
		ModChangeTypes.onModConstruction();
		ModMultiStateSnapshotTypes.onModConstruction();
		ModDataComponentTypes.onModConstruction();
		ModEventHandler.onModConstruction();

		networkChannel.registerCommonMessages();

		PluginManger.getInstance().detect();
		PluginManger.getInstance().run("construction", IChiselsAndBitsPlugin::onConstruction);
	}

	public static ChiselsAndBits getInstance() {
		return instance;
	}

	public IChiselsAndBitsConfiguration getConfiguration() {
		return configuration;
	}

	public NetworkChannel getNetworkChannel() {
		return networkChannel;
	}

	public void onInitialize() {
		PluginManger.getInstance().run("initialize", IChiselsAndBitsPlugin::onInitialize);
	}
}

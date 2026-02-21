package mod.chiselsandbits.forge;

import com.communi.suggestu.scena.core.dist.DistExecutor;
import com.communi.suggestu.scena.core.init.PlatformInitializationHandler;
import com.mojang.logging.LogUtils;
import mod.chiselsandbits.ChiselsAndBits;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.client.ChiselsAndBitsClient;
import mod.chiselsandbits.forge.platform.ForgeAdaptingBitInventoryManager;
import mod.chiselsandbits.forge.platform.ForgePluginDiscoverer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.extensions.IBlockExtension;
import org.slf4j.Logger;

import java.util.function.Consumer;

@Mod(Constants.MOD_ID)
public class Forge
{
    private static final Logger LOGGER = LogUtils.getLogger();

    private ChiselsAndBits chiselsAndBits;

    private void setChiselsAndBits(final ChiselsAndBits chiselsAndBits)
    {
        this.chiselsAndBits = chiselsAndBits;
    }

    public Forge(IEventBus modBus)
	{
        LOGGER.info("Initialized Chisels&Bits - Forge");
        //We need to use the platform initialization manager to handle the init in the constructor since this runs in parallel with scena itself.
        PlatformInitializationHandler.getInstance().onInit(platform -> {
            setChiselsAndBits(new ChiselsAndBits(
                    block -> block == IBlockExtension.class,
                    ForgeAdaptingBitInventoryManager.getInstance(),
                    ForgePluginDiscoverer.getInstance()
            ));
        });
        
        modBus.addListener((Consumer<FMLCommonSetupEvent>) event -> chiselsAndBits.onInitialize());
	}

    @Mod(value = Constants.MOD_ID, dist = Dist.CLIENT)
    public static final class Client {

        public Client(ModContainer container)
        {
            LOGGER.info("Initialized Chisels&Bits-Forge client");
            //We need to use the platform initialization manager to handle the init in the constructor since this runs in parallel with scena itself.
            PlatformInitializationHandler.getInstance().onInit(platform -> {
                setChiselsAndBitsClient(new ChiselsAndBitsClient());
            });

            container.registerExtensionPoint(IConfigScreenFactory.class, (mc, parent) -> new ConfigurationScreen(container, parent));
        }

        private static ChiselsAndBitsClient chiselsAndBitsClient;

        public static void setChiselsAndBitsClient(final ChiselsAndBitsClient chiselsAndBitsClient)
        {
            Client.chiselsAndBitsClient = chiselsAndBitsClient;
        }
    }
}

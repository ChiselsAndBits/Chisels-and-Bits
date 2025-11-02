package mod.chiselsandbits.forge.client.event;

import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.forge.client.block.ForgeClientChiseledBlockExtensions;
import mod.chiselsandbits.registrars.ModBlocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerBlock(new ForgeClientChiseledBlockExtensions(), ModBlocks.CHISELED_BLOCK.get());
    }
}

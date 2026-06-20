package mod.chiselsandbits.forge.data.tag;

import java.util.concurrent.CompletableFuture;

import mod.chiselsandbits.api.util.constants.Constants;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;

@EventBusSubscriber(modid = Constants.MOD_ID)
public class TagGeneratorEventHandler {
	@SubscribeEvent
	public static void dataGeneratorSetup(final GatherDataEvent.Client event) {
		DataGenerator gen = event.getGenerator();
		PackOutput packOutput = gen.getPackOutput();
		CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

		ModBlockTagGenerator modBlockTags = new ModBlockTagGenerator(packOutput, lookupProvider);
		event.getGenerator().addProvider(true, modBlockTags);
		event.getGenerator().addProvider(true, new ModItemTagGenerator(packOutput, lookupProvider));
	}
}

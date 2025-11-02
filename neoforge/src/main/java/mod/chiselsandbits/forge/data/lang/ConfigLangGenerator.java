package mod.chiselsandbits.forge.data.lang;

import com.communi.suggestu.scena.forge.platform.configuration.ForgeConfigurationManager;
import com.google.gson.JsonObject;
import mod.chiselsandbits.api.util.constants.Constants;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = Constants.MOD_ID)
public class ConfigLangGenerator implements DataProvider
{

    @SubscribeEvent
    public static void dataGeneratorSetup(final GatherDataEvent.Client event)
    {
        event.getGenerator().addProvider(true, new ConfigLangGenerator(event.getGenerator().getPackOutput()));
    }

    private final PackOutput packOutput;

    public ConfigLangGenerator(PackOutput packOutput) {
        this.packOutput = packOutput;
    }

    @Override
    public CompletableFuture<?> run(@NotNull final CachedOutput cache) {
        final List<String> langKeys = new ArrayList<>(ForgeConfigurationManager.getInstance().getAvailableKeys());
        langKeys.sort(Comparator.comparing(s -> s.replace(".comment", "")));
        final JsonObject returnValue = new JsonObject();

        for (String langKey : langKeys)
        {
            returnValue.addProperty(langKey, "");
        }

        final Path configLangFolder = this.packOutput.getOutputFolder().resolve(Constants.DataGenerator.CONFIG_LANG_DIR);
        final Path langPath = configLangFolder.resolve("config.json");

        return DataProvider.saveStable(cache, returnValue, langPath);
    }

    @NotNull
    @Override
    public String getName()
    {
        return "Chiseled config lang generator";
    }
}

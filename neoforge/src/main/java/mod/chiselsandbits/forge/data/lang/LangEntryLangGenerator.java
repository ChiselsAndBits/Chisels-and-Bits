package mod.chiselsandbits.forge.data.lang;

import com.google.gson.JsonObject;
import mod.chiselsandbits.api.util.LocalStrings;
import mod.chiselsandbits.api.util.constants.Constants;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@EventBusSubscriber(modid = Constants.MOD_ID)
public class LangEntryLangGenerator implements DataProvider
{
    @SubscribeEvent
    public static void dataGeneratorSetup(final GatherDataEvent.Client event)
    {
        event.getGenerator().addProvider(true, new LangEntryLangGenerator(event.getGenerator().getPackOutput()));
    }

    private final PackOutput generator;

    private LangEntryLangGenerator(final PackOutput generator) {this.generator = generator;}

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull final CachedOutput cache) {
        final List<String> langKeys = Arrays.stream(LocalStrings.values()).map(LocalStrings::toString).collect(Collectors.toList());
        Collections.sort(langKeys);
        final JsonObject returnValue = new JsonObject();

        for (String langKey : langKeys)
        {
            returnValue.addProperty(langKey, "");
        }

        final Path configLangFolder = this.generator.getOutputFolder().resolve(Constants.DataGenerator.CONFIG_LANG_DIR);
        final Path langPath = configLangFolder.resolve("localstrings.json");

        return DataProvider.saveStable(cache, returnValue, langPath);
    }

    @NotNull
    @Override
    public String getName()
    {
        return "Local strings lang generator";
    }
}

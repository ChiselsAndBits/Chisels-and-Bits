package mod.chiselsandbits.forge.data.lang;

import com.google.gson.JsonObject;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.keys.KeyBindingManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = Constants.MOD_ID)
public class KeyMapLangGenerator implements DataProvider {
    @SubscribeEvent
    public static void dataGeneratorSetup(final GatherDataEvent.Client event) {
        event.getGenerator().addProvider(true, new KeyMapLangGenerator(event.getGenerator().getPackOutput()));
    }

    private final PackOutput generator;

    private KeyMapLangGenerator(final PackOutput generator) {
        this.generator = generator;
    }

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull final CachedOutput cache) {
        final List<String> langKeys = new ArrayList<>(KeyBindingManager.getInstance().getAllKeyBinds()
                .stream()
                .map(KeyMapping::getName)
                .toList());

        Collections.sort(langKeys);
        final JsonObject returnValue = new JsonObject();

        for (String langKey : langKeys) {
            returnValue.addProperty(langKey, "");
        }

        final Path configLangFolder = this.generator.getOutputFolder().resolve(Constants.DataGenerator.CONFIG_LANG_DIR);
        final Path langPath = configLangFolder.resolve("keymappings.json");

        return DataProvider.saveStable(cache, returnValue, langPath);
    }

    @NotNull
    @Override
    public String getName() {
        return "Key Mapping generator";
    }
}

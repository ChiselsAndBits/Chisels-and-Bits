package mod.chiselsandbits.forge.data.recipe;

import com.google.gson.JsonObject;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.registrars.ModRecipeSerializers;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = Constants.MOD_ID)
public class SpecialCraftingRecipeGenerator implements DataProvider
{
    @SubscribeEvent
    public static void dataGeneratorSetup(final GatherDataEvent.Client event)
    {
        event.getGenerator().addProvider(true, new SpecialCraftingRecipeGenerator(event.getGenerator().getPackOutput()));
    }

    private final PackOutput generator;

    private SpecialCraftingRecipeGenerator(final PackOutput generator) {this.generator = generator;}

    @Override
    public @NotNull CompletableFuture<?> run(final @NotNull CachedOutput cache) {
        saveRecipe(cache, ModRecipeSerializers.BAG_DYEING.getId());
        return CompletableFuture.allOf();
    }

    private void saveRecipe(final CachedOutput cache, final ResourceLocation location) {
        final JsonObject object = new JsonObject();
        object.addProperty("type", location.toString());

        final Path recipeFolder = this.generator.getOutputFolder().resolve(Constants.DataGenerator.RECIPES_DIR);
        final Path recipePath = recipeFolder.resolve(location.getPath() + ".json");

        DataProvider.saveStable(cache, object, recipePath);
    }

    @Override
    public @NotNull String getName()
    {
        return "Special crafting recipe generator";
    }
}

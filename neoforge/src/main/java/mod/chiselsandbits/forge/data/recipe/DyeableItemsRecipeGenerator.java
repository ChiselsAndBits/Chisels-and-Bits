package mod.chiselsandbits.forge.data.recipe;

import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.registrars.ModItems;
import mod.chiselsandbits.registrars.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = Constants.MOD_ID)
public class DyeableItemsRecipeGenerator extends RecipeProvider
{

    public DyeableItemsRecipeGenerator(
        final HolderLookup.Provider registries,
        final RecipeOutput output
        )
    {
        super(registries, output);
    }

    @SubscribeEvent
    public static void dataGeneratorSetup(final GatherDataEvent.Client event)
    {
        event.getGenerator().addProvider(true,
            new Runner(
                event.getGenerator().getPackOutput(),
                event.getLookupProvider()
            )
        );
    }

    private static final class Runner extends RecipeProvider.Runner {

        Runner(final PackOutput packOutput, final CompletableFuture<HolderLookup.Provider> registries)
        {
            super(packOutput, registries);
        }

        @Override
        protected @NonNull RecipeProvider createRecipeProvider(final HolderLookup.@NonNull Provider registries, final @NonNull RecipeOutput output)
        {
            return new DyeableItemsRecipeGenerator(registries, output);
        }

        @Override
        public @NonNull String getName()
        {
            return "Dyeable Chisels & Bits Items";
        }
    }

    @Override
    protected void buildRecipes()
    {
        dyedItem(ModItems.ITEM_BIT_BAG.get(), "dyed_bit_bag");
    }
}

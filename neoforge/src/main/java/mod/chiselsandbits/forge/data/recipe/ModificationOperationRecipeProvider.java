package mod.chiselsandbits.forge.data.recipe;

import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.recipe.modificationtable.ModificationTableRecipe;
import mod.chiselsandbits.registrars.ModModificationOperation;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = Constants.MOD_ID)
public class ModificationOperationRecipeProvider extends RecipeProvider
{

    protected ModificationOperationRecipeProvider(final HolderLookup.Provider registries, final RecipeOutput output)
    {
        super(registries, output);
    }

    @SubscribeEvent
    public static void dataGeneratorSetup(final GatherDataEvent.Client event)
    {
        event.getGenerator().addProvider(true,
            new Runner(event.getGenerator().getPackOutput(), event.getLookupProvider()) {
                @Override
                protected @NotNull RecipeProvider createRecipeProvider(final HolderLookup.@NotNull Provider registries, final @NotNull RecipeOutput output)
                {
                    return new ModificationOperationRecipeProvider(registries, output);
                }

                @Override
                public @NotNull String getName()
                {
                    return "Modification table recipes";
                }
            }
        );
    }

    @Override
    protected void buildRecipes()
    {
        ModModificationOperation.REGISTRY_SUPPLIER.get().forEach(
            operation -> {
                this.output.accept(
                    ResourceKey.create(Registries.RECIPE, operation.getRegistryName()),
                    new ModificationTableRecipe(operation),
                    null
                );
            }
        );
    }
}

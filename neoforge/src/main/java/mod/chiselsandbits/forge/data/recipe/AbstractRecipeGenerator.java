package mod.chiselsandbits.forge.data.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public abstract class AbstractRecipeGenerator extends RecipeProvider
{
    private final ItemLike itemProvider;

    public AbstractRecipeGenerator(HolderLookup.Provider registries, RecipeOutput output, ItemLike itemProvider) {
        super(registries, output);
        this.itemProvider = itemProvider;
    }


    public ItemLike getItemProvider()
    {
        return itemProvider;
    }

    public interface GeneratorFactory<T extends AbstractRecipeGenerator> {
        T build(HolderLookup.Provider registries, RecipeOutput output, ItemLike itemProvider);
    }

    public static final class Runner extends RecipeProvider.Runner {

        private final ItemLike itemProvider;
        private final GeneratorFactory<?> factory;

        public Runner(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            final ItemLike itemProvider,
            final GeneratorFactory<?> factory
        ) {
            super(output, lookupProvider);
            this.itemProvider = itemProvider;
            this.factory = factory;
        }

        @Override
        protected @NotNull RecipeProvider createRecipeProvider(HolderLookup.@NotNull Provider lookupProvider, @NotNull RecipeOutput output) {
            return this.factory.build(lookupProvider, output, itemProvider);
        }

        @Override
        public final @NotNull String getName()
        {
            return Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(itemProvider.asItem())) + " recipe generator";
        }
    }
}
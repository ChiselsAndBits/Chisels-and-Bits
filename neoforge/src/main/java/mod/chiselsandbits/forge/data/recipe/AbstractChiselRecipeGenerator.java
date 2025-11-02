package mod.chiselsandbits.forge.data.recipe;

import mod.chiselsandbits.api.item.chisel.IChiselItem;
import mod.chiselsandbits.api.util.ParamValidator;
import mod.chiselsandbits.registrars.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public abstract class AbstractChiselRecipeGenerator extends AbstractRecipeGenerator
{
    private final TagKey<Item> rodTag;
    private final TagKey<Item> ingredientTag;

    public AbstractChiselRecipeGenerator(
        final HolderLookup.Provider registries, final RecipeOutput output, final ItemLike itemProvider,
        final TagKey<Item> rodTag,
        final TagKey<Item> ingredientTag)
    {
        super(registries, output, itemProvider);
        this.rodTag = rodTag;
        this.ingredientTag = ingredientTag;
    }

    @Override
    protected void buildRecipes()
    {
        ShapedRecipeBuilder.shaped(this.registries.lookupOrThrow(Registries.ITEM), RecipeCategory.TOOLS, getItemProvider())
            .pattern("st")
            .pattern("  ")
            .define('s', rodTag)
            .define('t', ingredientTag)
            .unlockedBy("has_rod", has(rodTag))
            .unlockedBy("has_ingredient", has(ingredientTag))
            .save(this.output);
    }
}
package mod.chiselsandbits.forge.data.recipe;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.forge.utils.CollectorUtils;
import mod.chiselsandbits.registrars.ModItems;
import mod.chiselsandbits.registrars.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = Constants.MOD_ID)
public class SimpleItemsRecipeGenerator extends AbstractRecipeGenerator
{
    private final boolean                      shapeless;
    private final List<String>                 pattern;
    private final Map<Character, TagKey<Item>> tagMap;
    private final Map<Character, ItemLike>     itemMap;

    public SimpleItemsRecipeGenerator(
        final HolderLookup.Provider registries,
        final RecipeOutput output,
        final ItemLike itemProvider,
        final String pattern,
        final Map<Character, TagKey<Item>> tagMap,
        final Map<Character, ItemLike> itemMap
        )
    {
        super(registries, output, itemProvider);
        this.shapeless = false;
        this.pattern = Arrays.asList(pattern.split(";"));
        this.tagMap = tagMap;
        this.itemMap = itemMap;
    }

    public SimpleItemsRecipeGenerator(
        final HolderLookup.Provider registries,
        final RecipeOutput output,
        final ItemLike itemProvider,
        final List<TagKey<Item>> tagMap,
        final List<ItemLike> itemMap)
    {
        super(registries, output, itemProvider);
        this.shapeless = true;
        this.pattern = ImmutableList.of("   ", "   ", "   ");
        this.tagMap = tagMap.stream().collect(CollectorUtils.toEnumeratedCharacterKeyedMap());
        this.itemMap = itemMap.stream().collect(CollectorUtils.toEnumeratedCharacterKeyedMap());
    }

    @SubscribeEvent
    public static void dataGeneratorSetup(final GatherDataEvent.Client event)
    {
        event.getGenerator().addProvider(true,
            new AbstractRecipeGenerator.Runner(
                event.getGenerator().getPackOutput(),
                event.getLookupProvider(),
                ModItems.ITEM_BIT_BAG.get(),
                (registries, output, itemProvider) -> new SimpleItemsRecipeGenerator(
                    registries,
                    output,
                    itemProvider,
                    "www;wbw;www",
                    ImmutableMap.of(
                        'w', ItemTags.WOOL
                    ),
                    ImmutableMap.of(
                        'b', ModItems.ITEM_BLOCK_BIT.get()
                    )
                )
            )
        );

        event.getGenerator().addProvider(true,
            new AbstractRecipeGenerator.Runner(
                event.getGenerator().getPackOutput(),
                event.getLookupProvider(),
                ModItems.MAGNIFYING_GLASS.get(),
                (registries, output, itemProvider) -> new SimpleItemsRecipeGenerator(
                    registries,
                    output,
                    itemProvider,
                    "cg ;s  ;   ",
                    ImmutableMap.of(
                        'c', ModTags.Items.CHISEL,
                        'g', Tags.Items.GLASS_BLOCKS,
                        's', Tags.Items.RODS_WOODEN
                    ),
                    ImmutableMap.of()
                )
            )
        );

        event.getGenerator().addProvider(true,
            new AbstractRecipeGenerator.Runner(
                event.getGenerator().getPackOutput(),
                event.getLookupProvider(),
                ModItems.MEASURING_TAPE.get(),
                (registries, output, itemProvider) -> new SimpleItemsRecipeGenerator(
                    registries,
                    output,
                    itemProvider,
                    "  s;isy;ii ",
                    ImmutableMap.of(
                        'i', Tags.Items.INGOTS_IRON,
                        's', Tags.Items.STRINGS,
                        'y', Tags.Items.DYES_YELLOW
                    ),
                    ImmutableMap.of()
                )
            )
        );

        event.getGenerator().addProvider(true,
            new AbstractRecipeGenerator.Runner(
                event.getGenerator().getPackOutput(),
                event.getLookupProvider(),
                ModItems.QUILL.get(),
                (registries, output, itemProvider) -> new SimpleItemsRecipeGenerator(
                    registries,
                    output,
                    itemProvider,
                    ImmutableList.of(
                        Tags.Items.FEATHERS,
                        Tags.Items.DYES_BLACK,
                        Tags.Items.DYES_YELLOW
                    ),
                    ImmutableList.of()
                )
            )
        );

        event.getGenerator().addProvider(true,
            new AbstractRecipeGenerator.Runner(
                event.getGenerator().getPackOutput(),
                event.getLookupProvider(),
                ModItems.SEALANT_ITEM.get(),
                (registries, output, itemProvider) -> new SimpleItemsRecipeGenerator(
                    registries,
                    output,
                    itemProvider,
                    ImmutableList.of(
                        Tags.Items.SLIME_BALLS,
                        Tags.Items.DRINKS_HONEY
                    ),
                    ImmutableList.of()
                )
            )
        );

        event.getGenerator().addProvider(true,
            new AbstractRecipeGenerator.Runner(
                event.getGenerator().getPackOutput(),
                event.getLookupProvider(),
                ModItems.WRENCH.get(),
                (registries, output, itemProvider) -> new SimpleItemsRecipeGenerator(
                    registries,
                    output,
                    itemProvider,
                    " pb; pp;p  ",
                    ImmutableMap.of(
                        'p', ItemTags.PLANKS
                    ),
                    ImmutableMap.of(
                        'b', ModItems.ITEM_BLOCK_BIT.get()
                    )
                )
            )
        );

        event.getGenerator().addProvider(true,
            new AbstractRecipeGenerator.Runner(
                event.getGenerator().getPackOutput(),
                event.getLookupProvider(),
                ModItems.UNSEAL_ITEM.get(),
                (registries, output, itemProvider) -> new SimpleItemsRecipeGenerator(
                    registries,
                    output,
                    itemProvider,
                    ImmutableList.of(),
                    ImmutableList.of(
                        Blocks.WET_SPONGE
                    )
                )
            )
        );
    }

    @Override
    protected void buildRecipes()
    {
        if (this.shapeless)
        {
            final ShapelessRecipeBuilder builder = ShapelessRecipeBuilder.shapeless(this.items, RecipeCategory.TOOLS, getItemProvider());
            tagMap.forEach((ingredientKey, tag) -> {
                builder.requires(tag);
                builder.unlockedBy("has_tag_" + ingredientKey, has(tag));
            });
            itemMap.forEach((ingredientKey, item) -> {
                builder.requires(item);
                builder.unlockedBy("has_item_" + ingredientKey, has(item));
            });
            builder.save(this.output);
        }
        else
        {
            final ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(this.items, RecipeCategory.TOOLS, getItemProvider());
            pattern.forEach(builder::pattern);
            tagMap.forEach((ingredientKey, tag) -> {
                builder.define(ingredientKey, tag);
                builder.unlockedBy("has_" + ingredientKey, has(tag));
            });
            itemMap.forEach((ingredientKey, item) -> {
                builder.define(ingredientKey, item);
                builder.unlockedBy("has_" + ingredientKey, has(item));
            });
            builder.save(this.output);
        }
    }
}

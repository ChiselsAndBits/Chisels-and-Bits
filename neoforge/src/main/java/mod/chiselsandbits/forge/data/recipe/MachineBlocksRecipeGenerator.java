package mod.chiselsandbits.forge.data.recipe;

import com.google.common.collect.ImmutableMap;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.registrars.ModBlocks;
import mod.chiselsandbits.registrars.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
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
public class MachineBlocksRecipeGenerator extends AbstractRecipeGenerator {
    @SubscribeEvent
    public static void dataGeneratorSetup(final GatherDataEvent.Client event) {
        event.getGenerator().addProvider(
            true,
            new AbstractRecipeGenerator.Runner(
                event.getGenerator().getPackOutput(),
                event.getLookupProvider(),
                ModBlocks.CHISELED_PRINTER.get(),
                (registries, output, itemProvider) -> new MachineBlocksRecipeGenerator(
                    registries,
                    output,
                    itemProvider,
                    " c ;l l;sss",
                    ImmutableMap.of(
                        'c', ModTags.Items.CHISEL,
                        'l', ItemTags.LOGS
                    ),
                    ImmutableMap.of(
                        's', Blocks.SMOOTH_STONE_SLAB
                    )
                )
            )
        );

        event.getGenerator().addProvider(
            true,
            new AbstractRecipeGenerator.Runner(
                event.getGenerator().getPackOutput(),
                event.getLookupProvider(),
                ModBlocks.MODIFICATION_TABLE.get(),
                (registries, output, itemProvider) -> new MachineBlocksRecipeGenerator(
                    registries,
                    output,
                    itemProvider,
                    "scs;nbn;ppp",
                    ImmutableMap.of(
                        's', ItemTags.WOODEN_SLABS,
                        'n', Tags.Items.NUGGETS_IRON,
                        'b', ItemTags.LOGS,
                        'p', ItemTags.PLANKS,
                        'c', ModTags.Items.CHISEL
                    ),
                    ImmutableMap.of()
                )
            )
        );

        event.getGenerator().addProvider(
            true,
            new AbstractRecipeGenerator.Runner(
                event.getGenerator().getPackOutput(),
                event.getLookupProvider(),
                ModBlocks.BIT_STORAGE.get(),
                (registries, output, itemProvider) -> new MachineBlocksRecipeGenerator(
                    registries,
                    output,
                    itemProvider,
                    "igi;glg;ici",
                    ImmutableMap.of(
                        'g', Tags.Items.GLASS_BLOCKS,
                        'l', ItemTags.LOGS,
                        'i', Tags.Items.INGOTS_IRON,
                        'c', ModTags.Items.CHISEL
                    ),
                    ImmutableMap.of()
                )
            )
        );
    }

    private final List<String> pattern;
    private final Map<Character, TagKey<Item>> tagMap;
    private final Map<Character, ItemLike> itemMap;

    private MachineBlocksRecipeGenerator(
            final HolderLookup.Provider registries,
            final RecipeOutput output,
            final ItemLike itemProvider,
            final String pattern,
            final Map<Character, TagKey<Item>> tagMap,
            final Map<Character, ItemLike> itemMap) {
        super(registries, output, itemProvider);
        this.pattern = Arrays.asList(pattern.split(";"));
        this.tagMap = tagMap;
        this.itemMap = itemMap;
    }

    @Override
    protected void buildRecipes() {
        final ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(this.registries.lookupOrThrow(Registries.ITEM), RecipeCategory.TOOLS, getItemProvider());
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

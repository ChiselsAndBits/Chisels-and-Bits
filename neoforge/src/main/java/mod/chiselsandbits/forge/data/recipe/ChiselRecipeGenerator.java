package mod.chiselsandbits.forge.data.recipe;

import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.registrars.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = Constants.MOD_ID)
public class ChiselRecipeGenerator extends AbstractChiselRecipeGenerator
{

    private ChiselRecipeGenerator(
        final HolderLookup.Provider registries,
        final RecipeOutput output,
        final ItemLike itemProvider,
        final TagKey<Item> rodTag,
        final TagKey<Item> ingredientTag)
    {
        super(registries, output, itemProvider, rodTag, ingredientTag);
    }

    private static GeneratorFactory<ChiselRecipeGenerator> withWoodenRodFor(TagKey<Item> head) {
        return (registries, output, itemProvider) -> new ChiselRecipeGenerator(registries, output, itemProvider, Tags.Items.RODS_WOODEN, head);
    }

    private static GeneratorFactory<ChiselRecipeGenerator> withBlazeRodFor(TagKey<Item> head) {
        return (registries, output, itemProvider) -> new ChiselRecipeGenerator(registries, output, itemProvider, Tags.Items.RODS_BLAZE, head);
    }

    @SubscribeEvent
    public static void dataGeneratorSetup(final GatherDataEvent.Client event)
    {
        event.getGenerator().addProvider(
            true,
            new AbstractRecipeGenerator.Runner(
                event.getGenerator().getPackOutput(),
                event.getLookupProvider(),
                ModItems.ITEM_CHISEL_STONE.get(),
                withWoodenRodFor(Tags.Items.STONES)
            )
        );

        event.getGenerator().addProvider(
            true,
            new AbstractRecipeGenerator.Runner(
                event.getGenerator().getPackOutput(),
                event.getLookupProvider(),
                ModItems.ITEM_CHISEL_COPPER.get(),
                withWoodenRodFor(Tags.Items.INGOTS_COPPER)
            )
        );

        event.getGenerator().addProvider(
            true,
            new AbstractRecipeGenerator.Runner(
                event.getGenerator().getPackOutput(),
                event.getLookupProvider(),
                ModItems.ITEM_CHISEL_IRON.get(),
                withWoodenRodFor(Tags.Items.INGOTS_IRON)
            )
        );

        event.getGenerator().addProvider(
            true,
            new AbstractRecipeGenerator.Runner(
                event.getGenerator().getPackOutput(),
                event.getLookupProvider(),
                ModItems.ITEM_CHISEL_GOLD.get(),
                withWoodenRodFor(Tags.Items.INGOTS_GOLD)
            )
        );

        event.getGenerator().addProvider(
            true,
            new AbstractRecipeGenerator.Runner(
                event.getGenerator().getPackOutput(),
                event.getLookupProvider(),
                ModItems.ITEM_CHISEL_DIAMOND.get(),
                withWoodenRodFor(Tags.Items.GEMS_DIAMOND)
            )
        );

        event.getGenerator().addProvider(
            true,
            new AbstractRecipeGenerator.Runner(
                event.getGenerator().getPackOutput(),
                event.getLookupProvider(),
                ModItems.ITEM_CHISEL_NETHERITE.get(),
                withBlazeRodFor(Tags.Items.INGOTS_NETHERITE)
            )
        );
    }
}

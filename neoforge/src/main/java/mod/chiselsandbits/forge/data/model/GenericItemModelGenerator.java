package mod.chiselsandbits.forge.data.model;

import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.registrars.ModItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.core.Holder;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

@EventBusSubscriber(modid = Constants.MOD_ID)
public class GenericItemModelGenerator extends ModelProvider
{
    public GenericItemModelGenerator(final PackOutput output)
    {
        super(output, Constants.MOD_ID);
    }

    @SubscribeEvent
    public static void dataGeneratorSetup(final GatherDataEvent.Client event)
    {
        event.getGenerator().addProvider(true, new GenericItemModelGenerator(event.getGenerator().getPackOutput()));
    }

    @Override
    protected @NotNull Stream<? extends Holder<Block>> getKnownBlocks()
    {
        return Stream.of();
    }

    @Override
    protected @NotNull Stream<? extends Holder<Item>> getKnownItems()
    {
        return Stream.of(
            ModItems.ITEM_CHISEL_STONE.get(),
            ModItems.ITEM_CHISEL_IRON.get(),
            ModItems.ITEM_CHISEL_GOLD.get(),
            ModItems.ITEM_CHISEL_DIAMOND.get(),
            ModItems.ITEM_CHISEL_NETHERITE.get(),
            ModItems.MAGNIFYING_GLASS.get(),
            ModItems.ITEM_BIT_BAG_DEFAULT.get(),
            ModItems.ITEM_BIT_BAG_DYED.get(),
            ModItems.SINGLE_USE_PATTERN_ITEM.get(),
            ModItems.MULTI_USE_PATTERN_ITEM.get(),
            ModItems.WRENCH.get(),
            ModItems.MONOCLE_ITEM.get()
        ).map(Item::builtInRegistryHolder);
    }

    @Override
    public @NotNull String getName()
    {
        return "Generic item model provider";
    }

    @Override
    protected void registerModels(final @NotNull BlockModelGenerators blockModels, final @NotNull ItemModelGenerators itemModels)
    {
        getKnownItems().forEach(item -> actOnItem(item, itemModels));
    }

    public void actOnItem(Holder<Item> item, ItemModelGenerators generators) {
        generators.itemModelOutput.accept(
            item.value(),
            ItemModelUtils.plainModel(generators.createFlatItemModel(item.value(), "", ModelTemplates.FLAT_ITEM))
        );
    }
}

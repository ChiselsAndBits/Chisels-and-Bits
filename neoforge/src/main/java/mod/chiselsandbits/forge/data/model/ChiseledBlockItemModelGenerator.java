package mod.chiselsandbits.forge.data.model;

import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.client.model.item.BitBlockItemModel;
import mod.chiselsandbits.client.model.item.ChiseledBlockItemModel;
import mod.chiselsandbits.registrars.ModItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

@EventBusSubscriber(modid = Constants.MOD_ID)
public class ChiseledBlockItemModelGenerator extends ModelProvider implements DataProvider {
    @SubscribeEvent
    public static void dataGeneratorSetup(final GatherDataEvent.Client event) {
        event.getGenerator().addProvider(true, new ChiseledBlockItemModelGenerator(event.getGenerator()));
    }

    public ChiseledBlockItemModelGenerator(final DataGenerator generator) {
        super(generator.getPackOutput(), Constants.MOD_ID);
    }

    @Override
    protected void registerModels(final @NotNull BlockModelGenerators blockModels, final @NotNull ItemModelGenerators itemModels)
    {
        actOnBlockWithLoader(ModItems.CHISELED_BLOCK.get(), itemModels);
    }

    public void actOnBlockWithLoader(final Item item, final ItemModelGenerators itemModels) {
        itemModels.itemModelOutput.accept(item, new ChiseledBlockItemModel.Unbaked());
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
            BuiltInRegistries.ITEM.wrapAsHolder(
                ModItems.CHISELED_BLOCK.get()
            )
        );
    }

    @NotNull
    @Override
    public String getName() {
        return "Chiseled Block item model generator";
    }
}

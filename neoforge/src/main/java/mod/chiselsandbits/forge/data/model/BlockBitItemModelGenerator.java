package mod.chiselsandbits.forge.data.model;

import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.client.model.item.BitBlockItemModel;
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
public class BlockBitItemModelGenerator extends ModelProvider implements DataProvider {
    @SubscribeEvent
    public static void dataGeneratorSetup(final GatherDataEvent.Client event) {
        event.getGenerator().addProvider(true, new BlockBitItemModelGenerator(event.getGenerator()));
    }

    public BlockBitItemModelGenerator(final DataGenerator generator) {
        super(generator.getPackOutput(), Constants.MOD_ID);
    }

    @Override
    protected void registerModels(final BlockModelGenerators blockModels, final ItemModelGenerators itemModels)
    {
        actOnBlockWithLoader(ModItems.ITEM_BLOCK_BIT.get(), itemModels);
    }

    public void actOnBlockWithLoader(final Item item, final ItemModelGenerators itemModels) {
        itemModels.itemModelOutput.accept(item, new BitBlockItemModel.Unbaked());
    }

    @Override
    protected Stream<? extends Holder<Block>> getKnownBlocks()
    {
        return Stream.of();
    }

    @Override
    protected Stream<? extends Holder<Item>> getKnownItems()
    {
        return Stream.of(
            BuiltInRegistries.ITEM.wrapAsHolder(
                ModItems.ITEM_BLOCK_BIT.get()
            )
        );
    }

    @NotNull
    @Override
    public String getName() {
        return "Bit item model generator";
    }
}

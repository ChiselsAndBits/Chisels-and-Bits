package mod.chiselsandbits.forge.data.model;

import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.client.item.properties.IsMeasuringItemProperty;
import mod.chiselsandbits.registrars.ModItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.*;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.DataGenerator;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.stream.Stream;

@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public class MeasuringTapeModelGenerator extends ModelProvider
{
    private static final ModelTemplate FLAT_ITEM_IS_MEASURING = new ModelTemplate(
        ModelTemplates.FLAT_ITEM.model,
        Optional.of("_is_measuring"),
        ModelTemplates.FLAT_ITEM.requiredSlots.toArray(TextureSlot[]::new)
    );

    private MeasuringTapeModelGenerator(final DataGenerator generator)
    {
        super(generator.getPackOutput(), Constants.MOD_ID);
    }

    @SubscribeEvent
    public static void dataGeneratorSetup(final GatherDataEvent.Client event)
    {
        event.getGenerator().addProvider(true, new MeasuringTapeModelGenerator(event.getGenerator()));
    }

    @Override
    protected void registerModels(final @NotNull BlockModelGenerators blockModels, final @NotNull ItemModelGenerators itemModels)
    {
        final ItemModel.Unbaked model = ItemModelUtils.conditional(
            new IsMeasuringItemProperty(),
            ItemModelUtils.plainModel(itemModels.createFlatItemModel(ModItems.MEASURING_TAPE.get(), "_default", ModelTemplates.FLAT_ITEM)),
            ItemModelUtils.plainModel(itemModels.createFlatItemModel(ModItems.MEASURING_TAPE.get(), "_is_measuring", FLAT_ITEM_IS_MEASURING))
        );

        itemModels.itemModelOutput.accept(ModItems.MEASURING_TAPE.get(), model);
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
                ModItems.MEASURING_TAPE.get()
            )
        );
    }

    @Override
    public @NotNull String getName()
    {
        return "Measuring tape item model generator";
    }
}

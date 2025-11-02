package mod.chiselsandbits.forge.data.blockstate;

import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.registrars.ModBlocks;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.*;
import net.minecraft.client.renderer.item.BlockModelWrapper;
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

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@EventBusSubscriber(modid = Constants.MOD_ID)
public class PatternScannerBlockStateGenerator extends ModelProvider implements DataProvider
{
    @SubscribeEvent
    public static void dataGeneratorSetup(final GatherDataEvent.Client event)
    {
        event.getGenerator().addProvider(true, new PatternScannerBlockStateGenerator(event.getGenerator()));
    }


    public PatternScannerBlockStateGenerator(final DataGenerator gen)
    {
        super(gen.getPackOutput(), Constants.MOD_ID);
    }

    @Override
    protected void registerModels(final @NotNull BlockModelGenerators blockModels, final @NotNull ItemModelGenerators itemModels)
    {
        actOnBlock(ModBlocks.PATTERN_SCANNER.get(), blockModels);
    }

    public void actOnBlock(final Block block, final BlockModelGenerators generators)
    {
        generators.createHorizontallyRotatedBlock(block, new TexturedModel.Provider() {
            @Override
            public @NotNull TexturedModel get(final @NotNull Block block)
            {
                return new TexturedModel(
                    new TextureMapping()
                        .put(TextureSlot.LAYER0, ModelLocationUtils.getModelLocation(block))
                        .put(TextureSlot.PARTICLE, ModelLocationUtils.getModelLocation(block)),
                    new ModelTemplate(
                        Optional.of(ModelLocationUtils.getModelLocation(block).withSuffix("_spec")),
                        Optional.empty(),
                        TextureSlot.LAYER0,
                        TextureSlot.PARTICLE
                    )
                );
            }
        });

        generators.itemModelOutput.accept(block.asItem(), new BlockModelWrapper.Unbaked(
            ModelLocationUtils.getModelLocation(block),
            List.of()
        ));
    }

    @Override
    protected @NotNull Stream<? extends Holder<Block>> getKnownBlocks()
    {
        return Stream.of(
            BuiltInRegistries.BLOCK.wrapAsHolder(
                ModBlocks.PATTERN_SCANNER.get()
            )
        );
    }

    @Override
    protected @NotNull Stream<? extends Holder<Item>> getKnownItems()
    {
        return Stream.of();
    }

    @Override
    public @NotNull String getName()
    {
        return "Pattern scanner blockstate generator";
    }
}

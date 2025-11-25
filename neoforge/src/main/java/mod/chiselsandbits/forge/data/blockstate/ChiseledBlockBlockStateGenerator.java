package mod.chiselsandbits.forge.data.blockstate;

import com.communi.suggestu.scena.core.client.models.IModelManager;
import com.communi.suggestu.scena.forge.platform.client.model.unbaked.UnbakedCustomModelWrapper;
import com.mojang.datafixers.util.Function3;
import com.mojang.serialization.MapCodec;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.block.ChiseledBlock;
import mod.chiselsandbits.client.model.block.ChiseledBlockStateModel;
import mod.chiselsandbits.registrars.ModBlocks;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.BlockModelDefinitionGenerator;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.renderer.block.model.BlockModelDefinition;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.VariantMutator;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.model.block.CustomBlockModelDefinition;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import net.neoforged.neoforge.client.model.generators.blockstate.CustomBlockStateModelBuilder;
import net.neoforged.neoforge.client.model.generators.blockstate.UnbakedMutator;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public class ChiseledBlockBlockStateGenerator extends ModelProvider implements DataProvider {

    public ChiseledBlockBlockStateGenerator(final DataGenerator gen) {
        super(gen.getPackOutput(), Constants.MOD_ID);
    }

    @SubscribeEvent
    public static void dataGeneratorSetup(final GatherDataEvent.Client event) {
        event.getGenerator().addProvider(true, new ChiseledBlockBlockStateGenerator(event.getGenerator()));
    }

    @Override
    protected @NotNull Stream<? extends Holder<Block>> getKnownBlocks()
    {
        return Stream.of(
            BuiltInRegistries.BLOCK.wrapAsHolder(
                ModBlocks.CHISELED_BLOCK.get()
            )
        );
    }

    @Override
    protected @NotNull Stream<? extends Holder<Item>> getKnownItems()
    {
        return Stream.of();
    }

    @Override
    protected void registerModels(final @NotNull BlockModelGenerators blockModels, final @NotNull ItemModelGenerators itemModels)
    {
        actOnBlock(ModBlocks.CHISELED_BLOCK.get(), blockModels);
    }

    @NotNull
    @Override
    public String getName() {
        return "Chiseled block blockstate generator";
    }

    public void actOnBlock(final Block block, BlockModelGenerators generators) {
        generators.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(
            block,
            MultiVariant.of(new CustomBlockStateModelBuilder() {
                @Override
                public @NotNull CustomBlockStateModelBuilder with(final @NotNull VariantMutator variantMutator)
                {
                    return this;
                }

                @Override
                public @NotNull CustomBlockStateModelBuilder with(final @NotNull UnbakedMutator variantMutator)
                {
                    return this;
                }

                @Override
                public @NotNull CustomUnbakedBlockStateModel toUnbaked()
                {
                    //noinspection rawtypes,unchecked
                    return new UnbakedCustomModelWrapper(
                        ChiseledBlockStateModel.Unbaked.CODEC
                    );
                }
            })
        ));
    }
}

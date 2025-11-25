package mod.chiselsandbits.forge.data.blockstate;

import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.registrars.ModBlocks;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public class ReflectionHelperBlockStateGenerator extends ModelProvider implements DataProvider
{
    public ReflectionHelperBlockStateGenerator(final DataGenerator gen)
    {
        super(gen.getPackOutput(), Constants.MOD_ID);
    }

    @SubscribeEvent
    public static void dataGeneratorSetup(final GatherDataEvent.Client event)
    {
        event.getGenerator().addProvider(true, new ReflectionHelperBlockStateGenerator(event.getGenerator()));
    }

    @Override
    protected void registerModels(final @NotNull BlockModelGenerators blockModels, final @NotNull ItemModelGenerators itemModels)
    {
        actOnBlock(ModBlocks.REFLECTION_HELPER_BLOCK.get(), blockModels);
    }

    public void actOnBlock(final Block block, final BlockModelGenerators blockModels)
    {
        blockModels.createNonTemplateModelBlock(block, Blocks.AIR);
    }

    @Override
    protected @NotNull Stream<? extends Holder<Block>> getKnownBlocks()
    {
        return Stream.of(
            BuiltInRegistries.BLOCK.wrapAsHolder(
                ModBlocks.REFLECTION_HELPER_BLOCK.get()
            )
        );
    }

    @Override
    protected @NotNull Stream<? extends Holder<Item>> getKnownItems()
    {
        return Stream.of();
    }

    @NotNull
    @Override
    public String getName()
    {
        return "Reflection helper block blockstate generator";
    }
}

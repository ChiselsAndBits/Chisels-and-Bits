package mod.chiselsandbits.forge.data.blockstate;

import java.util.stream.Stream;

import com.communi.suggestu.scena.forge.platform.client.model.unbaked.UnbakedCustomModelWrapper;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.client.model.block.ChiseledBlockStateModel;
import mod.chiselsandbits.registrars.ModBlocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import net.neoforged.neoforge.client.model.generators.blockstate.CustomBlockStateModelBuilder;
import net.neoforged.neoforge.client.model.generators.blockstate.UnbakedMutator;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

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
	protected Stream<? extends Holder<Block>> getKnownBlocks() {
		return Stream.of(
				BuiltInRegistries.BLOCK.wrapAsHolder(
						ModBlocks.CHISELED_BLOCK.get()
				)
		);
	}

	@Override
	protected Stream<? extends Holder<Item>> getKnownItems() {
		return Stream.of();
	}

	@Override
	protected void registerModels(final BlockModelGenerators blockModels, final ItemModelGenerators itemModels) {
		actOnBlock(ModBlocks.CHISELED_BLOCK.get(), blockModels);
	}

	@Override
	public String getName() {
		return "Chiseled block blockstate generator";
	}

	public void actOnBlock(final Block block, BlockModelGenerators generators) {
		generators.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(
						block,
						MultiVariant.of(new CustomBlockStateModelBuilder() {
							@Override
							public CustomBlockStateModelBuilder with(final VariantMutator variantMutator) {
								return this;
							}

							@Override
							public CustomBlockStateModelBuilder with(final UnbakedMutator variantMutator) {
								return this;
							}

							@Override
							public CustomUnbakedBlockStateModel toUnbaked() {
							//noinspection rawtypes, unchecked
								return new UnbakedCustomModelWrapper(
										ChiseledBlockStateModel.Unbaked.CODEC
								);
							}
						})
				));
	}
}

package mod.chiselsandbits.forge.data.blockstate;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.registrars.ModBlocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public class BitStorageBlockStateGenerator extends ModelProvider implements DataProvider {
	@SubscribeEvent
	public static void dataGeneratorSetup(final GatherDataEvent.Client event) {
		event.getGenerator().addProvider(true, new BitStorageBlockStateGenerator(event.getGenerator()));
	}

	public BitStorageBlockStateGenerator(final DataGenerator gen) {
		super(gen.getPackOutput(), Constants.MOD_ID);
	}

	@Override
	protected void registerModels(final BlockModelGenerators blockModels, final ItemModelGenerators itemModels) {
		actOnBlock(ModBlocks.BIT_STORAGE.get(), blockModels);
	}

	public void actOnBlock(final Block block, final BlockModelGenerators generators) {
		generators.createHorizontallyRotatedBlock(block, new TexturedModel.Provider() {
			@Override
			public TexturedModel get(final Block block) {
				return new TexturedModel(
					new TextureMapping()
						.put(TextureSlot.LAYER0, new Material(ModelLocationUtils.getModelLocation(block)))
						.put(TextureSlot.PARTICLE, new Material(ModelLocationUtils.getModelLocation(block))),
					new ModelTemplate(
							Optional.of(ModelLocationUtils.getModelLocation(block).withSuffix("_spec")),
							Optional.empty(),
							TextureSlot.LAYER0,
							TextureSlot.PARTICLE
					)
				);
			}
		});

		generators.itemModelOutput.accept(block.asItem(), new CuboidItemModelWrapper.Unbaked(
				ModelLocationUtils.getModelLocation(block),
				Optional.empty(),
				List.of()
		));
	}

	@Override
	protected Stream<? extends Holder<Block>> getKnownBlocks() {
		return Stream.of(
				BuiltInRegistries.BLOCK.wrapAsHolder(
						ModBlocks.BIT_STORAGE.get()
				)
		);
	}

	@Override
	protected Stream<? extends Holder<Item>> getKnownItems() {
		return Stream.of();
	}

	@Override
	public String getName() {
		return "Bit storage blockstate generator";
	}
}

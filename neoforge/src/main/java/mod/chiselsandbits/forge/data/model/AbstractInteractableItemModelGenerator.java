package mod.chiselsandbits.forge.data.model;

import com.communi.suggestu.scena.core.registries.deferred.IRegistryObject;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.client.model.item.InteractableItemModel;
import mod.chiselsandbits.registrars.ModItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Stream;

public abstract class AbstractInteractableItemModelGenerator extends ModelProvider
{
    private final IRegistryObject<? extends Item> targetRegistryObject;

    protected AbstractInteractableItemModelGenerator(
            final DataGenerator generator,
            final IRegistryObject<? extends Item> targetRegistryObject) {
        super(generator.getPackOutput(), Constants.MOD_ID);
        this.targetRegistryObject = targetRegistryObject;
    }

    @Override
    protected void registerModels(final @NotNull BlockModelGenerators blockModels, final ItemModelGenerators itemModels)
    {
        itemModels.itemModelOutput.accept(
            this.targetRegistryObject.get(),
            new InteractableItemModel.Unbaked(
                createSpecModel(itemModels)
            )
        );
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
                targetRegistryObject.get()
            )
        );
    }

    protected BlockModelWrapper.Unbaked createSpecModel(final ItemModelGenerators itemModelGenerators) {
        return new BlockModelWrapper.Unbaked(ModelTemplates.FLAT_ITEM
            .create(
                ModelLocationUtils.getModelLocation(this.targetRegistryObject.get()).withSuffix("_spec"),
                TextureMapping.layer0(this.targetRegistryObject.get()),
                itemModelGenerators.modelOutput
            ), List.of());
    }
}

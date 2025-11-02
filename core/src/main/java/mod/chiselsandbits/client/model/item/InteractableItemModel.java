package mod.chiselsandbits.client.model.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mod.chiselsandbits.client.ister.InteractionISTER;
import mod.scena.client.utils.ItemModelUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An {@link ItemModel} which indicates interaction with it.
 */
public record InteractableItemModel(ResourceLocation modelLocation) implements ItemModel
{

    /**
     * {@return The inner {@link ItemModel} which represents the static content of the model}
     */
    public ItemModel model() {
        return Minecraft.getInstance().getModelManager().getItemModel(modelLocation());
    }

    @Override
    public void update(
        final ItemStackRenderState renderState,
        final @NotNull ItemStack stack,
        final @NotNull ItemModelResolver itemModelResolver,
        final @NotNull ItemDisplayContext displayContext,
        @Nullable final ClientLevel level,
        @Nullable final ItemOwner owner,
        final int seed)
    {
        final var renderer = new InteractionISTER();
        final var layer = renderState.newLayer();
        layer.setupSpecialModel(
            renderer,
            renderer.extractArgument(stack)
        );
        layer.setExtents(ItemModelUtils.redirectExtendsTo(
            stack, model(), displayContext, level, owner, seed
        ));
    }

    public record Unbaked(ResourceLocation model) implements ItemModel.Unbaked {

        public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("model").forGetter(Unbaked::model)
            ).apply(instance, Unbaked::new)
        );

        @Override
        public @NotNull MapCodec<? extends ItemModel.Unbaked> type()
        {
            return CODEC;
        }

        @Override
        public @NotNull ItemModel bake(final @NotNull BakingContext context)
        {
            return new InteractableItemModel(model());
        }

        @Override
        public void resolveDependencies(final Resolver resolver)
        {
            resolver.markDependency(model);
        }
    }
}

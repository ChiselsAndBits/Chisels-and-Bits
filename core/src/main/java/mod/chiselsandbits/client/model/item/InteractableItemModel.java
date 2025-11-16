package mod.chiselsandbits.client.model.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mod.chiselsandbits.client.ister.InteractionISTER;
import mod.scena.client.utils.ItemModelUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.*;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An {@link ItemModel} which indicates interaction with it.
 */
public record InteractableItemModel(ItemModel model) implements ItemModel
{

    @Override
    public void update(
        final @NotNull ItemStackRenderState renderState,
        final @NotNull ItemStack stack,
        final @NotNull ItemModelResolver itemModelResolver,
        final @NotNull ItemDisplayContext displayContext,
        @Nullable final ClientLevel level,
        @Nullable final ItemOwner owner,
        final int seed)
    {
        final var renderer = new InteractionISTER(this);
        final var specialRenderState = renderer.extractArgument(stack);
        if (specialRenderState == null)
            return;

        renderState.appendModelIdentityElement(this);
        renderState.appendModelIdentityElement(stack.getItem());
        renderState.appendModelIdentityElement(specialRenderState.item().isInteracting(stack));
        renderState.appendModelIdentityElement(specialRenderState.item().getInteractionTarget(stack));

        final var layer = renderState.newLayer();
        layer.setupSpecialModel(
            renderer,
            specialRenderState
        );
        layer.setExtents(ItemModelUtils.redirectExtendsTo(
            stack, model(), displayContext, level, owner, seed
        ));
    }

    public record Unbaked(BlockModelWrapper.Unbaked model) implements ItemModel.Unbaked {

        public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                BlockModelWrapper.Unbaked.MAP_CODEC.fieldOf("model").forGetter(Unbaked::model)
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
            final ItemModel wrapper = model().bake(context);
            return new InteractableItemModel(wrapper);
        }

        @Override
        public void resolveDependencies(final @NotNull Resolver resolver)
        {
            model().resolveDependencies(resolver);
        }
    }
}

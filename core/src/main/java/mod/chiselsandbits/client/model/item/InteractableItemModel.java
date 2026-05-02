package mod.chiselsandbits.client.model.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mod.chiselsandbits.client.ister.InteractionISTER;
import mod.chiselsandbits.client.util.ItemModelUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.*;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;
import org.jspecify.annotations.NonNull;

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
        final var renderer = new InteractionISTER(this, displayContext);
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

    public record Unbaked(CuboidItemModelWrapper.Unbaked model) implements ItemModel.Unbaked {

        public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                CuboidItemModelWrapper.Unbaked.MAP_CODEC.fieldOf("model").forGetter(Unbaked::model)
            ).apply(instance, Unbaked::new)
        );

        @Override
        public @NotNull MapCodec<? extends ItemModel.Unbaked> type()
        {
            return CODEC;
        }

        @Override
        public @NonNull ItemModel bake(final @NonNull BakingContext context, final @NonNull Matrix4fc transformation)
        {
            final ItemModel wrapper = model().bake(context, transformation);
            return new InteractableItemModel(wrapper);
        }

        @Override
        public void resolveDependencies(final @NotNull Resolver resolver)
        {
            model().resolveDependencies(resolver);
        }
    }
}

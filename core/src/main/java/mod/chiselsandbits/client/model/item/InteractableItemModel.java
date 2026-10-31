package mod.chiselsandbits.client.model.item;

// ILLEGAL IMPORT: -> import org.jetbrains.annotations.Nullable; -> We use JSpecify nullable annotations in this project.
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mod.chiselsandbits.client.ister.InteractionISTER;
import mod.chiselsandbits.client.util.ItemModelUtils;
import org.joml.Matrix4fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * An {@link ItemModel} which indicates interaction with it.
 */
public record InteractableItemModel(ItemModel model) implements ItemModel {
	@Override
	public void update(
			final ItemStackRenderState renderState,
			final ItemStack stack,
			final ItemModelResolver itemModelResolver,
			final ItemDisplayContext displayContext,
			@Nullable final ClientLevel level,
			@Nullable final ItemOwner owner,
			final int seed) {
		final InteractionISTER renderer = new InteractionISTER(this, displayContext);
		final InteractionISTER.RenderState specialRenderState = renderer.extractArgument(stack);

		if (specialRenderState == null) {
			return;
		}

		renderState.appendModelIdentityElement(this);
		renderState.appendModelIdentityElement(stack.getItem());
		renderState.appendModelIdentityElement(specialRenderState.item().isInteracting(stack));
		renderState.appendModelIdentityElement(specialRenderState.item().getInteractionTarget(stack));

		final ItemStackRenderState.LayerRenderState layer = renderState.newLayer();
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
		public MapCodec<? extends ItemModel.Unbaked> type() {
			return CODEC;
		}

		@Override
		public @NonNull ItemModel bake(final @NonNull BakingContext context, final @NonNull Matrix4fc transformation) {
			final ItemModel wrapper = model().bake(context, transformation);
			return new InteractableItemModel(wrapper);
		}

		@Override
		public void resolveDependencies(final Resolver resolver) {
			model().resolveDependencies(resolver);
		}
	}
}

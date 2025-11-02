package mod.chiselsandbits.client.model.item;

import com.mojang.serialization.MapCodec;
import mod.chiselsandbits.api.item.bit.IBitItem;
import mod.chiselsandbits.client.model.baked.bit.BitBlockBakedModelManager;
import mod.chiselsandbits.client.model.builder.BitBlockQuadCollectionBuilder;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record BitBlockItemModel(boolean usesBlockLight, ItemTransforms transforms) implements ItemModel
{
    @Override
    public void update(
        final ItemStackRenderState renderState,
        final ItemStack stack,
        final @NotNull ItemModelResolver itemModelResolver,
        final @NotNull ItemDisplayContext displayContext,
        @Nullable final ClientLevel level,
        @Nullable final ItemOwner owner,
        final int seed)
    {
        renderState.appendModelIdentityElement(this);
        if (!(stack.getItem() instanceof IBitItem bitItem))
        {
            return;
        }

        renderState.appendModelIdentityElement(bitItem.getBlockInformation(stack));

        BitBlockBakedModelManager.getInstance().get(
            stack,
            level
        ).forEach((renderType, quads) -> {
            ItemStackRenderState.LayerRenderState itemstackrenderstate$layerrenderstate = renderState.newLayer();

            itemstackrenderstate$layerrenderstate.setUsesBlockLight(this.usesBlockLight());
            itemstackrenderstate$layerrenderstate.setTransform(this.transforms().getTransform(displayContext));

            if (stack.hasFoil()) {
                itemstackrenderstate$layerrenderstate.setFoilType(ItemStackRenderState.FoilType.STANDARD);
                renderState.setAnimated();
                renderState.appendModelIdentityElement(ItemStackRenderState.FoilType.STANDARD);
            }

            itemstackrenderstate$layerrenderstate.setExtents(() -> BitBlockQuadCollectionBuilder.EXTENDS);

            itemstackrenderstate$layerrenderstate.setRenderType(renderType);
            itemstackrenderstate$layerrenderstate.prepareQuadList().addAll(quads.getAll());
        });
    }

    public record Unbaked() implements ItemModel.Unbaked {

        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(Unbaked::new);

        @Override
        public @NotNull MapCodec<? extends ItemModel.Unbaked> type()
        {
            return CODEC;
        }

        @Override
        public @NotNull ItemModel bake(final @NotNull BakingContext context)
        {
            final ResolvedModel model = context.blockModelBaker().getModel(ModelLocationUtils.decorateBlockModelLocation("block"));
            return new BitBlockItemModel(model.getTopGuiLight().lightLikeBlock(), model.getTopTransforms());
        }

        @Override
        public void resolveDependencies(final Resolver resolver)
        {
            resolver.markDependency(ModelLocationUtils.decorateBlockModelLocation("block"));
        }
    }
}

package mod.chiselsandbits.client.model.item;

import com.mojang.serialization.MapCodec;
import mod.chiselsandbits.api.item.bit.IBitItem;
import mod.chiselsandbits.client.model.builder.BitBlockQuadCollectionBuilder;
import mod.chiselsandbits.client.model.information.BitBlockModelInformation;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record BitBlockItemModel(
    boolean usesBlockLight,
    ItemTransforms transforms) implements ItemModel
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

        final BitBlockModelInformation information = BitBlockBakedModelManager.getInstance().get(
            stack,
            level
        );

        renderState.appendModelIdentityElement(information.isLarge());

        information.parts().forEach((part) -> {
            ItemStackRenderState.LayerRenderState itemstackrenderstate$layerrenderstate = renderState.newLayer();

            if (information.isBlock())
            {
                itemstackrenderstate$layerrenderstate.setUsesBlockLight(this.usesBlockLight());
                itemstackrenderstate$layerrenderstate.setTransform(this.transforms().getTransform(displayContext));
            }

            if (stack.hasFoil())
            {
                itemstackrenderstate$layerrenderstate.setFoilType(ItemStackRenderState.FoilType.STANDARD);
                renderState.setAnimated();
                renderState.appendModelIdentityElement(ItemStackRenderState.FoilType.STANDARD);
            }

            if (part.hasTints()) {
                final int[] targetTints = itemstackrenderstate$layerrenderstate.prepareTintLayers(part.tints().length);

                System.arraycopy(
                    part.tints(),
                    0,
                    targetTints,
                    0,
                    part.tints().length
                );

                renderState.appendModelIdentityElement(targetTints);
            }

            itemstackrenderstate$layerrenderstate.setExtents(() -> BitBlockQuadCollectionBuilder.EXTENDS);

            itemstackrenderstate$layerrenderstate.setRenderType(part.renderType());
            itemstackrenderstate$layerrenderstate.prepareQuadList().addAll(part.quads());
        });
    }

    public record Unbaked() implements ItemModel.Unbaked
    {

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

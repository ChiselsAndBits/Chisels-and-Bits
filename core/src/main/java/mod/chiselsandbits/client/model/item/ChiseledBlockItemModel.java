package mod.chiselsandbits.client.model.item;

import com.communi.suggestu.scena.core.client.utils.RenderTypeUtils;
import com.mojang.serialization.MapCodec;
import mod.chiselsandbits.client.model.block.ChiseledBlockStateModelManager;
import mod.chiselsandbits.client.model.information.ChiseledBlockModelInformation;
import mod.chiselsandbits.client.model.parts.ChiseledBlockModelPart;
import mod.scena.client.utils.ItemModelUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record ChiseledBlockItemModel(
    boolean usesBlockLight,
    ItemTransforms transforms
) implements ItemModel
{
    @Override
    public void update(
        final ItemStackRenderState renderState,
        final ItemStack stack,
        final ItemModelResolver itemModelResolver,
        final ItemDisplayContext displayContext,
        @Nullable final ClientLevel level,
        @Nullable final ItemOwner owner,
        final int seed)
    {
        renderState.appendModelIdentityElement(this);
        final ChiseledBlockModelInformation blockModelInformation = ChiseledBlockStateModelManager
            .getInstance().get(stack);

        for (final ChiseledBlockModelPart part : blockModelInformation.parts())
        {
            ItemStackRenderState.LayerRenderState itemstackrenderstate$layerrenderstate = renderState.newLayer();
            if (stack.hasFoil()) {
                ItemStackRenderState.FoilType itemstackrenderstate$foiltype = ItemStackRenderState.FoilType.STANDARD;
                itemstackrenderstate$layerrenderstate.setFoilType(itemstackrenderstate$foiltype);
                renderState.setAnimated();
                renderState.appendModelIdentityElement(itemstackrenderstate$foiltype);
            }


            final ItemStack lookupStack = new ItemStack(part.appearance().getBlock());
            final ItemModel model = Minecraft.getInstance().getModelManager().getItemModel(
                Objects.requireNonNull(lookupStack.get(DataComponents.ITEM_MODEL))
            );

            if (model instanceof BlockModelWrapper wrapper) {
                final List<ItemTintSource> tints = wrapper.tints;

                int k = tints.size();
                int[] tintLayers = itemstackrenderstate$layerrenderstate.prepareTintLayers(k);

                for (int i = 0; i < k; i++) {
                    try {
                        int j = tints.get(i).calculate(lookupStack, level, owner == null ? null : owner.asLivingEntity());
                        tintLayers[i] = j;
                        renderState.appendModelIdentityElement(j);
                    } catch (Exception ex) {
                        tintLayers[i] = -1;
                        renderState.appendModelIdentityElement(-1);
                    }

                }
            }

            itemstackrenderstate$layerrenderstate.setExtents(part.extendsCalculator());
            itemstackrenderstate$layerrenderstate.setRenderType(Objects.requireNonNull(RenderTypeUtils.renderTypeFor(part.renderType())));
            itemstackrenderstate$layerrenderstate.setUsesBlockLight(this.usesBlockLight());
            itemstackrenderstate$layerrenderstate.setTransform(this.transforms().getTransform(displayContext));

            itemstackrenderstate$layerrenderstate.prepareQuadList().addAll(part.quads().getAll());
        }
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
            return new ChiseledBlockItemModel(model.getTopGuiLight().lightLikeBlock(), model.getTopTransforms());
        }

        @Override
        public void resolveDependencies(final Resolver resolver)
        {
            resolver.markDependency(ModelLocationUtils.decorateBlockModelLocation("block"));
        }
    }
}

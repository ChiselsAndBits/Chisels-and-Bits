package mod.chiselsandbits.client.model.item;

import com.communi.suggestu.scena.core.client.utils.RenderTypeUtils;
import com.communi.suggestu.scena.core.util.SingleBlockBlockAndTintGetter;
import com.mojang.serialization.MapCodec;
import mod.chiselsandbits.client.model.block.ChiseledBlockStateModelManager;
import mod.chiselsandbits.client.model.information.ChiseledBlockModelInformation;
import mod.chiselsandbits.client.model.parts.ChiseledBlockModelPart;
import mod.chiselsandbits.client.util.BakedQuadUtils;
import mod.chiselsandbits.client.util.ItemModelUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        final @NotNull ItemStack stack,
        final @NotNull ItemModelResolver itemModelResolver,
        final @NotNull ItemDisplayContext displayContext,
        @Nullable final ClientLevel level,
        @Nullable final ItemOwner owner,
        final int seed)
    {
        renderState.appendModelIdentityElement(this);
        final ChiseledBlockModelInformation blockModelInformation = ChiseledBlockStateModelManager
            .getInstance().get(stack);

        renderState.appendModelIdentityElement(blockModelInformation.key());

        for (ChiseledBlockModelPart part : blockModelInformation.parts())
        {
            ItemStackRenderState.LayerRenderState itemstackrenderstate$layerrenderstate = renderState.newLayer();
            if (stack.hasFoil())
            {
                ItemStackRenderState.FoilType itemstackrenderstate$foiltype = ItemStackRenderState.FoilType.STANDARD;
                itemstackrenderstate$layerrenderstate.setFoilType(itemstackrenderstate$foiltype);
                renderState.setAnimated();
                renderState.appendModelIdentityElement(itemstackrenderstate$foiltype);
            }

            final ItemStack lookupStack = new ItemStack(part.appearance().getBlock());
            final Identifier itemModel = lookupStack.get(DataComponents.ITEM_MODEL);
            QuadCollection quads = part.quads();
            if (itemModel != null)
            {
                final ItemModel model = Minecraft.getInstance().getModelManager().getItemModel(
                    itemModel
                );

                if (model instanceof BlockModelWrapper wrapper)
                {
                    final List<ItemTintSource> tints = wrapper.tints;

                    int k = tints.size();
                    int[] tintLayers = itemstackrenderstate$layerrenderstate.prepareTintLayers(k);

                    for (int i = 0; i < k; i++)
                    {
                        try
                        {
                            int j = tints.get(i).calculate(lookupStack, level, owner == null ? null : owner.asLivingEntity());
                            tintLayers[i] = j;
                            renderState.appendModelIdentityElement(j);
                        }
                        catch (Exception ex)
                        {
                            tintLayers[i] = -1;
                            renderState.appendModelIdentityElement(-1);
                        }
                    }
                }
            }
            else
                //Fluids do not have block models we can extract tints from, so we use this work around to generate them regardless.
                //But we need a level for that.
                //This also assumes that there is only a single color per fluid, which is currently the only one possible.
                if (!part.appearance().getFluidState().isEmpty() && level != null)
                {
                    //Update the quad to a 0 tint index.
                    quads = ItemModelUtils.adapt(
                        quads,
                        quad ->
                            BakedQuadUtils.withTintIndex(quad, 0)
                    );

                    final SingleBlockBlockAndTintGetter blockAndTintGetter = new SingleBlockBlockAndTintGetter.Builder()
                        .withBlockState(part.source().blockState())
                        .withBlockEntity(part.source()::newBlockEntityAtZero)
                        .withPos(BlockPos.ZERO)
                        .withSource(level)
                        .createSingleBlockBlockAndTintGetter();

                    int[] tintLayers = itemstackrenderstate$layerrenderstate.prepareTintLayers(1);
                    tintLayers[0] =
                        ARGB.color(255,
                            Minecraft.getInstance().getBlockColors().getColor(
                                part.source().blockState(),
                                blockAndTintGetter,
                                BlockPos.ZERO,
                                0
                            ));
                }

            itemstackrenderstate$layerrenderstate.setExtents(part.extendsCalculator());
            itemstackrenderstate$layerrenderstate.setRenderType(Objects.requireNonNull(RenderTypeUtils.renderTypeFor(part.renderType())));
            itemstackrenderstate$layerrenderstate.setUsesBlockLight(this.usesBlockLight());
            itemstackrenderstate$layerrenderstate.setTransform(this.transforms().getTransform(displayContext));

            itemstackrenderstate$layerrenderstate.prepareQuadList().addAll(quads.getAll());
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

package mod.chiselsandbits.client.model.item;

import com.communi.suggestu.scena.core.util.SingleBlockBlockAndTintGetter;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import mod.chiselsandbits.client.model.block.ChiseledBlockStateModelManager;
import mod.chiselsandbits.client.model.builder.ChiseledBlockModelMaterial;
import mod.chiselsandbits.client.model.information.ChiseledBlockModelInformation;
import mod.chiselsandbits.client.model.parts.ChiseledBlockModelPart;
import mod.chiselsandbits.client.util.BakedQuadUtils;
import mod.chiselsandbits.client.util.ItemModelUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;
import org.jspecify.annotations.NonNull;

import java.util.List;

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


        IntList tintList = new IntArrayList();

        buildTintList(level, owner, tintList, blockModelInformation);

        renderState.appendModelIdentityElement(tintList);

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

            itemstackrenderstate$layerrenderstate.tintLayers().addAll(tintList);
            itemstackrenderstate$layerrenderstate.setExtents(part.extendsCalculator());
            itemstackrenderstate$layerrenderstate.setUsesBlockLight(this.usesBlockLight());
            itemstackrenderstate$layerrenderstate.setItemTransform(this.transforms().getTransform(displayContext));
            itemstackrenderstate$layerrenderstate.prepareQuadList().addAll(part.quads().getAll());
        }
    }

    private static void buildTintList(
        final @org.jspecify.annotations.Nullable ClientLevel level,
        final @org.jspecify.annotations.Nullable ItemOwner owner,
        final IntList tintList,
        final ChiseledBlockModelInformation blockModelInformation)
    {
        tintList.size(blockModelInformation.materials().size());

        List<ChiseledBlockModelMaterial> materials = blockModelInformation.materials();
        for (int i = 0; i < materials.size(); i++)
        {
            final ChiseledBlockModelMaterial material = materials.get(i);
            final ItemStack lookupStack = new ItemStack(material.blockInformation().blockState().getBlock());
            final Identifier itemModelId = lookupStack.get(DataComponents.ITEM_MODEL);

            if (itemModelId == null)
            {
                if (!material.blockInformation().blockState().getFluidState().isEmpty() && level != null)
                {
                    var fluidModel = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(
                        material.blockInformation().blockState().getFluidState()
                    );

                    if (fluidModel.tintSource() == null)
                    {
                        tintList.set(material.tintIndex(), ARGB.color(255, 255, 255, 255));
                        continue;
                    }

                    final SingleBlockBlockAndTintGetter blockAndTintGetter = new SingleBlockBlockAndTintGetter.Builder()
                        .withBlockState(material.blockInformation().blockState())
                        .withBlockEntity(material.blockInformation()::newBlockEntityAtZero)
                        .withPos(BlockPos.ZERO)
                        .withSource((BlockAndTintGetter) level)
                        .createSingleBlockBlockAndTintGetter();

                    var tint = fluidModel.tintSource().colorInWorld(
                        material.blockInformation().blockState(),
                        blockAndTintGetter,
                        BlockPos.ZERO
                    );

                    tintList.set(i, ARGB.color(255, tint));
                    continue;
                }

                tintList.set(i, ARGB.color(255, 255, 255, 255));
                continue;
            }

            final ItemModel itemModel = Minecraft.getInstance().getModelManager().getItemModel(
                itemModelId
            );

            if (itemModel instanceof CuboidItemModelWrapper wrapper)
            {
                final List<ItemTintSource> tintSources = wrapper.tints;
                if (material.tintIndex() >= tintSources.size() || material.tintIndex() < 0)
                {
                    tintList.set(i, ARGB.color(255, 255, 255, 255));
                    continue;
                }

                final ItemTintSource source = tintSources.get(material.tintIndex());

                tintList.set(i, source.calculate(
                    lookupStack,
                    level,
                    owner == null ? null : owner.asLivingEntity()
                ));
                continue;
            }

            tintList.set(i, ARGB.color(255, 255, 255, 255));
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
        public @NonNull ItemModel bake(final BakingContext context, final @NonNull Matrix4fc transformation)
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

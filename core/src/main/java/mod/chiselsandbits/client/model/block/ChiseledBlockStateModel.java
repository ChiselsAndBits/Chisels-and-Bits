package mod.chiselsandbits.client.model.block;

import com.communi.suggestu.scena.core.client.rendering.DataAwareBlockStateModel;
import com.mojang.serialization.MapCodec;
import mod.chiselsandbits.block.entities.ChiseledBlockEntity;
import mod.chiselsandbits.client.model.information.ChiseledBlockModelInformation;
import mod.chiselsandbits.client.model.parts.ChiseledBlockModelPart;
import mod.chiselsandbits.registrars.ModModelProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.core.BlockPos;
import net.minecraft.data.AtlasIds;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ChiseledBlockStateModel implements BlockStateModel, DataAwareBlockStateModel
{
    @Override
    public void collectParts(final @NotNull RandomSource random, final @NotNull List<BlockModelPart> output)
    {
        //Noop
    }

    @Override
    public @NotNull TextureAtlasSprite particleIcon()
    {
        return Minecraft.getInstance().getAtlasManager()
            .get(new Material(AtlasIds.BLOCKS, MissingTextureAtlasSprite.getLocation()));
    }

    protected @Nullable ChiseledBlockModelInformation getInformation(final BlockAndTintGetter blockAndTintGetter, final BlockPos blockPos)
    {
        final BlockEntity blockEntity = blockAndTintGetter.getBlockEntity(blockPos);
        if (!(blockEntity instanceof ChiseledBlockEntity chiseledBlockEntity))
            return null;

        return chiseledBlockEntity.getBlockModelData().getData(ModModelProperties.MODEL);
    }

    @Override
    public @Nullable Object createGeometryKey(
        final BlockAndTintGetter blockAndTintGetter,
        final BlockPos blockPos,
        final BlockState blockState,
        final RandomSource randomSource)
    {
        final ChiseledBlockModelInformation modelInformation = getInformation(blockAndTintGetter, blockPos);
        if (modelInformation == null)
            return null;

        return modelInformation.key();
    }

    @Override
    public void collectParts(
        final BlockAndTintGetter blockAndTintGetter,
        final BlockPos blockPos,
        final BlockState blockState,
        final RandomSource randomSource,
        final List<BlockModelPart> list)
    {
        final ChiseledBlockModelInformation modelInformation = getInformation(blockAndTintGetter, blockPos);
        if (modelInformation == null)
            return;

        list.addAll(
            modelInformation.parts()
                .stream()
                .map(ChiseledBlockModelPart::adaptForBlockModel)
                .toList()
        );
    }

    @Override
    public TextureAtlasSprite particleIcon(final BlockAndTintGetter blockAndTintGetter, final BlockPos blockPos, final BlockState blockState)
    {
        final ChiseledBlockModelInformation modelInformation = getInformation(blockAndTintGetter, blockPos);
        if (modelInformation == null)
            return particleIcon();

        return modelInformation.particleTexture();
    }

    public final static class Direct extends ChiseledBlockStateModel {
        private final ChiseledBlockModelInformation directInformation;

        public Direct(final ChiseledBlockModelInformation directInformation) {this.directInformation = directInformation;}

        @Override
        protected @Nullable ChiseledBlockModelInformation getInformation(final BlockAndTintGetter blockAndTintGetter, final BlockPos blockPos)
        {
            return directInformation;
        }
    }

    public record Unbaked() implements BlockStateModel.Unbaked {
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(new Unbaked());

        @Override
        public void resolveDependencies(final @NotNull Resolver resolver)
        {
        }

        @Override
        public @NotNull BlockStateModel bake(final @NotNull ModelBaker baker)
        {
            return new ChiseledBlockStateModel();
        }
    }
}

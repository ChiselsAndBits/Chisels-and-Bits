package mod.chiselsandbits.client.model.block;

import com.communi.suggestu.scena.core.client.rendering.DataAwareBlockStateModel;
import com.mojang.serialization.MapCodec;
import mod.chiselsandbits.block.entities.ChiseledBlockEntity;
import mod.chiselsandbits.client.model.information.ChiseledBlockModelInformation;
import mod.chiselsandbits.client.model.parts.ChiseledBlockModelPart;
import mod.chiselsandbits.registrars.ModModelProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.data.AtlasIds;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class ChiseledBlockStateModel implements BlockStateModel, DataAwareBlockStateModel
{
    @Override
    public void collectParts(final @NotNull RandomSource random, final @NotNull List<BlockStateModelPart> output)
    {
        //Noop
    }

    @Override
    public Material.@NonNull Baked particleMaterial()
    {
         return new Material.Baked(
             Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(MissingTextureAtlasSprite.getLocation()),
             false
         );
    }

    @Override
    public @BakedQuad.MaterialFlags int materialFlags()
    {
        return 0;
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
        final List<BlockStateModelPart> list)
    {
        final ChiseledBlockModelInformation modelInformation = getInformation(blockAndTintGetter, blockPos);
        if (modelInformation == null)
            return;

        modelInformation.collectParts(randomSource, list);
    }

    @Override
    public Material.Baked particleMaterial(final BlockAndTintGetter blockAndTintGetter, final BlockPos blockPos, final BlockState blockState)
    {
        final ChiseledBlockModelInformation modelInformation = getInformation(blockAndTintGetter, blockPos);
        if (modelInformation == null)
            return particleMaterial();

        return modelInformation.particleMaterial();
    }

    @Override
    public @BakedQuad.MaterialFlags int materialFlags(final BlockAndTintGetter blockAndTintGetter, final BlockPos blockPos, final BlockState blockState)
    {
        var information = getInformation(blockAndTintGetter, blockPos);
        if (information == null)
            return materialFlags();

        return information.materialFlags();
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

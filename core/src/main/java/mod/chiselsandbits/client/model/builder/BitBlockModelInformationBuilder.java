package mod.chiselsandbits.client.model.builder;

import com.communi.suggestu.scena.core.util.SingleBlockBlockAndTintGetter;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.client.model.information.BitBlockModelInformation;
import mod.chiselsandbits.client.model.parts.BitBlockModelPart;
import mod.chiselsandbits.client.util.QuadGenerationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record BitBlockModelInformationBuilder(BlockInformation information, boolean isLarge)
{

    private static final float    BIT_BEGIN = 4f / 16;
    public static final  Vector3f FROM      = new Vector3f(BIT_BEGIN, BIT_BEGIN, BIT_BEGIN);
    private static final float    BIT_END   = 12f / 16;
    public static final  Vector3f TO        = new Vector3f(BIT_END, BIT_END, BIT_END);

    public static final Vector3f[] EXTENDS = new Vector3f[] {
        FROM,
        TO
    };

    public BitBlockModelInformation build(final BlockAndTintGetter surroundings)
    {
        final Map<IntList, List<BakedQuad>> quadsByTints = new HashMap<>();

        final SingleBlockBlockAndTintGetter blockAndTintGetter = new SingleBlockBlockAndTintGetter.Builder()
            .withBlockState(information().blockState())
            .withBlockEntity(information()::newBlockEntityAtZero)
            .withSource(surroundings)
            .createSingleBlockBlockAndTintGetter();

        for (final Direction myFace : Direction.values())
        {
            QuadGenerationUtils.generateQuads(
                information(),
                myFace,
                blockAndTintGetter,
                BlockPos.ZERO,
                myFace.getAxisDirection() == Direction.AxisDirection.POSITIVE ? TO : FROM,
                myFace.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? TO : FROM,
                (layer, quad) -> {
                    if (layer.material().tintIndex() != -1)
                    {
                        quad.tintIndex(0);
                    }
                },
                generatedQuad -> {
                    final List<BlockTintSource> tintSources =
                        information.isFluid() ?
                        getFluidTintSources() :
                        Minecraft.getInstance().getBlockColors().getTintSources(information.blockState());
                    final IntList tints = new IntArrayList(tintSources.size());
                    tintSources.forEach(source -> {
                        tints.add(
                            source.colorInWorld(information.blockState(),
                                blockAndTintGetter,
                                BlockPos.ZERO)
                        );
                    });

                    quadsByTints.computeIfAbsent(tints, (_) -> new ArrayList<>())
                        .add(generatedQuad.quad());
                }
            );
        }


        final List<BitBlockModelPart> parts =
            quadsByTints.entrySet().stream()
                .map(c -> new BitBlockModelPart(c.getValue(), c.getKey()))
                .toList();

        return new BitBlockModelInformation(parts, true, isLarge());
    }

    private List<BlockTintSource> getFluidTintSources()
    {
        final var fluidModel = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(
            information.blockState().getFluidState()
        );
        final var tintSource = fluidModel.tintSource();

        if (tintSource == null)
            return List.of();

        return List.of(tintSource);
    }
}

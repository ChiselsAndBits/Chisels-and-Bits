package mod.chiselsandbits.client.model.builder;

import com.communi.suggestu.scena.core.util.SingleBlockBlockAndTintGetter;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.client.model.information.BitBlockModelInformation;
import mod.chiselsandbits.client.model.parts.BitBlockModelPart;
import mod.chiselsandbits.client.util.QuadGenerationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    public BitBlockModelInformation build(final Level finalLevel)
    {
        final Table<RenderType, Optional<Integer>, List<BakedQuad>> quads = HashBasedTable.create();

        final SingleBlockBlockAndTintGetter blockAndTintGetter = new SingleBlockBlockAndTintGetter.Builder()
            .withBlockState(information().blockState())
            .withBlockEntity(information()::newBlockEntityAtZero)
            .withSource(finalLevel)
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
                    if (layer.tint() != -1)
                    {
                        quad.tintIndex(0);
                    }
                },
                generatedQuad -> {
                    if (generatedQuad.renderType() == null)
                    {
                        return;
                    }

                    Optional<Integer> tint =
                        generatedQuad.source().tint() != -1 ?
                            Optional.of(
                                ARGB.color(
                                    255,
                                    Minecraft.getInstance().getBlockColors().getColor(
                                        information().blockState(),
                                        finalLevel,
                                        BlockPos.ZERO,
                                        generatedQuad.source().tint()
                                    )
                                )) :
                            Optional.empty();

                    //tint = Optional.of(ARGB.color(0, 0, 255));

                    if (!quads.contains(generatedQuad.renderType(), tint))
                    {
                        quads.put(generatedQuad.renderType(), tint, new ArrayList<>());
                    }

                    Objects.requireNonNull(quads.get(generatedQuad.renderType(), tint)).add(generatedQuad.quad());
                }
            );
        }

        final List<BitBlockModelPart> parts =
            quads.cellSet().stream()
                .map(c -> new BitBlockModelPart(c.getRowKey(), c.getValue(), c.getColumnKey().map(i -> new int[] {i}).orElse(new int[0])))
                .toList();

        return new BitBlockModelInformation(parts, true, isLarge());
    }
}

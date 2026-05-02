package mod.chiselsandbits.client.model.builder;

import com.communi.suggestu.scena.core.util.SingleBlockBlockAndTintGetter;
import com.google.common.collect.Maps;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.client.util.QuadGenerationUtils;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.joml.Vector3f;

import java.util.Map;
import java.util.stream.Collectors;

public record BitBlockQuadCollectionBuilder(BlockInformation information)
{
    private static final float    BIT_BEGIN = 4f / 16;
    public static final  Vector3f FROM      = new Vector3f(BIT_BEGIN, BIT_BEGIN, BIT_BEGIN);
    private static final float    BIT_END   = 12f / 16;
    public static final  Vector3f TO        = new Vector3f(BIT_END, BIT_END, BIT_END);

    public static final Vector3f[] EXTENDS = new Vector3f[] {
        FROM,
        TO
    };

    public QuadCollection build()
    {
        final QuadCollection.Builder result = new QuadCollection.Builder();

        for (final Direction myFace : Direction.values())
        {
            QuadGenerationUtils.generateQuads(
                information,
                myFace,
                null,
                BlockPos.ZERO,
                myFace.getAxisDirection() == Direction.AxisDirection.POSITIVE ? TO : FROM,
                myFace.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? TO : FROM,
                generatedQuad -> result.addUnculledFace(generatedQuad.quad())
            );
        }

        return result.build();
    }
}

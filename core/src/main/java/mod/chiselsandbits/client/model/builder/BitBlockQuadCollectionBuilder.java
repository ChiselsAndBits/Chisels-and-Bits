package mod.chiselsandbits.client.model.builder;

import com.communi.suggestu.scena.core.client.rendering.type.IRenderTypeManager;
import com.communi.suggestu.scena.core.client.utils.RenderTypeUtils;
import com.communi.suggestu.scena.core.util.SingleBlockBlockAndTintGetter;
import com.google.common.collect.Maps;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.client.util.QuadGenerationUtils;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.QuadCollection;
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

    public Map<RenderType, QuadCollection> build()
    {
        final Map<RenderType, QuadCollection.Builder> result = Maps.newHashMap();

        for (final Direction myFace : Direction.values())
        {
            QuadGenerationUtils.generateQuads(
                information,
                myFace,
                null,
                BlockPos.ZERO,
                myFace.getAxisDirection() == Direction.AxisDirection.POSITIVE ? TO : FROM,
                myFace.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? TO : FROM,
                generatedQuad -> {
                    var renderType = generatedQuad.renderType();
                    if (renderType == null)
                    {
                        var defaultChunkSectionLayer = IRenderTypeManager.getInstance().getRenderTypesFor(
                            new SingleBlockBlockAndTintGetter.Builder()
                                .withBlockState(information().blockState())
                                .withBlockEntity(information()::newBlockEntityAtZero)
                                .withPos(BlockPos.ZERO)
                                .createSingleBlockBlockAndTintGetter(),
                            information()::newBlockEntityAtZero,
                            BlockPos.ZERO,
                            information().blockState()
                        );
                        if (defaultChunkSectionLayer.size() != 1) {
                            return;
                        }

                        renderType = RenderTypeUtils.renderTypeFor(defaultChunkSectionLayer.iterator().next());
                    }

                    //We are inlaying and for sure not on the edge so we send everything to unculled.
                    result.computeIfAbsent(renderType, (t) -> new QuadCollection.Builder())
                        .addUnculledFace(generatedQuad.quad());
                }
            );
        }

        return result.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().build()));
    }
}

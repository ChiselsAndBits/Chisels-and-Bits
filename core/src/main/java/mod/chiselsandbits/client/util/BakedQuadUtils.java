package mod.chiselsandbits.client.util;

import com.communi.suggestu.scena.core.client.models.processing.VertexData;
import net.minecraft.client.resources.model.geometry.BakedQuad;

public class BakedQuadUtils
{

    public static BakedQuad withTintIndex(BakedQuad input, int tintIndex) {
        return new BakedQuad(
            input.position0(),
            input.position1(),
            input.position2(),
            input.position3(),
            input.packedUV0(),
            input.packedUV1(),
            input.packedUV2(),
            input.packedUV3(),
            input.direction(),
            new BakedQuad.MaterialInfo(
                input.materialInfo().sprite(),
                input.materialInfo().layer(),
                input.materialInfo().itemRenderType(),
                tintIndex,
                input.materialInfo().shade(),
                input.materialInfo().lightEmission()
            )
        );
    }

    public static VertexData[] getVertexData(final BakedQuad quad)
    {
        final var data = new VertexData[4];
        data[0] = VertexData.from(quad, 0);
        data[1] = VertexData.from(quad, 1);
        data[2] = VertexData.from(quad, 2);
        data[3] = VertexData.from(quad, 3);

        return data;
    }
}

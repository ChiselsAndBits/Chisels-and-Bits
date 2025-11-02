package mod.chiselsandbits.utils;

import com.communi.suggestu.scena.core.client.models.vertices.IVertexConsumer;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;

public final class LightUtil {

    private LightUtil() {
        throw new IllegalStateException("Tried to construct a LightUtil instance, but this is a utility class!");
    }

    public static void pack(float[] from, int[] to, int v, int e)
    {
        VertexFormatElement element = DefaultVertexFormat.BLOCK.getElements().get(e);
        int vertexStart = v * (DefaultVertexFormat.BLOCK.getVertexSize()) + (DefaultVertexFormat.BLOCK.getOffset(element));
        int count = element.count();
        VertexFormatElement.Type type = element.type();
        int size = type.size();
        int mask = (256 << (8 * (size - 1))) - 1;
        for(int i = 0; i < 4; i++)
        {
            if(i < count)
            {
                int pos = vertexStart + size * i;
                int index = pos >> 2;
                int offset = pos & 3;
                int bits = 0;
                float f = i < from.length ? from[i] : 0;
                if(type == VertexFormatElement.Type.FLOAT)
                {
                    bits = Float.floatToRawIntBits(f);
                }
                else if(
                        type == VertexFormatElement.Type.UBYTE ||
                                type == VertexFormatElement.Type.USHORT ||
                                type == VertexFormatElement.Type.UINT
                )
                {
                    bits = Math.round(f * mask);
                }
                else
                {
                    bits = Math.round(f * (mask >> 1));
                }
                to[index] &= ~(mask << (offset * 8));
                to[index] |= (((bits & mask) << (offset * 8)));
            }
        }
    }

    public static void put(IVertexConsumer consumer, BakedQuad quad)
    {
        consumer.setTexture(quad.sprite());
        consumer.setQuadOrientation(quad.direction());
        if(quad.isTinted())
        {
            consumer.setQuadTint(quad.tintIndex());
        }
        consumer.setApplyDiffuseLighting(quad.shade());
        float[] data = new float[4];
        VertexFormat format = DefaultVertexFormat.BLOCK;
        int elementCount = format.getElements().size();
        for(int v = 0; v < 4; v++)
        {
            for(int e = 0; e < elementCount; e++)
            {
                unpack(quad.vertices(), data, format, v, e);
                consumer.put(v, e, data);
            }
        }

        consumer.onComplete();
    }

    public static void unpack(int[] from, float[] to, VertexFormat formatFrom, int v, int e)
    {
        int length = Math.min(4, to.length);
        VertexFormatElement element = formatFrom.getElements().get(e);
        int vertexStart = v * (formatFrom.getVertexSize()) + formatFrom.getOffset(element);
        int count = element.count();
        VertexFormatElement.Type type = element.type();
        VertexFormatElement.Usage usage = element.usage();
        int size = type.size();
        int mask = (256 << (8 * (size - 1))) - 1;
        for(int i = 0; i < length; i++)
        {
            if(i < count)
            {
                int pos = vertexStart + size * i;
                int index = pos >> 2;
                int offset = pos & 3;
                int bits = from[index];
                bits = bits >>> (offset * 8);
                if((pos + size - 1) / 4 != index)
                {
                    bits |= from[index + 1] << ((4 - offset) * 8);
                }
                bits &= mask;
                if(type == VertexFormatElement.Type.FLOAT)
                {
                    to[i] = Float.intBitsToFloat(bits);
                }
                else if(type == VertexFormatElement.Type.UBYTE || type == VertexFormatElement.Type.USHORT)
                {
                    to[i] = (float)bits / mask;
                }
                else if(type == VertexFormatElement.Type.UINT)
                {
                    to[i] = (float)((double)(bits & 0xFFFFFFFFL) / 0xFFFFFFFFL);
                }
                else if(type == VertexFormatElement.Type.BYTE)
                {
                    to[i] = ((float)(byte)bits) / (mask >> 1);
                }
                else if(type == VertexFormatElement.Type.SHORT)
                {
                    to[i] = ((float)(short)bits) / (mask >> 1);
                }
                else if(type == VertexFormatElement.Type.INT)
                {
                    to[i] = (float)((double)(bits & 0xFFFFFFFFL) / (0xFFFFFFFFL >> 1));
                }
            }
            else
            {
                to[i] = (i == 3 && usage == VertexFormatElement.Usage.POSITION) ? 1 : 0;
            }
        }
    }


    public static float diffuseLight(Direction side)
    {
        return switch (side) {
            case DOWN -> .5f;
            case UP -> 1f;
            case NORTH, SOUTH -> .8f;
            default -> .6f;
        };
    }
}

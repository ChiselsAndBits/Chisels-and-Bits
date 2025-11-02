package mod.chiselsandbits.utils;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class TextureUtils
{

    private TextureUtils()
    {
        throw new IllegalStateException("Can not instantiate an instance of: TextureUtils. This is a utility class");
    }

    public static NativeImage downloadTexture(GpuTexture srcTexture)
    {
        GpuDevice device = RenderSystem.getDevice();
        CommandEncoder cmdEncoder = device.createCommandEncoder();

        int width = srcTexture.getWidth(0);
        int height = srcTexture.getHeight(0);
        int pixSize = srcTexture.getFormat().pixelSize();
        int bufSize = width * height * pixSize;
        GpuBuffer buffer = device.createBuffer(() -> "Texture output buffer", GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ, bufSize);
        NativeImage destImage = new NativeImage(width, height, false);
        cmdEncoder.copyTextureToBuffer(srcTexture, buffer, 0, () ->
        {
            try (GpuBuffer.MappedView bufView = cmdEncoder.mapBuffer(buffer, true, false); )
            {
                ByteBuffer data = bufView.data();
                for (int y = 0; y < height; y++)
                {
                    for (int x = 0; x < width; x++)
                    {
                        int pixel = data.getInt((x + y * width) * pixSize);
                        destImage.setPixelABGR(x, y, pixel);
                    }
                }
            }
            buffer.close();
        }, 0);
        return destImage;
    }
}

package mod.chiselsandbits.client.model.face;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

public interface IFaceBuilder
{
    void setFace(
      Direction myFace,
      int tintIndex );

    void put(
      int vertNum,
      int element,
      float... args );

    void begin();

    BakedQuad create(
      TextureAtlasSprite sprite );

    VertexFormat getFormat();
}

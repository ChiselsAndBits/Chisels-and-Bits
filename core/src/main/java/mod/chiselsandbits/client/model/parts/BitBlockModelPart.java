package mod.chiselsandbits.client.model.parts;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.QuadCollection;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

public record BitBlockModelPart(
    RenderType renderType,
    Collection<BakedQuad> quads,
    int[] tints
)
{
    public boolean hasTints()
    {
        return tints.length != 0 && Arrays.stream(tints).allMatch(i -> i != -1);
    }
}

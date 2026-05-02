package mod.chiselsandbits.client.model.parts;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.resources.model.geometry.BakedQuad;

import java.util.Arrays;
import java.util.Collection;

public record BitBlockModelPart(
    Collection<BakedQuad> quads,
    IntList tints
)
{
    public boolean hasTints()
    {
        return !tints.isEmpty();
    }
}

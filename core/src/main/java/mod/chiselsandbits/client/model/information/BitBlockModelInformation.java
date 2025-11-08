package mod.chiselsandbits.client.model.information;

import mod.chiselsandbits.client.model.parts.BitBlockModelPart;

import java.util.Collection;
import java.util.List;

public record BitBlockModelInformation(
    Collection<BitBlockModelPart> parts,
    boolean isBlock,
    boolean isLarge
)
{
    public static final BitBlockModelInformation EMPTY = new BitBlockModelInformation(List.of(), true, true);
}

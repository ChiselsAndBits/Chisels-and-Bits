package mod.chiselsandbits.client.model.information;

import java.util.Collection;
import java.util.List;

import mod.chiselsandbits.client.model.parts.BitBlockModelPart;

public record BitBlockModelInformation(
		Collection<BitBlockModelPart> parts,
		boolean isBlock,
		boolean isLarge
) {
	public static final BitBlockModelInformation EMPTY = new BitBlockModelInformation(List.of(), true, true);
}

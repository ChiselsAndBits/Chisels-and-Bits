package mod.chiselsandbits.client.model.parts;

import java.util.Collection;

import it.unimi.dsi.fastutil.ints.IntList;

import net.minecraft.client.resources.model.geometry.BakedQuad;

public record BitBlockModelPart(
		Collection<BakedQuad> quads,
		IntList tints
) {
	public boolean hasTints() {
		return !tints.isEmpty();
	}
}

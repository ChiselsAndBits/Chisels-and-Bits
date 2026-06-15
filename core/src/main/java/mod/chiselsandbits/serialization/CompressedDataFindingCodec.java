package mod.chiselsandbits.serialization;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import mod.chiselsandbits.api.util.constants.NbtConstants;

public class CompressedDataFindingCodec<V> implements Codec<V> {
	private final Codec<V> elementCodec;

	private CompressedDataFindingCodec(Codec<V> elementCodec) {
		this.elementCodec = elementCodec;
	}

	public static <V> CompressedDataFindingCodec<V> of(Codec<V> elementCodec) {
		return new CompressedDataFindingCodec<V>(elementCodec);
	}

	@Override
	public <T> DataResult<Pair<V, T>> decode(DynamicOps<T> ops, T input) {
		final var mapDataResult = ops.getMap(input);
		final MapLike<T> map;

		try {
			map = mapDataResult.getPartialOrThrow((s) -> new IllegalStateException("Legacy data is always a map!", new Exception(s)));
		} catch (IllegalStateException e) {
			return elementCodec.decode(ops, input);
		}

		final var compressedIndicator = map.get(NbtConstants.COMPRESSED);
		final var innerData = map.get(NbtConstants.DATA);

		if (compressedIndicator == null && innerData != null) {
			return decode(ops, innerData);
		}

		if (compressedIndicator != null && innerData != null) {
			return elementCodec.decode(ops, innerData);
		}

		throw new IllegalStateException("No compressed data found!");
	}

	@Override
	public <T> DataResult<T> encode(V input, DynamicOps<T> ops, T prefix) {
		throw new UnsupportedOperationException("This is a legacy codec that is only meant for reading data!");
	}
}

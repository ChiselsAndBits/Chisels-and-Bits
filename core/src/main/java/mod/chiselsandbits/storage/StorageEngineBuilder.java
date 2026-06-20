package mod.chiselsandbits.storage;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import mod.chiselsandbits.api.serialization.CBCodecs;
import org.jspecify.annotations.Nullable;

public class StorageEngineBuilder<TPayload> {
	private final LinkedList<MapCodec<TPayload>> storageHandlers = new LinkedList<>();

	@Nullable
	private MapCodec<TPayload> fallbackCodec;

	private int minimalVersion = 0;

	private StorageEngineBuilder() {
	}

	public static <T> StorageEngineBuilder<T> create() {
		return new StorageEngineBuilder<>();
	}

	public StorageEngineBuilder<TPayload> minimalVersion(final int version) {
		this.minimalVersion = version;
		return this;
	}

	public StorageEngineBuilder<TPayload> with(final MapCodec<TPayload> handler) {
		storageHandlers.add(handler);
		return this;
	}

	public StorageEngineBuilder<TPayload> fallback(final MapCodec<TPayload> handler) {
		this.fallbackCodec = handler;
		return this;
	}

	public Codec<TPayload> build() {
		if (storageHandlers.isEmpty()) {
			throw new IllegalStateException("No storage handlers defined");
		}

		final Map<Integer, MapCodec<TPayload>> versions = new HashMap<>();

		for (int i = 0; i < storageHandlers.size(); i++) {
			versions.put(i + minimalVersion, storageHandlers.get(i));
		}

		MapCodec<TPayload> fallbackCodec = this.fallbackCodec;

		if (fallbackCodec == null) {
			fallbackCodec = storageHandlers.get(minimalVersion);
		}

		return CBCodecs.versioned(versions, fallbackCodec);
	}

	public IMultiThreadedStorageEngine<TPayload> buildMultiThreaded() {
		return new MultiThreadAwareStorageEngine<>(build());
	}
}

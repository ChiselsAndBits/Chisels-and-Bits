package mod.chiselsandbits.utils;

import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import org.apache.commons.lang3.Validate;

public class SimpleMaxSizedCache<K, V> {
	private final Map<K, V> cache = new ConcurrentHashMap<>();
	private final Queue<K> keyQueue = new ConcurrentLinkedQueue<>();

	private final LongSupplier maxSizeSupplier;

	public SimpleMaxSizedCache(final long maxSize)

	{
		Validate.exclusiveBetween(0, 10000000000L, maxSize);
		this.maxSizeSupplier = () -> maxSize;
	}

	public SimpleMaxSizedCache(final LongSupplier longSupplier) {
		this.maxSizeSupplier = longSupplier;
	}

	public SimpleMaxSizedCache(final IntSupplier intSupplier) {
		this.maxSizeSupplier = intSupplier::getAsInt;
	}

	private void evictFromCacheIfNeeded() {
		while (cache.size() >= maxSizeSupplier.getAsLong()) {
			cache.remove(keyQueue.poll());
		}
	}

	public V get(final K key) {
		return cache.get(key);
	}

	public V get(final K key, final Supplier<V> valueSupplier) {
		if (!cache.containsKey(key))

		{
			final V value = valueSupplier.get();
			put(key, value);
			return value;
		}

		return cache.get(key);
	}

	public Optional<V> getIfPresent(final K key) {
		return Optional.ofNullable(get(key));
	}

	public void put(final K key, final V value) {
		if (!cache.containsKey(key)) {
			evictFromCacheIfNeeded();
			keyQueue.add(key);
		}

		cache.put(key, value);
	}

	public void clear() {
		this.cache.clear();
		this.keyQueue.clear();
	}
}

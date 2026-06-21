package mod.chiselsandbits.forge.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class CollectorUtils {
	private CollectorUtils() {
		throw new IllegalStateException("Can not instantiate an instance of: CollectorUtils.This is a utility class");
	}

	public static <T, M extends Map<Character, V>, V> Collector<T, M, M> toEnumeratedCharacterKeyedMap(
			final Supplier<M> mapConstructor,
			final Function<T, V> valueMapper
	) {
		return Collector.of(
				mapConstructor,
			new BiConsumer<>() {
				char c = 'a';

				@Override
				public void accept(final M m, final T t) {
					m.put(c, valueMapper.apply(t));

					c += 1;
				}
			},


				(m, m2) -> {
					m.putAll(m2);
					return m;
				});
	}

	public static <T> Collector<T, HashMap<Character, T>, HashMap<Character, T>> toEnumeratedCharacterKeyedMap(
	) {
		return toEnumeratedCharacterKeyedMap(
				HashMap::new,
				Function.identity()
		);
	}
}

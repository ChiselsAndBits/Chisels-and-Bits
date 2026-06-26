package mod.chiselsandbits.utils;

import java.util.function.Function;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

public class SingleObjectCache<T> {
	@Nullable
	private T value;

	private final Supplier<T> builder;

	private final Function<T, T> cloner;

	public SingleObjectCache(final Supplier<T> builder, final Function<T, T> cloner) {
		this.builder = builder;
		this.cloner = cloner;
	}

	public T get() {
		if (value == null) {
			value = builder.get();
		}

		return cloner.apply(value);
	}

	public void reset() {
		value = null;
	}
}

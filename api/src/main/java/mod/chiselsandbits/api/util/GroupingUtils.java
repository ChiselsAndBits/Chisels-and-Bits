package mod.chiselsandbits.api.util;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility class for managing grouping of objects
 * in a collection, based on some key.
 */
public final class GroupingUtils {
	private GroupingUtils() {
		throw new IllegalStateException("Tried to initialize: GroupingUtils but this is a Utility class.");
	}

	/**
	 * Group the given collection by the given key.
	 * Returns a {@link java.util.Set} and as such eliminates duplicates.
	 *
	 * @param source The source collection to group.
	 * @param extractor The key extractor.
	 * @param <T> The type of the objects to group.
	 * @param <O> The key to group by.
	 *
	 * @return A collection of collections, which contain all objects which have the same key.
	 */
	public static <T, O> Collection<Collection<T>> groupByUsingSet(final Iterable<T> source, Function<T, O> extractor) {
		return groupBy(HashMultimap.create(), source, extractor);
	}

	/**
	 * Group the given collection by the given key.
	 * Returns a {@link java.util.List} and as such does not eliminate duplicates.
	 *
	 * @param source The source collection to group.
	 * @param extractor The key extractor.
	 * @param <T> The type of the objects to group.
	 * @param <O> The key to group by.
	 *
	 * @return A collection of collections, which contain all objects which have the same key.
	 */
	public static <T, O> Collection<Collection<T>> groupByUsingList(final Iterable<T> source, Function<T, O> extractor) {
		return groupBy(ArrayListMultimap.create(), source, extractor);
	}

	private static <T, O> Collection<Collection<T>> groupBy(final Multimap<O, T> groups, final Iterable<T> source, Function<T, O> extractor) {
		source.forEach(
				e -> {
					groups.put(extractor.apply(e), e);
				});

		return groups
				.keySet()
				.stream()
				.map(groups::get)
				.collect(Collectors.toList());
	}
}

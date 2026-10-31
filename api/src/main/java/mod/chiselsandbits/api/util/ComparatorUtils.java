package mod.chiselsandbits.api.util;

import java.util.Comparator;
import java.util.Optional;

/**
 * Utility class for building {@link Comparator}s that order {@link Optional} values, comparing their contents and ranking empty values last.
 */
public class ComparatorUtils {
	private ComparatorUtils() {
		throw new IllegalStateException("Can not instantiate an instance of: ComparatorUtils.This is a utility class");
	}

	/**
	 * Creates a {@link Comparator} that orders {@link Optional} values by their contents in natural order, treating any present value as greater than an empty one.
	 *
	 * @param <T> The type of the wrapped, comparable element.
	 * @return A comparator over {@code Optional<T>} that compares present values and ranks empties below them.
	 */
	public static <T extends Comparable<T>> Comparator<Optional<T>> createOptionalComparator() {
		return createOptionalComparator(Comparator.<T>naturalOrder());
	}

	/**
	 * Creates a {@link Comparator} that orders {@link Optional} values by the supplied comparator for present contents, treating any present value as greater than an empty one.
	 *
	 * @param <T> The type of the wrapped element compared by {@code comparator}.
	 * @param comparator The ordering to apply to present values.
	 * @return A comparator over {@code Optional<T>} that delegates present comparisons and ranks empties below them.
	 */
	public static <T extends Comparable<T>> Comparator<Optional<T>> createOptionalComparator(Comparator<T> comparator) {
		return (o1, o2) -> {
			if (o1.isPresent() && o2.isPresent()) {
				return comparator.compare(o1.get(), o2.get());
			} else if (o1.isPresent()) {
				return 1;
			} else if (o2.isPresent()) {
				return -1;
			} else {
				return 0;
			}
		};
	}
}

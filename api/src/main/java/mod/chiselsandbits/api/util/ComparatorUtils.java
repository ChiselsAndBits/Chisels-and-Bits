package mod.chiselsandbits.api.util;

import java.util.Comparator;
import java.util.Optional;

public class ComparatorUtils {
	private ComparatorUtils()

	{
		throw new IllegalStateException("Can not instantiate an instance of: ComparatorUtils. This is a utility class");
	}

	public static <T extends Comparable<T>> Comparator<Optional<T>> createOptionalComparator() {
		return createOptionalComparator(Comparator.<T>naturalOrder());
	}

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
		}

		;
	}
}

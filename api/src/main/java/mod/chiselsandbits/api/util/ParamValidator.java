package mod.chiselsandbits.api.util;

/**
 * Validates method parameters and reports violations with descriptive exceptions.
 */

public class ParamValidator {
	private ParamValidator() {
		throw new IllegalStateException("Can not instantiate an instance of: ParamValidator.This is a utility class");
	}

	/**
	 * Checks that the target is an instance of the requested class.
	 *
	 * @param target object to type-check
	 * @param cls class whose membership is required
	 * @param <O> target value type
	 * @param <T> required class type
	 * @return {@code target} when it is an instance of {@code cls}
	 * @throws IllegalStateException if {@code target} is not an instance of {@code cls}
	 */

	public static <O, T> O isInstanceOf(final O target, final Class<T> cls) {
		if (cls.isInstance(target)) {
			return target;
		}

		throw new IllegalStateException(String.format("%s is not an instance of: %s", target, cls.getName()));
	}
}

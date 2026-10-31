package mod.chiselsandbits.api.util;

import java.util.function.Consumer;
import java.util.function.ToIntFunction;

/**
 * Adapters for using callbacks that declare checked exceptions with standard Java functional interfaces.
 */

@SuppressWarnings("unchecked")
public class LambdaExceptionUtils {
	/**
	 * Creates the utility object; all operations are exposed as static methods.
	 */
	public LambdaExceptionUtils() { }

	/**
	 * Adapts a consumer that declares a checked exception to a standard {@link Consumer}, rethrowing failures without wrapping.
	 *
	 * @param <T> the consumed value type
	 * @param <E> the checked exception type
	 * @param consumer action that may throw {@code E}
	 * @return a consumer that propagates the original exception as an unchecked throw
	 */
	public static <T, E extends Exception> Consumer<T> rethrowConsumer(Consumer_WithExceptions<T, E> consumer) {
		return t -> {
			try {
				consumer.accept(t);
			} catch (Exception exception) {
				throwAsUnchecked(exception);
			}
		};
	}

	/**
	 * Adapts a function that returns an integer and declares a checked exception to a standard {@link ToIntFunction}.
	 *
	 * @param <T> the input value type
	 * @param <E> the checked exception type
	 * @param consumer function that may throw {@code E}
	 * @return a function that propagates the original exception as an unchecked throw
	 */
	public static <T, E extends Exception> ToIntFunction<T> rethrowToIntFunction(ToIntFunction_WithExceptions<T, E> consumer) {
		return t -> {
			try {
				return consumer.applyAsInt(t);
			} catch (Exception exception) {
				throwAsUnchecked(exception);
				return -1; //Never reached :D
			}
		};
	}

	private static <E extends Throwable> void throwAsUnchecked(Exception exception) throws E {
		throw (E) exception;
	}

	/**
	 * Consumer functional interface whose operation may throw a checked exception.
	 *
	 * @param <T> type of value consumed
	 * @param <E> checked exception type that may be thrown
	 */
	@FunctionalInterface
	public interface Consumer_WithExceptions<T, E extends Exception> {
		/**
		 * Consumes a value and may throw the declared exception.
		 *
		 * @param t value to consume
		 * @throws E if the operation fails
		 */
		void accept(T t) throws E;
	}

	/**
	 * Integer-returning function whose operation may throw a checked exception.
	 *
	 * @param <T> type of input value
	 * @param <E> checked exception type that may be thrown
	 */
	@FunctionalInterface
	public interface ToIntFunction_WithExceptions<T, E extends Exception> {
		/**
		 * Applies the function and may throw the declared exception.
		 *
		 * @param t input value
		 * @return the computed integer
		 * @throws E if the operation fails
		 */
		int applyAsInt(T t) throws E;
	}
}

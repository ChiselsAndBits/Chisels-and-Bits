package mod.chiselsandbits.api.util;

import java.util.function.Consumer;
import java.util.function.ToIntFunction;

@SuppressWarnings("unchecked")
public class LambdaExceptionUtils {
	/**
	 * *.forEach(rethrowConsumer(name -> System.out.println(Class.forName(name)))); or.forEach(rethrowConsumer(ClassNameUtil::println));.
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

	@FunctionalInterface
	public interface Consumer_WithExceptions<T, E extends Exception> {
		void accept(T t) throws E;
	}

	@FunctionalInterface
	public interface ToIntFunction_WithExceptions<T, E extends Exception> {
		int applyAsInt(T t) throws E;
	}
}

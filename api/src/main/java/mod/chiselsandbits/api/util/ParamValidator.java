package mod.chiselsandbits.api.util;

public class ParamValidator {
	private ParamValidator() {
		throw new IllegalStateException("Can not instantiate an instance of: ParamValidator.This is a utility class");
	}

	public static <O, T> O isInstanceOf(final O target, final Class<T> cls) {
		if (cls.isInstance(target)) {
			return target;
		}

		throw new IllegalStateException(String.format("%s is not an instance of: %s", target, cls.getName()));
	}
}

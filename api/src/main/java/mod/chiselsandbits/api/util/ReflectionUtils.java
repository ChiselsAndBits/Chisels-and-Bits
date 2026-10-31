package mod.chiselsandbits.api.util;

import java.lang.reflect.Field;

/**
 * Helpers for reading and writing declared fields through reflection.
 */

public class ReflectionUtils {
	private ReflectionUtils() {
		throw new IllegalStateException("Can not instantiate an instance of: ReflectionUtils.This is a utility class");
	}

	/**
	 * Sets the named field on the target object using reflection.
	 *
	 * @param targetObject object whose declared field is changed
	 * @param fieldName name of the field to set
	 * @param value value assigned to the field
	 */

	public static void setField(final Object targetObject, final String fieldName, final Object value) {
		try {
			Field f = targetObject.getClass().getDeclaredField(fieldName);
			f.setAccessible(true);
			f.set(targetObject, value);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new IllegalStateException("Failed to set value!");
		}
	}

	/**
	 * Reads the named field from the target object using reflection.
	 *
	 * @param target object whose declared field is read
	 * @param name name of the field to read
	 * @return the field's current value
	 */

	public static Object getField(final Object target, final String name) {
		try {
			Field f = target.getClass().getDeclaredField(name);
			f.setAccessible(true);
			return f.get(target);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new IllegalStateException("Failed to get value!");
		}
	}
}

package mod.chiselsandbits.api.util;

import mod.chiselsandbits.api.IChiselsAndBitsAPI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.function.Function;

/**
 * Utility class for processing classes.
 */
public final class ClassUtils
{
	private static final Logger LOGGER = LogManager.getLogger();

	private ClassUtils()

	{
		throw new IllegalStateException("Tried to initialize: ClassUtils but this is a Utility class.");
	}

	/**
     * Gets or creates a new instance of the class with the given name.
     *
     * Verifies that the class is of a given base type, and checks if a field in this class potentially has the instance annotation.
     * If that annotation is present then the contents of that field are returned and verified for the requested type instance.
     *
     * @param className The name of the class to construct and load.
     * @param baseClass The base class to verify the class is of.
     * @param instanceAnnotation The annotation used to find a manually constructed instance.
     * @param nameFunction The function used to determine the name of the class, used for logging.
     * @param <T> The type of the base class.
     * @return An instance of the class, or null if the class is not of the given base type or construction fails.
     */
	@SuppressWarnings("unchecked")
	@Nullable
	public static <T> T createOrGetInstance(String className, Class<T> baseClass, Class<? extends Annotation> instanceAnnotation, Function<T, String> nameFunction) {
		//Try to create an instance of the class
		try {
			Class<? extends T> subClass = Class.forName(className).asSubclass(baseClass);
			//First try looking at the fields of the class to see if one of them is specified as the instance
			Field[] fields = subClass.getDeclaredFields();

			for (Field field : fields) {
				if (field.isAnnotationPresent(instanceAnnotation)) {
					if (Modifier.isStatic(field.getModifiers())) {
						try {
							Object fieldValue = field.get(null);

							if (baseClass.isInstance(fieldValue)) {
								T instance = (T) fieldValue;
								LOGGER.debug("Found specified {} instance for: {}. Using it rather than creating a new instance.", baseClass.getSimpleName(),
								  nameFunction.apply(instance));
								return instance;
							} else {
								LOGGER.error("{} annotation found on non {} field: {}", instanceAnnotation.getSimpleName(), baseClass.getSimpleName(), field);
								return null;
							}
						} catch (IllegalAccessException e) {
							LOGGER.error("{} annotation found on inaccessible field: {}", instanceAnnotation.getSimpleName(), field);
							return null;
						}
					} else {
						LOGGER.error("{} annotation found on non static field: {}", instanceAnnotation.getSimpleName(), field);
						return null;
					}
				}
			}

			return subClass.getDeclaredConstructor().newInstance();
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | LinkageError e) {
			LOGGER.error("Failed to load: {}", className, e);
		}

		catch (InvocationTargetException e)

		{
			LOGGER.error("Failed to instantiate a new instance of the class: " + className + ". Its constructor threw an exception", e);
		}

		catch (NoSuchMethodException e)

		{
			LOGGER.error("Failed to create a new instance of the class. Failed to find a parameterless constructor on: " + className);
		}

		return null;
	}
}

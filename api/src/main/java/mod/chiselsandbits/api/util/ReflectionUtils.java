package mod.chiselsandbits.api.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectionUtils
{

    private ReflectionUtils()
    {
        throw new IllegalStateException("Can not instantiate an instance of: ReflectionUtils. This is a utility class");
    }

    public static void setField(final Object targetObject, final String fieldName, final Object value) {
        try
        {
            Field f = targetObject.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(targetObject, value);
        }
        catch (NoSuchFieldException | IllegalAccessException e)
        {
            throw new IllegalStateException("Failed to set value!");
        }
    }

    public static Object getField(final Object target, final String name)
    {
        try
        {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(target);
        }
        catch (NoSuchFieldException | IllegalAccessException e)
        {
            throw new IllegalStateException("Failed to get value!");
        }
    }

    public static Object getFieldInherited(final Object target, final String name)
    {
        Class<?> clazz = target.getClass();
        while (clazz != null)
        {
            try
            {
                Field f = clazz.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(target);
            }
            catch (NoSuchFieldException e)
            {
                clazz = clazz.getSuperclass();
            }
            catch (IllegalAccessException e)
            {
                throw new IllegalStateException("Failed to get value!");
            }
        }
        throw new IllegalStateException("Failed to get value: field '" + name + "' not found in class hierarchy!");
    }

    public static Method getMethod(final Object target, final String methodName, final Class<?>... parameterTypes) {
        try {
            Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e ) {
            throw new IllegalStateException("Failed to find method!");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to invoke method!");
        }
    }
}

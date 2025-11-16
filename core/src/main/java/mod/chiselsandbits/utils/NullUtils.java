package mod.chiselsandbits.utils;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

public class NullUtils
{

    private NullUtils()
    {
        throw new IllegalStateException("Can not instantiate an instance of: NullUtils. This is a utility class");
    }

    public static <T> void whenNotNull(@Nullable final T obj, @NotNull final Consumer<@NotNull T> executor) {
        if (obj != null)
            executor.accept(obj);
    }

    @Contract("null, _, _ -> param2;")
    public static <T, R> R whenNotNull(@Nullable final T obj, @Nullable final R defaultValue, @NotNull final Function<@NotNull T, R> executor) {
        if (obj != null)
            return executor.apply(obj);

        return defaultValue;
    }
}

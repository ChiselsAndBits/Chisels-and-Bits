package mod.chiselsandbits.utils;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

public class SingleObjectCache<T>
{

    @Nullable
    private T value;

    @NotNull
    private final Supplier<T> builder;

    @NotNull
    private final Function<T, T> cloner;

    public SingleObjectCache(final @NotNull Supplier<T> builder, final @NotNull Function<T, T> cloner) {
        this.builder = builder;
        this.cloner = cloner;
    }

    public T get() {
        if (value == null)
            value = builder.get();

        return cloner.apply(value);
    }

    public void reset() {
        value = null;
    }
}

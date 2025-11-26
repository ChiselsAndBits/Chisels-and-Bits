package mod.chiselsandbits.profiling;

import mod.chiselsandbits.api.profiling.IProfiler;
import mod.chiselsandbits.api.profiling.IProfilerResult;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.ActiveProfiler;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class CandBProfiler implements IProfiler
{

    /**
     * Creates a callback to pass to {@link ProfilingManager#withProfiler(Consumer)} to start a new section.
     *
     * @param name The name of the new section.
     * @return The callback.
     */
    public static Consumer<IProfiler> start(final String name) {
        return profiler -> profiler.startSection(name);
    }

    /**
     * Creates a callback to pass to {@link ProfilingManager#withProfiler(Consumer)} to end the current section and start a new section.
     *
     * @param name The name of the new section.
     * @return The callback.
     */
    public static Consumer<IProfiler> endStart(final String name) {
        return profiler -> profiler.endStartSection(name);
    }

    private final ActiveProfiler inner = new ActiveProfiler(Util.timeSource, () -> 0, () -> false);

    public CandBProfiler()
    {
        inner.startTick();
    }

    @Override
    public void startSection(final String name)
    {
        inner.push(name);
    }

    @Override
    public void startSection(final Supplier<String> nameSupplier)
    {
        inner.push(nameSupplier);
    }

    @Override
    public void endSection()
    {
        inner.pop();
    }

    public IProfilerResult getResult() {
        inner.endTick();
        final IProfilerResult result = new CandBProfilingResult(inner.getResults());
        inner.startTick();
        return result;
    }

    public IProfilerResult stop() {
        inner.endTick();
        return new CandBProfilingResult(inner.getResults());
    }
}

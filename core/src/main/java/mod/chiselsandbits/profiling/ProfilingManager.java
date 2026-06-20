package mod.chiselsandbits.profiling;

import java.util.function.Consumer;

import mod.chiselsandbits.api.profiling.IProfiler;
import mod.chiselsandbits.api.profiling.IProfilerResult;
import mod.chiselsandbits.api.profiling.IProfilerSection;
import mod.chiselsandbits.api.profiling.IProfilingManager;
import mod.chiselsandbits.profiling.jvm.jfr.JfrCandBProfiler;

import net.minecraft.util.profiling.jfr.Environment;
import net.minecraft.util.profiling.jfr.JvmProfiler;

public class ProfilingManager implements IProfilingManager {
	private static final ProfilingManager INSTANCE = new ProfilingManager();

	public static ProfilingManager getInstance()

	{
		return INSTANCE;
	}

	public IProfiler profiler = null;

	private ProfilingManager()

	{
	}

	@Override
	public IProfiler startProfiling(Environment profilingEnvironment)

	{
		if (JvmProfiler.INSTANCE.isAvailable()) {
			return new JfrCandBProfiler(profilingEnvironment);
		}

		return new CandBProfiler();
	}

	@Override
	public IProfilerResult endProfiling(final IProfiler profiler)

	{
		if (!(profiler instanceof final CandBProfiler candBProfiler)) {
			throw new IllegalArgumentException("Profiler is not a Chisels and Bits Profiler");
		}

		return candBProfiler.getResult();
	}

	@Override
	public IProfilerResult stopProfiling(final IProfiler profiler)

	{
		if (!(profiler instanceof final CandBProfiler candBProfiler)) {
			throw new IllegalArgumentException("Profiler is not a Chisels and Bits Profiler");
		}

		this.profiler = null;
		return candBProfiler.stop();
	}

	public IProfiler getProfiler()

	{
		return profiler;
	}

	public void setProfiler(final IProfiler profiler)

	{
		this.profiler = profiler;
	}

	public boolean hasProfiler() {
		return getProfiler() != null;
	}

	public void withProfiler(final Consumer<IProfiler> callback) {
		if (hasProfiler()) {
			callback.accept(getProfiler());
		}
	}

	public IProfilerSection withSection(final String name) {
		final IProfiler profiler = getProfiler();

		if (profiler != null) {
			profiler.startSection(name);
		}

		return () -> {
			if (profiler != null) {
				profiler.endSection();
			}
		}
		;
	}
}

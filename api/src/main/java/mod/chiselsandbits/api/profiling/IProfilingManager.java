package mod.chiselsandbits.api.profiling;

import mod.chiselsandbits.api.IChiselsAndBitsAPI;
import net.minecraft.util.profiling.jfr.Environment;

/**
 * A profiling manager to handle the profiling of different interactions or managements.
 */
public interface IProfilingManager {
	/**
	 * Gives access to the current profiling manager.
	 *
	 * @return The current profiling manager.
	 */
	static IProfilingManager getInstance() {
		return IChiselsAndBitsAPI.getInstance().getProfilingManager();
	}

	/**
	 * Creates a new profiler.
	 *
	 * @param profilingEnvironment The environment the profiler runs in.
	 * @return The profiler
	 */
	IProfiler startProfiling(Environment profilingEnvironment);

	/**
	 * Ends the profiling of a given profiler
	 * @param profiler The profiler to end profiling.
	 *
	 * @return The result of the profiling.
	 */
	IProfilerResult endProfiling(IProfiler profiler);

	/**
	 * This stops the profiler given and clears out any profiling data, returns the collected data and stops further profiling.
	 *
	 * @param profiler The profiler to stop and cleanup.
	 * @return The result of the profiler.
	 */
	IProfilerResult stopProfiling(IProfiler profiler);
}

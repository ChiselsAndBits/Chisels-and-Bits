package mod.chiselsandbits.api.profiling;

/**
 * Represents a resource used for try-resource blocks that represents a
 * section of profileable code.
 */
public interface IProfilerSection extends AutoCloseable {
	@Override
	void close();
}

package mod.chiselsandbits.api.profiling;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A profiler used to track the time certain operations inside Chisels and Bits take.
 */
public interface IProfiler {
	/**
	 * Start a section with the given name.
	 *
	 * @param name the name to start the section with.
	 */
	void startSection(String name);

	/**
	 * Start section with the name provided by the given supplier.
	 *
	 * @param nameSupplier the supplier for the name of the new section.
	 */
	void startSection(Supplier<String> nameSupplier);

	/**
	 * Ends the current section.
	 */
	void endSection();

	/**
	 * End the current section and start a new section with the given name.
	 *
	 * @param name the name to start the new section with.
	 */
	default void endStartSection(String name) {
		endSection();
		startSection(name);
	}

	/**
	 * End the current section and start a new section with the name supplied by the given supplier.
	 *
	 * @param nameSupplier the supplier for the name to start the new section with.
	 */
	default void endStartSection(Supplier<String> nameSupplier) {
		endSection();
		startSection(nameSupplier);
	}
}

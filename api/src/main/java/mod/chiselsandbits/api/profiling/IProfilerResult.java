package mod.chiselsandbits.api.profiling;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * The result of a profiling operation.
 */
public interface IProfilerResult {
	/**
	 * Writes all profiling result data to the given file.Creating it if necessary.
	 * @param file The file to write to.
	 */
	void writeToFile(Path file);

	/**
	 * Writes all profiling result data in a readable manor into the given consumer, allows for the outputting of the results to a player in chat, or the server console.
	 * @param lineConsumer The line consumer.
	 */
	void writeAsResponse(Consumer<String> lineConsumer);
}

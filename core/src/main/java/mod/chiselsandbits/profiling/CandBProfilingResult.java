package mod.chiselsandbits.profiling;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import mod.chiselsandbits.api.profiling.IProfilerResult;

import net.minecraft.util.profiling.FilledProfileResults;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.util.profiling.ResultField;

public class CandBProfilingResult implements IProfilerResult {
	private final ProfileResults innerResult;

	public CandBProfilingResult(final ProfileResults innerResult) {this.innerResult = innerResult; }

	@Override
	public void writeToFile(final Path file) {
		innerResult.saveResults(file);
	}

	@Override
	public void writeAsResponse(final Consumer<String> lineConsumer) {
		if (!(innerResult instanceof final FilledProfileResults filledProfileResult)) {
			return;
		}

		lineConsumer.accept("Results:");
		writeDatapointsAsResponse("root", filledProfileResult::getTimes, lineConsumer, " ");
	}

	private void writeDatapointsAsResponse(final String name, final Function<String, List<ResultField>> producer, final Consumer<String> lineConsumer, final String indent) {
		final List<ResultField> dataPoints = producer.apply(name);

		dataPoints.forEach(
				dataPoint -> {
				lineConsumer.accept(String.format("%s> %s: %s (%s)", indent, dataPoint.name.substring(dataPoint.name.lastIndexOf('\u001e') + 1), dataPoint.percentage, dataPoint.globalPercentage));

				if (!dataPoint.name.equals(name)) {
					writeDatapointsAsResponse(name + '\u001e' + dataPoint.name, producer, lineConsumer, indent + " ");
			}
				}

		);
	}
}

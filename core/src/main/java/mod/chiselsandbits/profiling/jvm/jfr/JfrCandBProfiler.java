package mod.chiselsandbits.profiling.jvm.jfr;

import java.util.LinkedList;
import java.util.function.Supplier;

import jdk.jfr.FlightRecorder;
import mod.chiselsandbits.api.profiling.IProfilerResult;
import mod.chiselsandbits.profiling.CandBProfiler;
import mod.chiselsandbits.profiling.jvm.jfr.events.CandBProfilingFinishedEvent;
import mod.chiselsandbits.profiling.jvm.jfr.events.CandBProfilingSectionEvent;
import mod.chiselsandbits.profiling.jvm.jfr.events.CandBProfilingStartedEvent;
import mod.chiselsandbits.utils.StringUtils;

import net.minecraft.util.profiling.jfr.Environment;
import net.minecraft.util.profiling.jfr.JvmProfiler;

public class JfrCandBProfiler extends CandBProfiler {
	private final ThreadLocal<LinkedList<CandBProfilingSectionEvent>> eventStack = ThreadLocal.withInitial(LinkedList::new);
	private final ThreadLocal<LinkedList<String>> nameStack =  ThreadLocal.withInitial(LinkedList::new);

	public JfrCandBProfiler(final Environment profilingEnvironment)

	{
		super();

		FlightRecorder.register(CandBProfilingStartedEvent.class);
		FlightRecorder.register(CandBProfilingFinishedEvent.class);
		FlightRecorder.register(CandBProfilingSectionEvent.class);

		if (!JvmProfiler.INSTANCE.isRunning()) {
			JvmProfiler.INSTANCE.start(profilingEnvironment);
		}

		new CandBProfilingStartedEvent().commit();
	}

	@Override
	public void startSection(final String name)

	{
		onNewSectionWith(name);

		super.startSection(name);
	}

	@Override
	public void startSection(final Supplier<String> nameSupplier)

	{
		onNewSectionWith(nameSupplier.get());

		super.startSection(nameSupplier);
	}

	private void onNewSectionWith(final String name)

	{
		nameStack.get().addFirst(name);

		final String eventName = StringUtils.join(".", nameStack.get().descendingIterator());
		final CandBProfilingSectionEvent event = new CandBProfilingSectionEvent(eventName);
		event.begin();

		eventStack.get().addFirst(event);
	}

	@Override
	public void endSection()

	{
		super.endSection();

		final CandBProfilingSectionEvent event = eventStack.get().removeFirst();
		event.commit();

		nameStack.get().removeFirst();
	}

	@Override
	public IProfilerResult getResult()

	{
		new CandBProfilingFinishedEvent().commit();
		final IProfilerResult result = super.getResult();
		new CandBProfilingStartedEvent().commit();

		return result;
	}

	@Override
	public IProfilerResult stop()

	{
		new CandBProfilingFinishedEvent().commit();
		final IProfilerResult result = super.getResult();

		if (JvmProfiler.INSTANCE.isRunning()) {
			JvmProfiler.INSTANCE.stop();
		}

		return result;
	}
}

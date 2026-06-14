package mod.chiselsandbits.profiling.jvm.jfr.events;

import jdk.jfr.*;

@Name(CandBProfilingStartedEvent.NAME)
@Label("C&B Profiling Started")
@Category({"Minecraft", "C&B"})
public class CandBProfilingStartedEvent extends Event
{
	public static final String NAME = "minecraft.candb.Started";
	public static final EventType TYPE = EventType.getEventType(CandBProfilingStartedEvent.class);
}

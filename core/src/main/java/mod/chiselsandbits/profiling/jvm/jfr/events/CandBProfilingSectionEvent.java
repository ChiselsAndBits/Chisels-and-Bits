package mod.chiselsandbits.profiling.jvm.jfr.events;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Name(CandBProfilingSectionEvent.NAME)
@Label("C&B Profiling Phase Started")
@Category({"Minecraft", "C&B"})
@StackTrace(false)
public class CandBProfilingSectionEvent extends Event {
	public static final String NAME = "minecraft.candb.Section";

	@Name("name")
	@Label("Name")
	public String name;

	public CandBProfilingSectionEvent(final String name)

	{
		this.name = name;
	}
}

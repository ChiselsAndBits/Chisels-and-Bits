package mod.chiselsandbits.profiling.jvm.jfr.events;

import jdk.jfr.*;

@Name(CandBProfilingSectionEvent.NAME)
@Label("C&B Profiling Phase Started")
@Category({"Minecraft", "C&B"})
@StackTrace(false)
public class CandBProfilingSectionEvent extends Event
{
	public static final String NAME = "minecraft.candb.Section";

	@Name("name")
	@Label("Name")
	public String name;

	public CandBProfilingSectionEvent(final String name)
	{
		this.name = name;
	}
}

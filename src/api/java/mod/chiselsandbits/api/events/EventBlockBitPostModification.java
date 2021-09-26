package mod.chiselsandbits.api.events;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.Event;

public class EventBlockBitPostModification extends Event
{

	private final Level w;
	private final BlockPos pos;

	public EventBlockBitPostModification(
			final Level w,
			final BlockPos pos )
	{

		this.w = w;
		this.pos = pos;
	}

	public Level getWorld()
	{
		return w;
	}

	public BlockPos getPos()
	{
		return pos;
	}

}
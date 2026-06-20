package mod.chiselsandbits.change.changes;

import java.util.List;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mod.chiselsandbits.api.change.changes.IChange;
import mod.chiselsandbits.api.change.changes.IChangeType;
import mod.chiselsandbits.api.change.changes.IllegalChangeAttempt;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

public class CombinedChange implements IChange {
	public static final MapCodec<CombinedChange> MAP_CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					IChange.CODEC.listOf().fieldOf("changes").forGetter(CombinedChange::getChanges)
			).apply(instance, CombinedChange::new)
	);

	public static final StreamCodec<RegistryFriendlyByteBuf, CombinedChange> STREAM_CODEC = StreamCodec.composite(
			IChange.STREAM_CODEC.apply(ByteBufCodecs.list()),
			CombinedChange::getChanges,
			CombinedChange::new
	);

	private final List<IChange> changes;

	public CombinedChange(final List<IChange> changes) {this.changes = changes;}

	public List<IChange> getChanges() {
		return changes;
	}

	@Override
	public boolean canUndo(final Player player)

	{
		for (IChange change : changes)

		{
			if (!change.canUndo(player))

			{
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean canRedo(final Player player)

	{
		for (IChange change : changes)

		{
			if (!change.canRedo(player))

			{
				return false;
			}
		}

		return true;
	}

	@Override
	public void undo(final Player player) throws IllegalChangeAttempt

	{
		for (IChange change : changes)

		{
			change.undo(player);
		}
	}

	@Override
	public void redo(final Player player) throws IllegalChangeAttempt

	{
		for (IChange change : changes)

		{
			change.redo(player);
		}
	}

	@Override
	public IChangeType getType() {
		return ChangeType.COMBINED;
	}
}

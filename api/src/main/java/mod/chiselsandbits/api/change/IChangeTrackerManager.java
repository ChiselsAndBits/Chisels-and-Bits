package mod.chiselsandbits.api.change;

import mod.chiselsandbits.api.IChiselsAndBitsAPI;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * The central change tracker manager which gives access to each players individual change tracker if applicable.
 * This data is not synced.
 * On the client generally this data is only available for the current player, all other players return an empty change tracker.
 *
 * All change trackers are reset upon server restart or datapack reload.
 */
public interface IChangeTrackerManager {
	/**
	 * Short circuit method to get the manager instance from the api.
	 *
	 * @return The change tracker manager.
	 */
	@NotNull
	static IChangeTrackerManager getInstance() {
		return IChiselsAndBitsAPI.getInstance().getChangeTrackerManager();
	}

	/**
	 * Gives access to the change tracker of the given player.
	 *
	 * @param player The player in question.
	 * @return The change tracker for the given player.
	 */
	@NotNull
	IChangeTracker getChangeTracker(final Player player);
}

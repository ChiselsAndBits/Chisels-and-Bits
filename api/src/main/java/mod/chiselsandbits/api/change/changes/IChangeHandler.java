package mod.chiselsandbits.api.change.changes;

import net.minecraft.world.entity.player.Player;

/**
 * Represents an entry point for a change that can be undone and redone.
 */
public interface IChangeHandler {
	/**
	 * Checks if the change can still be undone.
	 *
	 * @param player The player for which the can undo check is performed.
	 * @return True when the change can be undone.
	 */
	boolean canUndo(Player player);

	/**
	 * Checks if the change can still be redone.
	 *
	 * @param player The player for which the can redo check is performed.
	 * @return True when the change can be redone.
	 */
	boolean canRedo(Player player);

	/**
	 * Undoes the change.
	 *
	 * @param player The player for which undoes the change.
	 * @throws IllegalChangeAttempt when the change can not be undone.
	 */
	void undo(Player player) throws IllegalChangeAttempt;

	/**
	 * Redoes the change.
	 *
	 * @param player The player for which redoes the change.
	 * @throws IllegalChangeAttempt when the change can not be redone.
	 */
	void redo(Player player) throws IllegalChangeAttempt;
}

package mod.chiselsandbits.api.change.changes;

/**
 * Exception thrown when change attempt is made before checking if the change attempt is possible.
 */
public class IllegalChangeAttempt extends Exception {
	/**
	 * Performs the illegal change attempt operation.
	 */
	public IllegalChangeAttempt() {
		super("Tried to make a change (either redo or undo) without checking for possibility primary!");
	}
}

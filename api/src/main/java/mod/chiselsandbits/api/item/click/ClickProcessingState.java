package mod.chiselsandbits.api.item.click;

/**
 * Represents the continuous processing state of a click interaction.
 */
public class ClickProcessingState {
	/**
	 * The click was successfully processed, and not further processing is needed.
	 */
	public static final ClickProcessingState ALLOW = new ClickProcessingState(true, ProcessingResult.ALLOW);
	/**
	 * The click was successfully processed, but the interactions processing should continue.
	 */
	public static final ClickProcessingState ALLOW_NO_CANCEL = new ClickProcessingState(false, ProcessingResult.ALLOW);
	/**
	 * The click was not processed successfully, and the interactions processing should stop.
	 */
	public static final ClickProcessingState DENIED = new ClickProcessingState(true, ProcessingResult.DENY);
	/**
	 * No processing of the click was done, and the interactions processing should continue.
	 */
	public static final ClickProcessingState DEFAULT = new ClickProcessingState(false, ProcessingResult.DEFAULT);

	private final boolean shouldCancel;
	private final ProcessingResult nextState;

	/**
	 * Creates a new processing state.
	 *
	 * @param shouldCancel Indicates if the state should cancel the interaction.
	 * @param nextState The next state to process.
	 */
	public ClickProcessingState(final boolean shouldCancel, final ProcessingResult nextState) {
		this.shouldCancel = shouldCancel;
		this.nextState = nextState;
	}

	/**
	 * Indicates if this state should cancel the interaction.
	 *
	 * @return {@code true} if the interaction should be cancelled, {@code false} otherwise.
	 */
	public boolean shouldCancel() {
		return shouldCancel;
	}

	/**
	 * Gets the next state to process.
	 *
	 * @return The next processing state.
	 */
	public ProcessingResult getNextState() {
		return nextState;
	}

	/**
	 * The processing result of the click interaction.
	 */
	public enum ProcessingResult

	{
		/**
		 * Deny the further processing of the interaction.
		 */
		DENY,
		/**
		 * Continue the further processing of the interaction.
		 * This state did not consume the interaction.
		 */
		DEFAULT,
		/**
		 * Continue the further processing of the interaction.
		 * This state consumed the interaction.
		 */
		ALLOW
	}
}

package mod.chiselsandbits.api.client.sharing;

import net.minecraft.network.chat.Component;

/**
 * I/O failure that carries both a user-facing component and a diagnostic log message.
 */
public class PatternIOException extends Exception {
	/**
	 * User-facing message shown when a pattern import or export fails.
	 */
	private final Component errorMessage;

	/**
	 * Creates a pattern I/O exception with a user message and a log message.
	 *
	 * @param errorMessage localized message suitable for displaying to the user
	 * @param logMessage diagnostic message for logs and the exception detail
	 */
	public PatternIOException(final Component errorMessage, final String logMessage) {
		super(logMessage);
		this.errorMessage = errorMessage;
	}

	/**
	 * Creates a pattern I/O exception with a user message, log message, and underlying cause.
	 *
	 * @param errorMessage localized message suitable for displaying to the user
	 * @param message diagnostic message for logs and the exception detail
	 * @param cause underlying I/O or parsing failure
	 */
	public PatternIOException(final Component errorMessage, final String message, final Throwable cause) {
		super(message, cause);
		this.errorMessage = errorMessage;
	}

	/**
	 * Returns the localized component intended to be shown to the user.
	 *
	 * @return the user-facing error component
	 */
	public Component getErrorMessage() {
		return errorMessage;
	}
}

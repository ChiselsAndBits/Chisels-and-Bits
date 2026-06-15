package mod.chiselsandbits.api.client.sharing;

import net.minecraft.network.chat.Component;

public class PatternIOException extends Exception
{
	private final Component errorMessage;

	public PatternIOException(final Component errorMessage, final String logMessage) {
		super(logMessage);
		this.errorMessage = errorMessage;
	}

	public PatternIOException(final Component errorMessage, final String message, final Throwable cause)

	{
		super(message, cause);
		this.errorMessage = errorMessage;
	}

	public Component getErrorMessage()

	{
		return errorMessage;
	}
}

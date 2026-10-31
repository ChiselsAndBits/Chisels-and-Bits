package mod.chiselsandbits.api.exceptions;

/**
 * Thrown When a block cannot hold chiseled bits, or be converted to a chiseled block.
 */
public class CannotBeChiseledException extends Exception {
	/**
	 * Creates an exception with no message, indicating that the target block cannot hold chiseled
	 * bits or be converted to a chiseled block.
	 */
	public CannotBeChiseledException() { }

	private static final long serialVersionUID = -2164869172949828669L;
}

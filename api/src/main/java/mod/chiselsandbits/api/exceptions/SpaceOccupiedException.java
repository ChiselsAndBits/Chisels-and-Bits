package mod.chiselsandbits.api.exceptions;

/**
 * Thrown when a bit cannot be placed in that space, this is not caused by bits already occupying that space, but by multiparts.
 */
public class SpaceOccupiedException extends Exception
{
	private static final long serialVersionUID = -5620527137925750306L;
}

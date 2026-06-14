package mod.chiselsandbits.api.placement;

import com.communi.suggestu.scena.core.dist.DistExecutor;
import mod.chiselsandbits.api.config.IClientConfiguration;
import net.minecraft.network.chat.Component;
import org.joml.Vector4f;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Represents a result of a placement attempt.
 */
public class PlacementResult
{
	private static final Vector4f ZERO = new Vector4f(0,0,0,0);

	/**
     * Creates a new successful placement result with client config specified color if clientside.
     *
     * @return A new result indicating successful placement.
     */
	public static PlacementResult success()
	{
		return success(IClientConfiguration::getSuccessfulPlacementColor);
	}

	/**
     * Creates a new successful placement result with specified client config color if clientside.
     *
     * @param clientColor The client config specified color of the ghost or wireframe to indicate the failure type.
     * @return A new result indicating successful placement.
     */
	public static PlacementResult success(final Function<IClientConfiguration, Supplier<Vector4f>> clientColor)
	{
		return new PlacementResult(true, clientColor, Component.empty());
	}

	/**
     * Creates a new failed placement result with specified client config color if clientside.
     *
     * @param clientColor The client config specified color of the ghost or wireframe to indicate the failure type.
     * @return A new result indicating failed placement.
     */
	public static PlacementResult failure(final Function<IClientConfiguration, Supplier<Vector4f>> clientColor)
	{
		return failure(clientColor, Component.empty());
	}

	/**
     * Creates a new failed placement result with specified failure message and client config color if clientside.
     *
     * @param clientColor The client config specified color of the ghost or wireframe to indicate the failure type.
     * @param message The message to show to the user.
     * @return A new result indicating failed placement.
     */
	public static PlacementResult failure(final Function<IClientConfiguration, Supplier<Vector4f>> clientColor, final Component message)
	{
		return new PlacementResult(false, clientColor, message);
	}

	/**
     * Creates a new successful placement result with specified color.
     *
     * @param color The color of the ghost or wireframe to indicate the success type.
     * @return A new result indicating successful placement.
     */
	public static PlacementResult success(final Vector4f color)
	{
		return new PlacementResult(true, color, Component.empty());
	}

	/**
     * Creates a new failed placement result with specified color.
     *
     * @param color The color of the ghost or wireframe to indicate the failure type.
     * @return A new result indicating failed placement.
     */
	public static PlacementResult failure(final Vector4f color)
	{
		return failure(color, Component.empty());
	}

	/**
     * Creates a new failed placement result with specified color and failure message.
     *
     * @param color The color of the ghost or wireframe to indicate the failure type.
     * @param message The message to show to the user.
     * @return A new result indicating failed placement.
     */
	public static PlacementResult failure(final Vector4f color, final Component message)
	{
		return new PlacementResult(false, color, message);
	}

	private final boolean success;
	private final Vector4f color;
	private final Component failureMessage;

	private PlacementResult(final boolean success, final Function<IClientConfiguration, Supplier<Vector4f>> clientColor, final Component failureMessage)
	{
		this(success,
				DistExecutor.unsafeRunForDist(
						() -> clientColor.apply(IClientConfiguration.getInstance()),
						() -> () -> ZERO),
				failureMessage);
	}

	private PlacementResult(final boolean success, final Vector4f color, final Component failureMessage)
	{
		this.success = success;
		this.color = color;
		this.failureMessage = failureMessage;
	}

	public boolean isSuccess()
	{
		return success;
	}

	public Vector4f getColor()
	{
		return color;
	}

	public Component getFailureMessage()
	{
		return failureMessage;
	}
}

package mod.chiselsandbits.api.util;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * An object with a color associated with it.
 */
public interface IWithColor
{
	/**
     * The color used to render.
     *
     * @return The color in a 3d double vector as RGB.
     */
	@NotNull Vec3 getColorVector();

	/**
     * The alpha channel intensity to render with.
     * By default, this is 1.0.
     *
     * @return The alpha channel intensity.
     */
	default double getAlphaChannel() {
		return 1d;
	}
}

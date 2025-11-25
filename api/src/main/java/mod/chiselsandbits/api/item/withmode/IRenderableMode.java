package mod.chiselsandbits.api.item.withmode;

import mod.chiselsandbits.api.util.IWithColor;
import mod.chiselsandbits.api.util.IWithDisplayName;
import mod.chiselsandbits.api.util.IWithIcon;
import mod.chiselsandbits.api.util.IWithIconAndColor;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a tool mode which can be rendered.
 */
public interface IRenderableMode extends IWithDisplayName, IWithIconAndColor
{

    /**
     * Indicates if the mode is currently active and as such should be rendered or not.
     *
     * @return True for active modes, false for not.
     */
    default boolean isActive() {
        return true;
    }

    /**
     * Indicates if the name of the mode should be rendered.
     *
     * @return {@code true} when the name should be rendered.
     */
    default boolean shouldRenderName() {
        return true;
    }

    /**
     * Indicates if this mode should render his name in the menu.
     *
     * @return {@code true} when then name should be rendered in the menu.
     */
    default boolean shouldRenderDisplayNameInMenu() {
        return true;
    }

    @Override
    @NotNull
    default Vec3 getColorVector() {
        return new Vec3(1d, 1d,1d);
    }

    default Vec2 getPositionVector() {
        return new Vec2(-0.1f, -0.1f);
    }

    default Vec2 getScaleVector() {
        return new Vec2(0.45f, 0.45f);
    }
}

package mod.chiselsandbits.api.notifications;

import mod.chiselsandbits.api.IChiselsAndBitsAPI;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Manager which handles notifying players of events in the game.
 */
public interface INotificationManager
{
    /**
     * The current instance of the notification manager.
     *
     * @return The notification manager.
     */
    public static INotificationManager getInstance() {
        return IChiselsAndBitsAPI.getInstance().getNotificationManager();
    }

    /**
     * Notifies the player with a simple message, icon and color.
     *
     * @param icon The icon to display.
     * @param color The color of the icon.
     * @param message The message to display.
     */
    default void notify(final Identifier icon, final Vec3 color, final Component message) {
        notify(new INotification() {
            @Override
            public @NotNull Vec3 getColorVector()
            {
                return color;
            }

            @Override
            public Identifier getIcon()
            {
                return icon;
            }

            @Override
            public @NotNull Component getText()
            {
                return message;
            }
        });
    }

    /**
     * Notifies the player with a simple message and icon.
     *
     * @param icon The icon to display.
     * @param message The message to display.
     */
    default void notify(final Identifier icon, final Component message) {
        notify(icon, new Vec3(1, 1, 1), message);
    }

    /**
     * Notifies the player with the given notification.
     *
     * @param notification The notification to display.
     */
    void notify(final INotification notification);
}

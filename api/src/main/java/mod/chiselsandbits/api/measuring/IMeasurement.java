package mod.chiselsandbits.api.measuring;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Represents a measurement made in a given world by a given player.
 */
public interface IMeasurement
{
    /**
     * The id of the player who made the measurement.
     *
     * @return The id of the owner.
     */
    UUID getOwner();

    /**
     * The lower left front corner of the measurement.
     *
     * @return The start point of the measurement.
     */
    Vec3 getFrom();

    /**
     * The top right back corner of the measurement.
     *
     * @return The end point of the measurement.
     */
    Vec3 getTo();

    /**
     * The size of the measurement.
     *
     * @return The size of the measurement.
     */
    default Vec3 getSize() {
        return getTo().subtract(getFrom());
    }

    /**
     * The mode of the measurement.
     *
     * @return The mode of the measurement.
     */
    IMeasuringMode getMode();

    /**
     * The id of the world that this measurement was made in.
     *
     * @return The id of the world.
     */
    ResourceLocation getWorldKey();
}

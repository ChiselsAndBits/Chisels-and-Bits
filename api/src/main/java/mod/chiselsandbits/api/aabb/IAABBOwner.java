package mod.chiselsandbits.api.aabb;

import net.minecraft.world.phys.AABB;

/**
 * Defines objects which have an axis-aligned bounding box.
 */
public interface IAABBOwner {
	/**
	 * Gives access to the bounding box of this object.
	 *
	 * @return The axis aligned bounding box.
	 */
	AABB getBoundingBox();
}

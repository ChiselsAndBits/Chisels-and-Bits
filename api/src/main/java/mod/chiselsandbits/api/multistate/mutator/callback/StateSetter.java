package mod.chiselsandbits.api.multistate.mutator.callback;

import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.exceptions.SpaceOccupiedException;
import net.minecraft.world.phys.Vec3;

/**
 * Functional callback interface for setting the state of a bit.
 */
@FunctionalInterface
public interface StateSetter
{
	/**
     * Sets the block information of the targeted bit to the given block information.
     *
     * @param blockInformation The block information to set the bit to.
     * @param inAreaTarget The target area.
     * @throws SpaceOccupiedException If the target area is occupied.
     */
	void set(BlockInformation blockInformation, Vec3 inAreaTarget) throws SpaceOccupiedException;
}

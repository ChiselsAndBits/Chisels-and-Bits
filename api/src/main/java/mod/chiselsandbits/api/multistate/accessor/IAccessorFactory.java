package mod.chiselsandbits.api.multistate.accessor;

import mod.chiselsandbits.api.IChiselsAndBitsAPI;
import mod.chiselsandbits.api.multistate.accessor.world.IWorldAreaAccessor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

/**
 * Allows for the creation of new accessors used to access chiselable areas.
 */
public interface IAccessorFactory {
	/**
	 * Gives access to the accessor factory.
	 * @return The accessor factory.
	 */
	static IAccessorFactory getInstance() {
		return IChiselsAndBitsAPI.getInstance().getAccessorFactory();
	}

	/**
	 * Creates a accessor which mutates a particular block only.
	 *
	 * @param world The world to mutate in.
	 * @param pos The position to mutate.
	 * @return The accessor.
	 */
	IWorldAreaAccessor in(
			final LevelAccessor world,
			final BlockPos pos
	);

	/**
	 * Creates an accessor which mutates a given area.
	 *
	 * @param world The world to mutate in.
	 * @param from The block to function as a start point.
	 * @param to The block to function as an end point.
	 * @return The accessor.
	 */
	IWorldAreaAccessor covering(
			final LevelAccessor world,
			final BlockPos from,
			final BlockPos to
	);

	/**
	 * Creates an accessor which mutates a given area.
	 *
	 * @param world The world to mutate in.
	 * @param from The start point.
	 * @param to The end point.
	 * @return The accessor.
	 */
	IWorldAreaAccessor covering(
			final LevelAccessor world,
			final Vec3 from,
			final Vec3 to
	);
}

package mod.chiselsandbits.api.multistate.mutator.callback;

import net.minecraft.world.phys.Vec3;

/**
 * Defines state clearer.
 */

@FunctionalInterface
public interface StateClearer {
	/**
	 * Applies the callback to the supplied position.
	 *
	 * @param pos position to inspect.
	 */
	void accept(Vec3 pos);
}

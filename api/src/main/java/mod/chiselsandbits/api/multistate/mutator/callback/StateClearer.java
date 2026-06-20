package mod.chiselsandbits.api.multistate.mutator.callback;

import net.minecraft.world.phys.Vec3;

@FunctionalInterface
public interface StateClearer {
	void accept(Vec3 pos);
}

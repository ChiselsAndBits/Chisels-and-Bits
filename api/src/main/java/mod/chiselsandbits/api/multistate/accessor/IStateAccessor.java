package mod.chiselsandbits.api.multistate.accessor;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public interface IStateAccessor {
	/**
	 * Gets the target state in the current area, using the offset from the area as well as the in area target offset.
	 * Note if this accessor potentially targets more than 1 block position (even if it does not in reality the potential is what matters here)
	 * you will need to pass in the world position exactly.
	 *
	 * <p>
	 * If this accessor can not potentially ever target more than 1 block position, you can pass in the relative position.
	 * This is a nasty implementation detail, and matters most when you are making new chisel or pattern placement modes!
	 *
	 * @param inAreaTarget The in area offset.
	 * @return An optional potentially containing the state entry of the requested target.
	 */
	Optional<IStateEntryInfo> getInAreaTarget(
			Vec3 inAreaTarget
	);

	/**
	 * Gets the target state in the current area, using the in area block position offset as well as the in block target offset to calculate the in area offset for setting.
	 * Note if this accessor potentially targets more than 1 block position (even if it does not in reality the potential is what matters here)
	 * you will need to pass in the world position exactly.
	 *
	 * <p>
	 * If this accessor can not potentially ever target more than 1 block position, you can pass in the relative position.
	 * This is a nasty implementation detail, and matters most when you are making new chisel or pattern placement modes!
	 *
	 * @param inAreaBlockPosOffset The offset of blocks in the current area.
	 * @param inBlockTarget The offset in the targeted block.
	 * @return An optional potentially containing the state entry of the requested target.
	 */
	Optional<IStateEntryInfo> getInBlockTarget(
			BlockPos inAreaBlockPosOffset,
			Vec3 inBlockTarget
	);
}

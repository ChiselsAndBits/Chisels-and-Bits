package mod.chiselsandbits.api.multistate.statistics;

import java.util.Map;
import java.util.Set;

import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.item.multistate.IStatistics;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.AirBlock;

/**
 * The statistics of a multistate block.
 */
public interface IMultiStateObjectStatistics extends IStatistics {
	/**
	 * The primary blockstate.
	 * If the multistate block is empty, then {@link AirBlock#defaultBlockState()} is returned.
	 * Generally speaking this is the blockstate that occurs the most often
	 * in the multistate block.
	 * But this is not guaranteed.
	 *
	 * @return The primary block state of the multi state block.
	 */
	BlockInformation getPrimaryState();

	/**
	 * An immutable copy of the map that contains the counts of all blockstates in the multistate block.
	 * @return The blockstate count map.
	 */
	Map<BlockInformation, Integer> getStateCounts();

	/**
	 * Returns all states (without count) that are contained in the object.
	 *
	 * @return All states in the object.
	 */
	@Override
	default Set<BlockInformation> getContainedStates() {
		return getStateCounts().keySet();
	}

	/**
	 * Indicates if the block that this statistics belongs to should check for weak power.
	 *
	 * @return Indicates if this block should check for weak power.
	 */
	boolean shouldCheckWeakPower();

	/**
	 * Gives access to the fullness factor of the current block.
	 *
	 * @return The fullness factor.
	 */
	float getFullnessFactor();

	/**
	 * Indicates if the current block is full.
	 * Generally coincides with {@link #getFullnessFactor()} being equal to {@code 1f}.
	 *
	 * @return Indicates if this block is full or not.
	 */
	default boolean isFullBlock() {
		return getFullnessFactor() == 1f;
	}

	/**
	 * Indicates the slipperiness of the current block.
	 *
	 * @return The slipperiness.
	 */
	float getSlipperiness();

	/**
	 * The factor of how much light is emitted by the block.
	 *
	 * @return A factor of how much light is emitted by the block, between 0 and 1.
	 */
	float getLightEmissionFactor();

	/**
	 * The factor with which the bits block light.
	 *
	 * @return A factor of how much light is blocked by the block, between 0 and 1.
	 */
	float getLightBlockingFactor();

	/**
	 * Indicates the relative block hardness for a given player.
	 *
	 * @param player The players to get the hardness for.
	 *
	 * @return The relative block hardness.
	 */
	float getRelativeBlockHardness(Player player);

	/**
	 * Indicates if the current multistate block is empty.
	 *
	 * @return The current multistate block.
	 */
	default boolean isEmptyBlock() {
		return getFullnessFactor() == 0f;
	}

	/**
	 * Indicates if the current multistate block can propagate skylight.
	 *
	 * @return True when the block can propagate skylight, false when not.
	 */
	boolean canPropagateSkylight();

	/**
	 * Indicates if the current multistate block can sustain grass below.
	 *
	 * @return {@code true} for grass sustainers, {@code false} for non-grass sustainers.
	 */
	boolean canSustainGrassBelow();
}

package mod.chiselsandbits.api.config;

import java.util.List;
import java.util.function.Supplier;

import mod.chiselsandbits.api.multistate.StateEntrySize;

/**
 * Gives access to the current server's configuration.Elements in this configuration are relevant for both the server and client side of C{@literal &}B.Since this options
 * influence gameplay mechanics they need to be kept in sync.
 */
public interface IServerConfiguration {
	/**
	 * Gives access to the current server's configuration.Elements in this configuration are relevant for both the server and client side of C{@literal &}B.Since this options
	 * influence gameplay mechanics they need to be kept in sync.
	 *
	 * @return The server configuration.
	 */
	static IServerConfiguration getInstance() {
		return IChiselsAndBitsConfiguration.getInstance().getServer();
	}

	/**
	 * Determines if random ticking blocks like grass or others should be eligible for chiselability.
	 *
	 * @return A supplier that determines if random ticking blocks should be chiselable.
	 */
	Supplier<Boolean> getBlackListRandomTickingBlocks();

	/**
	 * Determines if the eligibility compatibility mode is active or not.
	 *
	 * @return A supplier that determines if the compatibility mode is active or not.
	 */
	Supplier<Boolean> getCompatabilityMode();

	/**
	 * The size of the bit stack in a bit bag.
	 *
	 * @return A supplier that determines the size of the bit stack.
	 */
	Supplier<Integer> getBagStackSize();

	/**
	 * The size of a bit in the world.
	 *
	 * @return A supplier that determines the size of a bit in the world.
	 */
	Supplier<StateEntrySize> getBitSize();

	/**
	 * Ths size of the change tracker, aka how much the user can undo.
	 *
	 * @return A supplier that determines the size of the change tracker.
	 */
	Supplier<Integer> getChangeTrackerSize();

	/**
	 * Indicates if Chisels and Bits will delete excess bits when a block is broken.
	 *
	 * @return A supplier that determines if excess bits should be deleted.#
	 */
	Supplier<Boolean> getDeleteExcessBits();

	/**
	 * Provides access to the factor with which the bit light strength is multiplied.
	 *
	 * @return A supplier that determines the factor with which the bit light strength is multiplied.
	 */
	Supplier<Double> getLightFactorMultiplier();

	/**
	 * Indicates if the player should be required to have a chisel in their offhand to break bits with a bit block.
	 *
	 * @return A supplier that determines if the player should be required to have a chisel in their offhand to break bits.
	 */
	Supplier<Boolean> getRequireChiselInOffHandForBitBreaking();
}

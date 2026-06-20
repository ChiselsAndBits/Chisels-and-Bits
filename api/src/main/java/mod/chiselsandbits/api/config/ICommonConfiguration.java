package mod.chiselsandbits.api.config;

import java.util.function.Supplier;

public interface ICommonConfiguration {
	/**
	 * The common configuration.
	 * Elements in this configuration are relevant for both the server and the client side of C{@literal &}B.
	 * This configuration does not need to be in-sync with the server values.
	 *
	 * @return The common configuration.
	 */
	static ICommonConfiguration getInstance() {
		return IChiselsAndBitsConfiguration.getInstance().getCommon();
	}

	/**
	 * Indicates if the help tooltips should be enabled or not.
	 *
	 * @return A supplier that can indicate of the help tooltips should be shown or not.
	 */
	Supplier<Boolean> getEnableHelp();

	/**
	 * Determines the size of the collision box cache size.
	 *
	 * @return A supplier that determines the size of the collision box cache size.
	 */
	Supplier<Long> getCollisionBoxCacheSize();

	/**
	 * Determines the size of caches which hold class metadata.
	 *
	 * @return The class metadata cache size.
	 */
	Supplier<Long> getClassMetadataCacheSize();

	/**
	 * Indicates how many threads can be used to calculate and compress off-thread NBT data for saving.
	 * Loading always happens on thread.
	 *
	 * @return A supplier that determines the number of threads to use for block saving.
	 */
	Supplier<Integer> getBlockSaveThreadCount();
}

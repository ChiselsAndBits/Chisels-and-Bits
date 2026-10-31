package mod.chiselsandbits.api.chiseling;

import java.util.Optional;

import mod.chiselsandbits.api.IChiselsAndBitsAPI;

/**
 * This small cache is used on the client side, primarily, to store a single active context
 * which is valid across multiple frames.
 */
public interface ILocalChiselingContextCache {
	/**
	 * Gives access to the instance.
	 *
	 * @return The instance.
	 */
	static ILocalChiselingContextCache getInstance() {
		return IChiselsAndBitsAPI.getInstance().getLocalChiselingContextCache();
	}

	/**
	 * The current active context for the last few frames.
	 * The caller needs to validate that the returned value is still valid for his usage.
	 *
	 * @param operation The operation to get the context for from the cache.
	 * @return An optional with the last current active instance.
	 */
	Optional<IChiselingContext> get(ChiselingOperation operation);

	/**
	 * Sets the current context in the cache.
	 *
	 * @param operation The operation to get the context for from the cache.
	 * @param context The new cached context.
	 */
	void set(ChiselingOperation operation, IChiselingContext context);

	/**
	 * Clears the current context.
	 *
	 * @param operation The operation to get the context for from the cache.
	 */
	void clear(ChiselingOperation operation);
}

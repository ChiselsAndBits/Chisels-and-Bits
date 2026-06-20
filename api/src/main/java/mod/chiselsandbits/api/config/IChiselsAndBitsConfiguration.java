package mod.chiselsandbits.api.config;

import mod.chiselsandbits.api.IChiselsAndBitsAPI;

/**
 * Represents the configuration of chisels and bits.
 */
public interface IChiselsAndBitsConfiguration {
	/**
     * Gives access to the current configuration of C{@literal &}B.
     * @return The current configuration.
     */
	static IChiselsAndBitsConfiguration getInstance() {
		return IChiselsAndBitsAPI.getInstance().getConfiguration();
	}

	/**
     * The client configuration.
     * Elements in this configuration are only relevant for the client side of C{@literal &}B.
     * This configuration does not need to be in-sync with the server values.
     *
     * @return The client configuration.
     */
	IClientConfiguration getClient();

	/**
     * The common configuration.
     * Elements in this configuration are relevant for both the server and the client side of C{@literal &}B.
     * This configuration does not need to be in-sync with the server values.
     *
     * @return The common configuration.
     */
	ICommonConfiguration getCommon();

	/**
     * Gives access to the current server's configuration.
     * Elements in this configuration are relevant for both the server and client side of C{@literal &}B.
     * Since this options influence gameplay mechanics they need to be kept in sync.
     *
     * @return The server configuration.
     */
	IServerConfiguration getServer();
}

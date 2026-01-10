package mod.chiselsandbits.api.plugin;

/**
 * Represents a plugin for ChiselsAndBits.
 * <p>
 *     Plugins have callbacks that can be invoked by chisels and bits.
 *     See their documentation for more information.
 * </p>
 * <p>
 *     All methods are potentially invoked in parallel with other plugins, or even chisels and bits itself.
 * </p>
 * <p>
 *     Plugins are loaded through the {@link java.util.ServiceLoader}. If annotated by {@link ChiselsAndBitsPlugin}
 *     then additional metadata is checked, like required mods or experimental flags.
 * </p>
 */
public interface IChiselsAndBitsPlugin
{
    /**
     * The id of the plugin.
     * Has to be unique over all plugins.
     *
     * @return The id.
     */
    String getId();

    /**
     * Invoked when the plugin is constructed.
     */
    default void onConstruction() {}

    /**
     * Called after ChiselsAndBits client construction completes.
     */
    default void onClientConstruction() {}

    /**
     * Invoked by chisels and bits when the platform it runs on (so forge or fabric) indicates that mod initialization should happen.
     */
    default void onInitialize() {}
}

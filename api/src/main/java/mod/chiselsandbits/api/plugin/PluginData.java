package mod.chiselsandbits.api.plugin;

/**
 * Pairs a discovered plugin instance with its service metadata.
 *
 * @param <T> plugin implementation type
 * @param plugin discovered plugin instance
 */
public record PluginData<T>(T plugin) {
}

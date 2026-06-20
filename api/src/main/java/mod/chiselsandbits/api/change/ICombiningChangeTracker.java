package mod.chiselsandbits.api.change;

/**
 * Represents a change tracker which is used to combine several change steps into one action.
 * Use with a try-with-resources block.
 */
public interface ICombiningChangeTracker extends IChangeTracker, AutoCloseable {
}

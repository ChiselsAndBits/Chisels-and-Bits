package mod.chiselsandbits.api.util;

/**
 * Defines an object which is capable of making a deep clone of itself.
 * @param <T> The type of the object.
 */
public interface ISnapshotable<T> {
	T createSnapshot();
}

package mod.chiselsandbits.api.util;

/**
 * Defines an object which is capable of making a deep clone of itself.
 *
 * @param <T> The type of the object.
 */
public interface ISnapshotable<T> {
	/**
	 * Creates and returns a deep, independent clone of this object.
	 *
	 * @return an independent snapshot of this object
	 */
	T createSnapshot();
}

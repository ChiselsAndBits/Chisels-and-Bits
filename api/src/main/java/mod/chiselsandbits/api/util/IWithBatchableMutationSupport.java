package mod.chiselsandbits.api.util;

/**
 * Defines an object which supports batch mutations.
 */
public interface IWithBatchableMutationSupport {
	/**
     * Trigger a batch mutation start.
     * @return The batch mutation lock.
     */
	IBatchMutation batch();
}

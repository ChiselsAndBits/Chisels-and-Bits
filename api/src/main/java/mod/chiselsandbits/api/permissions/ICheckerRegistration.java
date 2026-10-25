package mod.chiselsandbits.api.permissions;

/**
 * A marker interface which can be used to dynamically register and un-register
 * a {@link IPermissionChecker} to the {@link IPermissionHandler}.
 *
 * <p>
 * If the {@link #close()} is called then the {@link IPermissionChecker} will be
 * unregistered from the {@link IPermissionHandler}.
 */
public interface ICheckerRegistration extends AutoCloseable {
}

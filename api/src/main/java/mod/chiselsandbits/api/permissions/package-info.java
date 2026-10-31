/**
 * Permission checks for player edits to world areas.
 *
 * <p>{@link mod.chiselsandbits.api.permissions.IPermissionHandler} coordinates
 * registered checkers; an edit is denied if any checker rejects it.
 * Registration handles allow checkers to be unregistered when closed.
 */
package mod.chiselsandbits.api.permissions;

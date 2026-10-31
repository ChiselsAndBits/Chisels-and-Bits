/**
 * Discovery, metadata, and lifecycle callbacks for integration plugins.
 *
 * <p>Implement {@link mod.chiselsandbits.api.plugin.IChiselsAndBitsPlugin}
 * and expose it through {@link java.util.ServiceLoader}. The optional
 * {@link mod.chiselsandbits.api.plugin.ChiselsAndBitsPlugin} annotation supplies
 * additional metadata. Plugin callbacks may run in parallel.
 */
package mod.chiselsandbits.api.plugin;

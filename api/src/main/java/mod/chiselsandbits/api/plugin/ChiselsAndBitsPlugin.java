package mod.chiselsandbits.api.plugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation which marks a given class as being a chisels and bits plugin.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ChiselsAndBitsPlugin
{
    /**
     * Mod ids of the mods required to load this plugin.
     * Leave on the default empty to not care for mod specific filtering.
     *
     * @return The ids of the mods required to load.
     */
    String[] requiredMods() default {};

    /**
     * Indicator for an experimental plugin.
     * Is only loaded when the user enables experimental plugins in the config.
     * <p>
     * Experimental plugins get constructed if their mods list matches, however their
     * initialization methods are never called!
     *
     * @return True for an experimental plugin.
     */
    boolean isExperimental() default false;
}

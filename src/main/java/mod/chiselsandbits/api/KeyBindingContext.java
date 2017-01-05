package mod.chiselsandbits.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The key bindings for the following operations are only active when holding C&B items:
 * 
 * - "chisel" - Setting the chisel mode
 * - "postivepattern" - Setting the positive pattern mode
 * - "tapemeasure" - Setting the tape measure mode
 * - "rotateable" - Rotating blocks
 * - "menuitem" - Opening the radial menu
 * 
 * If you put this annotation on a class that extends Item, you can allow C&B to bypass
 * the normal activity checks when holding an item that is an instance of that class. If 
 * you include and set true the optional applyToSubClasses argument in the annotation or
 * use an IMC, this will apply not only to any item that is of that class, but also to
 * any class that extends that class.
 * 
 * 
 * If use any annotation with applyToSubClasses set to true, you need to send the
 * following IMC (of any type - String type is used below) after you register your items
 * to find/register/initialize those classes:
 * 
 * FMLInterModComms.sendMessage( "chiselsandbits", "initkeybindingannotations", "" );
 * 
 * Doing so will not only find and initialize classes of registered items, but also any
 * base classes that registered items may extend, but are never directly instantiated.
 * 
 * 
 * 
 * ~Example 1~
 * Putting the following annotation on an item class will allow the key bindings for
 * chisel modes to be active when holding an item of that class:
 * 
 * @KeyBindingContext( chisel )
 * 
 * The following two IMCs would do the same for item(s) of that class or of any subclass:
 * 
 * FMLInterModComms.sendMessage( "chiselsandbits", "chisel", [myItemName] );
 * 
 * 
 * 
 * ~Example 2~
 * The following IMC and IMC/annotation set will both allow the key binding for chisel
 * modes to be active when holding an item of that class or of any subclass:
 * 
 * @KeyBindingContext( value = { "chisel", "menuitem" }, applyToSubClasses = true )
 * AND
 * FMLInterModComms.sendMessage( "chiselsandbits", "initkeybindingannotations", "" );
 * 
 * OR
 * 
 * FMLInterModComms.sendMessage( "chiselsandbits", "chisel", [myItemName] );
 * FMLInterModComms.sendMessage( "chiselsandbits", "menuitem", [myItemName] );
 * 
 * 
 */
@Retention( RetentionPolicy.RUNTIME )
public @interface KeyBindingContext
{
	/**
     * A list of contexts that will allow all key bindings that use them to be active
     * when holding an item of a class (or a subclass, if applyToSubClasses is true)
     * with this annotation.
     * 
     * @return a list of key bindings contexts
     */
	String[] value ();

	/**
     * If true, the key binding context activity check will be bypassed not only for items
     * of a class with this annotation, but also to items of any class that extends it.
     * 
     * This argument is optional.
     * 
     * @return whether or not this annotation applies to subclasses
     */
	boolean applyToSubClasses() default false;
}

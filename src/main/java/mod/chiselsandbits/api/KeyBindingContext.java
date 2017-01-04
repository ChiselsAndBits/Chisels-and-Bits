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
 * you instead use the IMC below, this will apply not only to any item that is of that
 * class, but also to any class that extends that class.
 * 
 * For example, putting this on an item class will allow the key bindings for chisel modes
 * and the key bind for opening the radial menu to be active when holding your item(s):
 * 
 * @KeyBindingContext( { chisel, menuitem } )
 * 
 * Using the same example, the following two IMCs would accomplish the same result:
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
     * when holding an instance of the item class with this annotation.
     * @return a list of key bindings contexts
     */
	String[] value ();
}

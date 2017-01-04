package mod.chiselsandbits.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * When checking for blocks to allow for chiseling C&B checks various methods...
 *
 * hasTileEntity, getTickRandomly, quantityDropped, quantityDroppedWithBonus,
 * onEntityCollidedWithBlock, and isFullBlock
 *
 * If you include this annotation or use the IMC below, you can force C&B to
 * overlook these custom implementations, please use with care and test before
 * releasing usage.
 *
 * Put this on the block, or use the IMC,
 *
 * FMLInterModComms.sendMessage( "chiselsandbits", "ignoreblocklogic",
 * [myBlockName] );
 * 
 * If you wish to make a single state compatible, or incompatible you must use
 * "forcestatecompatibility" instead, if your entire block is intended to be
 * compatible use the above option stead, this should only be used for state
 * specific changes.
 * 
 * 
 * FMLInterModComms.sendFunctionMessage( MODID, "forcestatecompatibility",
 * CompatTest.class.getName() ); }
 * 
 * @SuppressWarnings( "rawtypes" ) public static class CompatTest implements
 * Function<List, Boolean> {
 * 
 * //Add IBlockState to input LIST, and return true to whitelist, or return
 * false to blacklist.
 * 
 * @Override public Boolean apply( final List input ) { input.add(
 *           MyBlocks.MYBLOCK.getDefaultState() ); return true; }
 * 
 *           };
 * 
 * 
 */
@Retention( RetentionPolicy.RUNTIME )
public @interface IgnoreBlockLogic
{

}

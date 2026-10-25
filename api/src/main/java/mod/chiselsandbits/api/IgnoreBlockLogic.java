package mod.chiselsandbits.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * When checking for blocks to allow for chiseling Chisels and Bits checks various methods...
 *
 * <p>
 * hasTileEntity, getTickRandomly, quantityDropped, quantityDroppedWithBonus,
 * onEntityCollidedWithBlock, and isFullBlock
 *
 * <p>
 * If you include this annotation or use the tag, you can force Chisels and Bits to
 * overlook these custom implementations, please use with care and test before
 * releasing usage.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface IgnoreBlockLogic {
}

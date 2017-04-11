package mod.chiselsandbits.api;

import javax.annotation.Nullable;

import mod.chiselsandbits.api.APIExceptions.SpaceOccupied;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/**
 * Do not implement, acquire from {@link IChiselAndBitsAPI}
 */
public interface IBitAccess
{

	/**
	 * Request a summary of the contents of a range of bits, all range values
	 * are clamped to 0 - 15.
	 * 
	 * @param a
	 * @param b
	 * @return summary of the contents of the range.
	 */
	BitQueryResults queryBitRange(
			BlockPos a,
			BlockPos b );

	/**
	 * Process each bit in the {@link IBitAccess} and return a new bit in its
	 * place, can be used to optimize large changes, or iteration.
	 *
	 * @param visitor
	 */
	void visitBits(
			IBitVisitor visitor );

	/**
	 * Returns the bit at the specific location, this should always return a
	 * valid IBitBrush, never null.
	 *
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	IBitBrush getBitAt(
			int x,
			int y,
			int z );

	/**
	 * Sets the bit at the specific location, if you pass null it will use air.
	 *
	 * @param x
	 * @param y
	 * @param z
	 * @param bit
	 * @throws SpaceOccupied
	 */
	void setBitAt(
			int x,
			int y,
			int z,
			IBitBrush bit ) throws SpaceOccupied;

	/**
	 * Any time you modify a block you must commit your changes for them to take
	 * affect, optionally you can trigger updates or not.
	 *
	 * If the {@link IBitAccess} is not in the world this method does nothing.
	 * 
	 * All changes made by a player should be committed on the client and the
	 * server, failure to commit changes on the client will cause corruption of
	 * the Undo Pipeline, causing 'Block Has Changed' errors when trying to undo
	 * blocks that have been modified only on the server, doing so also
	 * increases responsiveness in those changes for the player making them.
	 *
	 * @param triggerUpdates
	 *            normally true, only use false if your doing something special.
	 */
	void commitChanges(
			boolean triggerUpdates );

	/**
	 * Returns an ItemStack for the {@link IBitAccess}
	 *
	 * Usable for any {@link IBitAccess}
	 *
	 * @param side
	 *            angle the player is looking at, can be null.
	 * @param type
	 *            what type of item to give.
	 * @param crossWorld
	 *            determines if the NBT for the ItemStack is specific to this
	 *            world or if it is portable, cross world NBT is larger and
	 *            slower, you should only request cross world NBT if you
	 *            specifically need it.
	 * @return an ItemStack for bits, which is empty if there are no bits.
	 */
	ItemStack getBitsAsItem(
			@Nullable EnumFacing side,
			ItemType type,
			boolean crossWorld );

}

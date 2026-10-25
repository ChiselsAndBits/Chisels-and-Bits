package mod.chiselsandbits.api.item.withmode;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.world.item.ItemStack;

/**
 * Represents an item with several operational modes.
 *
 * @param <M> The enum specifying the mode.
 */
public interface IWithModeItem<M extends IToolMode<?>> {
	/**
	 * Indicates to the ux system that this item requires an additional
	 * update when the player closes the interface.
	 *
	 * @return True when the update is required, false when not.
	 */
	default boolean requiresUpdateOnClosure() {
		return true;
	}

	/**
	 * Returns the current mode of the itemstack.
	 * @param stack The stack in question.
	 * @return The mode the given stack has.
	 */
	M getMode(final ItemStack stack);

	/**
	 * Set the given mode on the given itemstack.
	 *
	 * @param stack The stack to set the mode on.
	 * @param mode The mode to set on the stack.
	 */
	void setMode(final ItemStack stack, final M mode);

	/**
	 * Set the mode with the given index on the given itemstack.
	 *
	 * @param stack The stack to set the mode on.
	 * @param modeIndex The modes index to set on the stack.
	 */
	default void setMode(final ItemStack stack, final int modeIndex) {
		if (modeIndex == -1) {
			return;
		}

		final List<M> modes = Lists.newArrayList(getPossibleModes());
		setMode(stack, modes.get(modeIndex));
	}

	/**
	 * Returns all possible modes this item can have.
	 *
	 * @return The possible modes in an array.
	 */
	Collection<M> getPossibleModes();
}

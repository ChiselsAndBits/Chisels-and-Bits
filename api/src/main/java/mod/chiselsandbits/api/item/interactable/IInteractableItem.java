package mod.chiselsandbits.api.item.interactable;

import net.minecraft.world.item.ItemStack;

/**
 * Represents an item which can be used to interact with other items in the other hand.
 */
public interface IInteractableItem
{
	/**
     * Indicates if the current given itemstack is in use during an interaction.
     * @param stack The stack in question.
     * @return {@code true} when an interaction with the given stack is going on.
     */
	boolean isInteracting(final ItemStack stack);

	/**
     * Gives access to the stack the interaction is occurring with.
     * @param stack The stack that is leading the interaction.
     * @return The stack that the interaction is occuring with.
     */
	ItemStack getInteractionTarget(final ItemStack stack);

	/**
     * Indicates if the interactable stack is used in a simulation for display purposes.
     * @param stack The stack in question
     * @return {@code true} when a simulation is taking place.
     */
	boolean isRunningASimulatedInteraction(final ItemStack stack);

	/**
     * Indicates how often the bobbing animation needs to play inside the use time of the item.
     * This is done by returning the bobbing animation length from this method.
     *
     * @return The amount of ticks a bobbing animation takes.
     */
	float getBobbingTickCount();
}

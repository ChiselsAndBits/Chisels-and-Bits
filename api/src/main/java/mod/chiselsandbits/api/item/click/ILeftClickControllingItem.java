package mod.chiselsandbits.api.item.click;

import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/**
 * Deals with the way items are able to handle left click interactions.
 */
public interface ILeftClickControllingItem {
	/**
     * Callback invoked when a supported item is used to left click.
     *
     * Allows for said item to take over the processing logic of the left clicking
     * and can afterwards block the further vanilla processing.
     *
     * @param playerEntity The entity who left clicked.
     * @param hand The hand with which the entity left clicked.
     * @param position The position on which the entity left clicked.
     * @param face The face on which the entity left clicked.
     * @param currentState The current state of the left click processing.
     *
     * @return The processing state with which the processing of the left click should continue.
     */
	ClickProcessingState handleLeftClickProcessing(
	  final Player playerEntity,
	  final InteractionHand hand,
	  final BlockPos position,
	  final Direction face,
	  final ClickProcessingState currentState
	);

	/**
     * Invoked by the platform to indicate to this item that a left-clicking procedure has ended.
     * @param player The player who stopped left-clicking.
     * @param stack The stack on which the clicking ended.
     */
	void onLeftClickProcessingEnd(
	  final Player player,
	  final ItemStack stack
	);

	/**
     * Indicates if the player can use the item in the current interaction.
     * If this returns false, then no processing is performed and the interaction
     * event is cancelled.
     *
     * @param playerEntity The player in question.
     *
     * @return True when useable, false when not.
     */
	default boolean canUse(final Player playerEntity,
						   final ItemStack stack) {
		return true;
	}
}

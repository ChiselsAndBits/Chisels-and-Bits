package mod.chiselsandbits.api.client.variant.state;

import java.util.List;

import com.communi.suggestu.scena.core.client.models.data.IBlockModelData;
import mod.chiselsandbits.api.variant.state.IStateVariant;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

/**
 * A state variant provider for client side interactions.
 */
public interface IClientStateVariantProvider {
	/**
	 * Retrieves the block model data for the given state variant.
	 *
	 * @param variant The state variant.
	 * @return The block model data.
	 */
	IBlockModelData getBlockModelData(IStateVariant variant);

	/**
	 * Invoked to add tooltip lines to the tooltip of an item containing the given variant.
	 *
	 * @param variant The variant contained in the stack in question.
	 * @param context The context.
	 * @param tooltip The tooltip lines
	 * @param flags The tooltip flags.
	 */
	void appendHoverText(IStateVariant variant, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flags);
}

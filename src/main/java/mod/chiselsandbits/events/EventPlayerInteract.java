package mod.chiselsandbits.events;

import java.util.WeakHashMap;

import mod.chiselsandbits.items.ItemChisel;
import mod.chiselsandbits.items.ItemChiseledBit;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Disable breaking blocks when using a chisel / bit, some items break too fast
 * for the other code to prevent which is where this comes in.
 *
 * This manages survival chisel actions, creative some how skips this and calls
 * onBlockStartBreak on its own, but when in creative this is called on the
 * server... which still needs to be canceled or it will break the block.
 *
 * The whole things, is very strange.
 */
public class EventPlayerInteract
{

	private static WeakHashMap<EntityPlayer, Boolean> serverSuppressEvent = new WeakHashMap<EntityPlayer, Boolean>();

	public static void setPlayerSuppressionState(
			final EntityPlayer player,
			final boolean state )
	{
		if ( state )
		{
			serverSuppressEvent.put( player, state );
		}
		else
		{
			serverSuppressEvent.remove( player );
		}
	}

	@SubscribeEvent
	public void interaction(
			final LeftClickBlock event )
	{
		if ( event.getEntityPlayer() != null && event.getUseItem() != Result.DENY)
		{
			final ItemStack is = event.getItemStack();
			if ( is != null && ( is.getItem() instanceof ItemChisel || is.getItem() instanceof ItemChiseledBit ) )
			{
				if ( event.getWorld().isRemote )
				{
					// this is called when the player is survival - client side.
					is.getItem().onBlockStartBreak( is, event.getPos(), event.getEntityPlayer() );
				}

				// cancel all interactions, creative is magic.
				event.setCanceled( true );
			}
		}
	}

	@SubscribeEvent
	public void interaction(
			final RightClickBlock event )
	{
		if ( event.getEntityPlayer() != null && event.getUseItem() != Result.DENY)
		{
			final ItemStack is = event.getItemStack();
			if ( is != null && ( is.getItem() instanceof ItemChisel || is.getItem() instanceof ItemChiseledBit ) )
			{
				if ( serverSuppressEvent.containsKey( event.getEntityPlayer() ) )
				{
					event.setCanceled( true );
				}
			}
		}
	}
}

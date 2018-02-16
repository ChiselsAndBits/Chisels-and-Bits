package mod.chiselsandbits.helpers;

import java.util.*;

import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.items.ItemBitBag;
import mod.chiselsandbits.items.ItemChiseledBit;
import mod.chiselsandbits.items.ItemBitBag.BagPos;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.fml.common.eventhandler.Event.Result;

public class BitInventoryFeeder
{
	private final static Random itemRand = new Random();
	ArrayList<Integer> seenBits = new ArrayList<>();
	boolean hasSentMessage = false;
	final EntityPlayer player;
	final World world;

	public BitInventoryFeeder(
			final EntityPlayer p,
			final World w)
	{
		player = p;
		world = w;
	}

	public void addItem(
			final EntityItem ei)
	{
		ItemStack is = ModUtil.nonNull( ei.getEntityItem() );

		final List<BagPos> bags = ItemBitBag.getBags( player.inventory );

		if ( !ModUtil.containsAtLeastOneOf( player.inventory, is ) )
		{
			final ItemStack minSize = is.copy();

			if ( ModUtil.getStackSize( minSize ) > minSize.getMaxStackSize() )
			{
				ModUtil.setStackSize( minSize, minSize.getMaxStackSize() );
			}

			ModUtil.adjustStackSize( is, -ModUtil.getStackSize( minSize ) );
			player.inventory.addItemStackToInventory( minSize );
			ModUtil.adjustStackSize( is, ModUtil.getStackSize( minSize ) );
		}

		for ( final BagPos bp : bags )
		{
			is = bp.inv.insertItem( is );
		}

		if ( ModUtil.isEmpty( is ) )
			return;

		ei.setEntityItemStack( is );
		EntityItemPickupEvent event = new EntityItemPickupEvent( player, ei );

		if ( MinecraftForge.EVENT_BUS.post( event ) )
		{
			// cancelled...
			spawnItem( world, ei );
		}
		else
		{
			if ( event.getResult() != Result.DENY )
			{
				is = ei.getEntityItem();

				if ( is != null && !player.inventory.addItemStackToInventory( is ) )
				{
					ei.setEntityItemStack( is );
					//Never spawn the items for dropped excess items if setting is enabled.
					if ( !ChiselsAndBits.getConfig().voidExcessBits )
					{
						spawnItem( world, ei );
					}
				}
				else
				{
					if ( !ei.isSilent() )
					{
						ei.worldObj.playSound( (EntityPlayer) null, ei.posX, ei.posY, ei.posZ, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.2F, ( ( itemRand.nextFloat() - itemRand.nextFloat() ) * 0.7F + 1.0F ) * 2.0F );
					}
				}

				player.inventory.markDirty();

				if ( player.inventoryContainer != null )
				{
					player.inventoryContainer.detectAndSendChanges();
				}

			}
			else
				spawnItem( world, ei );
		}

		final int blk = ItemChiseledBit.getStackState( is );
		if ( ChiselsAndBits.getConfig().voidExcessBits && !seenBits.contains(blk) && !hasSentMessage )
		{
			if ( !ItemChiseledBit.hasBitSpace( player, blk ) )
			{
				player.addChatMessage( new TextComponentTranslation( "mod.chiselsandbits.result.void_excess" ) );
				hasSentMessage = true;
			}
			if ( !seenBits.contains( blk ))
			{
				seenBits.add( blk );
			}
		}
	}

	private static void spawnItem(
			World world,
			EntityItem ei )
	{
		if ( world.isRemote ) // no spawning items on the client.
			return;

		world.spawnEntityInWorld( ei );
	}
}

package mod.chiselsandbits.items;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import mod.chiselsandbits.bitbag.BagCapabilityProvider;
import mod.chiselsandbits.bitbag.BagInventory;
import mod.chiselsandbits.bitbag.BagStorage;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.core.ClientSide;
import mod.chiselsandbits.helpers.LocalStrings;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.network.NetworkRouter;
import mod.chiselsandbits.network.packets.PacketOpenBagGui;
import mod.chiselsandbits.render.helpers.SimpleInstanceCache;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.oredict.OreDictionary;

public class ItemBitBag extends Item
{

	public static final int INTS_PER_BIT_TYPE = 2;
	public static final int OFFSET_STATE_ID = 0;
	public static final int OFFSET_QUANTITY = 1;

	SimpleInstanceCache<ItemStack, List<String>> tooltipCache = new SimpleInstanceCache<ItemStack, List<String>>( null, new ArrayList<String>() );

	public ItemBitBag()
	{
		setMaxStackSize( 1 );
		ChiselsAndBits.registerWithBus( this );
	}

	@Override
	public ICapabilityProvider initCapabilities(
			final ItemStack stack,
			final NBTTagCompound nbt )
	{
		return new BagCapabilityProvider( stack, nbt );
	}

	@Override
	@SideOnly( Side.CLIENT )
	public void addInformation(
			final ItemStack stack,
			final World worldIn,
			final List<String> tooltip,
			final ITooltipFlag advanced )
	{
		super.addInformation( stack, worldIn, tooltip, advanced );
		ChiselsAndBits.getConfig().helpText( LocalStrings.HelpBitBag, tooltip );

		if ( tooltipCache.needsUpdate( stack ) )
		{
			final BagInventory bi = new BagInventory( stack );
			tooltipCache.updateCachedValue( bi.listContents( new ArrayList<String>() ) );
		}

		final List<String> details = tooltipCache.getCached();
		if ( details.size() <= 2 || ClientSide.instance.holdingShift() )
		{
			tooltip.addAll( details );
		}
		else
		{
			tooltip.add( LocalStrings.ShiftDetails.getLocal() );
		}
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(
			final World worldIn,
			final EntityPlayer playerIn,
			final EnumHand hand )
	{
		final ItemStack itemStackIn = playerIn.getHeldItem( hand );

		if ( worldIn.isRemote )
		{
			NetworkRouter.instance.sendToServer( new PacketOpenBagGui() );
		}

		return new ActionResult<ItemStack>( EnumActionResult.SUCCESS, itemStackIn );
	}

	public static class BagPos
	{
		public BagPos(
				final BagInventory bagInventory )
		{
			inv = bagInventory;
		}

		final public BagInventory inv;
	};

	@SubscribeEvent
	public void pickupItems(
			final EntityItemPickupEvent event )
	{
		boolean modified = false;

		final EntityItem entityItem = event.getItem();
		if ( entityItem != null )
		{
			final ItemStack is = entityItem.getEntityItem();
			final EntityPlayer player = event.getEntityPlayer();
			if ( is != null && is.getItem() instanceof ItemChiseledBit )
			{
				final int originalSize = ModUtil.getStackSize( is );
				final IInventory inv = player.inventory;
				final List<BagPos> bags = getBags( inv );

				// has the stack?
				final boolean seen = ModUtil.containsAtLeastOneOf( inv, is );

				if ( seen )
				{
					for ( final BagPos i : bags )
					{
						if ( !entityItem.isDead )
						{
							modified = updateEntity( player, entityItem, i.inv.insertItem( ModUtil.nonNull( entityItem.getEntityItem() ) ), originalSize ) || modified;
						}
					}
				}
				else
				{
					if ( ModUtil.getStackSize( is ) > is.getMaxStackSize() && !entityItem.isDead )
					{
						final ItemStack singleStack = is.copy();
						ModUtil.setStackSize( singleStack, singleStack.getMaxStackSize() );

						if ( player.inventory.addItemStackToInventory( singleStack ) == false )
						{
							ModUtil.adjustStackSize( is, -( singleStack.getMaxStackSize() - ModUtil.getStackSize( is ) ) );
						}

						modified = updateEntity( player, entityItem, is, originalSize ) || modified;
					}
					else
					{
						return;
					}

					for ( final BagPos i : bags )
					{

						if ( !entityItem.isDead )
						{
							modified = updateEntity( player, entityItem, i.inv.insertItem( ModUtil.nonNull( entityItem.getEntityItem() ) ), originalSize ) || modified;
						}
					}
				}
			}

			cleanupInventory( player, is );
		}

		if ( modified )
		{
			event.setCanceled( true );
		}
	}

	private boolean updateEntity(
			final EntityPlayer player,
			final EntityItem ei,
			ItemStack is,
			final int originalSize )
	{
		if ( is == null )
		{
			ei.setDead();
			// is = new ItemStack( ei.getEntityItem().getItem(), 0 );
			// ei.setEntityItemStack( is );
			// ei.setDead();
			//
			// net.minecraftforge.fml.common.FMLCommonHandler.instance().firePlayerItemPickupEvent(
			// player, ei );
			//
			// if ( !ei.isSilent() )
			// {
			// ei.worldObj.playSound( (EntityPlayer) null, ei.posX, ei.posY,
			// ei.posZ, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS,
			// 0.2F, ( ( itemRand.nextFloat() - itemRand.nextFloat() ) * 0.7F +
			// 1.0F ) * 2.0F );
			// }
			//
			// player.onItemPickup( ei, originalSize );
			//
			return true;
		}
		else
		{
			final int changed = ModUtil.getStackSize( is ) - ModUtil.getStackSize( ei.getEntityItem() );
			ei.setEntityItemStack( is );
			return changed != 0;
		}
	}

	static public void cleanupInventory(
			final EntityPlayer player,
			final ItemStack is )
	{
		if ( is != null && is.getItem() instanceof ItemChiseledBit )
		{
			// time to clean up your inventory...
			final IInventory inv = player.inventory;
			final List<BagPos> bags = getBags( inv );

			int firstSeen = -1;
			for ( int slot = 0; slot < inv.getSizeInventory(); slot++ )
			{
				int actingSlot = slot;
				@Nonnull
				ItemStack which = ModUtil.nonNull( inv.getStackInSlot( actingSlot ) );

				if ( which != null && which.getItem() == is.getItem() && ( ItemChiseledBit.sameBit( which, ItemChiseledBit.getStackState( is ) ) || is.getItemDamage() == OreDictionary.WILDCARD_VALUE ) )
				{
					if ( actingSlot == player.inventory.currentItem )
					{
						if ( firstSeen != -1 )
						{
							actingSlot = firstSeen;
						}
						else
						{
							continue;
						}
					}

					which = ModUtil.nonNull( inv.getStackInSlot( actingSlot ) );

					if ( firstSeen == -1 )
					{
						firstSeen = actingSlot;
					}
					else
					{
						for ( final BagPos i : bags )
						{
							which = i.inv.insertItem( which );
							if ( ModUtil.isEmpty( which ) )
							{
								inv.setInventorySlotContents( actingSlot, which );
								break;
							}
						}
					}
				}

			}
		}
	}

	public static List<BagPos> getBags(
			final IInventory inv )
	{
		final ArrayList<BagPos> bags = new ArrayList<BagPos>();
		for ( int x = 0; x < inv.getSizeInventory(); x++ )
		{
			final ItemStack which = inv.getStackInSlot( x );
			if ( which != null && which.getItem() instanceof ItemBitBag )
			{
				bags.add( new BagPos( new BagInventory( which ) ) );
			}
		}
		return bags;
	}

	@Override
	public boolean showDurabilityBar(
			final ItemStack stack )
	{
		final Object o = stack.getCapability( CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null );

		if ( o instanceof BagStorage )
		{
			final int qty = ( (BagStorage) o ).getSlotsUsed();
			return qty != 0;
		}

		return false;
	}

	@Override
	public double getDurabilityForDisplay(
			final ItemStack stack )
	{
		final Object o = stack.getCapability( CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null );

		if ( o instanceof BagStorage )
		{
			final int qty = ( (BagStorage) o ).getSlotsUsed();

			final double value = qty / (float) BagStorage.BAG_STORAGE_SLOTS;
			return Math.min( 1.0d, Math.max( 0.0d, ChiselsAndBits.getConfig().invertBitBagFullness ? value : 1.0 - value ) );
		}

		return 0;
	}

	public static boolean hasBagSpace(
			final EntityPlayer player,
			final int blk )
	{
		final List<BagPos> bags = getBags( player.inventory );
		for ( final BagPos bp : bags )
		{
			for ( int x = 0; x < bp.inv.getSizeInventory(); x++ )
			{
				final ItemStack is = bp.inv.getStackInSlot( x );
				if( ( ItemChiseledBit.sameBit( is, blk ) && ModUtil.getStackSize( is ) < bp.inv.getInventoryStackLimit() ) || ModUtil.isEmpty( is ) ) {
					return true;
				}
			}
		}
		return false;
	}

}

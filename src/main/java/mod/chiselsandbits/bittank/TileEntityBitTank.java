package mod.chiselsandbits.bittank;

import javax.annotation.Nonnull;

import mod.chiselsandbits.api.IBitBag;
import mod.chiselsandbits.api.ItemType;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.helpers.DeprecationHelper;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.items.ItemChiseledBit;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.IItemHandler;

public class TileEntityBitTank extends TileEntity implements IItemHandler, IFluidHandler
{

	public static final int MAX_CONTENTS = 4096;

	// best conversion...
	// 125mb = 512bits
	public static final int MB_PER_BIT_CONVERSION = 125;
	public static final int BITS_PER_MB_CONVERSION = 512;

	private Fluid myFluid = null;
	private int bits = 0;

	private int oldLV = -1;

	@Override
	public void onDataPacket(
			final NetworkManager net,
			final SPacketUpdateTileEntity pkt )
	{
		deserializeFromNBT( pkt.getNbtCompound() );
	}

	@Override
	public NBTTagCompound getUpdateTag()
	{
		final NBTTagCompound nbttagcompound = new NBTTagCompound();
		writeToNBT( nbttagcompound );
		return nbttagcompound;
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket()
	{
		final NBTTagCompound t = new NBTTagCompound();
		serializeToNBT( t );
		return new SPacketUpdateTileEntity( getPos(), 0, t );
	}

	public void deserializeFromNBT(
			final NBTTagCompound compound )
	{
		final String fluid = compound.getString( "fluid" );

		if ( fluid == null || fluid.equals( "" ) )
		{
			myFluid = null;
		}
		else
		{
			myFluid = FluidRegistry.getFluid( fluid );
		}

		bits = compound.getInteger( "bits" );
	}

	public void serializeToNBT(
			final NBTTagCompound compound )
	{
		compound.setString( "fluid", myFluid == null ? "" : myFluid.getName() );
		compound.setInteger( "bits", bits );
	}

	@Override
	public void readFromNBT(
			final NBTTagCompound compound )
	{
		deserializeFromNBT( compound );
		super.readFromNBT( compound );
	}

	@Override
	public NBTTagCompound writeToNBT(
			final NBTTagCompound compound )
	{
		serializeToNBT( compound );
		super.writeToNBT( compound );
		return compound;
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public <T> T getCapability(
			final Capability<T> capability,
			final EnumFacing facing )
	{
		if ( capability == net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY )
		{
			return (T) this;
		}

		if ( capability == net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY )
		{
			return (T) this;
		}

		return super.getCapability( capability, facing );
	}

	@Override
	public boolean hasCapability(
			final Capability<?> capability,
			final EnumFacing facing )
	{
		if ( capability == net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY )
		{
			return true;
		}
		
		if ( capability == net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY )
		{
			return true;
		}

		return super.hasCapability( capability, facing );
	}

	@Override
	public int getSlots()
	{
		return 1;
	}

	@Override
	public ItemStack getStackInSlot(
			final int slot )
	{
		if ( bits > 0 && slot == 0 )
		{
			return getFluidBitStack( myFluid, bits );
		}

		return ModUtil.getEmptyStack();
	}

	public @Nonnull ItemStack getFluidBitStack(
			final Fluid liquid,
			final int amount )
	{
		if ( liquid == null || liquid.getBlock() == null )
		{
			return ModUtil.getEmptyStack();
		}

		return ItemChiseledBit.createStack( ModUtil.getStateId( liquid.getBlock().getDefaultState() ), amount, false );
	}

	@Override
	public @Nonnull ItemStack insertItem(
			final int slot,
			final ItemStack stack,
			final boolean simulate )
	{
		if ( !ModUtil.isEmpty( stack ) && stack.getItem() instanceof ItemChiseledBit )
		{
			final int state = ItemChiseledBit.getStackState( stack );
			final IBlockState blk = ModUtil.getStateById( state );

			Fluid f = null;
			for ( final Fluid fl : FluidRegistry.getRegisteredFluids().values() )
			{
				if ( fl.getBlock() == blk.getBlock() )
				{
					f = fl;
					break;
				}
			}

			if ( f == null )
			{
				return stack;
			}

			final ItemStack bitItem = getFluidBitStack( myFluid, bits );
			final boolean canInsert = ModUtil.isEmpty( bitItem ) || ItemStack.areItemStackTagsEqual( bitItem, stack ) && bitItem.getItem() == stack.getItem();

			if ( canInsert )
			{
				final int merged = bits + ModUtil.getStackSize( stack );
				final int amount = Math.min( merged, MAX_CONTENTS );

				if ( !simulate )
				{
					final Fluid oldFluid = myFluid;
					final int oldBits = bits;

					myFluid = f;
					bits = amount;

					if ( bits != oldBits || myFluid != oldFluid )
					{
						saveAndUpdate();
					}
				}

				if ( amount < merged )
				{
					final ItemStack out = ModUtil.copy( stack );
					ModUtil.setStackSize( out, merged - amount );
					return out;
				}

				return ModUtil.getEmptyStack();
			}
		}
		return stack;
	}

	private void saveAndUpdate()
	{
		markDirty();
		ModUtil.sendUpdate( worldObj, getPos() );

		final int lv = getLightValue();
		if ( oldLV != lv )
		{
			getWorld().checkLight( getPos() );
			oldLV = lv;
		}
	}

	/**
	 * Dosn't limit to stack size...
	 *
	 * @param slot
	 * @param amount
	 * @param simulate
	 * @return
	 */
	public @Nonnull ItemStack extractBits(
			final int slot,
			final int amount,
			final boolean simulate )
	{
		final ItemStack contents = getStackInSlot( slot );

		if ( contents != null && amount > 0 )
		{
			// how many to extract?
			ModUtil.setStackSize( contents, Math.min( amount, ModUtil.getStackSize( contents ) ) );

			// modulate?
			if ( !simulate )
			{
				final int oldBits = bits;

				bits -= ModUtil.getStackSize( contents );
				if ( bits == 0 )
				{
					myFluid = null;
				}

				if ( bits != oldBits )
				{
					saveAndUpdate();
				}
			}

			return contents;
		}

		return ModUtil.getEmptyStack();
	}

	@Override
	public boolean shouldRenderInPass(
			final int pass )
	{
		return true;
	}

	@Override
	public ItemStack extractItem(
			final int slot,
			final int amount,
			final boolean simulate )
	{
		return extractBits( slot, Math.min( amount, ChiselsAndBits.getItems().itemBlockBit.getItemStackLimit() ), simulate );
	}

	public FluidStack getAccessableFluid()
	{
		int mb = ( bits - bits % BITS_PER_MB_CONVERSION ) / BITS_PER_MB_CONVERSION;
		mb *= MB_PER_BIT_CONVERSION;

		if ( mb > 0 && myFluid != null )
		{
			return new FluidStack( myFluid, mb );
		}

		return null;
	}

	@Override
	public boolean hasFastRenderer()
	{
		// https://github.com/MinecraftForge/MinecraftForge/issues/2528
		return false; // true can cause crashes when rendering in pass1.
	}

	FluidStack getBitsAsFluidStack()
	{
		if ( bits > 0 && myFluid != null )
		{
			return new FluidStack( myFluid, bits );
		}

		return null;
	}

	public int getLightValue()
	{
		if ( myFluid == null || myFluid.getBlock() == null )
		{
			return 0;
		}

		final int lv = DeprecationHelper.getLightValue( myFluid.getBlock().getDefaultState() );
		return lv;

	}

	boolean extractBits(
			final EntityPlayer playerIn,
			final float hitX,
			final float hitY,
			final float hitZ,
			final BlockPos pos )
	{
		if ( !playerIn.isSneaking() )
		{
			final ItemStack is = extractItem( 0, 64, false );
			if ( is != null )
			{
				ChiselsAndBits.getApi().giveBitToPlayer( playerIn, is, new Vec3d( (double) hitX + pos.getX(), (double) hitY + pos.getY(), (double) hitZ + pos.getZ() ) );
			}
			return true;
		}

		return false;
	}

	boolean addAllPossibleBits(
			final EntityPlayer playerIn )
	{
		if ( playerIn.isSneaking() )
		{
			boolean change = false;
			for ( int x = 0; x < playerIn.inventory.getSizeInventory(); x++ )
			{
				final ItemStack stackInSlot = ModUtil.nonNull( playerIn.inventory.getStackInSlot( x ) );
				if ( ChiselsAndBits.getApi().getItemType( stackInSlot ) == ItemType.CHISLED_BIT )
				{
					playerIn.inventory.setInventorySlotContents( x, insertItem( 0, stackInSlot, false ) );
					change = true;
				}

				if ( ChiselsAndBits.getApi().getItemType( stackInSlot ) == ItemType.BIT_BAG )
				{
					final IBitBag bag = ChiselsAndBits.getApi().getBitbag( stackInSlot );

					if ( bag == null )
					{
						continue;
					}

					for ( int y = 0; y < bag.getSlots(); ++y )
					{
						bag.insertItem( y, insertItem( 0, bag.extractItem( y, bag.getSlotLimit( y ), false ), false ), false );
						change = true;
					}
				}
			}

			if ( change )
			{
				playerIn.inventory.markDirty();
			}

			return change;
		}

		return false;
	}

	boolean addHeldBits(
			final @Nonnull ItemStack current,
			final EntityPlayer playerIn )
	{
		if ( playerIn.isSneaking() )
		{
			if ( ChiselsAndBits.getApi().getItemType( current ) == ItemType.CHISLED_BIT )
			{
				playerIn.inventory.setInventorySlotContents( playerIn.inventory.currentItem, insertItem( 0, current, false ) );
				playerIn.inventory.markDirty();
				return true;
			}
		}

		return false;
	}

	@Override
	public IFluidTankProperties[] getTankProperties()
	{
		return new FluidTankProperties[] { new FluidTankProperties( getAccessableFluid(), 1000 ) };
	}

	@Override
	public int fill(
			final FluidStack resource,
			final boolean doFill )
	{
		if ( resource == null )
		{
			return 0;
		}

		final int possibleAmount = resource.amount - resource.amount % TileEntityBitTank.MB_PER_BIT_CONVERSION;

		if ( possibleAmount > 0 )
		{
			final int bitCount = possibleAmount * TileEntityBitTank.BITS_PER_MB_CONVERSION / TileEntityBitTank.MB_PER_BIT_CONVERSION;
			final ItemStack bitItems = getFluidBitStack( resource.getFluid(), bitCount );
			final ItemStack leftOver = insertItem( 0, bitItems, !doFill );

			if ( ModUtil.isEmpty( leftOver ) )
			{
				return possibleAmount;
			}

			int mbUsedUp = ModUtil.getStackSize( leftOver );

			// round up...
			mbUsedUp *= TileEntityBitTank.MB_PER_BIT_CONVERSION;
			mbUsedUp += TileEntityBitTank.BITS_PER_MB_CONVERSION - 1;
			mbUsedUp /= TileEntityBitTank.BITS_PER_MB_CONVERSION;

			return resource.amount - mbUsedUp;
		}

		return 0;
	}

	@Override
	public FluidStack drain(
			final FluidStack resource,
			final boolean doDrain )
	{
		if ( resource == null )
		{
			return null;
		}

		final FluidStack a = getAccessableFluid();

		if ( a != null && resource.containsFluid( a ) ) // right type of fluid.
		{
			final int aboutHowMuch = resource.amount;

			final int mbThatCanBeRemoved = Math.min( a.amount, aboutHowMuch - aboutHowMuch % TileEntityBitTank.MB_PER_BIT_CONVERSION );
			if ( mbThatCanBeRemoved > 0 )
			{
				a.amount = mbThatCanBeRemoved;

				if ( doDrain )
				{
					final int bitCount = mbThatCanBeRemoved * TileEntityBitTank.BITS_PER_MB_CONVERSION / TileEntityBitTank.MB_PER_BIT_CONVERSION;
					extractBits( 0, bitCount, false );
				}

				return a;
			}
		}

		return null;
	}

	@Override
	public FluidStack drain(
			final int maxDrain,
			final boolean doDrain )
	{
		final FluidStack a = getAccessableFluid();

		final int mbThatCanBeRemoved = Math.min( a == null ? 0 : a.amount, maxDrain - maxDrain % TileEntityBitTank.MB_PER_BIT_CONVERSION );
		if ( mbThatCanBeRemoved > 0 && a != null )
		{
			a.amount = mbThatCanBeRemoved;

			if ( doDrain )
			{
				final int bitCount = mbThatCanBeRemoved * TileEntityBitTank.BITS_PER_MB_CONVERSION / TileEntityBitTank.MB_PER_BIT_CONVERSION;
				extractBits( 0, bitCount, false );
			}

			return a;
		}

		return null;
	}

	@Override
	public int getSlotLimit(
			final int slot )
	{
		return TileEntityBitTank.BITS_PER_MB_CONVERSION;
	}

}

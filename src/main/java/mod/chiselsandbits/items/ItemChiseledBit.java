package mod.chiselsandbits.items;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import mod.chiselsandbits.bittank.BlockBitTank;
import mod.chiselsandbits.chiseledblock.BlockBitInfo;
import mod.chiselsandbits.chiseledblock.BlockChiseled;
import mod.chiselsandbits.chiseledblock.TileEntityBlockChiseled;
import mod.chiselsandbits.chiseledblock.data.BitLocation;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.core.ClientSide;
import mod.chiselsandbits.core.Log;
import mod.chiselsandbits.helpers.ActingPlayer;
import mod.chiselsandbits.helpers.BitOperation;
import mod.chiselsandbits.helpers.ChiselModeManager;
import mod.chiselsandbits.helpers.ChiselToolType;
import mod.chiselsandbits.helpers.DeprecationHelper;
import mod.chiselsandbits.helpers.IContinuousInventory;
import mod.chiselsandbits.helpers.IItemInInventory;
import mod.chiselsandbits.helpers.LocalStrings;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.interfaces.ICacheClearable;
import mod.chiselsandbits.interfaces.IChiselModeItem;
import mod.chiselsandbits.interfaces.IItemScrollWheel;
import mod.chiselsandbits.modes.ChiselMode;
import mod.chiselsandbits.modes.IToolMode;
import mod.chiselsandbits.network.NetworkRouter;
import mod.chiselsandbits.network.packets.PacketChisel;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemChiseledBit extends Item implements IItemScrollWheel, IChiselModeItem, ICacheClearable
{

	public static boolean bitBagStackLimitHack;

	private ArrayList<ItemStack> bits;

	public ItemChiseledBit()
	{
		setHasSubtypes( true );
		ChiselsAndBits.getInstance().addClearable( this );
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
		ChiselsAndBits.getConfig().helpText( LocalStrings.HelpBit, tooltip,
				ClientSide.instance.getKeyName( Minecraft.getMinecraft().gameSettings.keyBindAttack ),
				ClientSide.instance.getKeyName( Minecraft.getMinecraft().gameSettings.keyBindUseItem ),
				ClientSide.instance.getModeKey() );
	}

	@Override
	public String getHighlightTip(
			final ItemStack item,
			final String displayName )
	{
		if ( ChiselsAndBits.getConfig().itemNameModeDisplay && FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT )
		{
			String extra = "";
			if ( getBitOperation( ClientSide.instance.getPlayer(), EnumHand.MAIN_HAND, item ) == BitOperation.REPLACE )
			{
				extra = " - " + LocalStrings.BitOptionReplace.getLocal();
			}

			return displayName + " - " + ChiselModeManager.getChiselMode( ClientSide.instance.getPlayer(), ChiselToolType.BIT, EnumHand.MAIN_HAND ).getName().getLocal() + extra;
		}

		return displayName;
	}

	@Override
	/**
	 * alter digging behavior to chisel, uses packets to enable server to stay
	 * in-sync.
	 */
	public boolean onBlockStartBreak(
			final ItemStack itemstack,
			final BlockPos pos,
			final EntityPlayer player )
	{
		return ItemChisel.fromBreakToChisel( ChiselMode.castMode( ChiselModeManager.getChiselMode( player, ChiselToolType.BIT, EnumHand.MAIN_HAND ) ), itemstack, pos, player, EnumHand.MAIN_HAND );
	}

	public static String getBitStateName(
			final IBlockState state )
	{
		ItemStack target = null;
		Block blk = null;

		if ( state == null )
		{
			return "Null";
		}

		try
		{
			// for an unknown reason its possible to generate mod blocks without
			// proper state here...
			blk = state.getBlock();

			final Item item = Item.getItemFromBlock( blk );
			if ( ModUtil.isEmpty( item ) )
			{
				final Fluid f = BlockBitInfo.getFluidFromBlock( blk );
				if ( f != null )
				{
					return f.getLocalizedName( new FluidStack( f, 10 ) );
				}
			}
			else
			{
				target = new ItemStack( blk, 1, blk.damageDropped( state ) );
			}
		}
		catch ( final IllegalArgumentException e )
		{
			Log.logError( "Unable to get Item Details for Bit.", e );
		}

		if ( target == null || target.getItem() == null )
		{
			return null;
		}

		try
		{
			final String myName = target.getDisplayName();

			final Set<String> extra = new HashSet<String>();
			if ( blk != null && state != null )
			{
				for ( final IProperty<?> p : state.getPropertyNames() )
				{
					if ( p.getName().equals( "axis" ) || p.getName().equals( "facing" ) )
					{
						extra.add( DeprecationHelper.translateToLocal( "mod.chiselsandbits.pretty." + p.getName() + "-" + state.getProperties().get( p ).toString() ) );
					}
				}
			}

			if ( extra.isEmpty() )
			{
				return myName;
			}

			final StringBuilder b = new StringBuilder( myName );

			for ( final String x : extra )
			{
				b.append( ' ' ).append( x );
			}

			return b.toString();
		}
		catch ( final Exception e )
		{
			return "ERROR";
		}
	}

	public static String getBitTypeName(
			final ItemStack stack )
	{
		return getBitStateName( ModUtil.getStateById( ItemChiseledBit.getStackState( stack ) ) );
	}

	@Override
	public String getItemStackDisplayName(
			final ItemStack stack )
	{
		final String typeName = getBitTypeName( stack );

		if ( typeName == null )
		{
			return super.getItemStackDisplayName( stack );
		}

		return new StringBuilder().append( super.getItemStackDisplayName( stack ) ).append( " - " ).append( typeName ).toString();
	}

	@SuppressWarnings( "deprecation" )
	@Override
	public int getItemStackLimit()
	{
		return bitBagStackLimitHack ? ChiselsAndBits.getConfig().bagStackSize : super.getItemStackLimit();
	}

	@Override
	public EnumActionResult onItemUse(
			final @Nonnull EntityPlayer player,
			final @Nonnull World world,
			final @Nonnull BlockPos usedBlock,
			final @Nonnull EnumHand hand,
			final @Nonnull EnumFacing side,
			final float hitX,
			final float hitY,
			final float hitZ )
	{
		final ItemStack stack = player.getHeldItem( hand );

		if ( !player.canPlayerEdit( usedBlock, side, stack ) )
		{
			return EnumActionResult.FAIL;
		}

		// forward interactions to tank...
		final IBlockState usedState = world.getBlockState( usedBlock );
		final Block blk = usedState.getBlock();
		if ( blk instanceof BlockBitTank )
		{
			if ( blk.onBlockActivated( world, usedBlock, usedState, player, hand, side, hitX, hitY, hitZ ) )
			{
				return EnumActionResult.SUCCESS;
			}
			return EnumActionResult.FAIL;
		}

		if ( world.isRemote )
		{
			final IToolMode mode = ChiselModeManager.getChiselMode( player, ClientSide.instance.getHeldToolType( hand ), hand );
			final BitLocation bitLocation = new BitLocation( new RayTraceResult( RayTraceResult.Type.BLOCK, new Vec3d( hitX, hitY, hitZ ), side, usedBlock ), false, getBitOperation( player, hand, stack ) );

			IBlockState blkstate = world.getBlockState( bitLocation.blockPos );
			TileEntityBlockChiseled tebc = ModUtil.getChiseledTileEntity( world, bitLocation.blockPos, true );
			if ( tebc == null && BlockChiseled.replaceWithChisled( world, bitLocation.blockPos, blkstate, ItemChiseledBit.getStackState( stack ), true ) )
			{
				blkstate = world.getBlockState( bitLocation.blockPos );
				tebc = ModUtil.getChiseledTileEntity( world, bitLocation.blockPos, true );
			}

			if ( tebc != null )
			{
				PacketChisel pc = null;
				if ( mode == ChiselMode.DRAWN_REGION )
				{
					if ( world.isRemote )
					{
						ClientSide.instance.pointAt( getBitOperation( player, hand, stack ).getToolType(), bitLocation, hand );
					}
					return EnumActionResult.FAIL;
				}
				else
				{
					pc = new PacketChisel( getBitOperation( player, hand, stack ), bitLocation, side, ChiselMode.castMode( mode ), hand );
				}

				final int result = pc.doAction( player );

				if ( result > 0 )
				{
					NetworkRouter.instance.sendToServer( pc );
				}
			}
		}

		return EnumActionResult.SUCCESS;

	}

	public static BitOperation getBitOperation(
			final EntityPlayer player,
			final EnumHand hand,
			final ItemStack stack )
	{
		return ChiselsAndBits.getConfig().replaceingBits ? BitOperation.REPLACE : BitOperation.PLACE;
	}

	@Override
	public void clearCache()
	{
		bits = null;
	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	@Override
	public void getSubItems(
			final CreativeTabs tab,
			final NonNullList subItems )
	{
		if ( !this.func_194125_a( tab ) ) // is this my creative tab?
		{
			return;
		}

		if ( bits == null )
		{
			bits = new ArrayList<ItemStack>();

			final NonNullList<ItemStack> List = NonNullList.func_191196_a();
			final BitSet used = new BitSet( 4096 );

			for ( final Object obj : Item.REGISTRY )
			{
				if ( !( obj instanceof ItemBlock ) )
				{
					continue;
				}

				try
				{
					Item it = (Item) obj;
					final CreativeTabs ctab = it.getCreativeTab();

					if ( ctab != null )
					{
						it.getSubItems( ctab, List );
					}

					for ( final ItemStack out : List )
					{
						it = out.getItem();

						if ( !( it instanceof ItemBlock ) )
						{
							continue;
						}

						final IBlockState state = DeprecationHelper.getStateFromItem( out );
						if ( state != null && BlockBitInfo.supportsBlock( state ) )
						{
							used.set( ModUtil.getStateId( state ) );
							bits.add( ItemChiseledBit.createStack( ModUtil.getStateId( state ), 1, false ) );
						}
					}

				}
				catch ( final Throwable t )
				{
					// a mod did something that isn't acceptable, let them crash
					// in their own code...
				}

				List.clear();
			}

			for ( final Fluid o : FluidRegistry.getRegisteredFluids().values() )
			{
				if ( o.canBePlacedInWorld() && o.getBlock() != null )
				{
					if ( used.get( Block.getStateId( o.getBlock().getDefaultState() ) ) )
					{
						continue;
					}

					bits.add( ItemChiseledBit.createStack( Block.getStateId( o.getBlock().getDefaultState() ), 1, false ) );
				}
			}
		}

		subItems.addAll( bits );
	}

	public static boolean sameBit(
			final ItemStack output,
			final int blk )
	{
		return output.hasTagCompound() ? getStackState( output ) == blk : false;
	}

	public static @Nonnull ItemStack createStack(
			final int id,
			final int count,
			final boolean RequireStack )
	{
		if ( ChiselsAndBits.getItems().itemBlockBit == null )
		{
			if ( !RequireStack )
			{
				return ModUtil.getEmptyStack();
			}
		}

		final ItemStack out = new ItemStack( ChiselsAndBits.getItems().itemBlockBit, count );
		out.setTagInfo( "id", new NBTTagInt( id ) );
		return out;
	}

	@Override
	public void scroll(
			final EntityPlayer player,
			final ItemStack stack,
			final int dwheel )
	{
		final IToolMode mode = ChiselModeManager.getChiselMode( player, ChiselToolType.BIT, EnumHand.MAIN_HAND );
		ChiselModeManager.scrollOption( ChiselToolType.BIT, mode, mode, dwheel );
	}

	public static int getStackState(
			final ItemStack inHand )
	{
		return inHand != null && inHand.hasTagCompound() ? ModUtil.getTagCompound( inHand ).getInteger( "id" ) : 0;
	}

	public static boolean placeBit(
			final IContinuousInventory bits,
			final ActingPlayer player,
			final VoxelBlob vb,
			final int x,
			final int y,
			final int z )
	{
		if ( vb.get( x, y, z ) == 0 )
		{
			final IItemInInventory slot = bits.getItem( 0 );
			final int stateID = ItemChiseledBit.getStackState( slot.getStack() );

			if ( slot.isValid() )
			{
				if ( !player.isCreative() )
				{
					if ( bits.useItem( stateID ) )
						vb.set( x, y, z, stateID );
				}
				else
					vb.set( x, y, z, stateID );
			}

			return true;
		}

		return false;
	}

	public static boolean hasInventorySpace(
			final EntityPlayer player,
			final int blk )
	{
		for ( int x = 0; x < 36; x++ )
		{
			final ItemStack is = player.inventory.getStackInSlot( x );
			if( ( ItemChiseledBit.sameBit( is, blk ) && ModUtil.getStackSize( is ) < is.getMaxStackSize() ) || ModUtil.isEmpty( is ) )
			{
				return true;
			}
		}
		return ItemBitBag.hasBagSpace( player, blk );
	}
}

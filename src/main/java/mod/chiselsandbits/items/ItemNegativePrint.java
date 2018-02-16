package mod.chiselsandbits.items;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import mod.chiselsandbits.chiseledblock.BlockChiseled;
import mod.chiselsandbits.chiseledblock.NBTBlobConverter;
import mod.chiselsandbits.chiseledblock.TileEntityBlockChiseled;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob.BlobStats;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.core.ClientSide;
import mod.chiselsandbits.helpers.ActingPlayer;
import mod.chiselsandbits.helpers.ContinousChisels;
import mod.chiselsandbits.helpers.IContinuousInventory;
import mod.chiselsandbits.helpers.LocalStrings;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.integration.mcmultipart.MCMultipartProxy;
import mod.chiselsandbits.interfaces.IItemScrollWheel;
import mod.chiselsandbits.interfaces.IPatternItem;
import mod.chiselsandbits.interfaces.IVoxelBlobItem;
import mod.chiselsandbits.network.NetworkRouter;
import mod.chiselsandbits.network.packets.PacketRotateVoxelBlob;
import mod.chiselsandbits.render.helpers.SimpleInstanceCache;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemNegativePrint extends Item implements IVoxelBlobItem, IItemScrollWheel, IPatternItem
{

	public ItemNegativePrint()
	{

	}

	@SideOnly( Side.CLIENT )
	protected void defaultAddInfo(
			final ItemStack stack,
			final World worldIn,
			final List<String> tooltip,
			final ITooltipFlag advanced )
	{
		super.addInformation( stack, worldIn, tooltip, advanced );
	}

	// add info cached info
	SimpleInstanceCache<ItemStack, List<String>> toolTipCache = new SimpleInstanceCache<ItemStack, List<String>>( null, new ArrayList<String>() );

	@Override
	@SideOnly( Side.CLIENT )
	public void addInformation(
			final ItemStack stack,
			final World worldIn,
			final List<String> tooltip,
			final ITooltipFlag advanced )
	{
		defaultAddInfo( stack, worldIn, tooltip, advanced );
		ChiselsAndBits.getConfig().helpText( LocalStrings.HelpNegativePrint, tooltip,
				ClientSide.instance.getKeyName( Minecraft.getMinecraft().gameSettings.keyBindUseItem ),
				ClientSide.instance.getKeyName( Minecraft.getMinecraft().gameSettings.keyBindUseItem ) );

		if ( isWritten( stack ) )
		{
			if ( ClientSide.instance.holdingShift() )
			{
				final List<String> details = toolTipCache.getCached();

				if ( toolTipCache.needsUpdate( stack ) )
				{
					details.clear();

					final VoxelBlob blob = ModUtil.getBlobFromStack( stack, null );

					final int solid = blob.filled();
					final int air = blob.air();

					if ( solid > 0 )
					{
						details.add( solid + " " + LocalStrings.Filled.getLocal() );
					}

					if ( air > 0 )
					{
						details.add( air + " " + LocalStrings.Empty.getLocal() );
					}
				}

				tooltip.addAll( details );
			}
			else
			{
				tooltip.add( LocalStrings.ShiftDetails.getLocal() );
			}
		}
	}

	@Override
	public String getUnlocalizedName(
			final ItemStack stack )
	{
		if ( isWritten( stack ) )
		{
			return super.getUnlocalizedName( stack ) + "_written";
		}
		return super.getUnlocalizedName( stack );
	}

	@Override
	public boolean isWritten(
			final ItemStack stack )
	{
		if ( stack != null && stack.hasTagCompound() )
		{
			final boolean a = ModUtil.getSubCompound( stack, ModUtil.NBT_BLOCKENTITYTAG, false ) != null;
			final boolean b = ModUtil.getTagCompound( stack ).hasKey( NBTBlobConverter.NBT_LEGACY_VOXEL );
			final boolean c = ModUtil.getTagCompound( stack ).hasKey( NBTBlobConverter.NBT_VERSIONED_VOXEL );
			return a || b || c;
		}
		return false;
	}

	@Override
	public EnumActionResult onItemUse(
			final EntityPlayer player,
			final World world,
			final BlockPos pos,
			final EnumHand hand,
			final EnumFacing side,
			final float hitX,
			final float hitY,
			final float hitZ )
	{
		final ItemStack stack = player.getHeldItem( hand );
		final IBlockState blkstate = world.getBlockState( pos );

		if ( ChiselsAndBits.getConfig().requireBagSpace && !player.isCreative() )
		{
			//Cycle every item in any bag, if the player can't store the clicked block then
			//send them a message.
			final int stateId = ModUtil.getStateId( blkstate );
			if ( !ItemChiseledBit.hasBitSpace( player, stateId ) )
			{
				if( player.worldObj.isRemote )
				{
					//Only client should handle messaging.
					player.addChatMessage( new TextComponentTranslation( "mod.chiselsandbits.result.require_bag" ) );
				}
				return EnumActionResult.FAIL;
			}
		}

		if ( !player.canPlayerEdit( pos, side, stack ) || !world.isBlockModifiable( player, pos ) )
		{
			return EnumActionResult.FAIL;
		}

		if ( !isWritten( stack ) )
		{
			final NBTTagCompound comp = getCompoundFromBlock( world, pos, player );
			if ( comp != null )
			{
				stack.setTagCompound( comp );
				return EnumActionResult.SUCCESS;
			}

			return EnumActionResult.FAIL;
		}

		final TileEntityBlockChiseled te = ModUtil.getChiseledTileEntity( world, pos, false );
		if ( te != null )
		{
			// we can do this!
		}
		else if ( !BlockChiseled.replaceWithChisled( world, pos, blkstate, true ) && !MCMultipartProxy.proxyMCMultiPart.isMultiPartTileEntity( world, pos ) )
		{
			return EnumActionResult.FAIL;
		}

		final TileEntityBlockChiseled tec = ModUtil.getChiseledTileEntity( world, pos, true );
		if ( tec != null )
		{
			final VoxelBlob vb = tec.getBlob();

			final VoxelBlob pattern = ModUtil.getBlobFromStack( stack, player );

			applyPrint( stack, world, pos, side, vb, pattern, player, hand );

			tec.completeEditOperation( vb );
			return EnumActionResult.SUCCESS;
		}

		return EnumActionResult.FAIL;
	}

	protected boolean convertToStone()
	{
		return true;
	}

	protected NBTTagCompound getCompoundFromBlock(
			final World world,
			final BlockPos pos,
			final EntityPlayer player )
	{

		final TileEntityBlockChiseled te = ModUtil.getChiseledTileEntity( world, pos, false );
		if ( te != null )
		{
			final NBTTagCompound comp = new NBTTagCompound();
			te.writeChisleData( comp );

			if ( convertToStone() )
			{
				final TileEntityBlockChiseled tmp = new TileEntityBlockChiseled();
				tmp.readChisleData( comp );

				final VoxelBlob bestBlob = tmp.getBlob();
				bestBlob.binaryReplacement( 0, ModUtil.getStateId( Blocks.STONE.getDefaultState() ) );

				tmp.setBlob( bestBlob );
				tmp.writeChisleData( comp );
			}

			comp.setByte( ModUtil.NBT_SIDE, (byte) ModUtil.getPlaceFace( player ).ordinal() );
			return comp;
		}

		return null;
	}

	@Override
	public ItemStack getPatternedItem(
			final ItemStack stack,
			final boolean craftingBlocks )
	{
		if ( !isWritten( stack ) )
		{
			return null;
		}

		final NBTTagCompound tag = ModUtil.getTagCompound( stack );

		// Detect and provide full blocks if pattern solid full and solid.
		final NBTBlobConverter conv = new NBTBlobConverter();
		conv.readChisleData( tag, VoxelBlob.VERSION_ANY );

		if ( craftingBlocks && ChiselsAndBits.getConfig().fullBlockCrafting )
		{
			final BlobStats stats = conv.getBlob().getVoxelStats();
			if ( stats.isFullBlock )
			{
				final IBlockState state = ModUtil.getStateById( stats.mostCommonState );
				final ItemStack is = ModUtil.getItemFromBlock( state );

				if ( !ModUtil.isEmpty( is ) )
				{
					return is;
				}
			}
		}

		final IBlockState state = conv.getPrimaryBlockState();
		final ItemStack itemstack = new ItemStack( ChiselsAndBits.getBlocks().getConversionWithDefault( state ), 1 );

		itemstack.setTagInfo( ModUtil.NBT_BLOCKENTITYTAG, tag );
		return itemstack;
	}

	protected void applyPrint(
			@Nonnull final ItemStack stack,
			@Nonnull final World world,
			@Nonnull final BlockPos pos,
			@Nonnull final EnumFacing side,
			@Nonnull final VoxelBlob vb,
			@Nonnull final VoxelBlob pattern,
			@Nonnull final EntityPlayer who,
			@Nonnull final EnumHand hand )
	{
		// snag a tool...
		final ActingPlayer player = ActingPlayer.actingAs( who, hand );
		final IContinuousInventory selected = new ContinousChisels( player, pos, side );
		ItemStack spawnedItem = null;

		final List<EntityItem> spawnlist = new ArrayList<EntityItem>();

		for ( int z = 0; z < vb.detail && selected.isValid(); z++ )
		{
			for ( int y = 0; y < vb.detail && selected.isValid(); y++ )
			{
				for ( int x = 0; x < vb.detail && selected.isValid(); x++ )
				{
					final int blkID = vb.get( x, y, z );
					if ( blkID != 0 && pattern.get( x, y, z ) == 0 )
					{
						spawnedItem = ItemChisel.chiselBlock( selected, player, vb, world, pos, side, x, y, z, spawnedItem, spawnlist );
					}
				}
			}
		}

		//The state id of the last item in spawnlist.
		int entityItemState = 0;

		for ( final EntityItem ei : spawnlist )
		{
			ModUtil.feedPlayer( world, who, ei );
			entityItemState = ItemChiseledBit.getStackState( ei.getEntityItem() );
		}

		//entityItemState is always 0 when remote
		if ( !world.isRemote && entityItemState != 0 )
		{
			if( ChiselsAndBits.getConfig().voidExcessBits )
			{
				if( !ItemChiseledBit.hasBitSpace( who, entityItemState ) )
				{
					who.addChatMessage( new TextComponentTranslation( "mod.chiselsandbits.result.void_excess" ) );
				}
			}
		}
	}

	@Override
	public void scroll(
			final EntityPlayer player,
			final ItemStack stack,
			final int dwheel )
	{
		final PacketRotateVoxelBlob p = new PacketRotateVoxelBlob();
		p.rotationDirection = dwheel;
		NetworkRouter.instance.sendToServer( p );
	}

	@Override
	public void rotate(
			final ItemStack stack,
			final int rotationDirection )
	{
		EnumFacing side = ModUtil.getSide( stack );

		if ( side.getAxis() == Axis.Y )
		{
			side = EnumFacing.NORTH;
		}

		side = rotationDirection > 0 ? side.rotateY() : side.rotateYCCW();
		ModUtil.setSide( stack, side );
	}

}
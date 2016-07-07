package mod.chiselsandbits.items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import mod.chiselsandbits.bitbag.BagInventory;
import mod.chiselsandbits.chiseledblock.BlockBitInfo;
import mod.chiselsandbits.chiseledblock.BlockChiseled;
import mod.chiselsandbits.chiseledblock.ItemBlockChiseled;
import mod.chiselsandbits.chiseledblock.NBTBlobConverter;
import mod.chiselsandbits.chiseledblock.data.BitState;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.core.ClientSide;
import mod.chiselsandbits.helpers.ActingPlayer;
import mod.chiselsandbits.helpers.ContinousChisels;
import mod.chiselsandbits.helpers.IContinuousInventory;
import mod.chiselsandbits.helpers.LocalStrings;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.helpers.ModUtil.ItemStackSlot;
import mod.chiselsandbits.integration.mcmultipart.MCMultipartProxy;
import mod.chiselsandbits.interfaces.IChiselModeItem;
import mod.chiselsandbits.modes.PositivePatternMode;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemPositivePrint extends ItemNegativePrint implements IChiselModeItem
{

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	@Override
	public void addInformation(
			final ItemStack stack,
			final EntityPlayer playerIn,
			final List tooltip,
			final boolean advanced )
	{
		defaultAddInfo( stack, playerIn, tooltip, advanced );
		ChiselsAndBits.getConfig().helpText( LocalStrings.HelpPositivePrint, tooltip, ClientSide.instance.getModeKey() );

		if ( stack.hasTagCompound() )
		{
			if ( ClientSide.instance.holdingShift() )
			{
				if ( toolTipCache.needsUpdate( stack ) )
				{
					final VoxelBlob blob = ModUtil.getBlobFromStack( stack, null );
					toolTipCache.updateCachedValue( blob.listContents( new ArrayList<String>() ) );
				}

				tooltip.addAll( toolTipCache.getCached() );
			}
			else
			{
				tooltip.add( LocalStrings.ShiftDetails.getLocal() );
			}
		}
	}

	@Override
	protected NBTTagCompound getCompoundFromBlock(
			final World world,
			final BlockPos pos,
			final EntityPlayer player )
	{
		final IBlockState state = world.getBlockState( pos );
		final Block blkObj = state.getBlock();

		if ( !( blkObj instanceof BlockChiseled ) && BlockBitInfo.supportsBlock( state ) )
		{
			final NBTBlobConverter tmp = new NBTBlobConverter();

			tmp.fillWith( state );
			final NBTTagCompound comp = new NBTTagCompound();
			tmp.writeChisleData( comp, false );

			comp.setByte( ItemBlockChiseled.NBT_SIDE, (byte) ModUtil.getPlaceFace( player ).ordinal() );
			return comp;
		}

		return super.getCompoundFromBlock( world, pos, player );
	}

	@Override
	protected boolean convertToStone()
	{
		return false;
	}

	@Override
	public EnumActionResult onItemUse(
			final ItemStack stack,
			final EntityPlayer player,
			final World world,
			final BlockPos pos,
			final EnumHand hand,
			final EnumFacing side,
			final float hitX,
			final float hitY,
			final float hitZ )
	{
		if ( PositivePatternMode.getMode( stack ) == PositivePatternMode.PLACEMENT )
		{
			final ItemStack output = getPatternedItem( stack );
			if ( output != null )
			{
				final VoxelBlob pattern = ModUtil.getBlobFromStack( stack, player );
				final Map<BitState, Integer> stats = pattern.getBlockSums();

				if ( consumeEntirePattern( pattern, stats, pos, ActingPlayer.testingAs( player, hand ) ) )
				{
					final EnumActionResult res = output.getItem().onItemUse( output, player, world, pos, hand, side, hitX, hitY, hitZ );

					if ( res == EnumActionResult.SUCCESS )
					{
						consumeEntirePattern( pattern, stats, pos, ActingPlayer.actingAs( player, hand ) );
					}

					return res;
				}

				return EnumActionResult.FAIL;
			}
		}

		return super.onItemUse( stack, player, world, pos, hand, side, hitX, hitY, hitZ );
	}

	private boolean consumeEntirePattern(
			final VoxelBlob pattern,
			final Map<BitState, Integer> stats,
			final BlockPos pos,
			final ActingPlayer player )
	{
		final List<BagInventory> bags = ModUtil.getBags( player );

		for ( final Entry<BitState, Integer> type : stats.entrySet() )
		{
			final BitState inPattern = type.getKey();

			if ( type.getKey().isEmpty() )
			{
				continue;
			}

			ItemStackSlot bit = ModUtil.findBit( player, pos, inPattern );
			int stillNeeded = type.getValue() - ModUtil.consumeBagBit( bags, inPattern, type.getValue() );
			if ( stillNeeded != 0 )
			{
				for ( int x = stillNeeded; x > 0 && bit.isValid(); --x )
				{
					bit.consume();
					stillNeeded--;
					bit = ModUtil.findBit( player, pos, inPattern );
				}

				if ( stillNeeded != 0 )
				{
					return false;
				}
			}
		}

		return true;
	}

	@Override
	protected void applyPrint(
			final ItemStack stack,
			final World world,
			final BlockPos pos,
			final EnumFacing side,
			final VoxelBlob vb,
			final VoxelBlob pattern,
			final EntityPlayer who,
			final EnumHand hand )
	{
		// snag a tool...
		final ActingPlayer player = ActingPlayer.actingAs( who, hand );
		final IContinuousInventory selected = new ContinousChisels( player, pos, side );
		ItemStack spawnedItem = null;

		final VoxelBlob filled = new VoxelBlob();
		MCMultipartProxy.proxyMCMultiPart.addFiller( world, pos, filled );

		final List<BagInventory> bags = ModUtil.getBags( player );
		final List<EntityItem> spawnlist = new ArrayList<EntityItem>();

		final PositivePatternMode chiselMode = PositivePatternMode.getMode( stack );
		final boolean chisel_bits = chiselMode == PositivePatternMode.IMPOSE || chiselMode == PositivePatternMode.REPLACE;
		final boolean chisel_to_air = chiselMode == PositivePatternMode.REPLACE;

		final BitState air = vb.getStateFor( null );
		for ( int y = 0; y < vb.detail; y++ )
		{
			for ( int z = 0; z < vb.detail; z++ )
			{
				for ( int x = 0; x < vb.detail; x++ )
				{
					BitState inPlace = vb.getState( x, y, z );
					final BitState inPattern = pattern.getState( x, y, z );
					if ( inPlace != inPattern )
					{
						if ( inPlace.isFilled() && chisel_bits && selected.isValid() )
						{
							if ( chisel_to_air || inPattern.isFilled() )
							{
								spawnedItem = ItemChisel.chiselBlock( selected, player, vb, world, pos, side, x, y, z, spawnedItem, spawnlist );

								if ( spawnedItem != null )
								{
									inPlace = air;
								}
							}
						}

						if ( inPlace.isEmpty() && inPattern.isFilled() && filled.getState( x, y, z ).isEmpty() )
						{
							final ItemStackSlot bit = ModUtil.findBit( player, pos, inPattern );
							if ( ModUtil.consumeBagBit( bags, inPattern, 1 ) == 1 )
							{
								vb.setState( x, y, z, inPattern );
							}
							else if ( bit.isValid() )
							{
								vb.setState( x, y, z, inPattern );

								if ( !player.isCreative() )
								{
									bit.consume();
								}
							}
						}
					}
				}
			}
		}

		for ( final EntityItem ei : spawnlist )
		{
			ModUtil.feedPlayer( world, who, ei );
			ItemBitBag.cleanupInventory( who, ei.getEntityItem() );
		}

	}

	@Override
	public String getHighlightTip(
			final ItemStack item,
			final String displayName )
	{
		if ( ChiselsAndBits.getConfig().itemNameModeDisplay )
		{
			return displayName + " - " + PositivePatternMode.getMode( item ).string.getLocal();
		}

		return displayName;
	}

}

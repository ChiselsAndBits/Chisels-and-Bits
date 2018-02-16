package mod.chiselsandbits.network.packets;

import java.util.ArrayList;
import java.util.List;

import mod.chiselsandbits.chiseledblock.BlockChiseled;
import mod.chiselsandbits.chiseledblock.TileEntityBlockChiseled;
import mod.chiselsandbits.chiseledblock.data.BitLocation;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.chiseledblock.iterators.ChiselIterator;
import mod.chiselsandbits.chiseledblock.iterators.ChiselTypeIterator;
import mod.chiselsandbits.client.UndoTracker;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.helpers.ActingPlayer;
import mod.chiselsandbits.helpers.BitInventoryFeeder;
import mod.chiselsandbits.helpers.BitOperation;
import mod.chiselsandbits.helpers.ContinousBits;
import mod.chiselsandbits.helpers.ContinousChisels;
import mod.chiselsandbits.helpers.IContinuousInventory;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.helpers.VoxelRegionSrc;
import mod.chiselsandbits.integration.mcmultipart.MCMultipartProxy;
import mod.chiselsandbits.items.ItemBitBag;
import mod.chiselsandbits.items.ItemChisel;
import mod.chiselsandbits.items.ItemChiseledBit;
import mod.chiselsandbits.modes.ChiselMode;
import mod.chiselsandbits.network.ModPacket;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

public class PacketChisel extends ModPacket
{
	BitLocation from;
	BitLocation to;

	BitOperation place;
	EnumFacing side;
	ChiselMode mode;
	EnumHand hand;

	@Deprecated
	// never call this...
	public PacketChisel()
	{
	}

	public PacketChisel(
			final BitOperation place,
			final BitLocation from,
			final BitLocation to,
			final EnumFacing side,
			final ChiselMode mode,
			final EnumHand hand )
	{
		this.place = place;
		this.from = BitLocation.min( from, to );
		this.to = BitLocation.max( from, to );
		this.side = side;
		this.mode = mode;
		this.hand = hand;
	}

	public PacketChisel(
			final BitOperation place,
			final BitLocation location,
			final EnumFacing side,
			final ChiselMode mode,
			final EnumHand hand )
	{
		this.place = place;
		from = to = location;
		this.side = side;
		this.mode = mode;
		this.hand = hand;
	}

	@Override
	public void server(
			final EntityPlayerMP playerEntity )
	{
		doAction( playerEntity );
	}

	public int doAction(
			final EntityPlayer who )
	{
		final World world = who.worldObj;
		final ActingPlayer player = ActingPlayer.actingAs( who, hand );

		final int minX = Math.min( from.blockPos.getX(), to.blockPos.getX() );
		final int maxX = Math.max( from.blockPos.getX(), to.blockPos.getX() );
		final int minY = Math.min( from.blockPos.getY(), to.blockPos.getY() );
		final int maxY = Math.max( from.blockPos.getY(), to.blockPos.getY() );
		final int minZ = Math.min( from.blockPos.getZ(), to.blockPos.getZ() );
		final int maxZ = Math.max( from.blockPos.getZ(), to.blockPos.getZ() );

		int returnVal = 0;

		boolean update = false;
		ItemStack extracted = null;
		ItemStack bitPlaced = null;

		final List<EntityItem> spawnlist = new ArrayList<EntityItem>();

		UndoTracker.getInstance().beginGroup( who );

		try
		{
			for ( int xOff = minX; xOff <= maxX; ++xOff )
			{
				for ( int yOff = minY; yOff <= maxY; ++yOff )
				{
					for ( int zOff = minZ; zOff <= maxZ; ++zOff )
					{
						final BlockPos pos = new BlockPos( xOff, yOff, zOff );

						final int placeStateID = place.usesBits() ? ItemChiseledBit.getStackState( who.getHeldItem( hand ) ) : 0;
						final IContinuousInventory chisels = new ContinousChisels( player, pos, side );
						final IContinuousInventory bits = new ContinousBits( player, pos, placeStateID );

						IBlockState blkstate = world.getBlockState( pos );
						Block blkObj = blkstate.getBlock();

						if ( place.usesChisels() )
						{
							if ( !chisels.isValid() || blkObj == null || blkstate == null || !ItemChisel.canMine( chisels, blkstate, who, world, pos ) )
							{
								continue;
							}
						}

						if ( place.usesBits() )
						{
							if ( !bits.isValid() || blkObj == null || blkstate == null )
							{
								continue;
							}
						}

						if ( world.getBlockState( pos ).getBlock().isReplaceable( world, pos ) && place.usesBits() )
						{
							world.setBlockToAir( pos );
						}

						if ( BlockChiseled.replaceWithChisled( world, pos, blkstate, placeStateID, true ) )
						{
							blkstate = world.getBlockState( pos );
							blkObj = blkstate.getBlock();
						}

						final TileEntity te = ModUtil.getChiseledTileEntity( world, pos, place.usesBits() );
						if ( te instanceof TileEntityBlockChiseled )
						{
							final TileEntityBlockChiseled tec = (TileEntityBlockChiseled) te;

							final VoxelBlob mask = new VoxelBlob();
							MCMultipartProxy.proxyMCMultiPart.addFiller( world, pos, mask );

							// adjust voxel state...
							final VoxelBlob vb = tec.getBlob();

							final ChiselIterator i = getIterator( new VoxelRegionSrc( world, pos, 1 ), pos, place );
							while ( i.hasNext() )
							{
								if ( place.usesChisels() && chisels.isValid() )
								{
									if ( !place.usesBits() || vb.get( i.x(), i.y(), i.z() ) != placeStateID )
									{
										extracted = ItemChisel.chiselBlock( chisels, player, vb, world, pos, i.side(), i.x(), i.y(), i.z(), extracted, spawnlist );
									}
								}

								if ( place.usesBits() && bits.isValid() )
								{
									if ( mask.get( i.x(), i.y(), i.z() ) == 0 )
									{
										bitPlaced = bits.getItem( 0 ).getStack();
										update = ItemChiseledBit.placeBit( bits, player, vb, i.x(), i.y(), i.z() ) || update;
									}
								}
							}

							if ( update )
							{
								tec.completeEditOperation( vb );
								returnVal++;
							}
							else if ( extracted != null )
							{
								tec.completeEditOperation( vb );
								returnVal++;
							}

						}

					}
				}
			}

			BitInventoryFeeder feeder = new BitInventoryFeeder( who, world );
			for ( final EntityItem ei : spawnlist )
			{
				feeder.addItem(ei);
				ItemBitBag.cleanupInventory( who, ei.getEntityItem() );
			}
			
			if ( place.usesBits() )
			{
				ItemBitBag.cleanupInventory( who, bitPlaced != null ? bitPlaced : new ItemStack( ChiselsAndBits.getItems().itemBlockBit, 1, OreDictionary.WILDCARD_VALUE ) );
			}

		}
		finally
		{
			UndoTracker.getInstance().endGroup( who );
		}

		return returnVal;
	}

	private ChiselIterator getIterator(
			final VoxelRegionSrc vb,
			final BlockPos pos,
			final BitOperation place )
	{
		if ( mode == ChiselMode.DRAWN_REGION )
		{
			final int bitX = pos.getX() == from.blockPos.getX() ? from.bitX : 0;
			final int bitY = pos.getY() == from.blockPos.getY() ? from.bitY : 0;
			final int bitZ = pos.getZ() == from.blockPos.getZ() ? from.bitZ : 0;

			final int scaleX = ( pos.getX() == to.blockPos.getX() ? to.bitX : 15 ) - bitX + 1;
			final int scaleY = ( pos.getY() == to.blockPos.getY() ? to.bitY : 15 ) - bitY + 1;
			final int scaleZ = ( pos.getZ() == to.blockPos.getZ() ? to.bitZ : 15 ) - bitZ + 1;

			return new ChiselTypeIterator( VoxelBlob.dim, bitX, bitY, bitZ, scaleX, scaleY, scaleZ, side );
		}

		return ChiselTypeIterator.create( VoxelBlob.dim, from.bitX, from.bitY, from.bitZ, vb, mode, side, place.usePlacementOffset() );
	}

	@Override
	public void readPayload(
			final PacketBuffer buffer )
	{
		from = readBitLoc( buffer );
		to = readBitLoc( buffer );

		place = buffer.readEnumValue( BitOperation.class );
		side = EnumFacing.VALUES[buffer.readVarIntFromBuffer()];
		mode = ChiselMode.values()[buffer.readVarIntFromBuffer()];
		hand = EnumHand.values()[buffer.readVarIntFromBuffer()];
	}

	@Override
	public void getPayload(
			final PacketBuffer buffer )
	{
		writeBitLoc( from, buffer );
		writeBitLoc( to, buffer );

		buffer.writeEnumValue( place );
		buffer.writeVarIntToBuffer( side.ordinal() );
		buffer.writeVarIntToBuffer( mode.ordinal() );
		buffer.writeVarIntToBuffer( hand.ordinal() );
	}

	private BitLocation readBitLoc(
			final PacketBuffer buffer )
	{
		return new BitLocation( buffer.readBlockPos(), buffer.readByte(), buffer.readByte(), buffer.readByte() );
	}

	private void writeBitLoc(
			final BitLocation from2,
			final PacketBuffer buffer )
	{
		buffer.writeBlockPos( from2.blockPos );
		buffer.writeByte( from2.bitX );
		buffer.writeByte( from2.bitY );
		buffer.writeByte( from2.bitZ );
	}

}

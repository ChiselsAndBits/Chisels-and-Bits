package mod.chiselsandbits.network.packets;

import mod.chiselsandbits.chiseledblock.BlockChiseled;
import mod.chiselsandbits.chiseledblock.BlockChiseled.ReplaceWithChiseledValue;
import mod.chiselsandbits.chiseledblock.TileEntityBlockChiseled;
import mod.chiselsandbits.chiseledblock.data.BitLocation;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.chiseledblock.iterators.ChiselIterator;
import mod.chiselsandbits.chiseledblock.iterators.ChiselTypeIterator;
import mod.chiselsandbits.client.UndoTracker;
import mod.chiselsandbits.helpers.*;
import mod.chiselsandbits.items.ItemBitBag;
import mod.chiselsandbits.items.ItemChisel;
import mod.chiselsandbits.items.ItemChiseledBit;
import mod.chiselsandbits.modes.ChiselMode;
import mod.chiselsandbits.network.ModPacket;
import mod.chiselsandbits.registry.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.ArrayList;
import java.util.List;

public class PacketChisel extends ModPacket
{
	BitLocation from;
	BitLocation to;

	BitOperation place;
	Direction    side;
	ChiselMode   mode;
	Hand         hand;

	public PacketChisel(PacketBuffer buffer)
	{
	    readPayload(buffer);
	}

	public PacketChisel(
			final BitOperation place,
			final BitLocation from,
			final BitLocation to,
			final Direction side,
			final ChiselMode mode,
			final Hand hand )
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
			final Direction side,
			final ChiselMode mode,
			final Hand hand )
	{
		this.place = place;
		from = to = location;
		this.side = side;
		this.mode = mode;
		this.hand = hand;
	}

	@Override
	public void server(
			final ServerPlayerEntity playerEntity )
	{
		doAction( playerEntity );
	}

	public int doAction(
			final PlayerEntity who )
	{
		final World world = who.getEntityWorld();
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

		final List<ItemEntity> spawnlist = new ArrayList<ItemEntity>();

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

						BlockState blkstate = world.getBlockState( pos );
						Block blkObj = blkstate.getBlock();

						BlockSnapshot blocksnapshot = BlockSnapshot.create(world.getDimensionKey(), world, pos);

						if (ForgeEventFactory.onBlockPlace(player.getPlayer(), blocksnapshot, Direction.UP)) {
							return 0;
						}

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

						if (world instanceof ServerWorld && world.getServer() != null && world.getServer().isBlockProtected((ServerWorld) world, pos, player.getPlayer() ) )
						{
							continue;
						}

						if ( !world.isBlockModifiable( player.getPlayer(), pos ) )
						{
							continue;
						}

						if ( world.getBlockState( pos ).isReplaceable(new BlockItemUseContext(who, hand, ItemStack.EMPTY, new BlockRayTraceResult(Vector3d.ZERO, Direction.NORTH, pos, false))) && place.usesBits() )
						{
							world.setBlockState(pos, Blocks.AIR.getDefaultState());
						}

						ReplaceWithChiseledValue rv = null;
						if ( (rv=BlockChiseled.replaceWithChiseled( world, pos, blkstate, placeStateID, true )).success )
						{
							blkstate = world.getBlockState( pos );
							blkObj = blkstate.getBlock();
						}

						final TileEntity te = rv.te != null ? rv.te : ModUtil.getChiseledTileEntity( world, pos, place.usesBits() );
						if ( te instanceof TileEntityBlockChiseled )
						{
							final TileEntityBlockChiseled tec = (TileEntityBlockChiseled) te;

							final VoxelBlob mask = new VoxelBlob();

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
			for ( final ItemEntity ei : spawnlist )
			{
				feeder.addItem( ei );
				ItemBitBag.cleanupInventory( who, ei.getItem() );
			}

			if ( place.usesBits() )
			{
				ItemBitBag.cleanupInventory( who, bitPlaced != null ? bitPlaced : new ItemStack(ModItems.ITEM_BLOCK_BIT.get(), 1) );
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
		side = Direction.values()[buffer.readInt()];
		mode = ChiselMode.values()[buffer.readInt()];
		hand = Hand.values()[buffer.readInt()];
	}

	@Override
	public void getPayload(
			final PacketBuffer buffer )
	{
		writeBitLoc( from, buffer );
		writeBitLoc( to, buffer );

		buffer.writeEnumValue( place );
		buffer.writeInt( side.ordinal() );
		buffer.writeInt( mode.ordinal() );
		buffer.writeInt( hand.ordinal() );
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

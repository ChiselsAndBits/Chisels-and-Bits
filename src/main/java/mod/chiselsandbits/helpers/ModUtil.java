package mod.chiselsandbits.helpers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Pair;

import mod.chiselsandbits.bitbag.BagInventory;
import mod.chiselsandbits.chiseledblock.ItemBlockChiseled;
import mod.chiselsandbits.chiseledblock.NBTBlobConverter;
import mod.chiselsandbits.chiseledblock.TileEntityBlockChiseled;
import mod.chiselsandbits.chiseledblock.data.IntegerBox;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.helpers.StateLookup.CachedStateLookup;
import mod.chiselsandbits.integration.mcmultipart.MCMultipartProxy;
import mod.chiselsandbits.integration.mods.LittleTiles;
import mod.chiselsandbits.items.ItemBitBag;
import mod.chiselsandbits.items.ItemBitBag.BagPos;
import mod.chiselsandbits.items.ItemChiseledBit;
import mod.chiselsandbits.items.ItemNegativePrint;
import mod.chiselsandbits.items.ItemPositivePrint;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.fml.common.eventhandler.Event.Result;

public class ModUtil
{

	@Nonnull
	public static final String NBT_SIDE = "side";

	@Nonnull
	public static final String NBT_BLOCKENTITYTAG = "BlockEntityTag";

	private final static Random RAND = new Random();
	private final static float DEG_TO_RAD = 0.017453292f;

	static public EnumFacing getPlaceFace(
			final EntityLivingBase placer )
	{
		return EnumFacing.getHorizontal( MathHelper.floor_double( placer.rotationYaw * 4.0F / 360.0F + 0.5D ) & 3 ).getOpposite();
	}

	static public Pair<Vec3d, Vec3d> getPlayerRay(
			final EntityPlayer playerIn )
	{
		double reachDistance = 5.0d;

		final double x = playerIn.prevPosX + ( playerIn.posX - playerIn.prevPosX );
		final double y = playerIn.prevPosY + ( playerIn.posY - playerIn.prevPosY ) + playerIn.getEyeHeight();
		final double z = playerIn.prevPosZ + ( playerIn.posZ - playerIn.prevPosZ );

		final float playerPitch = playerIn.prevRotationPitch + ( playerIn.rotationPitch - playerIn.prevRotationPitch );
		final float playerYaw = playerIn.prevRotationYaw + ( playerIn.rotationYaw - playerIn.prevRotationYaw );

		final float yawRayX = MathHelper.sin( -playerYaw * DEG_TO_RAD - (float) Math.PI );
		final float yawRayZ = MathHelper.cos( -playerYaw * DEG_TO_RAD - (float) Math.PI );

		final float pitchMultiplier = -MathHelper.cos( -playerPitch * DEG_TO_RAD );
		final float eyeRayY = MathHelper.sin( -playerPitch * DEG_TO_RAD );
		final float eyeRayX = yawRayX * pitchMultiplier;
		final float eyeRayZ = yawRayZ * pitchMultiplier;

		if ( playerIn instanceof EntityPlayerMP )
		{
			reachDistance = ( (EntityPlayerMP) playerIn ).interactionManager.getBlockReachDistance();
		}

		final Vec3d from = new Vec3d( x, y, z );
		final Vec3d to = from.addVector( eyeRayX * reachDistance, eyeRayY * reachDistance, eyeRayZ * reachDistance );

		return Pair.of( from, to );
	}

	static public IItemInInventory findBit(
			final ActingPlayer who,
			final BlockPos pos,
			final int StateID )
	{
		final ItemStack inHand = who.getCurrentEquippedItem();
		final IInventory inv = who.getInventory();
		final boolean canEdit = who.canPlayerManipulate( pos, EnumFacing.UP, inHand, true );

		if ( inHand != null && getStackSize( inHand ) > 0 && inHand.getItem() instanceof ItemChiseledBit && ItemChiseledBit.getStackState( inHand ) == StateID )
		{
			return new ItemStackSlot( inv, who.getCurrentItem(), inHand, who, canEdit );
		}

		for ( int x = 0; x < inv.getSizeInventory(); x++ )
		{
			final ItemStack is = inv.getStackInSlot( x );
			if ( is != null && getStackSize( is ) > 0 && is.getItem() instanceof ItemChiseledBit && ItemChiseledBit.sameBit( is, StateID ) )
			{
				return new ItemStackSlot( inv, x, is, who, canEdit );
			}
		}

		return new ItemStackSlot( inv, -1, ModUtil.getEmptyStack(), who, canEdit );
	}

	public static @Nonnull ItemStack copy(
			final ItemStack st )
	{
		if ( st == null )
		{
			return ModUtil.getEmptyStack();
		}

		return nonNull( st.copy() );
	}

	public static @Nonnull ItemStack nonNull(
			final ItemStack st )
	{
		if ( st == null )
		{
			return ModUtil.getEmptyStack();
		}

		return st;
	}

	public static boolean isHoldingPattern(
			final EntityPlayer player )
	{
		final ItemStack inHand = player.getHeldItemMainhand();

		if ( inHand != null && inHand.getItem() instanceof ItemPositivePrint )
		{
			return true;
		}

		if ( inHand != null && inHand.getItem() instanceof ItemNegativePrint )
		{
			return true;
		}

		return false;
	}

	public static boolean isHoldingChiseledBlock(
			final EntityPlayer player )
	{
		final ItemStack inHand = player.getHeldItemMainhand();

		if ( inHand != null && inHand.getItem() instanceof ItemBlockChiseled )
		{
			return true;
		}

		return false;
	}

	public static int getRotationIndex(
			final EnumFacing face )
	{
		return face.getHorizontalIndex();
	}

	public static int getRotations(
			final EntityLivingBase placer,
			final EnumFacing oldYaw )
	{
		final EnumFacing newFace = ModUtil.getPlaceFace( placer );

		int rotations = getRotationIndex( newFace ) - getRotationIndex( oldYaw );

		// work out the rotation math...
		while ( rotations < 0 )
		{
			rotations = 4 + rotations;
		}
		while ( rotations > 4 )
		{
			rotations = rotations - 4;
		}

		return 4 - rotations;
	}

	public static BlockPos getPartialOffset(
			final EnumFacing side,
			final BlockPos partial,
			final IntegerBox modelBounds )
	{
		int offset_x = modelBounds.minX;
		int offset_y = modelBounds.minY;
		int offset_z = modelBounds.minZ;

		final int partial_x = partial.getX();
		final int partial_y = partial.getY();
		final int partial_z = partial.getZ();

		int middle_x = ( modelBounds.maxX - modelBounds.minX ) / -2;
		int middle_y = ( modelBounds.maxY - modelBounds.minY ) / -2;
		int middle_z = ( modelBounds.maxZ - modelBounds.minZ ) / -2;

		switch ( side )
		{
			case DOWN:
				offset_y = modelBounds.maxY;
				middle_y = 0;
				break;
			case EAST:
				offset_x = modelBounds.minX;
				middle_x = 0;
				break;
			case NORTH:
				offset_z = modelBounds.maxZ;
				middle_z = 0;
				break;
			case SOUTH:
				offset_z = modelBounds.minZ;
				middle_z = 0;
				break;
			case UP:
				offset_y = modelBounds.minY;
				middle_y = 0;
				break;
			case WEST:
				offset_x = modelBounds.maxX;
				middle_x = 0;
				break;
			default:
				throw new NullPointerException();
		}

		final int t_x = -offset_x + middle_x + partial_x;
		final int t_y = -offset_y + middle_y + partial_y;
		final int t_z = -offset_z + middle_z + partial_z;

		return new BlockPos( t_x, t_y, t_z );
	}

	@SafeVarargs
	static public <T> T firstNonNull(
			final T... options )
	{
		for ( final T i : options )
		{
			if ( i != null )
			{
				return i;
			}
		}

		throw new NullPointerException( "Unable to find a non null item." );
	}

	public static TileEntity getTileEntitySafely(
			final @Nonnull IBlockAccess world,
			final @Nonnull BlockPos pos )
	{
		// not going to lie, this is really stupid.
		if ( world instanceof ChunkCache )
		{
			return ( (ChunkCache) world ).getTileEntity( pos, Chunk.EnumCreateEntityType.CHECK );
		}

		// also stupid...
		else if ( world instanceof World )
		{
			return ( (World) world ).getChunkFromBlockCoords( pos ).getTileEntity( pos, Chunk.EnumCreateEntityType.CHECK );
		}

		// yep... stupid.
		else
		{
			return world.getTileEntity( pos );
		}
	}

	public static TileEntityBlockChiseled getChiseledTileEntity(
			@Nonnull final IBlockAccess world,
			@Nonnull final BlockPos pos )
	{
		final TileEntity te = getTileEntitySafely( world, pos );
		if ( te instanceof TileEntityBlockChiseled )
		{
			return (TileEntityBlockChiseled) te;
		}

		return MCMultipartProxy.proxyMCMultiPart.getPartFromBlockAccess( world, pos );
	}

	public static TileEntityBlockChiseled getChiseledTileEntity(
			@Nonnull final World world,
			@Nonnull final BlockPos pos,
			final boolean create )
	{
		if ( world.isBlockLoaded( pos ) )
		{
			final TileEntity te = world.getTileEntity( pos );
			if ( te instanceof TileEntityBlockChiseled )
			{
				return (TileEntityBlockChiseled) te;
			}

			if ( te != null )
			{
				try
				{
					TileEntityBlockChiseled converted = LittleTiles.getConvertedTE( te, false );
					if ( converted != null )
					{
						return converted;
					}
				}
				catch ( Exception e )
				{
					e.printStackTrace();
				}
			}

			return MCMultipartProxy.proxyMCMultiPart.getChiseledTileEntity( world, pos, create );
		}
		return null;
	}

	public static void removeChisledBlock(
			@Nonnull final World world,
			@Nonnull final BlockPos pos )
	{
		final TileEntity te = world.getTileEntity( pos );

		if ( te instanceof TileEntityBlockChiseled )
		{
			world.setBlockToAir( pos ); // no physical matter left...
			return;
		}

		MCMultipartProxy.proxyMCMultiPart.removeChisledBlock( te );
		world.markBlockRangeForRenderUpdate( pos, pos );
	}

	private final static Random itemRand = new Random();

	public static void feedPlayer(
			final World world,
			final EntityPlayer player,
			final EntityItem ei )
	{
		ItemStack is = ModUtil.nonNull( ei.getEntityItem() );

		final List<BagPos> bags = ItemBitBag.getBags( player.inventory );

		if ( !containsAtLeastOneOf( player.inventory, is ) )
		{
			final ItemStack minSize = is.copy();

			if ( getStackSize( minSize ) > minSize.getMaxStackSize() )
			{
				setStackSize( minSize, minSize.getMaxStackSize() );
			}

			adjustStackSize( is, -getStackSize( minSize ) );
			player.inventory.addItemStackToInventory( minSize );
			adjustStackSize( is, getStackSize( minSize ) );
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
			// canceled...
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

	}

	private static void spawnItem(
			World world,
			EntityItem ei )
	{
		if ( world.isRemote ) // no spawning items on the client.
			return;

		world.spawnEntityInWorld( ei );
	}

	public static boolean containsAtLeastOneOf(
			final IInventory inv,
			final ItemStack is )
	{
		boolean seen = false;
		for ( int x = 0; x < inv.getSizeInventory(); x++ )
		{
			final ItemStack which = inv.getStackInSlot( x );

			if ( which != null && which.getItem() == is.getItem() && ItemChiseledBit.sameBit( which, ItemChiseledBit.getStackState( is ) ) )
			{
				if ( !seen )
				{
					seen = true;
				}
			}
		}
		return seen;
	}

	public static List<BagInventory> getBags(
			final ActingPlayer player )
	{
		if ( player.isCreative() )
		{
			return java.util.Collections.emptyList();
		}

		final List<BagInventory> bags = new ArrayList<BagInventory>();
		final IInventory inv = player.getInventory();

		for ( int zz = 0; zz < inv.getSizeInventory(); zz++ )
		{
			final ItemStack which = inv.getStackInSlot( zz );
			if ( which != null && which.getItem() instanceof ItemBitBag )
			{
				bags.add( new BagInventory( which ) );
			}
		}

		return bags;
	}

	public static int consumeBagBit(
			final List<BagInventory> bags,
			final int inPattern,
			final int howMany )
	{
		int remaining = howMany;
		for ( final BagInventory inv : bags )
		{
			remaining -= inv.extractBit( inPattern, remaining );
			if ( remaining == 0 )
			{
				return howMany;
			}
		}

		return howMany - remaining;
	}

	public static VoxelBlob getBlobFromStack(
			final ItemStack stack,
			final EntityLivingBase rotationPlayer )
	{
		if ( stack.hasTagCompound() )
		{
			final NBTBlobConverter tmp = new NBTBlobConverter();

			NBTTagCompound cData = getSubCompound( stack, NBT_BLOCKENTITYTAG, false );

			if ( cData == null )
			{
				cData = stack.getTagCompound();
			}

			tmp.readChisleData( cData, VoxelBlob.VERSION_ANY );
			VoxelBlob blob = tmp.getBlob();

			if ( rotationPlayer != null )
			{
				int xrotations = ModUtil.getRotations( rotationPlayer, ModUtil.getSide( stack ) );
				while ( xrotations-- > 0 )
				{
					blob = blob.spin( Axis.Y );
				}
			}

			return blob;
		}

		return new VoxelBlob();
	}

	public static void sendUpdate(
			@Nonnull final World worldObj,
			@Nonnull final BlockPos pos )
	{
		final IBlockState state = worldObj.getBlockState( pos );
		worldObj.notifyBlockUpdate( pos, state, state, 0 );
	}

	public static ItemStack getItemFromBlock(
			@Nonnull final IBlockState state )
	{
		final Block blk = state.getBlock();

		final Item i = blk.getItemDropped( state, RAND, 0 );
		final int meta = blk.getMetaFromState( state );
		final int damage = blk.damageDropped( state );
		final Item blockVarient = Item.getItemFromBlock( blk );

		// darn conversions...
		if ( blk == Blocks.GRASS )
		{
			return new ItemStack( Blocks.GRASS );
		}

		if ( i == null || blockVarient == null || blockVarient != i )
		{
			return ModUtil.getEmptyStack();
		}

		if ( blockVarient instanceof ItemBlock )
		{
			final ItemBlock ib = (ItemBlock) blockVarient;
			if ( meta != ib.getMetadata( damage ) )
			{
				// this item dosn't drop itself... BAIL!
				return ModUtil.getEmptyStack();
			}
		}

		return new ItemStack( i, 1, damage );
	}

	public static boolean isNormalCube(
			final IBlockState blockType )
	{
		return blockType.isNormalCube();
	}

	public static EnumFacing getSide(
			final ItemStack stack )
	{
		if ( stack != null )
		{
			final NBTTagCompound blueprintTag = stack.getTagCompound();

			int byteValue = EnumFacing.NORTH.ordinal();

			if ( blueprintTag == null )
			{
				return EnumFacing.NORTH;
			}

			if ( blueprintTag.hasKey( NBT_SIDE ) )
			{
				byteValue = blueprintTag.getByte( NBT_SIDE );
			}

			if ( blueprintTag.hasKey( NBT_BLOCKENTITYTAG ) )
			{
				final NBTTagCompound c = blueprintTag.getCompoundTag( NBT_BLOCKENTITYTAG );
				if ( c.hasKey( NBT_SIDE ) )
				{
					byteValue = c.getByte( NBT_SIDE );
				}
			}

			EnumFacing side = EnumFacing.NORTH;

			if ( byteValue >= 0 && byteValue < EnumFacing.values().length )
			{
				side = EnumFacing.values()[byteValue];
			}

			if ( side == EnumFacing.DOWN || side == EnumFacing.UP )
			{
				side = EnumFacing.NORTH;
			}

			return side;
		}

		return EnumFacing.NORTH;
	}

	public static void setSide(
			final ItemStack stack,
			final EnumFacing side )
	{
		if ( stack != null )
		{
			NBTTagCompound blueprintTag = stack.getTagCompound();

			if ( blueprintTag == null )
			{
				blueprintTag = new NBTTagCompound();
			}
			if ( blueprintTag.hasKey( NBT_BLOCKENTITYTAG ) )
			{
				blueprintTag.getCompoundTag( NBT_BLOCKENTITYTAG ).setByte( NBT_SIDE, (byte) +side.ordinal() );
			}

			blueprintTag.setInteger( NBT_SIDE, +side.ordinal() );

			stack.setTagCompound( blueprintTag );
		}
	}

	private static StateLookup IDRelay = new StateLookup();

	public static IBlockState getStateById(
			final int blockStateID )
	{
		return IDRelay.getStateById( blockStateID );
	}

	public static int getStateId(
			final IBlockState state )
	{
		return IDRelay.getStateId( state );
	}

	public static void cacheFastStates()
	{
		if ( !ChiselsAndBits.getConfig().lowMemoryMode )
		{
			// cache id -> state table as an array for faster rendering lookups.
			IDRelay = new CachedStateLookup();
		}
	}

	public static int getStackSize(
			final ItemStack stack )
	{
		return stack == null ? 0 : stack.func_190916_E();
	}

	public static void setStackSize(
			final @Nonnull ItemStack stack,
			final int stackSize )
	{
		stack.func_190920_e( stackSize );
	}

	public static void adjustStackSize(
			final @Nonnull ItemStack is,
			final int sizeDelta )
	{
		setStackSize( is, getStackSize( is ) + sizeDelta );
	}

	public static NBTTagCompound getSubCompound(
			final ItemStack stack,
			final String tag,
			final boolean create )
	{
		if ( create )
		{
			return stack.func_190925_c( tag );
		}
		else
		{
			return stack.getSubCompound( tag );
		}
	}

	public static @Nonnull ItemStack getEmptyStack()
	{
		return ItemStack.field_190927_a;
	}

	public static boolean notEmpty(
			final ItemStack itemStack )
	{
		return itemStack != null && !itemStack.func_190926_b();
	}

	public static boolean isEmpty(
			final ItemStack itemStack )
	{
		return itemStack == null || itemStack.func_190926_b();
	}

	public static @Nonnull NBTTagCompound getTagCompound(
			final ItemStack ei )
	{
		final NBTTagCompound c = ei.getTagCompound();

		if ( c == null )
		{
			return new NBTTagCompound();
		}

		return c;
	}

	@SuppressWarnings( "deprecation" )
	public static IBlockState getStateFromItem(
			final ItemStack is )
	{
		try
		{
			if ( !ModUtil.isEmpty( is ) && is.getItem() instanceof ItemBlock )
			{
				final ItemBlock iblk = (ItemBlock) is.getItem();
				final IBlockState state = iblk.getBlock().getStateFromMeta( iblk.getMetadata( is.getItemDamage() ) );
				final ItemStack out = ModUtil.getItemFromBlock( state );

				if ( !ModUtil.isEmpty( out ) && out.getItem() == is.getItem() && is.getItemDamage() == out.getItemDamage() )
				{
					return state;
				}
			}
		}
		catch ( final Throwable t )
		{
			// : (
		}

		return Blocks.AIR.getDefaultState();
	}

	public static void damageItem(
			@Nonnull final ItemStack is,
			@Nonnull final Random r )
	{
		if ( is.isItemStackDamageable() )
		{
			if ( is.attemptDamageItem( 1, r, null ) )
			{
				is.func_190918_g( 1 );
			}
		}
	}

	@Nonnull
	public static ItemStack makeStack(
			final Item item )
	{
		return makeStack( item, 1 );
	}

	@Nonnull
	public static ItemStack makeStack(
			final Item item,
			final int stackSize )
	{
		return makeStack( item, 1, 0 );
	}

	@Nonnull
	public static ItemStack makeStack(
			final Item item,
			final int stackSize,
			final int damage )
	{
		if ( item == null || stackSize < 1 )
		{
			return ModUtil.getEmptyStack();
		}

		return new ItemStack( item, stackSize, damage );
	}

	public static boolean isEmpty(
			final Item item )
	{
		return item == Item.REGISTRY.getObjectById( 0 );
	}

}

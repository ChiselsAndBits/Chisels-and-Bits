package mod.chiselsandbits.block;

import com.communi.suggestu.saecularia.caudices.core.block.IBlockWithWorldlyProperties;
import com.communi.suggestu.scena.core.blockstate.ILevelBasedPropertyAccessor;
import com.communi.suggestu.scena.core.dist.Dist;
import com.communi.suggestu.scena.core.dist.DistExecutor;
import com.communi.suggestu.scena.core.entity.IPlayerInventoryManager;
import com.google.common.base.Suppliers;
import mod.chiselsandbits.ChiselsAndBits;
import mod.chiselsandbits.api.axissize.CollisionType;
import mod.chiselsandbits.api.block.IMultiStateBlock;
import mod.chiselsandbits.api.block.entity.IMultiStateBlockEntity;
import mod.chiselsandbits.api.blockinformation.IBlockInformation;
import mod.chiselsandbits.api.change.IChangeTrackerManager;
import mod.chiselsandbits.api.chiseling.eligibility.IEligibilityManager;
import mod.chiselsandbits.api.config.IClientConfiguration;
import mod.chiselsandbits.api.config.IServerConfiguration;
import mod.chiselsandbits.api.exceptions.SpaceOccupiedException;
import mod.chiselsandbits.api.item.multistate.IMultiStateItemFactory;
import mod.chiselsandbits.api.item.multistate.IMultiStateItemStack;
import mod.chiselsandbits.api.multistate.StateEntrySize;
import mod.chiselsandbits.api.multistate.accessor.IAreaAccessor;
import mod.chiselsandbits.api.multistate.mutator.IMutableStateEntryInfo;
import mod.chiselsandbits.api.util.IBatchMutation;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshot;
import mod.chiselsandbits.api.util.ArrayUtils;
import mod.chiselsandbits.api.util.SingleBlockBlockReader;
import mod.chiselsandbits.api.util.SingleBlockWorldReader;
import mod.chiselsandbits.api.variant.state.IStateVariant;
import mod.chiselsandbits.api.variant.state.IStateVariantManager;
import mod.chiselsandbits.api.voxelshape.IVoxelShapeManager;
import mod.chiselsandbits.block.entities.ChiseledBlockEntity;
import mod.chiselsandbits.blockinformation.BlockInformation;
import mod.chiselsandbits.client.block.ClientChiseledBlockExtensions;
import mod.chiselsandbits.clipboard.CreativeClipboardUtils;
import mod.chiselsandbits.network.packets.NeighborBlockUpdatedPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class ChiseledBlock extends Block implements IMultiStateBlock, SimpleWaterloggedBlock, IBlockWithWorldlyProperties
{
    public ChiseledBlock(Properties properties)
    {
        super(
          properties
            .isViewBlocking(ChiseledBlock::isViewBlocking)
            .noOcclusion()
        );
    }

    private static boolean isViewBlocking(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos) {
        return false;
    }

    @Override
    public float getFriction(final BlockState state, final LevelReader levelReader, final BlockPos pos, @Nullable final Entity entity)
    {
        final float frictionValue = getBlockEntity(levelReader, pos)
                 .map(multiStateBlockEntity -> multiStateBlockEntity.getStatistics().getSlipperiness())
                 .orElse(0f);

        return Float.isNaN(frictionValue) || frictionValue <= 0.0001f ? 0.6f : frictionValue;
    }

    @Override
    public int getLightEmission(final BlockState state, final BlockGetter blockGetter, final BlockPos pos)
    {
        return getBlockEntity(blockGetter, pos)
          .map(multiStateBlockEntity -> blockGetter.getMaxLightLevel() * multiStateBlockEntity.getStatistics().getLightEmissionFactor())
          .map(inertValue -> inertValue * IServerConfiguration.getInstance().getLightFactorMultiplier().get())
          .map(consumedValue -> Math.max(consumedValue, 0))
          .map(consumedValue -> Math.min(consumedValue, blockGetter.getMaxLightLevel()))
          .orElse(0d).intValue();
    }

    @Override
    public boolean canHarvestBlock(final BlockState state, final BlockGetter blockGetter, final BlockPos pos, final Player player)
    {
        return getBlockEntity(blockGetter, pos)
                 .map(e -> {
                     final IBlockInformation primaryState = e.getStatistics().getPrimaryState();

                     return ILevelBasedPropertyAccessor.getInstance().canHarvestBlock(
                       new SingleBlockBlockReader(
                         primaryState,
                         pos,
                         blockGetter
                       ),
                       pos,
                       player
                     );
                 })
                 .orElse(true);
    }

    @Override
    public ItemStack getCloneItemStack(final BlockState state, final HitResult target, final BlockGetter blockGetter, final BlockPos pos, final Player player)
    {
        if (!(target instanceof final BlockHitResult blockRayTraceResult))
        {
            return ItemStack.EMPTY;
        }

        if (
          (!IClientConfiguration.getInstance().getInvertPickBlockBehaviour().get() && player.isShiftKeyDown()) ||
          (IClientConfiguration.getInstance().getInvertPickBlockBehaviour().get() && !player.isShiftKeyDown())
        )
        {
            return getBlockEntity(blockGetter, pos)
                    .map(e -> {
                        final IMultiStateBlockEntity.BlockStack result = e.getBlockStack();
                        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> CreativeClipboardUtils.addPickedBlock(result.multiStateItemStack()));
                        return result.stack();
                    })
              .orElse(ItemStack.EMPTY);
        }

        return getBlockEntity(blockGetter, pos)
          .flatMap(e -> {
              final Vec3 hitVec = blockRayTraceResult.getLocation();
              final BlockPos blockPos = blockRayTraceResult.getBlockPos();
              final Vec3 accuratePos = new Vec3(
                blockPos.getX(),
                blockPos.getY(),
                blockPos.getZ()
              );
              final Vec3 faceOffset = new Vec3(
                blockRayTraceResult.getDirection().getOpposite().getStepX() * StateEntrySize.current().getSizePerHalfBit(),
                blockRayTraceResult.getDirection().getOpposite().getStepY() * StateEntrySize.current().getSizePerHalfBit(),
                blockRayTraceResult.getDirection().getOpposite().getStepZ() * StateEntrySize.current().getSizePerHalfBit()
              );
              final Vec3 hitDelta = hitVec.subtract(accuratePos).add(faceOffset);

              try {
                  return e.getInAreaTarget(hitDelta);
              } catch (IllegalArgumentException exception) {
                  //Because people do stupid stuff.
                  return Optional.empty();
              }
          })
          .map(targetedStateEntry -> IMultiStateItemFactory.getInstance().createBlockFrom(targetedStateEntry))
          .orElseGet(() -> getBlockEntity(blockGetter, pos)
            .map(e -> {
                final IMultiStateSnapshot snapshot = e.createSnapshot();
                return snapshot.toItemStack().toBlockStack();
            })
            .orElse(ItemStack.EMPTY));
    }

    @Override
    public BlockState rotate(final BlockState state, final LevelAccessor levelAccessor, final BlockPos pos, final Rotation rotation)
    {
        for (final Direction.Axis axis : Direction.Axis.values())
        {
            if (rotation.rotation().inverts(axis))
            {
                getBlockEntity(levelAccessor, pos)
                  .ifPresent(e -> e.rotate(axis));

                return state;
            }
        }

        return state;
    }

    @Override
    public boolean shouldCheckWeakPower(BlockState blockState, SignalGetter signalGetter, BlockPos blockPos, Direction direction) {
        return getBlockEntity(signalGetter, blockPos)
                .map(multiStateBlockEntity -> multiStateBlockEntity.getStatistics().shouldCheckWeakPower())
                .orElse(false);
    }

    @Override
    public boolean shouldDisplayFluidOverlay(final BlockState state, final BlockAndTintGetter blockAndTintGetter, final BlockPos pos, final FluidState fluidState)
    {
        return true;
    }

    @NotNull
    private static Optional<IMultiStateBlockEntity> getBlockEntity(final BlockGetter worldIn, final BlockPos pos)
    {
        final BlockEntity tileEntity = worldIn.getBlockEntity(pos);
        if (!(tileEntity instanceof IMultiStateBlockEntity))
        {
            return Optional.empty();
        }

        return Optional.of((IMultiStateBlockEntity) tileEntity);
    }

    @Override
    public boolean propagatesSkylightDown(@NotNull final BlockState state, @NotNull final BlockGetter reader, @NotNull final BlockPos pos)
    {
        return getBlockEntity(reader, pos)
                 .map(multiStateBlockEntity -> multiStateBlockEntity.getStatistics().canPropagateSkylight())
                 .orElse(false);
    }

    @Override
    public boolean useShapeForLightOcclusion(final @NotNull BlockState blockState)
    {
        return true;
    }

    @Override
    public void playerDestroy(
      @NotNull final Level worldIn,
      @NotNull final Player player,
      @NotNull final BlockPos pos,
      @NotNull final BlockState state,
      @Nullable final BlockEntity te,
      @NotNull final ItemStack stack)
    {
        if (te instanceof final IMultiStateBlockEntity multiStateBlockEntity)
        {

            final IMultiStateSnapshot snapshot = multiStateBlockEntity.createSnapshot();
            popResource(worldIn, pos, snapshot.toItemStack().toBlockStack());
        }
    }

    @Override
    public void setPlacedBy(
      @NotNull final Level worldIn,
      @NotNull final BlockPos pos,
      @NotNull final BlockState state,
      @Nullable final LivingEntity placer,
      @NotNull final ItemStack stack)
    {
        getBlockEntity(worldIn, pos)
          .ifPresent(multiStateBlockEntity -> {
              final Direction placementDirection = placer == null ? Direction.NORTH : placer.getDirection().getOpposite();
              final int horizontalIndex = placementDirection.get2DDataValue();

              int rotationCount = horizontalIndex - 4;
              if (rotationCount < 0)
              {
                  rotationCount += 4;
              }

              multiStateBlockEntity.rotate(Direction.Axis.Y, rotationCount);
              super.setPlacedBy(worldIn, pos, state, placer, stack);
          });
    }

    @Override
    public boolean hasDynamicShape()
    {
        return true;
    }

    @NotNull
    @Override
    public IBlockInformation getPrimaryState(@NotNull final BlockGetter world, @NotNull final BlockPos pos)
    {
        return getBlockEntity(world, pos)
                 .map(e -> e.getStatistics().getPrimaryState())
                 .orElse(BlockInformation.AIR);
    }

    @Override
    public void onRemove(final @NotNull BlockState state, final @NotNull Level worldIn, final @NotNull BlockPos pos, final BlockState newState, final boolean isMoving)
    {
        if (newState.getBlock() instanceof ChiseledBlock)
        {
            return;
        }

        super.onRemove(state, worldIn, pos, newState, isMoving);
    }

    @Override
    public boolean canBeReplaced(@NotNull final BlockState state, final BlockPlaceContext useContext)
    {
        return getBlockEntity(useContext.getLevel(), useContext.getClickedPos())
                 .map(multiStateBlockEntity -> multiStateBlockEntity.getStatistics().isEmptyBlock())
                 .orElse(true);
    }

    @Override
    public @NotNull VoxelShape getBlockSupportShape(final @NotNull BlockState state, final @NotNull BlockGetter reader, final @NotNull BlockPos pos)
    {
        final VoxelShape shape = getBlockEntity(reader, pos)
                                   .map(multiStateBlockEntity -> IVoxelShapeManager.getInstance().get(multiStateBlockEntity, CollisionType.COLLIDEABLE_ONLY))
                                   .orElse(Shapes.empty());

        return shape.isEmpty() ? Shapes.block() : shape;
    }

    @Override
    public float getShadeBrightness(@NotNull final BlockState state, @NotNull final BlockGetter worldIn, @NotNull final BlockPos pos)
    {
        return getBlockEntity(worldIn, pos)
                 .map(b -> b.getStatistics().isFullBlock())
                 .map(f -> f ? 0.2f : 1f)
                 .orElse(1f);
    }

    @NotNull
    @Override
    public VoxelShape getShape(@NotNull final BlockState state, @NotNull final BlockGetter worldIn, @NotNull final BlockPos pos, @NotNull final CollisionContext context)
    {
        return getBlockEntity(worldIn, pos)
                                   .map(multiStateBlockEntity -> IVoxelShapeManager.getInstance().get(multiStateBlockEntity))
                                   .orElse(Shapes.empty());
    }

    @NotNull
    @Override
    public VoxelShape getCollisionShape(@NotNull final BlockState state, @NotNull final BlockGetter worldIn, @NotNull final BlockPos pos, @NotNull final CollisionContext context)
    {
        return getBlockEntity(worldIn, pos)
                                   .map(multiStateBlockEntity -> IVoxelShapeManager.getInstance().get(multiStateBlockEntity, CollisionType.COLLIDEABLE_ONLY))
                                   .orElse(Shapes.empty());
    }

    @NotNull
    @Override
    public VoxelShape getVisualShape(@NotNull final BlockState state, @NotNull final BlockGetter reader, @NotNull final BlockPos pos, @NotNull final CollisionContext context)
    {
        return getShape(state, reader, pos, context);
    }

    @Override
    public float getDestroyProgress(
      @NotNull final BlockState state,
      @NotNull final Player player,
      @NotNull final BlockGetter worldIn,
      @NotNull final BlockPos pos)
    {
        return getBlockEntity(worldIn, pos)
                 .map(multiStateBlockEntity -> multiStateBlockEntity.getStatistics().getRelativeBlockHardness(player))
                 .orElse(1f);
    }

    @Override
    public boolean canPlaceLiquid(final @NotNull BlockGetter worldIn, final @NotNull BlockPos pos, final @NotNull BlockState state, final Fluid fluidIn)
    {
        return IEligibilityManager.getInstance().canBeChiseled(new BlockInformation(fluidIn.defaultFluidState().createLegacyBlock(), IStateVariantManager.getInstance().getStateVariant(fluidIn.defaultFluidState()))) &&
              worldIn.getBlockEntity(pos) instanceof IMultiStateBlockEntity multiStateBlockEntity && multiStateBlockEntity.isCanBeFlooded();
    }

    @Override
    public boolean placeLiquid(final @NotNull LevelAccessor worldIn, final @NotNull BlockPos pos, final @NotNull BlockState state, final @NotNull FluidState fluidStateIn)
    {
        final Fluid still = fluidStateIn.getType() instanceof FlowingFluid ? ((FlowingFluid) fluidStateIn.getType()).getSource() : fluidStateIn.getType();
        
        return getBlockEntity(worldIn, pos)
                 .map(entity -> {
                     try (IBatchMutation ignored = entity.batch())
                     {
                         entity.mutableStream().forEach(
                           stateEntry -> {
                               if (stateEntry.getBlockInformation().isAir())
                               {
                                   final BlockState blockState = still.defaultFluidState().createLegacyBlock();
                                   final Optional<IStateVariant> additionalStateInfo = IStateVariantManager.getInstance()
                                     .getStateVariant(
                                       fluidStateIn
                                     );

                                   try
                                   {
                                       stateEntry.setBlockInformation(new BlockInformation(blockState, additionalStateInfo));
                                   }
                                   catch (SpaceOccupiedException e)
                                   {
                                       //Ignore
                                   }
                               }
                           }
                         );
                     }

                     return true;
                 })
                 .orElse(false);
    }

    @Override
    public @NotNull ItemStack pickupBlock(final @NotNull LevelAccessor p_154560_, final @NotNull BlockPos p_154561_, final @NotNull BlockState p_154562_)
    {
        return ItemStack.EMPTY;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final @NotNull BlockPos pos, final @NotNull BlockState state)
    {
        return new ChiseledBlockEntity(pos, state);
    }

    @Override
    public void neighborChanged(
      final @NotNull BlockState state,
      final Level level,
      final @NotNull BlockPos position,
      final @NotNull Block block,
      final @NotNull BlockPos otherPosition,
      final boolean update)
    {
        if (!(level.getBlockEntity(position) instanceof ChiseledBlockEntity chiseledBlockEntity))
            return;

        if (level.isClientSide())
            chiseledBlockEntity.updateModelData();
        else
        {
            ChiselsAndBits.getInstance().getNetworkChannel().sendToTrackingChunk(
                new NeighborBlockUpdatedPacket(position, otherPosition),
                level.getChunkAt(position)
            );
        }
    }

    @Override
    public float[] getBeaconColorMultiplier(final BlockState state, final LevelReader levelReader, final BlockPos pos, final BlockPos beaconPos)
    {
        return getBlockEntity(levelReader, pos)
                 .filter(e -> e.getStatistics().getStateCounts().keySet()
                                .stream()
                                .filter(entryState -> !entryState.isAir())
                                .allMatch(entryState -> ILevelBasedPropertyAccessor.getInstance().getBeaconColorMultiplier(
                                  new SingleBlockWorldReader(
                                    e.getStatistics().getPrimaryState(),
                                    pos,
                                    levelReader
                                  ),
                                  pos,
                                  beaconPos
                                ) != null)
                 )
                 .flatMap(e -> e.getStatistics().getStateCounts().entrySet()
                   .stream()
                   .filter(entryState -> !entryState.getKey().isAir())
                   .map(entryState -> ArrayUtils.multiply(ILevelBasedPropertyAccessor.getInstance().getBeaconColorMultiplier(
                     new SingleBlockWorldReader(
                       entryState.getKey(),
                       pos,
                       levelReader
                     ),
                     pos,
                     beaconPos
                   ), entryState.getValue())).reduce((floats, floats2) -> {
                       if (floats.length != floats2.length)
                           return null;

                       if (floats == null)
                           return null;

                       if (floats2 == null)
                           return null;

                       final float[] result = new float[floats.length];
                       for (int i = 0; i < floats.length; i++)
                       {
                           result[i] = floats[i] + floats2[i];
                       }
                       return result;
                   })
                   .filter(Objects::nonNull)
                   .flatMap(summedResult -> getBlockEntity(levelReader, pos)
                     .map(entity -> ArrayUtils.multiply(summedResult, 1f / (entity.getStatistics().getFullnessFactor() * StateEntrySize.current().getBitsPerBlock())))
                   )
                ).orElse(null);
    }

    @Override
    public SoundType getSoundType(final BlockState state, final LevelReader levelReader, final BlockPos pos, @Nullable final Entity entity)
    {
        return getBlockEntity(levelReader, pos)
                 .map(blockEntity -> blockEntity.getStatistics().getPrimaryState())
                 .map(blockState -> ILevelBasedPropertyAccessor.getInstance().getSoundType(
                   new SingleBlockWorldReader(blockState, pos, levelReader),
                   pos,
                   entity
                 ))
                 .orElse(SoundType.STONE);
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter blockGetter, BlockPos position, Explosion explosion) {
        return (float) (double) (getBlockEntity(blockGetter, position)
                .map(e -> e.getStatistics().getStateCounts().entrySet()
                        .stream()
                        .filter(entryState -> !entryState.getKey().isAir())
                        .mapToDouble(entryState -> ILevelBasedPropertyAccessor.getInstance().
                                getExplosionResistance(
                                        new SingleBlockBlockReader(
                                                entryState.getKey(),
                                                position,
                                                blockGetter
                                        ),
                                        position,
                                        explosion
                                ) * entryState.getValue()
                        )
                        .sum() / (e.getStatistics().getFullnessFactor() * StateEntrySize.current().getBitsPerBlock())
                ).orElse(0d));
    }

    @Override
    public void playerWillDestroy(final @NotNull Level level, final @NotNull BlockPos blockPos, final @NotNull BlockState blockState, final @NotNull Player player)
    {
        super.playerWillDestroy(level, blockPos, blockState, player);
        if (level.isClientSide())
            getBlockEntity(level, blockPos)
              .map(IMultiStateBlockEntity::createSnapshot)
              .map(IMultiStateSnapshot::toItemStack)
              .ifPresent(CreativeClipboardUtils::addBrokenBlock);
    }

    @Override
    public boolean canBeGrass(final LevelReader levelReader, final BlockState grassState, final BlockPos grassBlockPos, final BlockState targetState, final BlockPos targetPosition)
    {
        return getBlockEntity(levelReader, targetPosition)
                 .map(blockEntity -> blockEntity.getStatistics().canSustainGrassBelow())
                .orElse(false);
    }

    @Override
    public @NotNull InteractionResult use(
      final @NotNull BlockState blockState, final @NotNull Level level, final @NotNull BlockPos blockPos, final @NotNull Player player, final @NotNull InteractionHand hand, final @NotNull BlockHitResult blockHitResult)
    {
        final ItemStack itemStack = player.getItemInHand(hand);
        if (itemStack.is(Items.SPONGE)) {
            return getBlockEntity(level, blockPos)
                     .map(blockEntity -> {
                         try(IBatchMutation mutation = blockEntity.batch(IChangeTrackerManager.getInstance().getChangeTracker(player))) {
                             return blockEntity.mutableStream()
                               .filter(entry -> !entry.getBlockInformation().getBlockState().getFluidState().isEmpty())
                               .peek(IMutableStateEntryInfo::clear)
                               .count();
                         }
                     })
                     .map(count ->  {
                         if (count > 0) {
                             itemStack.shrink(1);
                             IPlayerInventoryManager.getInstance().giveToPlayer(
                               player, new ItemStack(Items.WET_SPONGE)
                             );
                         }

                         return count > 0 ? InteractionResult.SUCCESS : InteractionResult.PASS;
                     })
                     .orElse(InteractionResult.PASS);
        }
        
        if (itemStack.is(Items.GLOWSTONE_DUST)) {
            return getBlockEntity(level, blockPos)
                         .map(blockEntity -> {
                            if (blockEntity.isEmitsLightBasedOnFullBlock()) {
                                blockEntity.setEmitsLightBasedOnFullBlock(false);
                                return InteractionResult.CONSUME;
                            }
                            
                            return InteractionResult.PASS;
                         })
                         .orElse(InteractionResult.PASS);
        }
        
        if (itemStack.is(Items.BLACK_DYE)) {
            return getBlockEntity(level, blockPos)
                         .map(blockEntity -> {
                             if (!blockEntity.isEmitsLightBasedOnFullBlock()) {
                                 blockEntity.setEmitsLightBasedOnFullBlock(true);
                                 return InteractionResult.CONSUME;
                             }
                             
                             return InteractionResult.PASS;
                         })
                         .orElse(InteractionResult.PASS);
        }

        if (itemStack.is(Items.HONEYCOMB)) {
            return getBlockEntity(level, blockPos)
                         .map(blockEntity -> {
                             if (blockEntity.isCanBeFlooded()) {
                                 blockEntity.setCanBeFlooded(false);
                                 return InteractionResult.CONSUME;
                             }
                             
                             return InteractionResult.PASS;
                         }).orElse(InteractionResult.PASS);
        }
        
        if (itemStack.is(Items.SHEARS)) {
            return getBlockEntity(level, blockPos)
                         .map(blockEntity -> {
                             if (!blockEntity.isCanBeFlooded()) {
                                 blockEntity.setCanBeFlooded(true);
                                 if (player instanceof ServerPlayer serverPlayer) {
                                    itemStack.hurt(1, level.getRandom(), serverPlayer);
                                 }
                                 return InteractionResult.SUCCESS;
                             }
                             
                             return InteractionResult.PASS;
                         }).orElse(InteractionResult.PASS);
        }
        
        return InteractionResult.PASS;
    }

    private static final Supplier<ClientChiseledBlockExtensions> renderProperties = Suppliers.memoize(ClientChiseledBlockExtensions::new);

    public ClientChiseledBlockExtensions getRenderProperties()
    {
        return renderProperties.get();
    }
}

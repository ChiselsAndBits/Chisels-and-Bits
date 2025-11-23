package mod.chiselsandbits.block;

import com.google.common.collect.Lists;
import mod.chiselsandbits.api.block.bitbag.IBitBagAcceptingBlock;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.inventory.bit.IBitInventoryItemStack;
import mod.chiselsandbits.api.inventory.management.IBitInventoryManager;
import mod.chiselsandbits.api.multistate.StateEntrySize;
import mod.chiselsandbits.block.entities.BitStorageBlockEntity;
import mod.chiselsandbits.item.BitBagItem;
import mod.chiselsandbits.registrars.ModBlocks;
import mod.chiselsandbits.registrars.ModDataComponentTypes;
import mod.chiselsandbits.registrars.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class BitStorageBlock extends Block implements EntityBlock, IBitBagAcceptingBlock
{

    public static final Property<Direction> FACING = HorizontalDirectionalBlock.FACING;

    public BitStorageBlock(Properties properties)
    {
        super(properties);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context)
    {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final @NotNull BlockPos pos, final @NotNull BlockState state)
    {
        return new BitStorageBlockEntity(pos, state);
    }

    @Override
    protected @NotNull InteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, Level level, @NotNull BlockPos blockPos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult result) {
        final BlockEntity tileEntity = level.getBlockEntity(blockPos);
        if (!(tileEntity instanceof final BitStorageBlockEntity tank))
            return InteractionResult.FAIL;

        final ItemStack current = player.getInventory().getSelectedItem();

        if (current.getItem() instanceof BitBagItem)
            return InteractionResult.PASS;

        if (!current.isEmpty())
        {
            if (tank.addHeldBits(current, player))
            {
                return InteractionResult.SUCCESS;
            }
        }
        else
        {
            if (tank.addAllPossibleBits(player))
            {
                return InteractionResult.SUCCESS;
            }
        }

        if (tank.extractBits(player))
        {
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    public float getShadeBrightness(@NotNull BlockState state, @NotNull BlockGetter worldIn, @NotNull BlockPos pos)
    {
        return 1.0F;
    }

    public boolean propagatesSkylightDown(@NotNull BlockState state, @NotNull BlockGetter reader, @NotNull BlockPos pos)
    {
        return true;
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        if (!(level.getBlockEntity(pos) instanceof BitStorageBlockEntity bitStorageBlockEntity))
            return super.getCloneItemStack(level, pos, state, includeData);

        return getTankDrop(bitStorageBlockEntity);
    }

    @Override
    public @NotNull List<ItemStack> getDrops(final @NotNull BlockState state, final LootParams.Builder builder) {
        if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) == null)
        {
            return Lists.newArrayList();
        }

        return Lists.newArrayList(getTankDrop((BitStorageBlockEntity) Objects.requireNonNull(builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY))));
    }

    public ItemStack getTankDrop(final BitStorageBlockEntity bitTank)
    {
        final ItemStack tankStack = new ItemStack(ModItems.ITEM_BIT_STORAGE.get());
        if (bitTank.getContainedBlockInformation() != null && !bitTank.getContainedBlockInformation().isAir()) {
            tankStack.set(ModDataComponentTypes.BLOCK_INFORMATION.get(), bitTank.getContainedBlockInformation());
            tankStack.set(ModDataComponentTypes.COUNT.get(), bitTank.getBits());
        }
        return tankStack;
    }

    public static BitStorageBlockEntity createEntityFromStack(final ItemStack stack) {
        final BitStorageBlockEntity bitTank = new BitStorageBlockEntity(BlockPos.ZERO, ModBlocks.BIT_STORAGE.get().defaultBlockState());
        updateEntityFromStack(stack, bitTank);
        return bitTank;
    }

    public static void updateEntityFromStack(final ItemStack stack, final BitStorageBlockEntity blockEntity) {
        blockEntity.setContents(
            stack.getOrDefault(ModDataComponentTypes.BLOCK_INFORMATION.get(), BlockInformation.AIR),
            stack.getOrDefault(ModDataComponentTypes.COUNT.get(), 0)
        );
    }

    @Override
    public ItemStack onBitBagInteraction(final ItemStack bitBagStack, final Player player, final BlockHitResult blockRayTraceResult)
    {
        if (player == null)
            return bitBagStack;

        final BlockEntity tileEntity = player.level().getBlockEntity(blockRayTraceResult.getBlockPos());
        if (!(tileEntity instanceof final BitStorageBlockEntity storage))
            return bitBagStack;

        final IBitInventoryItemStack bitInventory = IBitInventoryManager.getInstance().create(bitBagStack);

        final BlockInformation containedState = storage.getContainedBlockInformation();

        if (player.isShiftKeyDown() && (containedState != null)) {
            final int maxAmountToInsert = bitInventory.getMaxInsertAmount(containedState);
            final int bitCountToInsert = Math.min(storage.getBits(), maxAmountToInsert);

            storage.extractBits(bitCountToInsert);
            bitInventory.insert(containedState, bitCountToInsert);
        }
        else if (containedState != null && storage.getBits() != 0) {
            final int maxAmountToInsert = StateEntrySize.current().getBitsPerBlock() - storage.getBits();
            final int bitCountToInsert = Math.min(bitInventory.getMaxExtractAmount(containedState), maxAmountToInsert);

            storage.insertBits(bitCountToInsert, containedState);
            bitInventory.extract(containedState, bitCountToInsert);
        }
        else if (!player.isShiftKeyDown()) {
            final Optional<BlockInformation> toExtractCandidate =
                bitInventory.getContainedStates()
                  .entrySet()
                  .stream()
                  .max(Map.Entry.comparingByValue())
                  .map(Map.Entry::getKey);
            if (toExtractCandidate.isPresent()) {
                final BlockInformation toExtractState = toExtractCandidate.get();
                final int maxAmountToInsert = StateEntrySize.current().getBitsPerBlock();
                final int bitCountToInsert = Math.min(bitInventory.getMaxExtractAmount(toExtractState), maxAmountToInsert);

                storage.insertBits(bitCountToInsert, toExtractState);
                bitInventory.extract(toExtractState, bitCountToInsert);
            }
        }

        return bitInventory.toItemStack();
    }
}
package mod.chiselsandbits.block.entities;

import com.communi.suggestu.scena.core.item.IItemComparisonHelper;
import com.google.common.collect.ImmutableList;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.chiseling.eligibility.IEligibilityManager;
import mod.chiselsandbits.api.item.chisel.IChiselItem;
import mod.chiselsandbits.api.item.multistate.IMultiStateItemStack;
import mod.chiselsandbits.api.item.pattern.IMultiUsePatternItem;
import mod.chiselsandbits.api.item.pattern.IPatternItem;
import mod.chiselsandbits.api.multistate.StateEntrySize;
import mod.chiselsandbits.api.multistate.mutator.IMutableStateEntryInfo;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshot;
import mod.chiselsandbits.api.util.LocalStrings;
import mod.chiselsandbits.api.util.constants.NbtConstants;
import mod.chiselsandbits.block.ChiseledPrinterBlock;
import mod.chiselsandbits.container.ChiseledPrinterContainer;
import mod.chiselsandbits.registrars.ModBlockEntityTypes;
import mod.chiselsandbits.utils.container.SimpleContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ChiseledPrinterBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer {

    private final MutableObject<ItemStack> currentRealisedWorkingStack = new MutableObject<>(ItemStack.EMPTY);
    private SimpleContainer tool_handler = new SimpleContainer(1);
    private SimpleContainer pattern_handler = new SimpleContainer(1);
    private SimpleContainer result_handler = new SimpleContainer(1);
    private int progress = 0;
    protected final ContainerData stationData = new ContainerData() {

        public int get(int index) {
            if (index == 0) {
                return ChiseledPrinterBlockEntity.this.progress;
            }
            return 0;
        }

        public void set(int index, int value) {
            if (index == 0) {
                ChiseledPrinterBlockEntity.this.progress = value;
            }
        }

        public int getCount() {
            return 1;
        }
    };
    private long lastTickTime = 0L;

    public ChiseledPrinterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.CHISELED_PRINTER.get(), pos, state);
    }

    @Override
    protected void loadAdditional(final ValueInput input)
    {
        super.loadAdditional(input);

        tool_handler = input.read(NbtConstants.TOOL, SimpleContainer.CODEC).orElse(new SimpleContainer(1));
        pattern_handler = input.read(NbtConstants.PATTERN, SimpleContainer.CODEC).orElse(new SimpleContainer(1));
        result_handler = input.read(NbtConstants.RESULT, SimpleContainer.CODEC).orElse(new SimpleContainer(1));

        progress = input.getIntOr(NbtConstants.PROGRESS, 0);
    }

    @Override
    protected void saveAdditional(final ValueOutput output)
    {
        super.saveAdditional(output);

        output.store(NbtConstants.TOOL, SimpleContainer.CODEC, tool_handler);
        output.store(NbtConstants.PATTERN, SimpleContainer.CODEC, pattern_handler);
        output.store(NbtConstants.RESULT, SimpleContainer.CODEC, result_handler);

        output.putInt(NbtConstants.PROGRESS, progress);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider loader) {
        return saveWithFullMetadata(loader);
    }

    public void tick() {
        if (getLevel() == null || lastTickTime == getLevel().getGameTime() || getLevel().isClientSide()) {
            return;
        }

        this.lastTickTime = getLevel().getGameTime();

        if (couldWork()) {
            if (canWork()) {
                progress++;
                if (progress >= 100) {
                    if (result_handler.getItem(0).isEmpty()) {
                        result_handler.setItem(0, realisePattern(true));
                        return;
                    }

                    currentRealisedWorkingStack.setValue(ItemStack.EMPTY);
                    progress = 0;
                    damageChisel();
                }
                setChanged();
            }
        } else if (progress != 0) {
            progress = 0;
            setChanged();
        }
    }

    public boolean hasPatternStack() {
        return !getPatternStack().isEmpty();
    }

    public boolean hasToolStack() {
        return !getToolStack().isEmpty();
    }

    public boolean hasRealisedStack() {
        return !getRealisedStack().isEmpty();
    }

    public boolean hasOutputStack() {
        return !getOutputStack().isEmpty();
    }

    public boolean canMergeOutputs() {
        if (!hasOutputStack()) {
            return true;
        }

        if (!hasRealisedStack()) {
            return false;
        }

        return IItemComparisonHelper.getInstance().canItemStacksStack(getOutputStack(), getRealisedStack()) && getOutputStack().getCount() + getRealisedStack().getCount() <= getOutputStack().getMaxStackSize();
    }

    public boolean canWork() {
        return hasPatternStack() && hasToolStack() && canMergeOutputs() && !getRealisedStack().isEmpty();
    }

    public boolean couldWork() {
        return hasPatternStack() && hasToolStack();
    }

    public ItemStack getRealisedStack() {
        ItemStack realisedStack = currentRealisedWorkingStack.getValue();
        if (realisedStack.isEmpty()) {
            realisedStack = realisePattern(false);
            currentRealisedWorkingStack.setValue(realisedStack);
        }

        return realisedStack;
    }

    private ItemStack realisePattern(final boolean consumeResources) {
        if (!hasPatternStack()) {
            return ItemStack.EMPTY;
        }

        final ItemStack stack = getPatternStack();
        if (!(stack.getItem() instanceof final IPatternItem patternItem)) {
            return ItemStack.EMPTY;
        }

        final IMultiStateItemStack realisedPattern = patternItem.createItemStack(stack);
        if (realisedPattern.getStatistics().isEmpty()) {
            return ItemStack.EMPTY;
        }

        final List<BlockInformationSource> firstStates = getAvailableBitsOnTheLeft();
        final List<BlockInformationSource> secondStates = getAvailableBitsOnTheRight();
        final List<BlockInformationSource> thirdStates = getAvailableBitsOnTheBack();

        final List<BlockInformationSources> states = ImmutableList.<BlockInformationSource>builder()
                .addAll(firstStates)
                .addAll(secondStates)
                .addAll(thirdStates)
                .build()
                .stream()
                .collect(Collectors.groupingBy(
                        BlockInformationSource::blockInformation,
                        Collectors.mapping(bis -> new PositionAndCount(bis.pos(), bis.count()), Collectors.toSet())
                ))
                .entrySet()
                .stream()
                .map(e -> new BlockInformationSources(
                        e.getKey(),
                        e.getValue().stream().mapToInt(PositionAndCount::count).sum(),
                        e.getValue()))
                .toList();

        if (states.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (states.stream().map(BlockInformationSources::blockInformation).noneMatch(IEligibilityManager.getInstance()::canBeChiseled)) {
            return ItemStack.EMPTY;
        }

        final IMultiStateSnapshot modifiableSnapshot = realisedPattern.createSnapshot();
        modifiableSnapshot.mutableStream()
                .filter(e -> states.stream().noneMatch(s -> s.blockInformation().equals(e.getBlockInformation())))
                .forEach(IMutableStateEntryInfo::clear);

        if (modifiableSnapshot.getStatics().getStateCounts().size() == 1 &&
                modifiableSnapshot.getStatics().getStateCounts().containsKey(BlockInformation.AIR)) {
            //Just air.
            return ItemStack.EMPTY;
        }

        if (states.stream().map(BlockInformationSources::blockInformation).map(state -> modifiableSnapshot.getStatics().getStateCounts().getOrDefault(state, 0))
                .allMatch(i -> i == 0)) {
            return ItemStack.EMPTY;
        }

        if (states.stream().anyMatch(state -> modifiableSnapshot.getStatics().getStateCounts().getOrDefault(state.blockInformation(), 0) > state.totalCount())) {
            return ItemStack.EMPTY;
        }

        if (consumeResources) {
            for (BlockInformationSources state : states) {
                int toDrain = modifiableSnapshot.getStatics().getStateCounts().getOrDefault(state.blockInformation(), 0);
                if (toDrain > 0) {
                    for (PositionAndCount pos : state.posses()) {
                        final int toDrainFromThis = Math.min(toDrain, pos.count());
                        drainStorage(toDrainFromThis, pos.pos());
                        toDrain -= toDrainFromThis;
                    }
                }
            }
        }

        return modifiableSnapshot.toItemStack().toBlockStack();
    }

    private void damageChisel() {
        if (getLevel() != null && !getLevel().isClientSide()) {
            getToolStack().hurtAndBreak(StateEntrySize.ONE_SIXTEENTH.getBitsPerBlock(), (ServerLevel) getLevel(), null, (item) -> {});
        }
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(final int containerId, @NotNull final Inventory playerInventory, @NotNull final Player playerEntity) {
        return new ChiseledPrinterContainer(
                containerId,
                playerInventory,
                getPatternHandler(),
                getToolHandler(),
                getResultHandler(),
                stationData);
    }

    public SimpleContainer getPatternHandler() {
        return pattern_handler;
    }

    public SimpleContainer getToolHandler() {
        return tool_handler;
    }

    public SimpleContainer getResultHandler() {
        return result_handler;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return LocalStrings.ChiselStationName.getText();
    }

    public List<BlockInformationSource> getAvailableBitsOnTheLeft() {
        final Direction facing = Objects.requireNonNull(this.getLevel()).getBlockState(this.getBlockPos()).getValue(ChiseledPrinterBlock.FACING);
        final Direction targetedFacing = facing.getClockWise();

        return getAvailableBitsIn(targetedFacing);
    }

    public List<BlockInformationSource> getAvailableBitsOnTheRight() {
        final Direction facing = Objects.requireNonNull(this.getLevel()).getBlockState(this.getBlockPos()).getValue(ChiseledPrinterBlock.FACING);
        final Direction targetedFacing = facing.getClockWise().getClockWise();

        return getAvailableBitsIn(targetedFacing);
    }

    public List<BlockInformationSource> getAvailableBitsOnTheBack() {
        final Direction facing = Objects.requireNonNull(this.getLevel()).getBlockState(this.getBlockPos()).getValue(ChiseledPrinterBlock.FACING);
        final Direction targetedFacing = facing.getCounterClockWise();

        return getAvailableBitsIn(targetedFacing);
    }

    private List<BlockInformationSource> getAvailableBitsIn(final Direction targetedFacing) {
        final List<BlockInformationSource> result = Lists.newArrayList();

        BlockPos pos = this.getBlockPos();
        for (int i = 0; i < 16; i++) {
            pos = pos.relative(targetedFacing);
            final BlockEntity targetedTileEntity = Objects.requireNonNull(this.getLevel()).getBlockEntity(pos);
            if (targetedTileEntity instanceof final BitStorageBlockEntity storage) {
                result.add(new BlockInformationSource(storage.getContainedBlockInformation(), storage.getBits(), pos));
            } else {
                break;
            }
        }

        return result;
    }

    private void drainStorage(final int amount, final BlockPos pos) {
        final BlockEntity targetedTileEntity = Objects.requireNonNull(this.getLevel()).getBlockEntity(pos);
        if (targetedTileEntity instanceof final BitStorageBlockEntity storage) {
            storage.extractBits(amount);
        }
    }

    @Override
    public void preRemoveSideEffects(final BlockPos pos, final BlockState state)
    {
        super.preRemoveSideEffects(pos, state);
        dropInventoryItems();
    }

    public void dropInventoryItems() {
        final Level worldIn = getLevel();
        final BlockPos pos = getBlockPos();
        Containers.dropItemStack(Objects.requireNonNull(worldIn),
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                getToolStack());

        Containers.dropItemStack(worldIn,
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                getOutputStack());

        Containers.dropItemStack(worldIn,
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                getPatternStack());
    }

    public ItemStack getToolStack() {
        return tool_handler.getItem(0);
    }

    public ItemStack getOutputStack() {
        return result_handler.getItem(0);
    }

    public ItemStack getPatternStack() {
        return pattern_handler.getItem(0);
    }

    @Override
    public int @NotNull [] getSlotsForFace(final @NotNull Direction direction) {
        return switch (direction) {
            case DOWN -> new int[] { 2 };
            case UP -> new int[] { 1 };
            case NORTH, SOUTH, WEST, EAST -> new int[] { 0 };
        };
    }

    @Override
    public boolean canPlaceItemThroughFace(final int index, final @NotNull ItemStack itemStack,final Direction direction) {
        return switch (Objects.requireNonNull(direction)) {
            case DOWN -> false;
            case UP -> itemStack.getItem() instanceof IChiselItem;
            case NORTH, SOUTH, WEST, EAST -> itemStack.getItem() instanceof IMultiUsePatternItem;
        };
    }

    @Override
    public boolean canTakeItemThroughFace(final int index, final @NotNull ItemStack itemStack, final @NotNull Direction direction) {
        return switch (direction) {
            case DOWN -> true;
            case UP -> itemStack.getItem() instanceof IChiselItem;
            case NORTH, SOUTH, WEST, EAST -> itemStack.getItem() instanceof IMultiUsePatternItem;
        };
    }

    @Override
    public int getContainerSize() {
        return 3;
    }

    @Override
    public boolean isEmpty() {
        return getPatternHandler().isEmpty() &&
                getToolHandler().isEmpty() &&
                getResultHandler().isEmpty();
    }

    @Override
    public @NotNull ItemStack getItem(final int index) {
        return switch (index) {
            case 0 -> getPatternStack();
            case 1 -> getToolStack();
            case 2 -> getOutputStack();
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public @NotNull ItemStack removeItem(final int index, final int count) {
        return switch (index) {
            case 0 -> getPatternHandler().removeItem(0, count);
            case 1 -> getToolHandler().removeItem(0, count);
            case 2 -> getResultHandler().removeItem(0, count);
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(final int index) {
        return switch (index) {
            case 0 -> getPatternHandler().removeItemNoUpdate(0);
            case 1 -> getToolHandler().removeItemNoUpdate(0);
            case 2 -> getResultHandler().removeItemNoUpdate(0);
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public void setItem(final int index, final @NotNull ItemStack itemStack) {
        switch (index) {
            case 0 -> getPatternHandler().setItem(0, itemStack);
            case 1 -> getToolHandler().setItem(0, itemStack);
            case 2 -> getResultHandler().setItem(0, itemStack);
        }
    }

    @Override
    public boolean stillValid(final @NotNull Player player) {
        if (Objects.requireNonNull(this.level).getBlockEntity(this.worldPosition) != this) {
            return false;
        } else {
            return player.distanceToSqr((double) this.worldPosition.getX() + 0.5D, (double) this.worldPosition.getY() + 0.5D, (double) this.worldPosition.getZ() + 0.5D) <= 64.0D;
        }
    }

    @Override
    public void clearContent() {
        getPatternHandler().clearContent();
        getToolHandler().clearContent();
        getResultHandler().clearContent();
    }

    public record PositionAndCount(BlockPos pos, int count) {}

    public record BlockInformationSource(BlockInformation blockInformation, int count, BlockPos pos) {}

    public record BlockInformationSources(BlockInformation blockInformation, int totalCount, Set<PositionAndCount> posses) {}

}

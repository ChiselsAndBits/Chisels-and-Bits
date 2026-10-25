package mod.chiselsandbits.change.changes;

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mod.chiselsandbits.api.block.entity.IMultiStateBlockEntity;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.change.changes.IChange;
import mod.chiselsandbits.api.change.changes.IChangeType;
import mod.chiselsandbits.api.change.changes.IllegalChangeAttempt;
import mod.chiselsandbits.api.chiseling.conversion.IConversionManager;
import mod.chiselsandbits.api.exceptions.SpaceOccupiedException;
import mod.chiselsandbits.api.inventory.bit.IBitInventory;
import mod.chiselsandbits.api.inventory.management.IBitInventoryManager;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshot;
import mod.chiselsandbits.api.util.IBatchMutation;
import mod.chiselsandbits.api.util.constants.NbtConstants;
import mod.chiselsandbits.api.variant.state.IStateVariant;
import mod.chiselsandbits.api.variant.state.IStateVariantManager;
import mod.chiselsandbits.utils.BitInventoryUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BitChange implements IChange {
	public static final MapCodec<BitChange> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			BlockPos.CODEC.fieldOf(NbtConstants.POSITION).forGetter(BitChange::getBlockPos),
			IMultiStateSnapshot.CODEC.fieldOf(NbtConstants.BEFORE).forGetter(BitChange::getBefore),
			IMultiStateSnapshot.CODEC.fieldOf(NbtConstants.AFTER).forGetter(BitChange::getAfter)
	).apply(instance, BitChange::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, BitChange> STREAM_CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC,
			BitChange::getBlockPos,
			IMultiStateSnapshot.STREAM_CODEC,
			BitChange::getBefore,
			IMultiStateSnapshot.STREAM_CODEC,
			BitChange::getAfter,
			BitChange::new
	);

	private final BlockPos blockPos;
	private final IMultiStateSnapshot before;
	private final IMultiStateSnapshot after;

	public BitChange(
			final BlockPos blockPos,
			final IMultiStateSnapshot before,
			final IMultiStateSnapshot after) {
		this.blockPos = blockPos;
		this.before = before;
		this.after = after;
	}

	private BlockPos getBlockPos() {
		return blockPos;
	}

	private IMultiStateSnapshot getBefore() {
		return before;
	}

	private IMultiStateSnapshot getAfter() {
		return after;
	}

	@Override
	public boolean canUndo(final Player player) {
		final BlockEntity tileEntity = player.level().getBlockEntity(blockPos);

		if (!(tileEntity instanceof final IMultiStateBlockEntity multiStateBlockEntity)) {
			final BlockState state = player.level().getBlockState(blockPos);
			final Optional<IStateVariant> additionalStateInfo = IStateVariantManager.getInstance().getStateVariant(
					state, Optional.ofNullable(player.level().getBlockEntity(blockPos))
			);
			final BlockInformation currentState = new BlockInformation(state, additionalStateInfo);

			return after.getStatics().getStateCounts().size() == 1 && after.getStatics().getStateCounts().getOrDefault(currentState, 0) == 4096;
		}

		return after.createNewShapeIdentifier().equals(multiStateBlockEntity.createNewShapeIdentifier()) && hasRequiredUndoBits(player);
	}

	@Override
	public boolean canRedo(final Player player) {
		final BlockEntity tileEntity = player.level().getBlockEntity(blockPos);

		if (!(tileEntity instanceof final IMultiStateBlockEntity multiStateBlockEntity)) {
			final BlockState state = player.level().getBlockState(blockPos);
			final Optional<IStateVariant> additionalStateInfo = IStateVariantManager.getInstance().getStateVariant(
					state, Optional.ofNullable(player.level().getBlockEntity(blockPos))
			);
			final BlockInformation currentState = new BlockInformation(state, additionalStateInfo);

			return before.getStatics().getStateCounts().size() == 1 && before.getStatics().getStateCounts().getOrDefault(currentState, 0) == 4096;
		}

		return before.createNewShapeIdentifier().equals(multiStateBlockEntity.createNewShapeIdentifier()) && hasRequiredRedoBits(player);
	}

	@Override
	public void undo(final Player player) throws IllegalChangeAttempt {
		if (!canUndo(player)) {
			throw new IllegalChangeAttempt();
		}

		BlockEntity tileEntity = player.level().getBlockEntity(blockPos);

		if (!(tileEntity instanceof IMultiStateBlockEntity)) {
			BlockState currentState = player.level().getBlockState(blockPos);
			final Optional<Block> convertedState = IConversionManager.getInstance().getChiseledVariantOf(currentState);

			if (convertedState.isEmpty()) {
				throw new IllegalChangeAttempt();
			}

			player.level().setBlock(blockPos, convertedState.get().defaultBlockState(), Block.UPDATE_ALL);
			tileEntity = player.level().getBlockEntity(blockPos);

			if (!(tileEntity instanceof IMultiStateBlockEntity)) {
				throw new IllegalChangeAttempt();
			}
		}

		final IMultiStateBlockEntity multiStateBlockEntity = (IMultiStateBlockEntity) tileEntity;
		final Map<BlockInformation, Integer> afterStates = after.getStatics().getStateCounts();
		final Map<BlockInformation, Integer> beforeStates = before.getStatics().getStateCounts();

		final Map<BlockInformation, Integer> difference = Maps.newHashMap();
		beforeStates.forEach((state, count) -> difference.put(state, afterStates.getOrDefault(state, 0) - count));
		afterStates.forEach((state, count) -> {
			if (!difference.containsKey(state)) {
				difference.put(state, count);
			}
		});

		try (IBatchMutation batch = multiStateBlockEntity.batch()) {
			multiStateBlockEntity.initializeWith(BlockInformation.AIR);
			before.stream().forEach(
							iStateEntryInfo -> {
								try {
									multiStateBlockEntity.setInAreaTarget(iStateEntryInfo.getBlockInformation(), iStateEntryInfo.getStartPoint());
								} catch (SpaceOccupiedException e) {
								//Noop
								}
							});
		}

		if (!player.isCreative()) {
			final IBitInventory bitInventory = IBitInventoryManager.getInstance().create(player);
			difference.forEach((state, diff) -> {
				if (state.isAir()) {
					return;
				}

				if (diff < 0) {
					bitInventory.extract(state, -diff);
				} else {
					BitInventoryUtils.insertIntoOrSpawn(player, state, diff);
				}
			});
		}
	}

	@Override
	public void redo(final Player player) throws IllegalChangeAttempt {
		if (!canRedo(player)) {
			throw new IllegalChangeAttempt();
		}

		BlockEntity tileEntity = player.level().getBlockEntity(blockPos);

		if (!(tileEntity instanceof IMultiStateBlockEntity)) {
			BlockState currentState = player.level().getBlockState(blockPos);
			BlockState initializationState = currentState;

			if (currentState.isAir()) {
				currentState = Blocks.STONE.defaultBlockState();
			}

			final Optional<Block> convertedState = IConversionManager.getInstance().getChiseledVariantOf(currentState);

			if (convertedState.isEmpty()) {
				throw new IllegalChangeAttempt();
			}

			player.level().setBlock(blockPos, convertedState.get().defaultBlockState(), Block.UPDATE_ALL);
			tileEntity = player.level().getBlockEntity(blockPos);

			if (!(tileEntity instanceof IMultiStateBlockEntity)) {
				throw new IllegalChangeAttempt();
			}
		}

		final IMultiStateBlockEntity multiStateBlockEntity = (IMultiStateBlockEntity) tileEntity;
		final Map<BlockInformation, Integer> afterStates = after.getStatics().getStateCounts();
		final Map<BlockInformation, Integer> beforeStates = before.getStatics().getStateCounts();

		final Map<BlockInformation, Integer> difference = Maps.newHashMap();
		beforeStates.forEach((state, count) -> difference.put(state, count - afterStates.getOrDefault(state, 0)));
		afterStates.forEach((state, count) -> {
			if (!difference.containsKey(state)) {
				difference.put(state, -count);
			}
		});

		try (IBatchMutation batch = multiStateBlockEntity.batch()) {
			multiStateBlockEntity.initializeWith(BlockInformation.AIR);
			after.stream().forEach(
							s -> {
								try {
									multiStateBlockEntity.setInAreaTarget(s.getBlockInformation(), s.getStartPoint());
								} catch (SpaceOccupiedException e) {
								//Noop
								}
							});
		}

		if (!player.isCreative()) {
			final IBitInventory bitInventory = IBitInventoryManager.getInstance().create(player);
			difference.forEach((state, diff) -> {
				if (diff < 0) {
					bitInventory.extract(state, -diff);
				} else {
					BitInventoryUtils.insertIntoOrSpawn(player, state, diff);
				}
			});
		}
	}

	private boolean hasRequiredUndoBits(final Player player) {
		if (player.isCreative()) {
			return true;
		}

		final Map<BlockInformation, Integer> afterStates = after.getStatics().getStateCounts();
		final Map<BlockInformation, Integer> beforeStates = before.getStatics().getStateCounts();

		final Map<BlockInformation, Integer> difference = Maps.newHashMap();
		beforeStates.forEach((state, count) -> difference.put(state, afterStates.getOrDefault(state, 0) - count));
		afterStates.forEach((state, count) -> {
			if (!difference.containsKey(state)) {
				difference.put(state, count);
			}
		});

		final IBitInventory bitInventory = IBitInventoryManager.getInstance().create(player);
		return difference.entrySet().stream()
				.filter(e -> e.getValue() < 0)
				.allMatch(e -> bitInventory.canExtract(e.getKey(), -e.getValue()));
	}

	private boolean hasRequiredRedoBits(final Player player) {
		if (player.isCreative()) {
			return true;
		}

		final Map<BlockInformation, Integer> afterStates = after.getStatics().getStateCounts();
		final Map<BlockInformation, Integer> beforeStates = before.getStatics().getStateCounts();

		final Map<BlockInformation, Integer> difference = Maps.newHashMap();
		beforeStates.forEach((state, count) -> difference.put(state, count - afterStates.getOrDefault(state, 0)));
		afterStates.forEach((state, count) -> {
			if (!difference.containsKey(state)) {
				difference.put(state, -count);
			}
		});

		final IBitInventory bitInventory = IBitInventoryManager.getInstance().create(player);
		return difference.entrySet().stream()
				.filter(e -> e.getValue() < 0)
				.allMatch(e -> bitInventory.canExtract(e.getKey(), -e.getValue()));
	}

	@Override
	public IChangeType getType() {
		return ChangeType.BIT;
	}
}

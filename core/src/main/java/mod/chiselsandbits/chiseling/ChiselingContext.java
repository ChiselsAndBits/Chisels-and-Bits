package mod.chiselsandbits.chiseling;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.common.collect.Maps;
import mod.chiselsandbits.api.chiseling.ChiselingOperation;
import mod.chiselsandbits.api.chiseling.IChiselingContext;
import mod.chiselsandbits.api.chiseling.metadata.IMetadataKey;
import mod.chiselsandbits.api.chiseling.mode.IChiselMode;
import mod.chiselsandbits.api.item.chisel.IChiselingItem;
import mod.chiselsandbits.api.multistate.StateEntrySize;
import mod.chiselsandbits.api.multistate.accessor.IAreaAccessor;
import mod.chiselsandbits.api.multistate.accessor.IStateEntryInfo;
import mod.chiselsandbits.api.multistate.mutator.IMutatorFactory;
import mod.chiselsandbits.api.multistate.mutator.world.IWorldAreaMutator;
import mod.chiselsandbits.api.permissions.IPermissionHandler;
import mod.chiselsandbits.api.util.VectorUtils;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

public class ChiselingContext implements IChiselingContext {
	private final LevelAccessor world;
	private final IChiselMode chiselMode;
	private final ChiselingOperation modeOfOperandus;
	private final boolean simulation;
	private final Runnable onCompleteCallback;
	private final boolean supportsDamaging;
	private final Player playerEntity;

	private boolean complete = false;
	private IWorldAreaMutator mutator = null;
	private Function<IAreaAccessor, Predicate<IStateEntryInfo>> filterBuilder = null;
	private Map<IMetadataKey<?>, Object> metadataKeyMap = Maps.newHashMap();
	private ItemStack causingItemStack;
	private MutableComponent error = null;

	public ChiselingContext(
			final LevelAccessor world,
			final IChiselMode chiselMode,
			final ChiselingOperation modeOfOperandus,
			final boolean simulation,
			final Runnable onCompleteCallback,
			final ItemStack causingItemStack,
			final Player playerEntity) {
		this.world = world;
		this.chiselMode = chiselMode;
		this.simulation = simulation;
		this.onCompleteCallback = onCompleteCallback;
		this.modeOfOperandus = modeOfOperandus;
		this.causingItemStack = causingItemStack;

		if (this.causingItemStack.getItem() instanceof IChiselingItem) {
			this.supportsDamaging = ((IChiselingItem) this.causingItemStack.getItem()).isDamageableDuringChiseling();
		} else {
			this.supportsDamaging = false;
		}

		this.playerEntity = playerEntity;
	}

	private ChiselingContext(
			final LevelAccessor world,
			final IChiselMode chiselMode,
			final ChiselingOperation modeOfOperandus,
			final boolean complete,
			final IWorldAreaMutator mutator,
			final Player playerEntity,
			final MutableComponent error) {
		this.world = world;
		this.chiselMode = chiselMode;
		this.causingItemStack = ItemStack.EMPTY;
		this.supportsDamaging = false;
		this.onCompleteCallback = () -> { }; //Noop this is the snapshot constructor which has no callback logic.
		this.simulation = true; //Always the case for snapshots.
		this.modeOfOperandus = modeOfOperandus;
		this.complete = complete;
		this.mutator = mutator;
		this.playerEntity = playerEntity;
		this.error = error;
	}

	private ChiselingContext(
			final LevelAccessor world,
			final IChiselMode chiselMode,
			final ChiselingOperation modeOfOperandus,
			final boolean complete,
			final Player playerEntity,
			final MutableComponent error) {
		this.world = world;
		this.chiselMode = chiselMode;
		this.causingItemStack = ItemStack.EMPTY;
		this.supportsDamaging = false;
		this.onCompleteCallback = () -> { }; //Noop this is the snapshot constructor which has no callback logic.
		this.simulation = true; //Always the case for snapshots.
		this.modeOfOperandus = modeOfOperandus;
		this.complete = complete;
		this.playerEntity = playerEntity;
		this.error = error;
	}

	private void setMetadataKeyMap(final Map<IMetadataKey<?>, Object> metadataKeyMap) {
		this.metadataKeyMap = metadataKeyMap;
	}

	@Override
	public Optional<IWorldAreaMutator> getMutator() {
		if (mutator == null || playerEntity == null || !(world instanceof Level)) {
			return Optional.ofNullable(mutator);
		}

		if (!IPermissionHandler.getInstance().canManipulate(
				playerEntity, mutator
		)) {
		//We are not allowed to edit the current area.
		//Nuke it.
			mutator = null;
			return Optional.empty();
		}

		return Optional.of(mutator);
	}

	@Override
	public LevelAccessor getWorld() {
		return world;
	}

	@Override
	public IChiselMode getMode() {
		return chiselMode;
	}

	@Override
	public IChiselingContext include(final Vec3 worldPosition) {
		if (getMutator().map(m -> !m.isInside(worldPosition)).orElse(true)) {
			if (getMutator().isPresent()) {
				final IWorldAreaMutator worldAreaMutator = getMutator().get();

				Vec3 start = new Vec3(
						Math.min(worldPosition.x(), worldAreaMutator.getInWorldStartPoint().x()),
						Math.min(worldPosition.y(), worldAreaMutator.getInWorldStartPoint().y()),
						Math.min(worldPosition.z(), worldAreaMutator.getInWorldStartPoint().z())
				);
				Vec3 end = new Vec3(
						Math.max(worldPosition.x(), worldAreaMutator.getInWorldEndPoint().x()),
						Math.max(worldPosition.y(), worldAreaMutator.getInWorldEndPoint().y()),
						Math.max(worldPosition.z(), worldAreaMutator.getInWorldEndPoint().z())
				);

				this.mutator = IMutatorFactory.getInstance().covering(world, start, end);
			} else {
				this.mutator = IMutatorFactory.getInstance().covering(
						world,
						worldPosition,
						worldPosition
				);
			}
		}

		return this;
	}

	@Override
	public void setComplete() {
		this.complete = true;
		this.onCompleteCallback.run();
	}

	@Override
	public boolean isComplete() {
		return complete;
	}

	@Override
	public boolean isSimulation() {
		return simulation;
	}

	@Override
	public ChiselingOperation getModeOfOperandus() {
		return modeOfOperandus;
	}

	@Override
	public IChiselingContext createSnapshot() {
		final ChiselingContext context = createInnerSnapshot();
		final Map<IMetadataKey<?>, Object> newMetadata = Maps.newHashMap();

		for (final IMetadataKey<?> key : this.metadataKeyMap.keySet()) {
			final Optional<?> value = snapshotMetadata(key);
			value.ifPresent(o -> newMetadata.put(key, o));
		}

		context.setMetadataKeyMap(newMetadata);
		return context;
	}

	private ChiselingContext createInnerSnapshot() {
		if (mutator == null) {
			return new ChiselingContext(
					world,
					chiselMode,
					modeOfOperandus,
					this.complete,
					playerEntity,
					error
			);
		}

		return new ChiselingContext(
				world,
				chiselMode,
				modeOfOperandus,
				this.complete,
				IMutatorFactory.getInstance().covering(
						world,
						mutator.getInWorldStartPoint(),
						mutator.getInWorldEndPoint()
				),
				playerEntity,
				error
		);
	}

	private <T> Optional<T> snapshotMetadata(final IMetadataKey<T> key) {
		final Optional<T> value = getMetadata(key);
		return value.map(key::snapshot);
	}

	@Override
	public int tryDamageItemAndDo(final int damage, final Runnable onDamaged, final Runnable onBroken) {
		if (!this.supportsDamaging || this.simulation) {
			onDamaged.run();
			return damage;
		}

		if (causingItemStack.isEmpty()) {
			onBroken.run();
			return 0;
		}

		if (world instanceof ServerLevel && playerEntity instanceof ServerPlayer player && player.hasInfiniteMaterials()) {
			onDamaged.run();
			return damage;
		}

		final AtomicBoolean broken = new AtomicBoolean(false);
		final int currentDamage = causingItemStack.getDamageValue();

		if (world instanceof ServerLevel level && playerEntity instanceof ServerPlayer player) {
			final int appliedDamage = damage * StateEntrySize.current().getDamageFactor();
			this.causingItemStack.hurtAndBreak(damage, level, player, item -> {
				broken.set(true);

				EquipmentSlot hand = EquipmentSlot.MAINHAND;

				if (playerEntity.getOffhandItem() == causingItemStack) {
					hand = EquipmentSlot.OFFHAND;
				}

				player.onEquippedItemBroken(item, hand);
			});
		}

		onDamaged.run();

		if (broken.get()) {
			causingItemStack = ItemStack.EMPTY;
		}

		return Math.min(damage, currentDamage);
	}

	@Override
	public void setStateFilter(final Function<IAreaAccessor, Predicate<IStateEntryInfo>> filter) {
		this.filterBuilder = filter;
	}

	@Override
	public void clearStateFilter() {
		this.filterBuilder = null;
	}

	@Override
	public Optional<Function<IAreaAccessor, Predicate<IStateEntryInfo>>> getStateFilter() {
		return Optional.ofNullable(this.filterBuilder);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Optional<T> getMetadata(final IMetadataKey<T> key) {
		final Object value = this.metadataKeyMap.get(key);

		if (value == null) {
			return Optional.empty();
		}

		try {
			final T castValue = (T) value;
			return Optional.ofNullable(castValue);
		} catch (ClassCastException e) {
			return Optional.empty();
		}
	}

	@Override
	public void removeMetadata(final IMetadataKey<?> key) {
		this.metadataKeyMap.remove(key);
	}

	@Override
	public <T> void setMetadata(final IMetadataKey<T> key, final T value) {
		this.metadataKeyMap.put(key, value);
	}

	@Override
	public void resetMutator() {
		this.mutator = null;
	}

	@Override
	public void setError(final MutableComponent errorText) {
		this.error = errorText;
	}

	@Override
	public Optional<MutableComponent> getError() {
		return Optional.ofNullable(this.error);
	}

	@Override
	public Optional<IStateEntryInfo> getInAreaTarget(final Vec3 inAreaTarget) {
		if (getMutator().isPresent() && getMutator().map(m -> m.isInside(inAreaTarget)).orElse(false)) {
			return getMutator().flatMap(m -> m.getInAreaTarget(inAreaTarget));
		}

		final BlockPos position = VectorUtils.toBlockPos(inAreaTarget);
		final Vec3 inBlockOffset = inAreaTarget.subtract(position.getX(), position.getY(), position.getZ());

		return IMutatorFactory.getInstance().in(
				getWorld(),
				position
				).getInAreaTarget(
						inBlockOffset
				);
	}

	@Override
	public Optional<IStateEntryInfo> getInBlockTarget(final BlockPos inAreaBlockPosOffset, final Vec3 inBlockTarget) {
		return getInAreaTarget(Vec3.atLowerCornerOf(inAreaBlockPosOffset).add(inBlockTarget));
	}
}

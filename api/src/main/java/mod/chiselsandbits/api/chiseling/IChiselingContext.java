package mod.chiselsandbits.api.chiseling;

import mod.chiselsandbits.api.chiseling.metadata.IMetadataKey;
import mod.chiselsandbits.api.chiseling.mode.IChiselMode;
import mod.chiselsandbits.api.multistate.accessor.IAreaAccessor;
import mod.chiselsandbits.api.multistate.accessor.IStateAccessor;
import mod.chiselsandbits.api.multistate.accessor.IStateEntryInfo;
import mod.chiselsandbits.api.multistate.mutator.world.IWorldAreaMutator;
import mod.chiselsandbits.api.util.LocalStrings;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The current context for the running chiseling operation.
 */
public interface IChiselingContext extends IStateAccessor
{
    /**
     * Returns the current {@link IWorldAreaMutator} if there is one.
     * If a new chiseling operation is started no {@link IWorldAreaMutator} is available,
     * as such an empty {@link Optional} will be returned in that case.
     *
     * Only after the primary call to {@link #include(Vec3)} or {@link #include(BlockPos, Vec3)}
     * the returned {@link Optional} can contain a {@link IWorldAreaMutator}.
     *
     * @return The {@link Optional} containing the {@link IWorldAreaMutator}.
     */
    @NotNull
    Optional<IWorldAreaMutator> getMutator();

    /**
     * The {@link LevelAccessor} in which the current chiseling context is valid.
     *
     * @return The {@link LevelAccessor}.
     */
    @NotNull
    LevelAccessor getWorld();

    /**
     * Returns the current {@link IChiselMode} for which this context is valid.
     *
     * @return The {@link IChiselMode}.
     */
    @NotNull
    IChiselMode getMode();

    /**
     * Includes the given exact position in the world of this context, retrievable via {@link #getWorld()}, in
     * the current {@link IWorldAreaMutator}.
     *
     * If the given position is already contained in the current {@link IWorldAreaMutator}, this method makes
     * no changes to the current context.
     *
     * It is up to the contexts implementation as well as the entire implementation of the chisels and bits api
     * to round the given value up and down into a precision which it can process, meaning that a given exact position
     * might already be included in the current {@link IWorldAreaMutator} if it is within the precision of the
     * current runtime. Even if the given exact vector itself is not included in the current {@link IWorldAreaMutator}.
     *
     * @param worldPosition The position in the current world to include.
     * @return The context, possibly with a mutated {@link IWorldAreaMutator}.
     */
    @NotNull
    IChiselingContext include(final Vec3 worldPosition);

    /**
     * Includes the given exact position in the world of this context, retrievable via {@link #getWorld()}, in
     * the current {@link IWorldAreaMutator}.
     *
     * If the given position is already contained in the current {@link IWorldAreaMutator}, this method makes
     * no changes to the current context.
     *
     * It is up to the contexts implementation as well as the entire implementation of the chisels and bits api
     * to round the given value up and down into a precision which it can process, meaning that a given exact position
     * might already be included in the current {@link IWorldAreaMutator} if it is within the precision of the
     * current runtime. Even if the given exact vector itself is not included in the current {@link IWorldAreaMutator}.
     *
     * @param inWorldPosition The position of the block relative to which the {@code relativeInBlockPosition} is processed.
     * @param relativeInBlockPosition The relative position to include. Relative to the given {@code inWorldPosition}
     * @return The context, possibly with a mutated {@link IWorldAreaMutator}.
     */
    @NotNull
    default IChiselingContext include(final BlockPos inWorldPosition, final Vec3 relativeInBlockPosition) {
        return this.include(Vec3.atLowerCornerOf(inWorldPosition).add(relativeInBlockPosition));
    }

    /**
     * Marks the current context as complete, so that it can not be reused for interactions which
     * will follow this one.
     *
     * Indicates that an action has been performed using this context, making it invalid.
     */
    void setComplete();

    /**
     * Indicates if the context is completed or not.
     *
     * @return True when complete.
     */
    boolean isComplete();

    /**
     * Indicates if the current context that is being executed is supposed to be a simulation.
     *
     * @return True when a simulation.
     */
    boolean isSimulation();

    /**
     * Indicates what kind of chiseling operation this context was created for.
     * This indicates if the mode is used for chiseling or placing.
     *
     * @return {@link ChiselingOperation#CHISELING} when the context is used for breaking blocks, {@link ChiselingOperation#PLACING} for bit placement.
     */
    @NotNull
    ChiselingOperation getModeOfOperandus();

    /**
     * Creates a deep copy of the context, so that the copy can be modified, without modifying this instance.
     * A snapshot is automatically a simulation.
     *
     * @return The snapshot context of this context.
     */
    @NotNull
    IChiselingContext createSnapshot();

    /**
     * Invoked to try to damage the item that caused the chiseling operation.
     * If no item was the cause of the operation this function always returns successfully.
     * If this item does not support damaging the item on a chiseling operation this method also
     * always returns successfully.
     *
     * Does exactly 1 damage to the item.
     *
     * The only case where this method does not return {@code True}, is when the item that caused the operation broke in the previous operation.
     * @return {@code True} when successful, {@code false} when not.
     */
    default boolean tryDamageItem() {
        return tryDamageItem(1);
    };

    /**
     * Invoked to try to damage the item that caused the chiseling operation.
     * If no item was the cause of the operation this function always returns successfully.
     * If this item does not support damaging the item on a chiseling operation this method also
     * always returns successfully.
     *
     * The only case where this method does not return {@code True}, is when the item that caused the operation broke in the previous operation.
     *
     * @param damage The damage to apply to the item that caused the chiseling operation.
     * @return {@code True} when successful, {@code false} when not.
     */
    default boolean tryDamageItem(final int damage) {
        return this.tryDamageItemAndDo(damage, () -> {}, () -> {}) > 0;
    }

    /**
     * Invoked to try to damage the item that caused the chiseling operations.
     *
     * If no item was the cause of the operation, then the {@code onDamaged} callback is always invoked.
     * If the item that caused the operation, does not support damaging the item on a chiseling operation, then the {@code onDamaged} callback is always invoked.
     * If the item that caused the operation is already broken, then the {@code onBroken} callback is always invoked.
     *
     * The total performed damaged is returned by this method, which is always {@code 0} if the item that caused the operation is already broken.
     *
     * Does exactly 1 damage to the item.
     *
     * @param onDamaged The callback to invoke when the item is damaged.
     * @param onBroken The callback to invoke when the item is broken.
     * @return The total damage applied to the item.
     */
    default int tryDamageItemAndDo(final Runnable onDamaged, final Runnable onBroken) {
        return tryDamageItemAndDo(1, onDamaged, onBroken);
    }

    /**
     * Invoked to try to damage the item that caused the chiseling operations.
     *
     * If no item was the cause of the operation, then the {@code onDamaged} callback is always invoked.
     * If the item that caused the operation, does not support damaging the item on a chiseling operation, then the {@code onDamaged} callback is always invoked.
     * If the item that caused the operation is already broken, then the broken chisel item error message is set.
     *
     * The total performed damaged is returned by this method, which is always {@code 0} if the item that caused the operation is already broken.
     *
     * Does exactly 1 damage to the item.
     *
     * @param onDamaged The callback to invoke when the item is damaged.
     * @return The total damage applied to the item.
     */
    default int tryDamageItemAndDoOrSetBrokenError(final Runnable onDamaged) {
        return tryDamageItemAndDo(1, onDamaged, () -> setError(LocalStrings.ChiselAttemptFailedChiselBroke.getText()));
    }

    /**
     * Invoked to try to damage the item that caused the chiseling operations.
     *
     * If no item was the cause of the operation, then the {@code onDamaged} callback is always invoked.
     * If the item that caused the operation, does not support damaging the item on a chiseling operation, then the {@code onDamaged} callback is always invoked.
     * If the item that caused the operation is already broken, then the broken chisel item error message is set.
     *
     * The total performed damaged is returned by this method, which is always {@code 0} if the item that caused the operation is already broken.
     *
     * @param damage The damage to apply to the item that caused the chiseling operation.
     * @param onDamaged The callback to invoke when the item is damaged.
     * @return The total damage applied to the item.
     */
    default int tryDamageItemAndDo(final int damage, final Runnable onDamaged) {
        return tryDamageItemAndDo(damage, onDamaged, () -> setError(LocalStrings.ChiselAttemptFailedChiselBroke.getText()));
    }

    /**
     * Invoked to try to damage the item that caused the chiseling operations.
     *
     * If no item was the cause of the operation, then the {@code onDamaged} callback is always invoked.
     * If the item that caused the operation, does not support damaging the item on a chiseling operation, then the {@code onDamaged} callback is always invoked.
     * If the item that caused the operation is already broken, then the {@code onBroken} callback is always invoked.
     *
     * The total performed damaged is returned by this method, which is always {@code 0} if the item that caused the operation is already broken.
     *
     * @param damage The damage to apply to the item that caused the chiseling operation.
     * @param onDamaged The callback to invoke when the item is damaged.
     * @param onBroken The callback to invoke when the item is broken.
     * @return The total damage applied to the item.
     */
    int tryDamageItemAndDo(final int damage, final Runnable onDamaged, final Runnable onBroken);

    /**
     * Allows for the setting of a filterBuilder on the context, which limits which {@link IStateEntryInfo} are returned from
     * the relevant accessor methods of the {@link IWorldAreaMutator}, as well as which limits the setter methods on the same {@link IWorldAreaMutator}
     * contained in the returned optional of the {@link #getMutator()}.
     *
     * If this context has currently no mutator available, and gets a mutator available afterwards then this filterBuilder will be applied to the new mutator.
     *
     * @param filterBuilder The new filterBuilder.
     */
    void setStateFilter(@NotNull final Function<IAreaAccessor, Predicate<IStateEntryInfo>> filterBuilder);

    /**
     * Clears the state filter which is applied to the {@link IWorldAreaMutator} for this context.
     */
    void clearStateFilter();

    /**
     * Returns the filter that is currently applied on the context.
     * If one is applied.
     *
     * @return An optional, potentially containing the filter.
     */
    Optional<Function<IAreaAccessor, Predicate<IStateEntryInfo>>> getStateFilter();

    /**
     * Allows the storage of metadata on the context.
     * @param key The key of the metadata.
     * @param <T> The type of the metadata.
     * @return An optional possibly containing the stored metadata, or empty if no metadata is stored with the given key.
     */
    <T> Optional<T> getMetadata(IMetadataKey<T> key);

    /**
     * Removes the metadata from the context.
     * @param key The key for the metadata.
     */
    void removeMetadata(IMetadataKey<?> key);

    /**
     * Allows for the setting of the metadata on the context.
     * @param key The key of the metadata.
     * @param value The value of the metadata.
     * @param <T> The type of the metadata.
     */
    <T> void setMetadata(IMetadataKey<T> key, T value);

    /**
     * Resets the mutator that is used to handle the current selected area.
     */
    void resetMutator();

    /**
     * Sets the error message that is displayed when the chiseling operation fails.
     * If an error is already set on the context, then subsequent calls to this method will be ignored.
     *
     * @param errorText The new error message.
     */
    void setError(MutableComponent errorText);

    /**
     * Returns the error message that is displayed when the chiseling operation fails.
     *
     * @return An optional with the potential error message included.
     */
    Optional<MutableComponent> getError();

    /**
     * Checks whether the mutator currently modifies blocks outside the levels build height.
     *
     * @return True when the build height is respected, false when not.
     */
    default boolean validateBuildHeights() {
        return getMutator().map(mutator -> {
            final BlockPos heightPos = mutator.getInWorldEndBlockPoint();
            if (heightPos.getY() >= getWorld().getMaxY())
            {
                setError(LocalStrings.ChiselAttemptFailedAttemptTooHigh.getText());
                return false;
            }
            else if (heightPos.getY() <= getWorld().getMinY()) {
                setError(LocalStrings.ChiselAttemptFailedAttemptTooLow.getText());
                return false;
            }
            return true;
        }).orElse(true);
    }
}

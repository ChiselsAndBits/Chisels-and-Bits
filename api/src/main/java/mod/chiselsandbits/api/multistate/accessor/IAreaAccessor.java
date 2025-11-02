package mod.chiselsandbits.api.multistate.accessor;

import mod.chiselsandbits.api.aabb.IAABBOwner;
import mod.chiselsandbits.api.item.multistate.IStatistics;
import mod.chiselsandbits.api.multistate.accessor.identifier.IAreaShapeIdentifier;
import mod.chiselsandbits.api.multistate.accessor.sortable.IPositionMutator;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Gives access to all states in a given area.
 * Might be larger than a single block.
 */
public interface IAreaAccessor extends IStateAccessor, IAABBOwner
{
    /**
     * Creates a new area shape identifier.
     *
     * Note: This method always returns a new instance.
     *
     * @return The new identifier.
     */
    IAreaShapeIdentifier createNewShapeIdentifier();

    /**
     * Gives access to a stream with the entry state info inside the accessors range.
     *
     * @return The stream with the inner states.
     */
    Stream<IStateEntryInfo> stream();

    /**
     * Indicates if the given target is inside of the current accessor.
     *
     * @param inAreaTarget The area target to check.
     * @return True when inside, false when not.
     */
    boolean isInside(final Vec3 inAreaTarget);

    /**
     * Indicates if the given target (with the given block position offset) is inside of the current accessor.
     *
     * @param inAreaBlockPosOffset The offset of blocks in the current area.
     * @param inBlockTarget        The offset in the targeted block.
     * @return True when inside, false when not.
     */
    boolean isInside(
      BlockPos inAreaBlockPosOffset,
      Vec3 inBlockTarget
    );

    /**
     * Creates a snapshot of the current state.
     *
     * @return The snapshot.
     */
    IMultiStateSnapshot createSnapshot();

    /**
     * Gives access to a stream with the entry state info inside the accessors range.
     * Allows for the entry state order to be mutated using a position mutator.
     *
     * @param positionMutator The mutator for the positional order.
     * @return The stream with the inner states.
     */
    Stream<IStateEntryInfo> streamWithPositionMutator(IPositionMutator positionMutator);

    /**
     * Runs a for each-loop over the states inside the accessor, with the ability to specify the loop order.
     *
     * @param positionMutator The position mutator to use.
     * @param consumer The consumer to pass the states to.
     */
    void forEachWithPositionMutator(final IPositionMutator positionMutator, final Consumer<IStateEntryInfo> consumer);

}

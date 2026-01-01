package mod.chiselsandbits.api.item.multistate;

import mod.chiselsandbits.api.multistate.accessor.IAreaAccessor;
import mod.chiselsandbits.api.multistate.mutator.IGenerallyModifiableAreaMutator;
import mod.chiselsandbits.api.multistate.mutator.IMirrorAndRotateble;
import mod.chiselsandbits.api.serialization.RawSerializable;
import net.minecraft.world.item.ItemStack;

/**
 * The itemstack sensitive version of the multistate item.
 */
public interface IMultiStateItemStack extends IAreaAccessor, IMirrorAndRotateble {
    /**
     * The statistics of the itemstack.
     *
     * @return The statistics.
     */
    IStatistics getStatistics();

    /**
     * Converts this multistate itemstack data to an actual use able itemstack.
     *
     * @return The itemstack with the data of this multistate itemstack.
     */
    ItemStack toBlockStack();

    /**
     * Converts this multistate itemstack data into a pattern that can be reused.
     * By default converts this into a single use pattern.
     *
     * @return The single use patter from this multi state itemstack.
     */
    ItemStack toPatternStack();

    /**
     * Writes the state data to the itemstack.
     */
    void writeDataTo(ItemStack stack);

    /**
     * Copies the current stack.
     *
     * @return The stack copied.
     */
    IMultiStateItemStack copy();
}

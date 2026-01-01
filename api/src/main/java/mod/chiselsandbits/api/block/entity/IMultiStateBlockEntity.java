package mod.chiselsandbits.api.block.entity;

import mod.chiselsandbits.api.axissize.CollisionType;
import mod.chiselsandbits.api.block.IMultiStateBlock;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.item.multistate.IMultiStateItemStack;
import mod.chiselsandbits.api.multistate.accessor.IAreaAccessorWithVoxelShape;
import mod.chiselsandbits.api.multistate.accessor.ISingleBlockAxisAlignedAreaAccessor;
import mod.chiselsandbits.api.multistate.accessor.world.IWorldAreaAccessor;
import mod.chiselsandbits.api.multistate.mutator.IGenerallyModifiableAreaMutator;
import mod.chiselsandbits.api.multistate.mutator.batched.IBatchedAreaMutator;
import mod.chiselsandbits.api.multistate.mutator.world.IWorldAreaMutator;
import mod.chiselsandbits.api.multistate.statistics.IMultiStateObjectStatistics;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Represents the block entity with the state data, which under-ly the information
 * provided by the {@link IMultiStateBlock} blocks.
 */
public interface IMultiStateBlockEntity extends IWorldAreaAccessor,
    IWorldAreaMutator,
    IBatchedAreaMutator,
    IGenerallyModifiableAreaMutator,
    IAreaAccessorWithVoxelShape,
    ISingleBlockAxisAlignedAreaAccessor
{

    /**
     * Indicates whether the current block entity can be flooded with water.
     *
     * @return True to allow flooding, false when not.
     */
    boolean isCanBeFlooded();

    /**
     * Sets the flooding indicator.
     *
     * @param canBeFlooded True to allow flooding, false when not.
     */
    void setCanBeFlooded(boolean canBeFlooded);

    /**
     * Indicates whether the lighting conditions of the block are based on the amount of bits in the block or the full block.
     *
     * @return True to use full block size calculation, false when not.
     */
    boolean isEmitsLightBasedOnFullBlock();

    /**
     * Sets the lighting conditions indicator.
     *
     * @param emitsLightBasedOnFullBlock True to calculate lighting conditions over the full block, false only over the set bits.
     */
    void setEmitsLightBasedOnFullBlock(boolean emitsLightBasedOnFullBlock);

    /**
     * The statistics of this block.
     *
     * @return The statistics.
     */
    IMultiStateObjectStatistics getStatistics();

    /**
     * Rotates the current multistate block 90 degrees around the given axis with the given rotation count.
     *
     * @param axis          The axis to rotate around.
     * @param rotationCount The amount of times to rotate the
     */
    void rotate(final Direction.Axis axis, final int rotationCount);

    /**
     * Initializes the block entity so that all its state entries
     * have the given block information as their block information.
     *
     * @param initialInformation The new initial block information.
     */
    void initializeWith(BlockInformation initialInformation);

    /**
     * Returns the current blocks shape for the given collision type.
     *
     * @param type The collision type to get the shape for.
     * @return The shape.
     */
    VoxelShape getShape(CollisionType type);

    /**
     * Creates an {@link ItemStack} for placement as a block.
     *
     * @return The block stack.
     */
    BlockStack getBlockStack();

    record BlockStack(ItemStack stack, IMultiStateItemStack multiStateItemStack) {
        public BlockStack copy() {
            return new BlockStack(stack.copy(), multiStateItemStack.copy());
        }
    }
}

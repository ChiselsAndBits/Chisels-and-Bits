package mod.chiselsandbits.api.multistate.accessor;

import mod.chiselsandbits.api.item.multistate.IStatistics;

/**
 * Represents a single block axis aligned area accessor
 */
public interface ISingleBlockAxisAlignedAreaAccessor extends IAreaAccessor
{
    /**
     * The statistics of the itemstack.
     *
     * @return The statistics.
     */
    IStatistics getStatistics();
}

package mod.chiselsandbits.item.multistate;

import mod.chiselsandbits.api.item.bit.IBitItemManager;
import mod.chiselsandbits.api.item.multistate.IMultiStateItemFactory;
import mod.chiselsandbits.api.multistate.accessor.IStateEntryInfo;
import net.minecraft.world.item.ItemStack;

public class MultiStateItemFactory implements IMultiStateItemFactory
{
    private static final MultiStateItemFactory INSTANCE = new MultiStateItemFactory();

    private MultiStateItemFactory()
    {
    }

    public static MultiStateItemFactory getInstance()
    {
        return INSTANCE;
    }

    /**
     * Creates a new multistate itemstack with a single state internally.
     *
     * @param stateEntryInfo The state entry info to create an itemstack for.
     * @return The itemstack containing only the given single state entry.
     */
    @Override
    public ItemStack createBlockFrom(final IStateEntryInfo stateEntryInfo)
    {
        if (stateEntryInfo.getBlockInformation().isAir())
            return ItemStack.EMPTY;

        return IBitItemManager.getInstance().create(stateEntryInfo.getBlockInformation());
    }
}

package mod.chiselsandbits.api.inventory.bit;

import java.util.Optional;

import mod.chiselsandbits.api.IChiselsAndBitsAPI;

public interface IAdaptingBitInventoryManager
{
	static IAdaptingBitInventoryManager getInstance() {
		return IChiselsAndBitsAPI.getInstance().getAdaptingBitInventoryManager();
	}

	/**
     * Creates a new bit inventory wrapping the given {@link Object}.
     *
     * This inventory is aware of items which themselves can act
     * as a bit inventory.
     *
     * This endpoint is platform specific and might or might not be able to convert the object given.
     * Importantly on forge this endpoint is able to accept IItemHandlers, while on Fabric it will only support
     * IInventory.
     *
     * @param target The {@link Object}.
     * @return The bit inventory which represents the inventory.
     */
	Optional<Object> create(final Object target);
}

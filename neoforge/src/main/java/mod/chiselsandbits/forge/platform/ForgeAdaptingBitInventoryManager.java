package mod.chiselsandbits.forge.platform;

import mod.chiselsandbits.api.inventory.bit.IAdaptingBitInventoryManager;
import mod.chiselsandbits.forge.inventory.bit.IItemHandlerBitInventory;
import net.neoforged.neoforge.transfer.ResourceHandler;

import java.util.Optional;

public final class ForgeAdaptingBitInventoryManager implements IAdaptingBitInventoryManager {
    private static final ForgeAdaptingBitInventoryManager INSTANCE = new ForgeAdaptingBitInventoryManager();

    public static ForgeAdaptingBitInventoryManager getInstance() {
        return INSTANCE;
    }
    private ForgeAdaptingBitInventoryManager() {
    }

    @Override
    public Optional<Object> create(Object target) {
        return Optional.of(target)
                .filter(ResourceHandler.class::isInstance)
                .map(ResourceHandler.class::cast)
                .map(IItemHandlerBitInventory::new);
    }


}

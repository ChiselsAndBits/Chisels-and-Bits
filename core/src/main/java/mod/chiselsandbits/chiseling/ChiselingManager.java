package mod.chiselsandbits.chiseling;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import mod.chiselsandbits.api.chiseling.ChiselingOperation;
import mod.chiselsandbits.api.chiseling.mode.IChiselMode;
import mod.chiselsandbits.api.chiseling.IChiselingContext;
import mod.chiselsandbits.api.chiseling.IChiselingManager;
import mod.chiselsandbits.api.util.constants.Constants;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;

import java.util.Optional;
import java.util.UUID;

public class ChiselingManager implements IChiselingManager
{
    private static final ChiselingManager INSTANCE = new ChiselingManager();

    private UUID activeInstanceId = UUID.randomUUID();

    private final ThreadLocal<UUID> activeThreadId = ThreadLocal.withInitial(() -> activeInstanceId);

    private final ThreadLocal<Table<UUID, Identifier, IChiselingContext>> contexts              = ThreadLocal.withInitial(HashBasedTable::create);
    private final ThreadLocal<Table<UUID, Identifier, Long>>              lastUsedChiselMoments = ThreadLocal.withInitial(HashBasedTable::create);

    private ChiselingManager()
    {
    }

    public static ChiselingManager getInstance()
    {
        return INSTANCE;
    }

    /**
     * Gives access to the chiseling context of the player, if it exists.
     *
     * @param playerEntity The player for which the context is looked up.
     * @param mode         The mode which the player wants to chisel in
     * @return An optional potentially containing the current context of the player.
     */
    @Override
    public Optional<IChiselingContext> get(final Player playerEntity, final IChiselMode mode)
    {
        final UUID playerId = playerEntity.getUUID();
        final Identifier worldId = playerEntity.level().dimension().identifier();

        final IChiselingContext currentStored = contexts.get().get(playerId, worldId);
        if (currentStored == null)
            return Optional.empty();

        return Optional.of(currentStored);
    }

    @Override
    public Optional<IChiselingContext> get(final Player playerEntity, final IChiselMode mode, final ChiselingOperation modeOfOperandus)
    {
        final UUID playerId = playerEntity.getUUID();
        final Identifier worldId = playerEntity.level().dimension().identifier();

        final IChiselingContext currentStored = contexts.get().get(playerId, worldId);
        if (currentStored == null)
            return Optional.empty();

        if (currentStored.getModeOfOperandus() == modeOfOperandus)
            return Optional.of(currentStored);

        return Optional.empty();
    }

    @Override
    public IChiselingContext create(final Player playerEntity, final IChiselMode mode, final ChiselingOperation modeOfOperandus, final boolean simulation, final ItemStack causingItemStack)
    {
        final UUID playerId = playerEntity.getUUID();
        final Identifier worldId = playerEntity.level().dimension().identifier();

        final IChiselingContext currentStored = contexts.get().get(playerId, worldId);

        //We might already have one stored.
        if (currentStored != null) {
            if (!simulation) {
                contexts.get().remove(playerId, worldId);
            }
        }

        final ChiselingContext newContext = new ChiselingContext(playerEntity.level(),
          mode,
          modeOfOperandus,
          simulation, () -> {
            if (simulation)
                return;

            this.lastUsedChiselMoments.get().put(playerId, worldId, (long) playerEntity.tickCount);
            contexts.get().remove(playerId, worldId);
        }, causingItemStack, playerEntity);

        if (!simulation)
        {
            contexts.get().put(playerId, worldId, newContext);
        }

        return newContext;
    }

    public boolean canChisel(final Player playerEntity) {
        validateOrSetup();

        final UUID playerId = playerEntity.getUUID();
        final Identifier worldId = playerEntity.level().dimension().identifier();

        final Long lastChiselTime = this.lastUsedChiselMoments.get().get(playerId, worldId);
        if (lastChiselTime == null)
            return true;

        final long time = playerEntity.tickCount;
        final long diffSinceLastUse = time - lastChiselTime;

        if (diffSinceLastUse < 0 || diffSinceLastUse > Constants.TICKS_BETWEEN_CHISEL_USAGE) {
            resetLastChiselCountdown(playerEntity);
            return true;
        }

        return false;
    }

    public void onServerStarting()
    {
        this.activeInstanceId = UUID.randomUUID();
    }

    private void validateOrSetup() {
        final UUID threadId = activeThreadId.get();
        if (threadId != activeInstanceId) {
            this.contexts.get().clear();
            this.lastUsedChiselMoments.get().clear();

            activeThreadId.set(activeInstanceId);
        }
    }

    public void resetLastChiselCountdown(final Player player)
    {
        this.lastUsedChiselMoments.get().row(player.getUUID()).clear();
    }
}

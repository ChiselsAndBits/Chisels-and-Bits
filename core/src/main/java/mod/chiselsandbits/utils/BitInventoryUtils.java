package mod.chiselsandbits.utils;

import com.communi.suggestu.scena.core.entity.IPlayerInventoryManager;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.config.IServerConfiguration;
import mod.chiselsandbits.api.inventory.bit.IBitInventory;
import mod.chiselsandbits.api.inventory.management.IBitInventoryManager;
import mod.chiselsandbits.api.item.bit.IBitItemManager;
import mod.chiselsandbits.registrars.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class BitInventoryUtils
{

    private BitInventoryUtils()
    {
        throw new IllegalStateException("Can not instantiate an instance of: BitInventoryUtils. This is a utility class");
    }

    public static void insertIntoOrSpawn(final Player playerEntity, final BlockInformation blockState, final int count) {
        if (playerEntity == null || playerEntity.level().isClientSide() || count <= 0)
            return;

        final IBitInventory inventory = IBitInventoryManager.getInstance().create(playerEntity);

        if (playerEntity.isCreative()) {
            if (inventory.canExtractOne(blockState))
                return;

            if (inventory.canInsertOne(blockState)) {
                inventory.insertOne(blockState);
            }
            return;
        }

        final int maxInsertionCount = inventory.getMaxInsertAmount(blockState);

        final int insertionCount = Math.min(maxInsertionCount, count);
        inventory.insert(blockState, insertionCount);

        int leftOverCount = count - insertionCount;
        if (leftOverCount <= 0)
            return;

        if (!IServerConfiguration.getInstance().getDeleteExcessBits().get()) {
            while(leftOverCount > 0) {
                final int spawnCount = Math.min(ModItems.ITEM_BLOCK_BIT.get().getDefaultMaxStackSize(), leftOverCount);
                if (spawnCount <= 0)
                    break;

                leftOverCount -= spawnCount;

                final ItemStack spawnStack = IBitItemManager.getInstance().create(blockState, spawnCount);
                IPlayerInventoryManager.getInstance().giveToPlayer(playerEntity, spawnStack);
            }
        }
    }
}

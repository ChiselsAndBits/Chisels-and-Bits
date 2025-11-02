package mod.chiselsandbits.registrars;

import com.communi.suggestu.scena.core.entity.block.IBlockEntityPositionManager;
import com.communi.suggestu.scena.core.event.*;
import mod.chiselsandbits.ChiselsAndBits;
import mod.chiselsandbits.api.item.click.ClickProcessingState;
import mod.chiselsandbits.block.entities.ChiseledBlockEntity;
import mod.chiselsandbits.logic.*;
import mod.chiselsandbits.network.packets.UpdateBlockEntityPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ModEventHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    private ModEventHandler() {
        throw new IllegalStateException("Can not instantiate an instance of: ModEventHandler. This is a utility class");
    }

    public static void onModConstruction() {
        LOGGER.info("Registering event handlers");

        IGameEvents.getInstance().getItemEntityPickupEvent().register(BitStackPickupHandler::pickupItems);
        IGameEvents.getInstance().getPlayerLeftClickEvent().register(new IPlayerLeftClickBlockEvent() {
            @Override
            public Result handle(Player player, InteractionHand interactionHand, ItemStack itemStack, BlockPos blockPos, Direction direction, Result result) {
                final ClickProcessingState state = LeftClickInteractionHandler.leftClickOnBlock(
                        player,
                        interactionHand,
                        itemStack,
                        blockPos,
                        direction,
                        result.handled(),
                        mapResult(result)
                );

                return new Result(state.shouldCancel(), mapResult(state.getNextState()), mapResult(state.getNextState()));
            }

            private static ClickProcessingState.ProcessingResult mapResult(
                    final IPlayerLeftClickBlockEvent.Result result)
            {
                return switch (result.useItemResult())
                        {
                            case DENY -> ClickProcessingState.ProcessingResult.DENY;
                            case DEFAULT -> ClickProcessingState.ProcessingResult.DEFAULT;
                            case ALLOW -> ClickProcessingState.ProcessingResult.ALLOW;
                        };
            }

            private static ProcessingResult mapResult(
                    final ClickProcessingState.ProcessingResult processingResult
            ) {
                return switch (processingResult)
                        {
                            case DENY -> ProcessingResult.DENY;
                            case DEFAULT -> ProcessingResult.DEFAULT;
                            case ALLOW -> ProcessingResult.ALLOW;
                        };
            }
        });
        IGameEvents.getInstance().getDataPackSyncEvent().register((list, players, recipeTypeSynchroniser) -> {
            MeasuringSynchronisationHandler.syncToAll();
        });
        IGameEvents.getInstance().getPlayerJoinedWorldEvent().register((player, level) -> {
            ChiselingManagerCountDownResetHandler.doResetFor(player);
            if (player instanceof ServerPlayer serverPlayer) {
                MeasuringSynchronisationHandler.syncTo(serverPlayer);
            }
        });
        IGameEvents.getInstance().getRegisterCommandsEvent().register(CommandRegistrationHandler::registerCommandsTo);
        IGameEvents.getInstance().getPlayerRightClickEvent().register(new IPlayerRightClickBlockEvent() {
            @Override
            public Result handle(Player player, InteractionHand interactionHand, ItemStack itemStack, BlockPos blockPos, Direction direction, Result result) {
                final ClickProcessingState state = RightClickInteractionHandler.rightClickOnBlock(
                        player.level(),
                        player,
                        interactionHand,
                        itemStack,
                        blockPos,
                        direction,
                        result.handled(),
                        mapItemResult(result)
                );

                return new Result(state.shouldCancel(), mapResult(state.getNextState(), result.useItemResult()), mapResult(state.getNextState(), result.useBlockResult()));
            }

            private static ClickProcessingState.ProcessingResult mapItemResult(
                    final IPlayerRightClickBlockEvent.Result result)
            {
                return switch (result.useItemResult())
                        {
                            case DENY -> ClickProcessingState.ProcessingResult.DENY;
                            case DEFAULT -> ClickProcessingState.ProcessingResult.DEFAULT;
                            case ALLOW -> ClickProcessingState.ProcessingResult.ALLOW;
                        };
            }

            private static ProcessingResult mapResult(
                    final ClickProcessingState.ProcessingResult processingResult,
                    final ProcessingResult current
            ) {
                return switch (processingResult)
                        {
                            case DENY -> ProcessingResult.DENY;
                            case DEFAULT -> current;
                            case ALLOW -> ProcessingResult.ALLOW;
                        };
            }
        });
        IGameEvents.getInstance().getServerAboutToStartEvent().register(minecraftServer -> ServerStartHandler.onServerStart());
        IGameEvents.getInstance().getServerPostTickEvent().register(minecraftServer -> ServerTickHandler.onPostServerTick());

        IGameEvents.getInstance().getChunkSentEvent().register((serverPlayer, levelChunk, serverLevel) -> {
            IBlockEntityPositionManager.getInstance().getPositions(ChiseledBlockEntity.class, serverLevel, levelChunk.getPos())
                    .stream()
                    .map(serverLevel::getBlockEntity)
                    .filter(ChiseledBlockEntity.class::isInstance)
                    .map(ChiseledBlockEntity.class::cast)
                    .forEach(chiseledBlockEntity -> {
                        ChiselsAndBits.getInstance().getNetworkChannel().sendToTrackingChunk(
                                new UpdateBlockEntityPacket(chiseledBlockEntity),
                                serverLevel.getChunkAt(chiseledBlockEntity.getBlockPos())
                        );
                    });
        });
    }
}

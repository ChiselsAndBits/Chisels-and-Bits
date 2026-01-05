package mod.chiselsandbits.client.registrars;

import com.communi.suggestu.scena.core.client.event.IClientEvents;
import com.communi.suggestu.scena.core.event.IGameEvents;
import mod.chiselsandbits.client.input.FrameBasedInputTracker;
import mod.chiselsandbits.client.logic.*;
import mod.chiselsandbits.client.time.TickHandler;
import mod.chiselsandbits.clipboard.CreativeClipboardManager;
import mod.chiselsandbits.keys.KeyBindingManager;
import mod.chiselsandbits.logic.MagnifyingGlassTooltipHandler;
import mod.chiselsandbits.client.logic.ScrollBasedModeChangeHandler;
import net.minecraft.world.level.chunk.LevelChunk;

public final class EventHandlers {

    private EventHandlers() {
        throw new IllegalStateException("Can not instantiate an instance of: EventHandlers. This is a utility class");
    }

    public static void onClientConstruction() {
        IGameEvents.getInstance().getChunkLoadEvent().register((levelAccessor, chunkAccess) -> {
            if (chunkAccess instanceof LevelChunk levelChunk)
                ChiseledBlockModelUpdateHandler.updateAllModelDataInChunk(levelChunk);
        });
        IGameEvents.getInstance().getPlayerLoggedInEvent().register((player) -> CreativeClipboardManager.getInstance().load());
        IClientEvents.getInstance().getClientTickStartedEvent().register(() -> {
            ToolNameHighlightTickHandler.handleClientTickForMagnifyingGlass();
            KeyBindingManager.getInstance().handleKeyPresses();
            TickHandler.onClientTick();
            MeasurementTapeItemResetHandler.checkAndDoReset();
        });
        IClientEvents.getInstance().getDrawHighlightEvent().register(SelectedObjectHighlightHandler::onDrawHighlight);
        IClientEvents.getInstance().getScrollEvent().register(ScrollBasedModeChangeHandler::onScroll);
        IClientEvents.getInstance().getHUDRenderEvent().register(SlotOverlayRenderHandler::renderSlotOverlays);
        IClientEvents.getInstance().getPostRenderWorldEvent().register((levelRenderer, poseStack, partialTickTime) -> {
            SelectedObjectRenderHandler.renderCustomWorldHighlight(
                    levelRenderer,
                    poseStack,
                    partialTickTime
            );

            MeasurementsRenderHandler.renderMeasurements(poseStack);

            MultiStateBlockPreviewRenderHandler.renderMultiStateBlockPreview(poseStack);

            FrameBasedInputTracker.getInstance().onRenderFrame();
        });
        IClientEvents.getInstance().getGatherTooltipEvent().register(MagnifyingGlassTooltipHandler::onItemTooltip);
    }
}

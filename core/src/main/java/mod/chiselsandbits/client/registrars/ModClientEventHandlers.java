package mod.chiselsandbits.client.registrars;

import com.communi.suggestu.scena.core.client.event.IClientEvents;
import com.communi.suggestu.scena.core.event.IGameEvents;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.client.clipboard.CreativeClipboardManager;
import mod.chiselsandbits.client.icon.IconManager;
import mod.chiselsandbits.client.input.FrameBasedInputTracker;
import mod.chiselsandbits.client.logic.*;
import mod.chiselsandbits.client.model.block.ChiseledBlockStateModel;
import mod.chiselsandbits.client.model.item.BitBlockItemModel;
import mod.chiselsandbits.client.model.item.ChiseledBlockItemModel;
import mod.chiselsandbits.client.model.item.InteractableItemModel;
import mod.chiselsandbits.client.reloading.ClientResourceReloadingManager;
import mod.chiselsandbits.client.screens.pips.RotatableItemRenderer;
import mod.chiselsandbits.client.screens.pips.Torus;
import mod.chiselsandbits.client.time.TickHandler;
import mod.chiselsandbits.keys.KeyBindingManager;
import mod.chiselsandbits.logic.MagnifyingGlassTooltipHandler;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.chunk.LevelChunk;

public final class ModClientEventHandlers
{

    private ModClientEventHandlers() {
        throw new IllegalStateException("Can not instantiate an instance of: EventHandlers. This is a utility class");
    }

    public static void onClientConstruction() {
        IGameEvents.getInstance().getChunkLoadEvent().register((_, chunkAccess) -> {
            if (chunkAccess instanceof LevelChunk levelChunk)
                ChiseledBlockModelUpdateHandler.updateAllModelDataInChunk(levelChunk);
        });
        IGameEvents.getInstance().getPlayerJoinedWorldEvent().register((_, level) -> CreativeClipboardManager.getInstance().load(level.registryAccess()));
        IClientEvents.getInstance().getClientTickStartedEvent().register(() -> {
            ToolNameHighlightTickHandler.handleClientTickForMagnifyingGlass();
            KeyBindingManager.getInstance().handleKeyPresses();
            TickHandler.onClientTick();
            MeasurementTapeTickHandler.tick();
        });
        IClientEvents.getInstance().getScrollEvent().register(ScrollBasedModeChangeHandler::onScroll);
        IClientEvents.getInstance().getHUDRenderEvent().register(SlotOverlayRenderHandler::renderSlotOverlays);
        IClientEvents.getInstance().getDrawHighlightEvent().register(SelectedObjectHighlightHandler::onDrawHighlight);
        IClientEvents.getInstance().getPostRenderWorldEvent().register((levelRenderer, poseStack, bufferSource, levelRenderState, partialTickTime) -> {
            SelectedObjectRenderHandler.renderCustomWorldHighlight(
                    levelRenderer,
                    poseStack,
                    bufferSource,
                    levelRenderState,
                    partialTickTime
            );

            MeasurementsRenderHandler.renderMeasurements(
                poseStack,
                bufferSource,
                partialTickTime
            );

            MultiStateBlockPreviewRenderHandler.renderMultiStateBlockPreview(
                poseStack,
                bufferSource
            );

            FrameBasedInputTracker.getInstance().onRenderFrame();
        });
        IClientEvents.getInstance().getGatherTooltipEvent().register((itemStack, _, _, list) -> {
            MagnifyingGlassTooltipHandler.onItemTooltip(itemStack, list);
        });
        IClientEvents.getInstance().getRegisterTextureAtlasesEvent().register(consumer -> {
            IconManager.getInstance().initialize(consumer);
        });
        IClientEvents.getInstance().getRegisterClientResourceReloadListenersEvent().register(ClientResourceReloadingManager::setup);
        IClientEvents.getInstance().getRegisterPIPRenderersEvent().register(registrar -> {
            registrar.register(RotatableItemRenderer.RenderState.class, RotatableItemRenderer::new);
            registrar.register(Torus.RenderState.class, Torus::new);
        });
        IClientEvents.getInstance().getRegisterBlockStateModelEvent().register(registrar -> {
            registrar.registerModel(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "chiseled_block"), ChiseledBlockStateModel.Unbaked.CODEC);
        });
        IClientEvents.getInstance().getRegisterItemModelEvent().register(registrar -> {
            registrar.registerModel(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "bit_block"), BitBlockItemModel.Unbaked.CODEC);
            registrar.registerModel(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "chiseled_block"), ChiseledBlockItemModel.Unbaked.CODEC);
            registrar.registerModel(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "interactable"), InteractableItemModel.Unbaked.CODEC);
        });
    }
}

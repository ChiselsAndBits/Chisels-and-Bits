package mod.chiselsandbits.client.highlight;

import com.communi.suggestu.scena.core.client.event.IExtractLevelRenderStateEvent;
import com.communi.suggestu.scena.core.client.event.ISubmitCustomGeometryEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import mod.chiselsandbits.api.chiseling.ChiselingOperation;
import mod.chiselsandbits.api.chiseling.IChiselingContext;
import mod.chiselsandbits.api.chiseling.IChiselingManager;
import mod.chiselsandbits.api.chiseling.ILocalChiselingContextCache;
import mod.chiselsandbits.api.chiseling.mode.IChiselMode;
import mod.chiselsandbits.api.client.highlight.IItemBasedHighlightRenderer;
import mod.chiselsandbits.api.client.render.preview.chiseling.IChiselContextPreviewRenderer;
import mod.chiselsandbits.api.item.bit.IBitItem;
import mod.chiselsandbits.api.item.chisel.IChiselingItem;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.client.chiseling.preview.render.ChiselContextPreviewRendererRegistry;
import mod.chiselsandbits.utils.ItemStackUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public class ChiselingModeHighlightRenderer implements IItemBasedHighlightRenderer {

    private static final Identifier CHISELING_PREVIEW_STATE = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "chiseling_preview_state");
    private static final Identifier BIT_PLACEMENT_PREVIEW_STATE = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "bit_placement_preview_state");

    private interface IChiselingAction {
        void applyAction(
            IChiselMode mode,
			Player playerEntity,
			IChiselingContext context
	    );
    }

    @Nullable
    private IChiselingContext createChiselingContextFor(
            Player playerEntity,
            ItemStack itemStack,
            IChiselingAction action
    ) {
        if (itemStack.isEmpty() || !(itemStack.getItem() instanceof IChiselingItem chiselingItem)) {
            return null;
        }

        final IChiselMode chiselMode = chiselingItem.getMode(itemStack);

        final Optional<IChiselingContext> potentiallyExistingContext =
                IChiselingManager.getInstance().get(playerEntity, chiselMode, ChiselingOperation.CHISELING);

        if (potentiallyExistingContext.isPresent()) {
            final IChiselingContext context = potentiallyExistingContext.get();

            if (context.getMutator().isPresent()) {
                return context;
            }

            final IChiselingContext currentContextSnapshot = context.createSnapshot();
            action.applyAction(chiselMode, playerEntity, currentContextSnapshot);

            return currentContextSnapshot;
        }

        final Optional<IChiselingContext> localCachedContext = ILocalChiselingContextCache
                .getInstance()
                .get(ChiselingOperation.CHISELING);

        if (localCachedContext.isPresent()
                && localCachedContext.get().getMode().isStillValid(playerEntity, localCachedContext.get(), ChiselingOperation.CHISELING)) {
            final IChiselingContext context = localCachedContext.get();

            if (context.getMode() == chiselMode) {
                if (context.getMutator().isPresent()) {
                    return context;
                }
            }

            return context;
        }

        final IChiselingContext context = IChiselingManager.getInstance().create(
                playerEntity,
                chiselMode,
                ChiselingOperation.CHISELING,
                true,
                itemStack);

        //We try a left click render primary.
        action.applyAction(chiselMode, playerEntity, context);

        //Store it in the local cache.
        if (!context.isComplete()) {
            ILocalChiselingContextCache.getInstance().set(ChiselingOperation.CHISELING, context);
        }

        return context;
    }

    @Override
    public boolean shouldDrawDefaultHighlight(Player playerEntity, ItemStack itemStack) {
        final IChiselingContext context = createChiselingContextFor(
                playerEntity,
                itemStack,
                IChiselMode::onLeftClickBy
        );

        if (context == null) {
            if (itemStack.getItem() instanceof IBitItem) {
                final IChiselingContext rightClickContext = createChiselingContextFor(
                        playerEntity,
                        itemStack,
                        IChiselMode::onRightClickBy
                );

                if (rightClickContext == null) {
                    return true;
                }

                return rightClickContext.getMutator().isEmpty();
            }

            return true;
        }

        return context.getMutator().isEmpty();
    }

    @Override
    public void extractState(LevelRenderState levelRenderState, ClientLevel clientLevel, Camera camera, DeltaTracker deltaTracker, IExtractLevelRenderStateEvent.IRenderStateStore store) {
        final IChiselContextPreviewRenderer<?> renderer = ChiselContextPreviewRendererRegistry.getInstance().getCurrent();
        final ItemStack heldChiselingItemStack = ItemBasedHighlightRendererRegistry.getItemWithCustomHighlightsFromHandsFor(Minecraft.getInstance().player);

        if (heldChiselingItemStack.isEmpty()) {
            return;
        }

        if (heldChiselingItemStack.getItem() instanceof IChiselingItem) {
            final IChiselingContext chiselingContext = createChiselingContextFor(
                    Minecraft.getInstance().player,
                    heldChiselingItemStack,
                    IChiselMode::onLeftClickBy
            );

            if (chiselingContext != null) {
                final Object state = renderer.extractState(
                        chiselingContext,
                        levelRenderState,
                        clientLevel,
                        camera,
                        deltaTracker
                );

                store.write(CHISELING_PREVIEW_STATE, state);
            }
        }

        if (heldChiselingItemStack.getItem() instanceof IBitItem) {
            final IChiselingContext chiselingContext = createChiselingContextFor(
                    Minecraft.getInstance().player,
                    heldChiselingItemStack,
                    IChiselMode::onRightClickBy
            );

            if (chiselingContext != null) {
                final Object state = renderer.extractState(
                        chiselingContext,
                        levelRenderState,
                        clientLevel,
                        camera,
                        deltaTracker
                );

                store.write(BIT_PLACEMENT_PREVIEW_STATE, state);
            }
        }
    }

    @Override
    public void submitHighlightGeometry(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, LevelRenderState levelRenderState, ISubmitCustomGeometryEvent.IRenderStateStore store) {
        final IChiselContextPreviewRenderer<?> renderer = ChiselContextPreviewRendererRegistry.getInstance().getCurrent();

        submitHighlightGeometryForState(
                submitNodeCollector,
                poseStack,
                levelRenderState,
                store,
                renderer,
                store.read(CHISELING_PREVIEW_STATE)
        );

        submitHighlightGeometryForState(
                submitNodeCollector,
                poseStack,
                levelRenderState,
                store,
                renderer,
                store.read(BIT_PLACEMENT_PREVIEW_STATE)
        );
    }

    private <T> void submitHighlightGeometryForState(
            SubmitNodeCollector submitNodeCollector,
            PoseStack poseStack,
            LevelRenderState levelRenderState,
            ISubmitCustomGeometryEvent.IRenderStateStore store,
            IChiselContextPreviewRenderer<T> renderer,
            @Nullable T renderState
    ) {
        if (renderState == null) {
            return;
        }

        renderer.submit(
                renderState,
                submitNodeCollector,
                poseStack,
                levelRenderState,
                store
        );
    }
}

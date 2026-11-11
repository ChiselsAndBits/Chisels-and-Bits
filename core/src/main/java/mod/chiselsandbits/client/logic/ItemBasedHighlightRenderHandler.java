package mod.chiselsandbits.client.logic;

import com.communi.suggestu.scena.core.client.event.IExtractLevelRenderStateEvent;
import com.communi.suggestu.scena.core.client.event.ISubmitCustomGeometryEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import mod.chiselsandbits.api.client.highlight.IItemBasedHighlightRenderer;
import mod.chiselsandbits.client.highlight.ItemBasedHighlightRendererRegistry;
import mod.chiselsandbits.utils.ItemStackUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

public class ItemBasedHighlightRenderHandler {
	public static void submitCustomHighlightGeometry(SubmitNodeCollector submitNodeCollector,
													 PoseStack poseStack,
													 LevelRenderState levelRenderState,
													 ISubmitCustomGeometryEvent.IRenderStateStore store) {
		withHighlightItem(
				withHighlightItem ->  {
					withHighlightItem.submitHighlightGeometry(
							submitNodeCollector,
							poseStack,
							levelRenderState,
							store
					);
				}
		);
	}

    public static void extractCustomWorldHighlightState(LevelRenderState levelRenderState, ClientLevel clientLevel, Camera camera, DeltaTracker deltaTracker, IExtractLevelRenderStateEvent.IRenderStateStore store) {
        withHighlightItem(
				withHighlightItem -> {
					withHighlightItem.extractState(
							levelRenderState,
							clientLevel,
							camera,
							deltaTracker,
							store
					);
				}
		);
    }

	private static void withHighlightItem(
			Consumer<IItemBasedHighlightRenderer> onDrawCustomHighlight
	) {
		final Player playerEntity = Minecraft.getInstance().player;

		if (playerEntity == null || playerEntity.isSpectator()) {
			return;
		}

		final ItemStack heldStack = ItemBasedHighlightRendererRegistry.getItemWithCustomHighlightsFromHandsFor(playerEntity);

		if (heldStack.isEmpty()) {
			return;
		}

		final Item heldItem = heldStack.getItem();
		final IItemBasedHighlightRenderer renderer = ItemBasedHighlightRendererRegistry.getInstance().get(heldItem);

		if (renderer == null) {
			return;
		}

		if (renderer.shouldDrawDefaultHighlight(playerEntity, heldStack)) {
			return;
		}

		onDrawCustomHighlight.accept(renderer);
	}
}

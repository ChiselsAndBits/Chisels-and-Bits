package mod.chiselsandbits.api.client.highlight;

import com.communi.suggestu.scena.core.client.event.IExtractLevelRenderStateEvent;
import com.communi.suggestu.scena.core.client.event.ISubmitCustomGeometryEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Indicates a renderer that is used to render custom highlights for items.
 */
public interface IItemBasedHighlightRenderer {
    /**
	 * Indicates if the given player is allowed to use the
	 * default render method.
	 *
	 * <p>
	 * If this is false.The default highlight rendering is cancelled.
	 *
	 * @param playerEntity The player entity in question.
     * @param stack The item stack in question.
	 * @return True to let minecraft handle the highlight rendering, false when not.
	 */
	boolean shouldDrawDefaultHighlight(Player playerEntity, ItemStack stack);

	/**
	 * Extracts the current state of the highlight rendering for the current item.
	 *
	 * @param levelRenderState The current levels render state.
	 * @param clientLevel The current client level.
	 * @param camera The current camera.
	 * @param deltaTracker The current delta tracker.
	 * @param store The store to store the extracted state in.
	 */
    void extractState(LevelRenderState levelRenderState, ClientLevel clientLevel, Camera camera, DeltaTracker deltaTracker, IExtractLevelRenderStateEvent.IRenderStateStore store);

	/**
	 * Submits the highlight geometry for the current item to be rendered.
	 *
	 * @param submitNodeCollector The submit node collector.
	 * @param poseStack The pose stack.
	 * @param levelRenderState The level render state.
	 * @param store The render state store.
	 */
    void submitHighlightGeometry(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, LevelRenderState levelRenderState, ISubmitCustomGeometryEvent.IRenderStateStore store);
}

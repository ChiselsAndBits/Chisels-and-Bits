package mod.chiselsandbits.api.client.render.preview.chiseling;

import com.communi.suggestu.scena.core.client.event.ISubmitCustomGeometryEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import mod.chiselsandbits.api.chiseling.IChiselingContext;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer which is used to render the chiseling preview.
 *
 * @param <T> The type of the render state which is extracted and submitted.
 */
public interface IChiselContextPreviewRenderer<T> {
	/**
	 * The id of the renderer.
	 * Used to give the player a selection option for the preview renderer.
	 *
	 * @return The id of the preview renderer.
	 */
	Identifier getId();

	/**
	 * Extracts the state of the renderer to be stored in the render state.
	 *
	 * @param context The chiseling context to extract the state from.
	 * @param levelRenderState The current level render state.
	 * @param clientLevel The client level in which the preview is being rendered.
	 * @param camera The camera which is used to render the preview.
	 * @param deltaTracker The delta tracker which is used to render the preview.
	 *
	 * @return The state of the renderer to be stored in the render state.
	 */
	T extractState(
            IChiselingContext context,
            LevelRenderState levelRenderState,
			ClientLevel clientLevel,
			Camera camera,
			DeltaTracker deltaTracker
	);

	/**
	 * Submits the render state to be rendered.
	 *
	 * @param renderState The render state to be rendered.
	 * @param submitNodeCollector The submit node collector.
	 * @param poseStack The pose stack.
	 * @param levelRenderState The level render state.
	 * @param store The render state store.
	 */
    void submit(T renderState,
				SubmitNodeCollector submitNodeCollector,
				PoseStack poseStack,
				LevelRenderState levelRenderState,
				ISubmitCustomGeometryEvent.IRenderStateStore store);
}

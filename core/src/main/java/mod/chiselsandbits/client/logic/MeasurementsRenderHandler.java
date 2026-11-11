package mod.chiselsandbits.client.logic;

import com.communi.suggestu.scena.core.client.event.IExtractLevelRenderStateEvent;
import com.communi.suggestu.scena.core.client.event.ISubmitCustomGeometryEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import mod.chiselsandbits.client.render.MeasurementRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;

public class MeasurementsRenderHandler {

	public static void extractMeasurementsRenderState(LevelRenderState levelRenderState, ClientLevel clientLevel, Camera camera, DeltaTracker deltaTracker, IExtractLevelRenderStateEvent.IRenderStateStore store) {
		if (Minecraft.getInstance().player != null && !Minecraft.getInstance().player.isSpectator()) {
			MeasurementRenderer.getInstance().extract(levelRenderState, clientLevel, camera, deltaTracker, store);
		}
	}

	public static void submitMeasurementsRenderGeometry(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, LevelRenderState levelRenderState, ISubmitCustomGeometryEvent.IRenderStateStore store) {
		if (Minecraft.getInstance().player != null && !Minecraft.getInstance().player.isSpectator()) {
			MeasurementRenderer.getInstance().submit(submitNodeCollector, poseStack, levelRenderState, store);
		}
	}
}

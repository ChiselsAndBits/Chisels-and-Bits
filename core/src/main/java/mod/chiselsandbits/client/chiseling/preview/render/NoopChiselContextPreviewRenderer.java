package mod.chiselsandbits.client.chiseling.preview.render;

import com.communi.suggestu.scena.core.client.event.ISubmitCustomGeometryEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import mod.chiselsandbits.api.chiseling.IChiselingContext;
import mod.chiselsandbits.api.client.render.preview.chiseling.IChiselContextPreviewRenderer;
import mod.chiselsandbits.api.util.constants.Constants;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;

public class NoopChiselContextPreviewRenderer implements IChiselContextPreviewRenderer<Unit> {
	static Identifier ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "noop");

	@Override
	public Identifier getId() {
		return ID;
	}

	@Override
	public Unit extractState(IChiselingContext context, LevelRenderState levelRenderState, ClientLevel clientLevel, Camera camera, DeltaTracker deltaTracker) {
		return Unit.INSTANCE;
	}

	@Override
	public void submit(Unit renderState, SubmitNodeCollector submitNodeCollector, PoseStack poseStack, LevelRenderState levelRenderState, ISubmitCustomGeometryEvent.IRenderStateStore store) {
		//Nothing gets submitted from here.
	}
}

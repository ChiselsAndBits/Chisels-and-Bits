package mod.chiselsandbits.client.render;

import java.text.DecimalFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import com.communi.suggestu.scena.core.client.event.IExtractLevelRenderStateEvent;
import com.communi.suggestu.scena.core.client.event.ISubmitCustomGeometryEvent;
import com.google.common.collect.Streams;
import com.mojang.blaze3d.vertex.PoseStack;
import mod.chiselsandbits.api.measuring.IMeasurement;
import mod.chiselsandbits.api.measuring.IMeasuringManager;
import mod.chiselsandbits.api.measuring.IMeasuringMode;
import mod.chiselsandbits.api.measuring.IMeasuringType;
import mod.chiselsandbits.api.util.LocalStrings;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.measures.MeasuringType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import org.jspecify.annotations.Nullable;

public final class MeasurementRenderer {
    private static final MeasurementRenderer INSTANCE = new MeasurementRenderer();
	private static final Identifier RENDERSTATE_ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "measurements_render_state");

    private MeasurementRenderer() {
    }

    public static MeasurementRenderer getInstance() {
        return INSTANCE;
    }

    public record MeasurementsRenderState(Iterable<MeasurementRenderState> measurements) {
        public static final Identifier ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "measurements_render_state");
    }

    public record MeasurementRenderState(
            @Nullable DirectRenderState directDistance,
            @Nullable CubicRenderState cubicDistance,
            Component owner
    ) {

        public boolean isDirectDistance() {
            return directDistance != null;
        }

        public boolean isCubicDistance() {
            return cubicDistance != null;
        }
    }

    public record DirectRenderState(
            int color,
            Vec3 start,
            Vec3 end,
            Component length
    ) {

        public Vec3 center() {
            return start.add(end).multiply(0.5, 0.5, 0.5);
        }
    }

    public record CubicRenderState(
            int color,
            CubicBoxRenderState box,
            CubicLengthRenderState length
    ) {
    }

    public record CubicBoxRenderState(
            AABB boundingBox
    ) {

        public Vec3 start() {
            return boundingBox().getMinPosition();
        }

        public Vec3 end() {
            return boundingBox().getMaxPosition();
        }

        public Vec3 center() {
            return boundingBox().getCenter();
        }

        public Stream<Vec3> corners() {
            return Stream.of(
                    new Vec3(boundingBox.minX, boundingBox.minY, boundingBox.minZ),
                    new Vec3(boundingBox.minX, boundingBox.minY, boundingBox.maxZ),
                    new Vec3(boundingBox.minX, boundingBox.maxY, boundingBox.minZ),
                    new Vec3(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ),
                    new Vec3(boundingBox.maxX, boundingBox.minY, boundingBox.minZ),
                    new Vec3(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ),
                    new Vec3(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ),
                    new Vec3(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ)
            );
        }

        public Stream<Vec3> primaryPlaneCenters() {
            return Stream.of(
                    new Vec3(boundingBox().minX, center().y(), boundingBox().minZ),
                    new Vec3(center().x, boundingBox().minY, boundingBox().minZ),
                    new Vec3(boundingBox().minX, boundingBox().minY, center().z())
            );
        }

        public Stream<CubicEdgeRenderState> edges() {
            return Stream.of(
                    new CubicEdgeRenderState(new Vec3(boundingBox.minX, boundingBox.minY, boundingBox.minZ), new Vec3(boundingBox.maxX, boundingBox.minY, boundingBox.minZ)),
                    new CubicEdgeRenderState(new Vec3(boundingBox.minX, boundingBox.minY, boundingBox.maxZ), new Vec3(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ)),
                    new CubicEdgeRenderState(new Vec3(boundingBox.minX, boundingBox.maxY, boundingBox.minZ), new Vec3(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ)),
                    new CubicEdgeRenderState(new Vec3(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ), new Vec3(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ)),

                    new CubicEdgeRenderState(new Vec3(boundingBox.minX, boundingBox.minY, boundingBox.minZ), new Vec3(boundingBox.minX, boundingBox.maxY, boundingBox.minZ)),
                    new CubicEdgeRenderState(new Vec3(boundingBox.maxX, boundingBox.minY, boundingBox.minZ), new Vec3(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ)),
                    new CubicEdgeRenderState(new Vec3(boundingBox.minX, boundingBox.minY, boundingBox.maxZ), new Vec3(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ)),
                    new CubicEdgeRenderState(new Vec3(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ), new Vec3(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ)),

                    new CubicEdgeRenderState(new Vec3(boundingBox.minX, boundingBox.minY, boundingBox.minZ), new Vec3(boundingBox.minX, boundingBox.minY, boundingBox.maxZ)),
                    new CubicEdgeRenderState(new Vec3(boundingBox.maxX, boundingBox.minY, boundingBox.minZ), new Vec3(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ)),
                    new CubicEdgeRenderState(new Vec3(boundingBox.minX, boundingBox.maxY, boundingBox.minZ), new Vec3(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ)),
                    new CubicEdgeRenderState(new Vec3(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ), new Vec3(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ))
            );
        }

    }

    public record CubicEdgeRenderState(
            Vec3 from,
            Vec3 to
    ) {
    }

    public record CubicLengthRenderState(
            Component x,
            Component y,
            Component z
    ) {

        public Stream<Component> primaryPlaneLengths() {
            return Stream.of(y(), x(), z());
        }
    }

    public void extract(LevelRenderState levelRenderState, ClientLevel clientLevel, Camera camera, DeltaTracker deltaTracker, IExtractLevelRenderStateEvent.IRenderStateStore store) {
		final IMeasuringManager measuringManager = IMeasuringManager.getInstance();
		final List<MeasurementRenderState> renderStates = measuringManager.getInWorld(clientLevel)
				.stream()
				.map(this::extractMeasurementRenderState)
				.toList();

		store.write(RENDERSTATE_ID, new MeasurementsRenderState(renderStates));
    }

    private MeasurementRenderState extractMeasurementRenderState(IMeasurement measurement) {
		if (measurement.getMode().getType() == MeasuringType.DISTANCE) {
			return new MeasurementRenderState(
					extractDirectRenderState(measurement),
					null,
					Component.literal(measurement.getOwner().toString())
			);
		} else {
			return new MeasurementRenderState(
					null,
					extractCubicRenderState(measurement),
					formatOwner(measurement.getOwner())
			);
		}
    }

    private DirectRenderState extractDirectRenderState(IMeasurement measurement) {
        return new DirectRenderState(
                ARGB.colorFromFloat(
                        (float) measurement.getMode().getAlphaChannel(),
                        (float) measurement.getMode().getColorVector().x(),
                        (float) measurement.getMode().getColorVector().y(),
                        (float) measurement.getMode().getColorVector().z()
                ),
                measurement.getFrom(),
                measurement.getTo(),
				formatLength(measurement.getMode(), measurement.getFrom().distanceTo(measurement.getTo()))
			);
    }

	private CubicRenderState extractCubicRenderState(IMeasurement measurement) {
		return new CubicRenderState(
				ARGB.colorFromFloat(
						(float) measurement.getMode().getAlphaChannel(),
						(float) measurement.getMode().getColorVector().x(),
						(float) measurement.getMode().getColorVector().y(),
						(float) measurement.getMode().getColorVector().z()
				),
				new CubicBoxRenderState(new AABB(measurement.getFrom(), measurement.getTo())),
				new CubicLengthRenderState(
						formatLength(measurement.getMode(), Math.abs(measurement.getTo().x() - measurement.getFrom().x())),
						formatLength(measurement.getMode(), Math.abs(measurement.getTo().y() - measurement.getFrom().y())),
						formatLength(measurement.getMode(), Math.abs(measurement.getTo().z() - measurement.getFrom().z()))
				)
		);
	}

	private Component formatLength(
			final IMeasuringMode mode,
			final double length
	) {
		final IMeasuringType type = mode.getType();

		if (type == MeasuringType.DISTANCE) {
			return Component.literal(new DecimalFormat("#.0#").format(length));
		} else if (type == MeasuringType.BLOCK) {
			return LocalStrings.MeasurementLengthBlock.getText(new DecimalFormat("#").format(Math.floor(length + 1)));

		}

		return LocalStrings.MeasurementLengthBit.getText(new DecimalFormat("#").format(Math.floor((length) * 16)));
	}

	private Component formatOwner(final UUID id) {
		if (id == (Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getUUID() : UUID.randomUUID())) {
			return LocalStrings.MeasurementOwnedByYou.getText();
		}

		final PlayerInfo playerInfo = Minecraft.getInstance().getConnection() != null ? Minecraft.getInstance().getConnection().getPlayerInfo(id) : null;

		if (playerInfo == null) {
			return LocalStrings.MeasurementOwnedByUnknown.getText();
		}

		return LocalStrings.MeasurementOwnedByPlayer.getText(
				playerInfo.getTabListDisplayName() != null
				? this.formatPlayerDisplayName(playerInfo, playerInfo.getTabListDisplayName().copy())
				: this.formatPlayerDisplayName(playerInfo, PlayerTeam.formatNameForTeam(playerInfo.getTeam(), Component.literal(playerInfo.getProfile().name())))
		);
	}

	private Component formatPlayerDisplayName(PlayerInfo playerInfo, MutableComponent component) {
		return playerInfo.getGameMode() == GameType.SPECTATOR ? component.withStyle(ChatFormatting.ITALIC) : component;
	}

    public void submit(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, LevelRenderState levelRenderState, ISubmitCustomGeometryEvent.IRenderStateStore store) {
        final MeasurementsRenderState renderState = store.read(MeasurementsRenderState.ID);
        if (renderState == null) {
            return;
        }

        renderState.measurements().forEach(state -> submit(submitNodeCollector, poseStack, levelRenderState, state));
    }

    public void submit(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, LevelRenderState levelRenderState, MeasurementRenderState renderState) {
        if (renderState.isDirectDistance()) {
            submitDirectDistance(submitNodeCollector, poseStack, levelRenderState, renderState.directDistance(), renderState.owner());
        } else if (renderState.isCubicDistance()) {
            submitCubicDistance(submitNodeCollector, poseStack, levelRenderState, renderState.cubicDistance(), renderState.owner());
        }
    }

    public void submitDirectDistance(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, LevelRenderState levelRenderState, DirectRenderState renderState, Component owner) {
        poseStack.pushPose();
        submitNodeCollector.submitGizmoPrimitives(
                createDirectDistanceGizmoGroup(renderState, owner),
                levelRenderState.cameraRenderState,
                true
        );
        poseStack.popPose();
    }

    public DrawableGizmoPrimitives.Group createDirectDistanceGizmoGroup(DirectRenderState renderState, Component owner) {
        return new DrawableGizmoPrimitives.Group(
                false,
                List.of(new DrawableGizmoPrimitives.Line(
                        renderState.start(),
                        renderState.end(),
                        renderState.color(),
                        2.5f
                )),
                List.of(),
                List.of(),
                List.of(
                        new DrawableGizmoPrimitives.Text(
                                renderState.center(),
                                renderState.length().getString(),
                                TextGizmo.Style.forColorAndCentered(renderState.color())
                        ),
                        new DrawableGizmoPrimitives.Text(
                                renderState.center().add(0, 0.25, 0),
                                owner.getString(),
                                TextGizmo.Style.forColorAndCentered(renderState.color())
                        )
                ),
                List.of(
                        new DrawableGizmoPrimitives.Point(
                                renderState.start(),
                                renderState.color(),
                                5f
                        ),
                        new DrawableGizmoPrimitives.Point(
                                renderState.end(),
                                renderState.color(),
                                5f
                        )
                )
        );
    }

    public void submitCubicDistance(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, LevelRenderState levelRenderState, CubicRenderState renderState, Component owner) {
        poseStack.pushPose();
        submitNodeCollector.submitGizmoPrimitives(
                createCubicDistanceGizmoGroup(renderState, owner),
                levelRenderState.cameraRenderState,
                true
        );
        poseStack.popPose();
    }

    @SuppressWarnings("UnstableApiUsage")
    public DrawableGizmoPrimitives.Group createCubicDistanceGizmoGroup(CubicRenderState renderState, Component owner) {
        return new DrawableGizmoPrimitives.Group(
                false,
                renderState.box().edges().map(edge -> new DrawableGizmoPrimitives.Line(edge.from(), edge.to(), renderState.color(), 2.5f)).toList(),
                List.of(),
                List.of(),
                Streams.concat(
                        Streams.zip(
                                renderState.box().primaryPlaneCenters(),
                                renderState.length().primaryPlaneLengths(),
                                (center, length) -> new DrawableGizmoPrimitives.Text(
                                        center,
                                        length.getString(),
                                        TextGizmo.Style.forColorAndCentered(renderState.color())
                                )
                        ),
						Stream.of(new DrawableGizmoPrimitives.Text(
								renderState.box().center(),
								owner.getString(),
								TextGizmo.Style.forColorAndCentered(renderState.color())
						))
				).toList(),
                renderState.box().corners().map(corner -> new DrawableGizmoPrimitives.Point(corner, renderState.color(), 5f)).toList()
        );
    }
}

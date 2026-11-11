package mod.chiselsandbits.client.render;

import java.util.Arrays;
import java.util.List;

import com.communi.suggestu.scena.core.client.event.IExtractLevelRenderStateEvent;
import com.communi.suggestu.scena.core.client.event.ISubmitCustomGeometryEvent;
import com.communi.suggestu.scena.core.util.SingleBlockBlockAndTintGetter;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mod.chiselsandbits.api.client.render.preview.placement.PlacementPreviewRenderMode;
import mod.chiselsandbits.api.item.wireframe.IWireframeProvidingItem;
import mod.chiselsandbits.api.placement.PlacementResult;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.client.model.block.ChiseledBlockStateModelManager;
import mod.chiselsandbits.client.model.information.ChiseledBlockModelInformation;
import mod.chiselsandbits.client.model.parts.ChiseledBlockModelPart;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class ChiseledBlockPreviewRenderer {
	private static final ChiseledBlockPreviewRenderer INSTANCE = new ChiseledBlockPreviewRenderer();
	private static final Identifier RENDERSTATE_ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "chiseled_block_ghost_renderer");
	private static final float GHOST_BLOCK_SCALE = 1.001F;

	public static ChiseledBlockPreviewRenderer getInstance() {
		return INSTANCE;
	}

	private ChiseledBlockPreviewRenderer() {
	}

	public enum RenderMode {
		COLORED_GHOST,
		MODELLED_GHOST,
		WIREFRAME
	}

	public record RenderState(
			Vec3 renderOffset,
			Vec3 targetedPosition,
			BlockPos placementPosition,
			Vector4f color,
			PlacementPreviewRenderMode mode,
			RenderMode renderMode,
			boolean ignoreDepth,
			VoxelShape wireframeShape,
			ChiseledBlockModelInformation model
	) {
	}

	public void extract(Camera camera,
						IExtractLevelRenderStateEvent.IRenderStateStore store,
						Vec3 targetedRenderPos,
						ItemStack renderStack,
						PlacementResult placementResult,
						PlacementPreviewRenderMode success,
						PlacementPreviewRenderMode failure,
						boolean ignoreDepth,
						boolean asWireframe) {
		final PlacementPreviewRenderMode previewRenderMode = placementResult.isSuccess() ? success : failure;
		final RenderMode renderMode = asWireframe ?
				RenderMode.WIREFRAME :
				previewRenderMode.isColoredGhost() ? RenderMode.COLORED_GHOST : RenderMode.MODELLED_GHOST;

		final VoxelShape wireframeShape = renderStack.getItem() instanceof IWireframeProvidingItem wireframeItem
				? wireframeItem.getWireFrame(renderStack, Minecraft.getInstance().player, null)
				: Shapes.empty();

		final RenderState renderState = new RenderState(
				new Vec3(targetedRenderPos.x() - camera.position().x() - 0.000125,
						targetedRenderPos.y() - camera.position().y() + 0.000125,
						targetedRenderPos.z() - camera.position().z() - 0.000125),
				targetedRenderPos,
				new BlockPos(
						(int) targetedRenderPos.x(),
						(int) targetedRenderPos.y(),
						(int) targetedRenderPos.z()
				),
				placementResult.getColor(),
				previewRenderMode,
				renderMode,
				ignoreDepth,
				wireframeShape,
				ChiseledBlockStateModelManager.getInstance().get(renderStack)
		);

		store.write(RENDERSTATE_ID, renderState);
	}

	public void submit(SubmitNodeCollector submitNodeCollector,
                       PoseStack poseStack,
					   ISubmitCustomGeometryEvent.IRenderStateStore store) {
		final RenderState renderState = store.read(RENDERSTATE_ID);
		if (renderState == null) {
			return;
		}

		poseStack.pushPose();
		poseStack.translate(renderState.renderOffset());
		poseStack.scale(GHOST_BLOCK_SCALE, GHOST_BLOCK_SCALE, GHOST_BLOCK_SCALE);

		if (renderState.renderMode() == RenderMode.COLORED_GHOST) {
			submitColoredGhost(
					submitNodeCollector, poseStack, renderState
			);
		} else if (renderState.renderMode() == RenderMode.MODELLED_GHOST) {
			submitModelledGhost(
					submitNodeCollector, poseStack, renderState
			);
		} else if (renderState.renderMode() == RenderMode.WIREFRAME) {
			submitWireframeGhost(
					submitNodeCollector, poseStack, renderState
			);
		}

		poseStack.popPose();
	}

	public void submitColoredGhost(SubmitNodeCollector submitNodeCollector,
									PoseStack poseStack,
									RenderState renderState) {
		submitNodeCollector.submitCustomGeometry(
				poseStack,
				renderState.ignoreDepth()
						? ModRenderTypes.GHOST_BLOCK_COLORED_PREVIEW_ALWAYS.get()
						: ModRenderTypes.GHOST_BLOCK_COLORED_PREVIEW.get(),
                (pose, buffer) -> {
                    final RandomSource random = RandomSource.create(42);

                    // Setup normals and shaded colors for each direction
                    final Vector3f[] normals = getNormals(poseStack.last());
                    final Vector3f[] shadedColors = getShadedColors(renderState.color());

                    // Initialize reusable renderOffset vector to avoid needless creation of new ones
                    final Vector4f pos = new Vector4f();

                    for (final ChiseledBlockModelPart part : renderState.model().parts()) {
                        for (final Direction direction : Direction.values()) {
                            // Render outer directional quads
                            random.setSeed(42L);
                            renderQuadList(
									pose.pose(),
                                    buffer,
                                    part.getQuads(direction),
                                    normals,
                                    shadedColors,
                                    pos);
                        }

                        // Render quads of unspecified direction
                        random.setSeed(42L);
                        renderQuadList(
								pose.pose(),
                                buffer,
                                part.getQuads(null),
                                normals,
                                shadedColors,
                                pos);
                    }
                }
        );
	}

	public void submitModelledGhost(SubmitNodeCollector submitNodeCollector,
									PoseStack poseStack,
									RenderState renderState) {
		submitNodeCollector.submitCustomGeometry(
				poseStack,
				renderState.ignoreDepth()
						? ModRenderTypes.GHOST_BLOCK_PREVIEW_GREATER.get()
						: ModRenderTypes.GHOST_BLOCK_PREVIEW.get(),
				(pose, buffer) -> {
					BlockState primaryBlockState = renderState.model().key().primaryState().blockState();
					final BlockAndTintGetter blockAndTintGetter = new SingleBlockBlockAndTintGetter.Builder()
							.withBlockState(primaryBlockState)
							.withBlockEntity(() -> renderState.model().key().primaryState().newBlockEntity(renderState.placementPosition()))
							.withPos(renderState.placementPosition())
							.withSource(Minecraft.getInstance().level)
							.createSingleBlockBlockAndTintGetter();

					final BlockQuadOutput output = (x, y, z, quad, instance) -> putBakedQuad(pose, buffer, x, y, z, quad, instance, renderState.color());
					ModelBlockRenderer blockRenderer = new ModelBlockRenderer(
							Minecraft.getInstance().options.ambientOcclusion().get(),
							false,
							Minecraft.getInstance().getBlockColors());

					blockRenderer.tesselateBlock(
							output,
							0, 0, 0,
							blockAndTintGetter,
							renderState.placementPosition(),
							primaryBlockState,
							renderState.model(),
							OverlayTexture.NO_OVERLAY);
				}
		);
	}

	public void submitWireframeGhost(SubmitNodeCollector submitNodeCollector,
									 PoseStack poseStack,
									 RenderState renderState) {
		submitNodeCollector.submitShapeOutline(
				poseStack,
				renderState.wireframeShape(),
				renderState.ignoreDepth()
						? ModRenderTypes.WIREFRAME_LINES_ALWAYS.get()
						: ModRenderTypes.WIREFRAME_LINES.get(),
				ARGB.colorFromFloat(1, renderState.color().x(), renderState.color().y(), renderState.color().z()),
				2.5f,
				true
		);
	}

	private static void putBakedQuad(
			PoseStack.Pose pose,
			VertexConsumer innerBuffer,
			float x,
			float y,
			float z,
			BakedQuad quad,
			QuadInstance instance,
			final Vector4f color
	) {
		final PoseStack.Pose translatedPose = new PoseStack.Pose();
		translatedPose.set(pose);
		translatedPose.translate(x, y, z);

		VertexConsumer buffer = new AlphaSettingVertexConsumer(color.w(), innerBuffer);
		buffer.putBakedQuad(translatedPose, quad, instance);
	}

	private record AlphaSettingVertexConsumer(
			int alpha,
			VertexConsumer delegate) implements VertexConsumer {
		private AlphaSettingVertexConsumer(final float alpha, final VertexConsumer delegate) {
			this((int) (alpha * 255F), delegate);
		}

		@Override
		public @NonNull VertexConsumer addVertex(final float x, final float y, final float z) {
			return delegate.addVertex(x, y, z);
		}

		@Override
		public @NonNull VertexConsumer setColor(final int red, final int green, final int blue, final int alpha) {
			return delegate.setColor(red, green, blue, this.alpha);
		}

		@Override
		public @NonNull VertexConsumer setColor(final int color) {
			//Color is in RGBA format
			//Extract the channels
			final int red = (color >> 24) & 0xFF;
			final int green = (color >> 16) & 0xFF;
			final int blue = (color >> 8) & 0xFF;
			final int alpha = color & 0xFF;

			return delegate.setColor(
					red,
					green,
					blue,
					alpha
			);
		}

		@Override
		public @NonNull VertexConsumer setUv(final float u, final float v) {
			return delegate.setUv(u, v);
		}

		@Override
		public @NonNull VertexConsumer setUv1(final int u, final int v) {
			return delegate.setUv1(u, v);
		}

		@Override
		public @NonNull VertexConsumer setUv2(final int u, final int v) {
			return delegate.setUv2(u, v);
		}

		@Override
		public @NonNull VertexConsumer setNormal(final float normalX, final float normalY, final float normalZ) {
			return delegate.setNormal(normalX, normalY, normalZ);
		}

		@Override
		public @NonNull VertexConsumer setLineWidth(final float lineWidth) {
			return delegate.setLineWidth(lineWidth);
		}
	}

	private static final float[] DIRECTIONAL_BRIGHTNESS = {0.5f, 1f, 0.7f, 0.7f, 0.6f, 0.6f};

	private static Vector3f[] getShadedColors(final Vector4f color) {
		// Directionally shade the color by the amount MC normally does
		return Arrays.stream(Direction.values())
				.map(direction -> {
					final float brightness = DIRECTIONAL_BRIGHTNESS[direction.get3DDataValue()];
					return new Vector3f(
							color.x() * brightness,
							color.y() * brightness,
							color.z() * brightness);
				}).toArray(Vector3f[]::new);
	}

	private static Vector3f[] getNormals(final PoseStack.Pose pose) {
		// Transform the normal vector of each direction by the pose's normal matrix
		return Arrays.stream(Direction.values())
				.map(direction -> {
					final Vec3i faceNormal = direction.getUnitVec3i();
					final Vector3f normal = new Vector3f(faceNormal.getX(), faceNormal.getY(), faceNormal.getZ());
					normal.mul(pose.normal());
					return normal;
				}).toArray(Vector3f[]::new);
	}

	/**
	 * Optimized version of ItemRenderer#renderQuadList.
	 */
	private static void renderQuadList(
			final Matrix4f pose,
			final VertexConsumer buffer,
			final List<BakedQuad> quads,
			final Vector3f[] normals,
			final Vector3f[] shadedColors,
			final Vector4f pos) {
		for (final BakedQuad quad : quads) {
			putBulkData(
					buffer,
					pose,
					quad,
					shadedColors[quad.direction().ordinal()],
					normals[quad.direction().ordinal()],
					pos);
		}
	}

	/**
	 * Optimized and stripped down version of IForgeVertexConsumer#putBulkData.
	 */
	private static void putBulkData(
			final VertexConsumer buffer,
			final Matrix4f pose,
			final BakedQuad bakedQuad,
			final Vector3f color,
			final Vector3f normal,
			final Vector4f pos) {
		// Get vertex data
		final Vector2f uv = new Vector2f();

		for (int v = 0; v < 4; ++v) {
			final Vector3fc vertexPos = bakedQuad.position(v);
			pos.set(vertexPos.x(), vertexPos.y(), vertexPos.z(), 1f);
			pos.mul(pose);

			// UV is the next 2 Floats (4 bytes each)
			final long packedUv = bakedQuad.packedUV(v);
			uv.set(
					UVPair.unpackU(packedUv),
					UVPair.unpackV(packedUv)
			);

			buffer.addVertex(pos.x(), pos.y(), pos.z())
					.setColor(color.x(), color.y(), color.z(), 1f)
					.setUv(uv.x(), uv.y())
					.setUv1(Short.MAX_VALUE, Short.MAX_VALUE)
					.setUv2(LightCoordsUtil.block(LightCoordsUtil.FULL_BRIGHT), LightCoordsUtil.sky(LightCoordsUtil.FULL_SKY))
					.setNormal(normal.x(), normal.y(), normal.z());
		}
	}
}

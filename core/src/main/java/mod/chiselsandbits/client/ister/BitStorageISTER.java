package mod.chiselsandbits.client.ister;

import java.util.Objects;
import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.chiselsandbits.block.BitStorageBlock;
import mod.chiselsandbits.block.entities.BitStorageBlockEntity;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.item.ItemStack;

public class BitStorageISTER implements SpecialModelRenderer<BitStorageISTER.RenderState> {
	public BitStorageISTER() {
	}

	public record RenderState(BitStorageBlockEntity entity) { }

	@Override
	public void submit(
			final BitStorageISTER.RenderState argument,
			final PoseStack poseStack,
			final @NonNull SubmitNodeCollector submitNodeCollector,
			final int lightCoords,
			final int overlayCoords,
			final boolean hasFoil,
			final int outlineColor) {
		poseStack.pushPose();

		final BlockEntityRenderState state = Minecraft.getInstance().getBlockEntityRenderDispatcher().tryExtractRenderState(
				Objects.requireNonNull(argument).entity(),
				Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false), //TODO: Figure out true or false......
				null
		);

		Minecraft.getInstance().getBlockEntityRenderDispatcher().submit(
						Objects.requireNonNull(state),
						poseStack,
						submitNodeCollector,
						new CameraRenderState()
		);

		poseStack.popPose();
	}

	@Override
	public void getExtents(final Consumer<Vector3fc> output) {
		//Noop., for now.Figure out later
	}

	@Override
	public @Nullable RenderState extractArgument(final ItemStack stack) {
		return new RenderState(BitStorageBlock.createEntityFromStack(stack));
	}
}

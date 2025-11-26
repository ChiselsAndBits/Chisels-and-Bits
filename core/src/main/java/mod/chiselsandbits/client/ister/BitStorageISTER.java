package mod.chiselsandbits.client.ister;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.block.BitStorageBlock;
import mod.chiselsandbits.block.entities.BitStorageBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Set;
import java.util.function.Consumer;

public class BitStorageISTER implements SpecialModelRenderer<BitStorageISTER.@NotNull RenderState>
{
    public BitStorageISTER()
    {
    }

    public record RenderState(BitStorageBlockEntity entity) {}

    @Override
    public void submit(
        @Nullable final BitStorageISTER.RenderState argument,
        final @NotNull ItemDisplayContext displayContext,
        final @NotNull PoseStack poseStack,
        final @NotNull SubmitNodeCollector nodeCollector,
        final int packedLight,
        final int packedOverlay,
        final boolean hasFoil,
        final int outlineColor)
    {
        poseStack.pushPose();

        assert argument != null;
        final BlockEntityRenderState state = Minecraft.getInstance().getBlockEntityRenderDispatcher().tryExtractRenderState(
            argument.entity(),
            Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false), //TODO: Figure out true or false......
            null
        );

        assert state != null;
        Minecraft.getInstance().getBlockEntityRenderDispatcher().submit(
            state,
            poseStack,
            nodeCollector,
            new CameraRenderState()
        );

        poseStack.popPose();
    }

    @Override
    public void getExtents(final @NotNull Consumer<Vector3fc> output)
    {
        //Noop., for now. Figure out later
    }

    @Override
    public @Nullable RenderState extractArgument(final @NotNull ItemStack stack)
    {
        return new RenderState(BitStorageBlock.createEntityFromStack(stack));
    }
}

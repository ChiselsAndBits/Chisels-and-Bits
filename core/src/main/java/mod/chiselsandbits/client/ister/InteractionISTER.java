package mod.chiselsandbits.client.ister;

import com.communi.suggestu.scena.core.util.TransformationUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import mod.chiselsandbits.api.item.interactable.IInteractableItem;
import mod.chiselsandbits.client.model.item.InteractableItemModel;
import mod.chiselsandbits.client.time.TickHandler;
import mod.chiselsandbits.client.util.ItemModelUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Objects;
import java.util.Set;

/**
 * This class animates an interaction between the items in the two hands of the players.
 *
 * Cloned from: Creators-of-Create: <a href="https://github.com/Creators-of-Create/Create/blob/mc1.16/dev/src/main/java/com/simibubi/create/content/curiosities/tools/SandPaperItemRenderer.java">...</a>
 * Modified some behaviour and fields to target the general use better, but functionally the same.
 */
public record InteractionISTER(InteractableItemModel mainModel) implements SpecialModelRenderer<InteractionISTER.RenderState>
{

    public record RenderState(ItemStack stack, IInteractableItem item) {}

    @Override
    public void submit(
        @Nullable final InteractionISTER.RenderState argument,
        final @NotNull ItemDisplayContext displayContext,
        final @NotNull PoseStack poseStack,
        final @NotNull SubmitNodeCollector nodeCollector,
        final int packedLight,
        final int packedOverlay,
        final boolean hasFoil,
        final int outlineColor)
    {
        if (argument == null)
            return;

        final ItemStack stack = argument.stack();
        final LocalPlayer player = Minecraft.getInstance().player;
        final IInteractableItem item = argument.item();
        ItemModel innerModel = mainModel().model();

        float partialTicks = Minecraft.getInstance().gameRenderer.getMainCamera().getPartialTickTime();

        boolean leftHand = displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        boolean firstPerson = leftHand || displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;

        poseStack.pushPose();
        poseStack.translate(.5f, .5f, .5f);

        boolean simulation = item.isRunningASimulatedInteraction(argument.stack());

        int modifier = leftHand ? -1 : 1;
        if (item.isInteracting(argument.stack())) {
            poseStack.pushPose();

            if (displayContext == ItemDisplayContext.GUI) {
                poseStack.translate(0.0F, .2f, 1.0F);
                poseStack.scale(.75f, .75f, .75f);
            } else {
                poseStack.mulPose(TransformationUtils.quatFromXYZ(new Vector3f(0, modifier * 40, 0), true));
            }

            // Reverse bobbing
            float time = 0;
            if (player != null)
            {
                time = (float) (!simulation ? player.getUseItemRemainingTicks()
                    : (-TickHandler.getNonePausedTicks()) % stack.getUseDuration(Objects.requireNonNull(Minecraft.getInstance().player))) - partialTicks + 1.0F;
            }
            if (time / (float) stack.getUseDuration(Objects.requireNonNull(Minecraft.getInstance().player)) < 0.8F) {
                float bobbing = -Mth.abs(Mth.cos(time / item.getBobbingTickCount() * (float) Math.PI) * 0.1F);

                if (displayContext == ItemDisplayContext.GUI)
                    poseStack.translate(bobbing, bobbing, 0.0F);
                else
                    poseStack.translate(0.0f, bobbing, 0.0F);
            }

            ItemModelUtils.render(
                stack,
                innerModel,
                nodeCollector,
                poseStack,
                packedLight,
                packedOverlay,
                outlineColor,
                displayContext,
                null,
                null,
                0
            );

            poseStack.popPose();
        }

        if (firstPerson && player != null) {
            int itemInUseCount = player.getUseItemRemainingTicks();
            if (itemInUseCount > 0) {
                poseStack.translate(modifier * .5f, 0, -.25f);
                poseStack.mulPose(TransformationUtils.quatFromXYZ(new Vector3f(0, modifier * 40, 0), true));
                poseStack.mulPose(TransformationUtils.quatFromXYZ(new Vector3f(0, modifier * 10, 0), true));
                poseStack.mulPose(TransformationUtils.quatFromXYZ(new Vector3f(0, modifier * 90, 0), true));
            }
        }

        if (!item.isInteracting(stack))
        {
            ItemModelUtils.render(
                stack,
                innerModel,
                nodeCollector,
                poseStack,
                packedLight,
                packedOverlay,
                outlineColor,
                displayContext,
                null,
                null,
                0
            );
        }
        else
        {
            poseStack.pushPose();
            poseStack.mulPose(TransformationUtils.quatFromXYZ(new Vector3f(0, modifier * -90, 0), true));

            final ItemStack target = item.getInteractionTarget(stack);
            ItemModelUtils.render(
                target,
                nodeCollector,
                poseStack,
                packedLight,
                packedOverlay,
                outlineColor,
                displayContext,
                null,
                null,
                0
            );

            poseStack.popPose();
        }

        poseStack.popPose();
    }

    @Override
    public void getExtents(final @NotNull Set<Vector3f> output)
    {
        //No clue how to do this, we do not have access to the outer layer!
    }

    @Override
    public @Nullable RenderState extractArgument(final @NotNull ItemStack stack)
    {
        if (stack.getItem() instanceof IInteractableItem item) {
            return new RenderState(stack, item);
        }

        return null;
    }
}

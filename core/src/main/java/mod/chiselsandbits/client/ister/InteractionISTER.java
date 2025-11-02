package mod.chiselsandbits.client.ister;

import com.communi.suggestu.scena.core.util.TransformationUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import mod.chiselsandbits.api.item.interactable.IInteractableItem;
import mod.chiselsandbits.client.model.item.InteractableItemModel;
import mod.chiselsandbits.client.time.TickHandler;
import mod.scena.client.models.item.DelegateAwareItemModel;
import mod.scena.client.utils.ItemModelUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.component.DataComponents;
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
public class InteractionISTER implements SpecialModelRenderer<InteractionISTER.RenderState>
{
    public InteractionISTER()
    {
    }

    public record RenderState(ItemStack stack) {}

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
        if (!(stack.getItem() instanceof final IInteractableItem item)) {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        ItemModel mainModel = Minecraft.getInstance().getModelManager().getItemModel(
            Objects.requireNonNull(stack.get(DataComponents.ITEM_MODEL))
        );

        if (mainModel instanceof DelegateAwareItemModel delegatingBakedModel) {
            mainModel = delegatingBakedModel.delegate();
        }

        if (!(mainModel instanceof InteractableItemModel interactableItemModel))
        {
            return;
        }
        ItemModel innerModel = interactableItemModel.model();

        float partialTicks = Minecraft.getInstance().gameRenderer.getMainCamera().getPartialTickTime();

        boolean leftHand = displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        boolean firstPerson = leftHand || displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;

        poseStack.pushPose();
        poseStack.translate(.5f, .5f, .5f);

        boolean jeiMode = item.isRunningASimulatedInteraction(stack);

        if (item.isInteracting(stack)) {
            poseStack.pushPose();

            if (displayContext == ItemDisplayContext.GUI) {
                poseStack.translate(0.0F, .2f, 1.0F);
                poseStack.scale(.75f, .75f, .75f);
            } else {
                int modifier = leftHand ? -1 : 1;
                poseStack.mulPose(TransformationUtils.quatFromXYZ(new Vector3f(0, modifier * 40, 0), true));
            }

            // Reverse bobbing
            float time = 0;
            if (player != null)
            {
                time = (float) (!jeiMode ? player.getUseItemRemainingTicks()
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
                int modifier = leftHand ? -1 : 1;
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
        return new RenderState(stack);
    }
}

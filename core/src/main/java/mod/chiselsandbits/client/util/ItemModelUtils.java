package mod.chiselsandbits.client.util;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class ItemModelUtils
{

    public static Supplier<Vector3f[]> redirectExtendsTo(
        ItemStack stack,
        ItemModel model,
        ItemDisplayContext context,
        @Nullable ClientLevel level,
        @Nullable ItemOwner owner,
        int seed) {

        final ItemStackRenderState renderState = update(stack, model, context, level, owner, seed);

        return () -> {
            final List<Vector3f> vectors = new ArrayList<>();
            renderState.visitExtents(v -> vectors.add(new Vector3f(v)));
            return vectors.toArray(Vector3f[]::new);
        };
    }

    public static ItemStackRenderState update(
        ItemStack stack,
        ItemModel model,
        ItemDisplayContext context,
        @Nullable ClientLevel level,
        @Nullable ItemOwner owner,
        int seed
    ) {
        final ItemStackRenderState renderState = new ItemStackRenderState();
        model.update(
            renderState,
            stack,
            Minecraft.getInstance().getItemModelResolver(),
            context,
            level,
            owner,
            seed
        );

        return renderState;
    }

    public static void render(
        ItemStack stack,
        ItemModel model,
        SubmitNodeCollector nodeCollector,
        PoseStack poseStack,
        int combinedLight,
        int combinedOverlay,
        int outlineColor,
        ItemDisplayContext context,
        @Nullable ClientLevel level,
        @Nullable ItemOwner owner,
        int seed
    ) {
        final ItemStackRenderState renderState = update(
            stack, model, context, level, owner, seed
        );

        renderState.submit(
            poseStack,
            nodeCollector,
            combinedLight,
            combinedOverlay,
            outlineColor
        );
    }

    public static void render(
        ItemStack stack,
        SubmitNodeCollector nodeCollector,
        PoseStack poseStack,
        int combinedLight,
        int combinedOverlay,
        int outlineColor,
        ItemDisplayContext context,
        @Nullable ClientLevel level,
        @Nullable ItemOwner owner,
        int seed
    ) {
        final ItemStackRenderState renderState = new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(
            renderState,
            stack,
            context,
            level,
            owner,
            seed
        );

        renderState.submit(
            poseStack,
            nodeCollector,
            combinedLight,
            combinedOverlay,
            outlineColor
        );
    }

    public static Map<RenderType, QuadCollection> quads(
        ItemStack stack,
        ItemModel model,
        ItemDisplayContext context,
        @Nullable ClientLevel level,
        @Nullable ItemOwner owner,
        int seed
    )
    {
        final ItemStackRenderState renderState = update(stack, model, context, level, owner, seed);
        final Map<RenderType, QuadCollection.Builder> builderMap = Maps.newHashMap();

        for (final ItemStackRenderState.LayerRenderState layer : renderState.layers)
        {
            layer.prepareQuadList()
                .forEach(builderMap.computeIfAbsent(
                    layer.renderType,
                    type -> new QuadCollection.Builder()
                )::addUnculledFace);
        }

        final Map<RenderType, QuadCollection> collectionMap = new HashMap<>();
        builderMap.forEach((type, builder) -> collectionMap.put(type, builder.build()));
        return collectionMap;
    }

    public static Map<RenderType, QuadCollection> quads(
        ItemStack stack,
        ItemDisplayContext context,
        @Nullable ClientLevel level,
        @Nullable ItemOwner owner,
        int seed
    ) {
        final ItemModel model = Minecraft.getInstance().getModelManager().getItemModel(
            Objects.requireNonNull(stack.get(DataComponents.ITEM_MODEL))
        );

        return quads(
            stack, model, context, level, owner, seed
        );
    }

    public static QuadCollection adapt(
        final QuadCollection input,
        final Function<BakedQuad, BakedQuad> adapter
    ) {
        final QuadCollection.Builder builder = new QuadCollection.Builder();
        for (final Direction direction : Direction.values())
        {
            input.getQuads(direction)
                .forEach(inputQuad -> builder.addCulledFace(direction, adapter.apply(inputQuad)));
        }
        input.getQuads(null)
            .forEach(inputQuad -> builder.addUnculledFace(adapter.apply(inputQuad)));

        return builder.build();
    }
}

package mod.chiselsandbits.client.util;

import com.communi.suggestu.scena.core.client.models.processing.BakedQuadBuilder;
import com.communi.suggestu.scena.core.client.models.processing.DeconstructedModelPartComponent;
import com.communi.suggestu.scena.core.client.models.processing.VertexData;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.client.model.face.FaceManager;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class QuadGenerationUtils
{

    private QuadGenerationUtils()
    {
        throw new IllegalStateException("Tried to instantiate: 'QuadGenerationUtils', but this is a utility class.");
    }

    public static void generateQuads(
        final BlockInformation blockInformation,
        @Nullable final Direction facingDirection,
        @Nullable final BlockAndTintGetter blockAndTintGetter,
        @Nullable final BlockPos pos,
        final Vector3f from,
        final Vector3f to,
        final Consumer<GeneratedQuad> target)
    {
        generateQuads(
            blockInformation,
            facingDirection,
            blockAndTintGetter,
            pos,
            from,
            to,
            ($, $$) -> {},
            target
        );
    }

    public static void generateQuads(
        final BlockInformation blockInformation,
        @Nullable final Direction facingDirection,
        @Nullable final BlockAndTintGetter blockAndTintGetter,
        @Nullable final BlockPos pos,
        final Vector3f from,
        final Vector3f to,
        final BiConsumer<DeconstructedModelPartComponent, BakedQuadBuilder> quadAdapter,
        final Consumer<GeneratedQuad> target)
    {

        FaceManager.getInstance().extractQuads(
            blockInformation,
            facingDirection,
            blockAndTintGetter,
            pos,
            (layer) -> {
                final Collection<VertexData> adaptedVertices;
                try
                {
                    adaptedVertices = VertexDataUtils.adaptVertices(layer.vertexData(), layer.cullDirection(), from, to);
                }
                catch (IllegalStateException e)
                {
                    return;
                }

                final BakedQuadBuilder adapter = new BakedQuadBuilder(layer.material());
                adaptedVertices.forEach(adapter);
                adapter.cullDirection(facingDirection);

                quadAdapter.accept(layer, adapter);

                final BakedQuad quad = adapter.build();

                target.accept(new GeneratedQuad(
                    layer,
                    quad
                ));
            }
        );
    }

    public record GeneratedQuad(
        DeconstructedModelPartComponent source,
        BakedQuad quad)
    {
    }
}

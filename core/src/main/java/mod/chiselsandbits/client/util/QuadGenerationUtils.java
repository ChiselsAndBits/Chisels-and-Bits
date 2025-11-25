package mod.chiselsandbits.client.util;

import com.communi.suggestu.scena.core.client.models.processing.BakedQuadAdapter;
import com.communi.suggestu.scena.core.client.models.processing.ModelQuadLayer;
import com.communi.suggestu.scena.core.client.models.processing.VertexData;
import com.communi.suggestu.scena.core.client.utils.RenderTypeUtils;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.client.model.face.FaceManager;
import mod.chiselsandbits.utils.LightUtil;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.TriState;
import net.minecraft.world.level.BlockAndTintGetter;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

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
        final BiConsumer<ModelQuadLayer, BakedQuadAdapter> quadAdapter,
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

                final BakedQuadAdapter adapter = new BakedQuadAdapter(adaptedVertices, layer.color());
                LightUtil.put(adapter, layer.sourceQuad());
                adapter.setQuadTint(layer.tint());
                adapter.setApplyDiffuseLighting(layer.shade());
                adapter.setTexture(layer.sprite());
                adapter.setQuadOrientation(facingDirection);

                quadAdapter.accept(layer, adapter);

                final BakedQuad quad = adapter.build();

                target.accept(new GeneratedQuad(
                    layer,
                    quad,
                    layer.usesAmbientOcclusion(),
                    layer.particleSprite(),
                    layer.renderType(),
                    layer.chunkSectionLayer()
                ));
            }
        );
    }

    public record GeneratedQuad(
        ModelQuadLayer source,
        BakedQuad quad,
        TriState ambientOcclusion,
        TextureAtlasSprite particleSprite,
        @Nullable RenderType renderType,
        @Nullable ChunkSectionLayer chunkSectionLayer)
    {

        @Nullable
        public RenderType renderType()
        {
            if (this.renderType != null)
            {
                return renderType;
            }

            if (this.chunkSectionLayer() == null)
            {
                return null;
            }

            return RenderTypeUtils.renderTypeFor(chunkSectionLayer());
        }
    }
}

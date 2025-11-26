package mod.chiselsandbits.client.model.parts;

import com.communi.suggestu.scena.core.client.rendering.ExtendedBlockModelPart;
import com.google.common.base.Suppliers;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.client.colors.ChiseledBlockBlockColor;
import mod.chiselsandbits.client.util.BakedQuadUtils;
import mod.chiselsandbits.client.util.ItemModelUtils;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.core.Direction;
import net.minecraft.util.TriState;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3fc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public record ChiseledBlockModelPart(
    BlockInformation source,
    BlockState appearance,
    ChunkSectionLayer renderType,
    QuadCollection quads,
    TriState ambientOcclusion,
    TextureAtlasSprite particleIcon,
    Supplier<Vector3fc[]> extendsCalculator
) implements BlockModelPart, ExtendedBlockModelPart
{

    public ChiseledBlockModelPart(
        final BlockInformation source,
        final BlockState appearance,
        final ChunkSectionLayer renderType,
        final QuadCollection quads,
        final TriState ambientOcclusion,
        final TextureAtlasSprite particleIcon)
    {
        this(
            source,
            appearance,
            renderType,
            quads,
            ambientOcclusion,
            particleIcon,
            Suppliers.memoize(
                () -> {
                    Set<Vector3fc> set = new HashSet<>();

                    for (BakedQuad bakedquad : quads.getAll())
                    {
                        for (int i = 0; i < 4; i++)
                        {
                            set.add(bakedquad.position(i));
                        }
                    }

                    return set.toArray(Vector3fc[]::new);
                }
            )
        );
    }

    @Override
    public @NotNull BlockState getBlockAppearance()
    {
        return appearance();
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable final Direction direction)
    {
        return quads.getQuads(direction);
    }

    @Override
    public boolean useAmbientOcclusion()
    {
        return ambientOcclusion().toBoolean(true);
    }

    @Override
    public ChunkSectionLayer getRenderType(final BlockState state)
    {
        return renderType();
    }

    public ChiseledBlockModelPart adaptForBlockModel()
    {
        return new ChiseledBlockModelPart(
            source(),
            appearance(),
            renderType(),
            ItemModelUtils.adapt(
                quads(),
                this::adaptForBlockModel
            ),
            ambientOcclusion(),
            particleIcon(),
            extendsCalculator()
        );
    }

    private BakedQuad adaptForBlockModel(BakedQuad quad)
    {
        return BakedQuadUtils.withTintIndex(
            quad,
            ChiseledBlockBlockColor.compress(
                appearance(),
                quad.tintIndex()
            )
        );
    }
}

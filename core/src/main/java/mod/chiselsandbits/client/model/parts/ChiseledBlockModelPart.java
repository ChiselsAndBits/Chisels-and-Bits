package mod.chiselsandbits.client.model.parts;

import com.communi.suggestu.scena.core.client.rendering.ExtendedBlockModelPart;
import com.google.common.base.Suppliers;
import net.minecraft.Util;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.core.Direction;
import net.minecraft.util.TriState;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public record ChiseledBlockModelPart(
    BlockState appearance,
    ChunkSectionLayer renderType,
    QuadCollection quads,
    TriState ambientOcclusion,
    TextureAtlasSprite particleIcon,
    Supplier<Vector3f[]> extendsCalculator
) implements BlockModelPart, ExtendedBlockModelPart {

    public ChiseledBlockModelPart(
        final BlockState appearance,
        final ChunkSectionLayer renderType,
        final QuadCollection quads,
        final TriState ambientOcclusion,
        final TextureAtlasSprite particleIcon)
    {
        this(
            appearance,
            renderType,
            quads,
            ambientOcclusion,
            particleIcon,
            Suppliers.memoize(
                () -> {
                    Set<Vector3f> set = new HashSet<>();

                    for (BakedQuad bakedquad : quads.getAll()) {
                        FaceBakery.extractPositions(bakedquad.vertices(), set::add);
                    }

                    return set.toArray(Vector3f[]::new);
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
}

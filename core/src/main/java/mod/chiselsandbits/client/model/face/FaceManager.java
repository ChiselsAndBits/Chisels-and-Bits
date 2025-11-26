package mod.chiselsandbits.client.model.face;

import com.communi.suggestu.scena.core.client.models.IModelManager;
import com.communi.suggestu.scena.core.client.models.processing.ModelQuadLayer;
import com.communi.suggestu.scena.core.client.rendering.IRenderingManager;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.config.IClientConfiguration;
import mod.chiselsandbits.utils.SimpleMaxSizedCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.util.TriState;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class FaceManager
{
    private static final FaceManager INSTANCE = new FaceManager();

    public static FaceManager getInstance()
    {
        return INSTANCE;
    }

    private final SimpleMaxSizedCache<Key, Collection<ModelQuadLayer>> quadCache = new SimpleMaxSizedCache<>(
        IClientConfiguration.getInstance().getFaceLayerCacheSize()::get
    );

    private record Key(
        BlockInformation blockInformation,
        @Nullable Direction direction,
        @Nullable Object modelCacheKey) {}

    private FaceManager()
    {
    }

    public void extractQuads(
        final BlockInformation blockInformation,
        final @Nullable Direction cullDirection,
        final @Nullable BlockAndTintGetter blockAndTintGetter,
        final BlockPos pos,
        final Consumer<ModelQuadLayer> pipeline)
    {
        final Key key = new Key(
            blockInformation,
            cullDirection,
            IModelManager.getInstance().determineModelCacheKey(
                blockInformation.blockState(),
                () -> blockInformation.newBlockEntity(pos),
                blockAndTintGetter,
                pos
            )
        );

        for (final ModelQuadLayer modelQuadLayer : quadCache.get(
            key,
            () -> buildQuadCollection(key, blockAndTintGetter, pos)
        ))
        {
            pipeline.accept(modelQuadLayer);
        }
    }

    private Collection<ModelQuadLayer> buildQuadCollection(
        final Key key,
        final @Nullable BlockAndTintGetter blockAndTintGetter,
        final BlockPos pos
    )
    {
        if (!key.blockInformation().blockState().getFluidState().isEmpty()) {
            return buildFluidQuadCollection(key);
        }

        final List<ModelQuadLayer> quads = new ArrayList<>();

        //TODO: Support color overriding.
        IModelManager.getInstance().extractQuads(
            key.blockInformation().blockState(),
            () -> key.blockInformation().newBlockEntity(pos),
            key.direction(),
            blockAndTintGetter,
            pos,
            quads::add
        );

        return quads;
    }

    private Collection<ModelQuadLayer> buildFluidQuadCollection(
        final Key key
    ) {
        final int lv = IClientConfiguration.getInstance().getUseGetLightValue().get() ? key.blockInformation().blockState().getLightEmission() : 0;

        final Fluid fluid = key.blockInformation().blockState().getFluidState().getType();

        TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS)
            .getSprite(IRenderingManager.getInstance().getFlowingFluidTexture(fluid));

        if (key.direction() != null && key.direction().getAxis() == Direction.Axis.Y) {
            sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS)
                .getSprite(IRenderingManager.getInstance().getStillFluidTexture(fluid));
        }

        final ModelQuadLayer.Builder builder = ModelQuadLayer.Builder.create(
            sprite,
            TriState.DEFAULT,
            ItemBlockRenderTypes.getRenderLayer(key.blockInformation().blockState().getFluidState()));

        if (key.direction() != null)
            builder.cullDirection(key.direction());

        builder.withLight(lv);

        float minV = sprite.getV0();
        float maxU = sprite.getU(16f / sprite.contents().width());
        float minU = sprite.getU0();
        float maxV = sprite.getV(16f / sprite.contents().height());

        builder.withSprite(sprite);
        injectFluidVertexDataForSide(builder, minU, maxU, minV, maxV, key.direction());

        builder.tintIndex(0xff);

        return Collections.singletonList(builder.build());
    }

    private static void injectFluidVertexDataForSide(final ModelQuadLayer.Builder builder, final float minU, final float maxU, final float minV, final float maxV, final Direction cullDirection) {
        if (cullDirection == null)
            return;

        switch (cullDirection) {
            case DOWN -> {
                builder.withVertexData(v -> {
                    v.withVertexIndex(0).withX(0).withY(0).withZ(1).withU(minU).withV(minV);
                });
                builder.withVertexData(v -> {
                    v.withVertexIndex(1).withX(0).withY(0).withZ(0).withU(minU).withV(maxV);
                });
                builder.withVertexData(v -> {
                    v.withVertexIndex(3).withX(1).withY(0).withZ(1).withU(maxU).withV(maxV);
                });
                builder.withVertexData(v -> {
                    v.withVertexIndex(2).withX(1).withY(0).withZ(0).withU(maxU).withV(minV);
                });
            }
            case UP -> {
                builder.withVertexData(v -> {
                    v.withVertexIndex(0).withX(0).withY(1).withZ(0).withU(minU).withV(minV);
                });
                builder.withVertexData(v -> {
                    v.withVertexIndex(1).withX(0).withY(1).withZ(1).withU(minU).withV(maxV);
                });
                builder.withVertexData(v -> {
                    v.withVertexIndex(2).withX(1).withY(1).withZ(1).withU(maxU).withV(maxV);
                });
                builder.withVertexData(v -> {
                    v.withVertexIndex(3).withX(1).withY(1).withZ(0).withU(maxU).withV(minV);
                });
            }
            case NORTH -> {
                builder.withVertexData(v -> {
                    v.withVertexIndex(0).withX(1).withY(1).withZ(0).withU(minU).withV(minV);
                });
                builder.withVertexData(v -> {
                    v.withVertexIndex(1).withX(1).withY(0).withZ(0).withU(minU).withV(maxV);
                });
                builder.withVertexData(v -> {
                    v.withVertexIndex(2).withX(0).withY(0).withZ(0).withU(maxU).withV(maxV);
                });
                builder.withVertexData(v -> {
                    v.withVertexIndex(3).withX(0).withY(1).withZ(0).withU(maxU).withV(minV);
                });
            }
            case SOUTH -> {
                builder.withVertexData(v -> {
                    v.withVertexIndex(0).withX(0).withY(1).withZ(1).withU(minU).withV(minV);
                });
                builder.withVertexData(v -> {
                    v.withVertexIndex(1).withX(0).withY(0).withZ(1).withU(minU).withV(maxV);
                });
                builder.withVertexData(v -> {
                    v.withVertexIndex(2).withX(1).withY(0).withZ(1).withU(maxU).withV(maxV);
                });
                builder.withVertexData(v -> {
                    v.withVertexIndex(3).withX(1).withY(1).withZ(1).withU(maxU).withV(minV);
                });
            }
            case WEST -> {
                builder.withVertexData(v -> {
                    v.withVertexIndex(0).withX(0).withY(1).withZ(0).withU(minU).withV(minV);
                });
                builder.withVertexData(v -> {
                    v.withVertexIndex(1).withX(0).withY(0).withZ(0).withU(minU).withV(maxV);
                });
                builder.withVertexData(v -> {
                    v.withVertexIndex(2).withX(0).withY(0).withZ(1).withU(maxU).withV(maxV);
                });
                builder.withVertexData(v -> {
                    v.withVertexIndex(3).withX(0).withY(1).withZ(1).withU(maxU).withV(minV);
                });
            }
            case EAST -> {
                builder.withVertexData(v -> {
                    v.withVertexIndex(0).withX(1).withY(1).withZ(1).withU(minU).withV(minV);
                });
                builder.withVertexData(v -> {
                    v.withVertexIndex(1).withX(1).withY(0).withZ(1).withU(minU).withV(maxV);
                });
                builder.withVertexData(v -> {
                    v.withVertexIndex(2).withX(1).withY(0).withZ(0).withU(maxU).withV(maxV);
                });
                builder.withVertexData(v -> {
                    v.withVertexIndex(3).withX(1).withY(1).withZ(0).withU(maxU).withV(minV);
                });
            }
        }
    }

    public void clearCache() {
        quadCache.clear();
    }
}

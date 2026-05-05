package mod.chiselsandbits.client.model.face;

import com.communi.suggestu.scena.core.client.models.IModelManager;
import com.communi.suggestu.scena.core.client.models.processing.DeconstructedModelPartComponent;
import com.communi.suggestu.scena.core.client.models.processing.ProtoStateModelPart;
import com.mojang.blaze3d.platform.Transparency;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.config.IClientConfiguration;
import mod.chiselsandbits.utils.SimpleMaxSizedCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.TriState;
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

    private final SimpleMaxSizedCache<Key, Collection<DeconstructedModelPartComponent>> quadCache = new SimpleMaxSizedCache<>(
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
        final Consumer<DeconstructedModelPartComponent> pipeline)
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

        for (final DeconstructedModelPartComponent component : quadCache.get(
            key,
            () -> buildQuadCollection(key, blockAndTintGetter, pos)
        ))
        {
            pipeline.accept(component);
        }
    }

    private Collection<DeconstructedModelPartComponent> buildQuadCollection(
        final Key key,
        final @Nullable BlockAndTintGetter blockAndTintGetter,
        final BlockPos pos
    )
    {
        if (!key.blockInformation().blockState().getFluidState().isEmpty()) {
            return buildFluidQuadCollection(key);
        }

        final List<DeconstructedModelPartComponent> quads = new ArrayList<>();

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

    private Collection<DeconstructedModelPartComponent> buildFluidQuadCollection(
        final Key key
    ) {
        final Fluid fluid = key.blockInformation().blockState().getFluidState().getType();
        final FluidModel model = Minecraft.getInstance().getModelManager().getFluidStateModelSet()
            .get(key.blockInformation().blockState().getFluidState());

        Material.Baked material = model.flowingMaterial();

        if (key.direction() != null && key.direction().getAxis() == Direction.Axis.Y) {
            material = model.stillMaterial();
        }

        var bakedMaterial = BakedQuad.MaterialInfo.of(
            material,
            computeFluidTransparency(material),
            model.tintSource() == null ? -1 : model.tintSource().color(fluid.defaultFluidState().createLegacyBlock()),
            false,
            0
        );

        final DeconstructedModelPartComponent.Builder builder = DeconstructedModelPartComponent.Builder.create();

        builder.part(new ProtoStateModelPart(TriState.DEFAULT, model.stillMaterial(), computeFlags(model, material)));

        if (key.direction() != null)
            builder.cullDirection(key.direction());

        float minV = material.sprite().getV0();
        float maxU = material.sprite().getU(16f / material.sprite().contents().width());
        float minU = material.sprite().getU0();
        float maxV = material.sprite().getV(16f / material.sprite().contents().height());

        builder.material(bakedMaterial);
        injectFluidVertexDataForSide(builder, minU, maxU, minV, maxV, key.direction());

        builder.tintIndex(0xff);

        return Collections.singletonList(builder.build());
    }

    private @BakedQuad.MaterialFlags int computeFlags(final FluidModel model, final Material.Baked material)
    {
        int materialFlags = 0;
        materialFlags |= (model.layer().translucent() ? BakedQuad.FLAG_TRANSLUCENT : 0);
        materialFlags |= (material.sprite().contents().isAnimated() ? BakedQuad.FLAG_ANIMATED : 0);
        return materialFlags;
    }

    private static Transparency computeFluidTransparency(Material.Baked material) {
        return material.forceTranslucent()
            ? Transparency.TRANSLUCENT
            : material.sprite()
                .contents().transparency();
    }

    private static void injectFluidVertexDataForSide(final DeconstructedModelPartComponent.Builder builder, final float minU, final float maxU, final float minV, final float maxV, final Direction cullDirection) {
        if (cullDirection == null)
            return;

        switch (cullDirection) {
            case DOWN -> {
                builder.vertex(v -> {
                    v.withVertexIndex(0).withX(0).withY(0).withZ(1).withU(minU).withV(minV);
                });
                builder.vertex(v -> {
                    v.withVertexIndex(1).withX(0).withY(0).withZ(0).withU(minU).withV(maxV);
                });
                builder.vertex(v -> {
                    v.withVertexIndex(3).withX(1).withY(0).withZ(1).withU(maxU).withV(maxV);
                });
                builder.vertex(v -> {
                    v.withVertexIndex(2).withX(1).withY(0).withZ(0).withU(maxU).withV(minV);
                });
            }
            case UP -> {
                builder.vertex(v -> {
                    v.withVertexIndex(0).withX(0).withY(1).withZ(0).withU(minU).withV(minV);
                });
                builder.vertex(v -> {
                    v.withVertexIndex(1).withX(0).withY(1).withZ(1).withU(minU).withV(maxV);
                });
                builder.vertex(v -> {
                    v.withVertexIndex(2).withX(1).withY(1).withZ(1).withU(maxU).withV(maxV);
                });
                builder.vertex(v -> {
                    v.withVertexIndex(3).withX(1).withY(1).withZ(0).withU(maxU).withV(minV);
                });
            }
            case NORTH -> {
                builder.vertex(v -> {
                    v.withVertexIndex(0).withX(1).withY(1).withZ(0).withU(minU).withV(minV);
                });
                builder.vertex(v -> {
                    v.withVertexIndex(1).withX(1).withY(0).withZ(0).withU(minU).withV(maxV);
                });
                builder.vertex(v -> {
                    v.withVertexIndex(2).withX(0).withY(0).withZ(0).withU(maxU).withV(maxV);
                });
                builder.vertex(v -> {
                    v.withVertexIndex(3).withX(0).withY(1).withZ(0).withU(maxU).withV(minV);
                });
            }
            case SOUTH -> {
                builder.vertex(v -> {
                    v.withVertexIndex(0).withX(0).withY(1).withZ(1).withU(minU).withV(minV);
                });
                builder.vertex(v -> {
                    v.withVertexIndex(1).withX(0).withY(0).withZ(1).withU(minU).withV(maxV);
                });
                builder.vertex(v -> {
                    v.withVertexIndex(2).withX(1).withY(0).withZ(1).withU(maxU).withV(maxV);
                });
                builder.vertex(v -> {
                    v.withVertexIndex(3).withX(1).withY(1).withZ(1).withU(maxU).withV(minV);
                });
            }
            case WEST -> {
                builder.vertex(v -> {
                    v.withVertexIndex(0).withX(0).withY(1).withZ(0).withU(minU).withV(minV);
                });
                builder.vertex(v -> {
                    v.withVertexIndex(1).withX(0).withY(0).withZ(0).withU(minU).withV(maxV);
                });
                builder.vertex(v -> {
                    v.withVertexIndex(2).withX(0).withY(0).withZ(1).withU(maxU).withV(maxV);
                });
                builder.vertex(v -> {
                    v.withVertexIndex(3).withX(0).withY(1).withZ(1).withU(maxU).withV(minV);
                });
            }
            case EAST -> {
                builder.vertex(v -> {
                    v.withVertexIndex(0).withX(1).withY(1).withZ(1).withU(minU).withV(minV);
                });
                builder.vertex(v -> {
                    v.withVertexIndex(1).withX(1).withY(0).withZ(1).withU(minU).withV(maxV);
                });
                builder.vertex(v -> {
                    v.withVertexIndex(2).withX(1).withY(0).withZ(0).withU(maxU).withV(maxV);
                });
                builder.vertex(v -> {
                    v.withVertexIndex(3).withX(1).withY(1).withZ(0).withU(maxU).withV(minV);
                });
            }
        }
    }

    public void clearCache() {
        quadCache.clear();
    }
}

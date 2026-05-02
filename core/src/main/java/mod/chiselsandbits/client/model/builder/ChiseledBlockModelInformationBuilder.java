package mod.chiselsandbits.client.model.builder;

import com.communi.suggestu.scena.core.client.models.IModelManager;
import com.communi.suggestu.scena.core.client.models.processing.BakedQuadBuilder;
import com.google.common.collect.Lists;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.multistate.StateEntrySize;
import mod.chiselsandbits.api.multistate.accessor.IAreaAccessor;
import mod.chiselsandbits.api.multistate.accessor.IStateEntryInfo;
import mod.chiselsandbits.api.profiling.IProfilerSection;
import mod.chiselsandbits.api.util.VectorUtils;
import mod.chiselsandbits.client.model.information.ChiseledBlockModelCacheKey;
import mod.chiselsandbits.client.model.information.ChiseledBlockModelInformation;
import mod.chiselsandbits.client.model.meshing.GreedyMeshBuilder;
import mod.chiselsandbits.client.model.meshing.GreedyMeshFace;
import mod.chiselsandbits.client.model.parts.ChiseledBlockModelPart;
import mod.chiselsandbits.client.model.parts.ChiseledBlockModelPartKey;
import mod.chiselsandbits.client.util.QuadGenerationUtils;
import mod.chiselsandbits.profiling.ProfilingManager;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public record ChiseledBlockModelInformationBuilder(
    IAreaAccessor data,
    ChiseledBlockModelCacheKey key,
    BlockAndTintGetter blockAndTintGetter,
    BlockPos pos
)
{

    public ChiseledBlockModelInformation build()
    {
        //Handle the case where we have no bits in the system.
        if (key().primaryState().isAir())
        {
            return ChiseledBlockModelInformation.EMPTY;
        }

        final PartsBuilder builder = new PartsBuilder();
        final GreedyMeshFace[] faces = generateFaces();

        try (final IProfilerSection ignoredQuadGeneration = ProfilingManager.getInstance().withSection("quadGeneration"))
        {
            for (final GreedyMeshFace region : faces)
            {
                final Direction cullDirection = region.normalDirection();

                QuadGenerationUtils.generateQuads(
                    region.faceValue(),
                    cullDirection,
                    blockAndTintGetter(),
                    pos(),
                    region.lowerLeft(),
                    region.upperRight(),
                    builder.newQuadProcessor(region, cullDirection)
                );
            }
        }

        return new ChiseledBlockModelInformation(
            IModelManager.getInstance().getParticleMaterial(
                key().primaryState().blockState(),
                () -> key().primaryState().newBlockEntity(pos()),
                blockAndTintGetter(),
                pos()
            ),
            builder.build(),
            builder.materials(),
            key()
        );
    }

    private GreedyMeshFace @NotNull [] generateFaces()
    {
        final GreedyMeshFace[] faces;
        try (final IProfilerSection ignoredFaceProcessing = ProfilingManager.getInstance().withSection("processing"))
        {
            faces = GreedyMeshBuilder.buildMesh(this::getBlockInformationForOffset);
        }
        return faces;
    }

    private BlockInformation getBlockInformationForOffset(
        int x, int y, int z
    )
    {
        final Vec3 targetOffset = new Vec3(x, y, z).multiply(StateEntrySize.current().getSizePerBitScalingVector());
        final Vec3 nominalTargetOffset = Vec3.ZERO.add(targetOffset);
        final BlockPos nominalTargetBlockOffset = VectorUtils.toBlockPos(nominalTargetOffset);
        final Vec3 inBlockOffset = nominalTargetOffset.subtract(Vec3.atLowerCornerOf(nominalTargetBlockOffset));
        final Vec3 inBlockOffsetTarget = VectorUtils.makePositive(inBlockOffset);

        final Direction offsetDirection = Direction.getApproximateNearest(
            nominalTargetBlockOffset.getX(),
            nominalTargetBlockOffset.getY(),
            nominalTargetBlockOffset.getZ()
        );

        IAreaAccessor neighborAccessor;
        if (targetOffset.x() >= 0 && targetOffset.x() < 1 &&
            targetOffset.y() >= 0 && targetOffset.y() < 1 &&
            targetOffset.z() >= 0 && targetOffset.z() < 1
        )
        {
            neighborAccessor = data();
        }
        else
        {
            neighborAccessor = key().neighborhood().getAreaAccessor(offsetDirection);
        }

        if (neighborAccessor != null)
        {
            return neighborAccessor.getInAreaTarget(inBlockOffsetTarget)
                .map(IStateEntryInfo::getBlockInformation)
                .orElse(BlockInformation.AIR);
        }

        return key().neighborhood().getBlockInformation(offsetDirection);
    }

    private static final class PartsBuilder
    {
        private final Set<ChiseledBlockModelMaterial>                        knownMaterials            = new HashSet<>();
        private final List<ChiseledBlockModelMaterial>                       materialRegistrationOrder = new ArrayList<>();
        private final Map<ChiseledBlockModelPartKey, QuadCollection.Builder> partBuilders              = new HashMap<>();

        public int material(BlockInformation information, int tintIndex) {
            var material = new ChiseledBlockModelMaterial(information, tintIndex);

            if (knownMaterials.add(material)) {
                materialRegistrationOrder.add(material);
                return materialRegistrationOrder.size() - 1;
            }

            return materialRegistrationOrder.indexOf(material);
        }

        public Consumer<QuadGenerationUtils.GeneratedQuad> newQuadProcessor(GreedyMeshFace region, @Nullable Direction cullDirection) {
            return quad -> {
                final ChiseledBlockModelPartKey key = new ChiseledBlockModelPartKey(
                    region.faceValue(),
                    region.faceValue().blockState(),
                    quad.source().part().ambientOcclusion(),
                    quad.source().part().particleMaterial(),
                    quad.quad().materialInfo().flags()
                );

                final QuadCollection.Builder builder = partBuilders.computeIfAbsent(
                    key,
                    $ -> new QuadCollection.Builder()
                );

                var materialIndex = material(region.faceValue(), quad.quad().materialInfo().tintIndex());
                var quadBuilder = new BakedQuadBuilder(quad.quad().materialInfo());
                quadBuilder.from(quad.quad());
                quadBuilder.tintIndex(materialIndex);
                var bakedQuad = quadBuilder.build();

                if (region.isOnOuterFace() && cullDirection != null)
                {
                    builder.addCulledFace(cullDirection, bakedQuad);
                }
                else
                {
                    builder.addUnculledFace(bakedQuad);
                }
            };
        }

        public List<ChiseledBlockModelPart> build() {
            return partBuilders.entrySet().stream()
                .map(e -> e.getKey().toPart(e.getValue().build()))
                .toList();
        }

        private List<ChiseledBlockModelMaterial> materials()
        {
            return Collections.unmodifiableList(materialRegistrationOrder);
        }
    }
}

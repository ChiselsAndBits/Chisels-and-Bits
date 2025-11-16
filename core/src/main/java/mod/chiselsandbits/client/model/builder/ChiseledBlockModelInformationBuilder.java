package mod.chiselsandbits.client.model.builder;

import com.communi.suggestu.scena.core.client.models.IModelManager;
import com.google.common.collect.Maps;
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
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public record ChiseledBlockModelInformationBuilder(
    IAreaAccessor data,
    ChiseledBlockModelCacheKey key,
    BlockAndTintGetter blockAndTintGetter,
    BlockPos pos
)
{
    public ChiseledBlockModelInformation build() {
        //Handle the case where we have no bits in the system.
        if (key().primaryState().isAir())
            return ChiseledBlockModelInformation.EMPTY;

        final Map<ChiseledBlockModelPartKey, QuadCollection.Builder> informationBuilder = Maps.newHashMap();
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
                    (quad) -> {
                        final ChiseledBlockModelPartKey key = new ChiseledBlockModelPartKey(
                            region.faceValue(),
                            region.faceValue().blockState(),
                            quad.chunkSectionLayer(),
                            quad.ambientOcclusion(),
                            quad.particleSprite()
                        );

                        final QuadCollection.Builder builder = informationBuilder.computeIfAbsent(
                            key,
                            $ -> new QuadCollection.Builder()
                        );

                        if (region.isOnOuterFace() && cullDirection != null)
                            builder.addCulledFace(cullDirection, quad.quad());
                        else
                            builder.addUnculledFace(quad.quad());
                    }
                );
            }
        }

        final List<ChiseledBlockModelPart> parts = informationBuilder.entrySet().stream()
            .map(e -> e.getKey().toPart(e.getValue().build()))
            .toList();

        return new ChiseledBlockModelInformation(
            IModelManager.getInstance().getParticleTexture(
                key().primaryState().blockState(),
                () -> key().primaryState().newBlockEntity(pos()),
                blockAndTintGetter(),
                pos()
            ),
            parts,
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
    ) {
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
}

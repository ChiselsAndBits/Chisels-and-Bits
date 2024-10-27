package mod.chiselsandbits.client.model.baked.chiseled;

import com.communi.suggestu.scena.core.client.models.IModelManager;
import mod.chiselsandbits.api.blockinformation.IBlockInformation;
import mod.chiselsandbits.api.multistate.StateEntrySize;
import mod.chiselsandbits.api.multistate.accessor.IAreaAccessor;
import mod.chiselsandbits.api.multistate.accessor.IStateEntryInfo;
import mod.chiselsandbits.api.neighborhood.IBlockNeighborhood;
import mod.chiselsandbits.api.profiling.IProfilerSection;
import mod.chiselsandbits.blockinformation.BlockInformation;
import mod.chiselsandbits.client.model.baked.base.BaseBakedBlockModel;
import mod.chiselsandbits.client.model.meshing.GreedyMeshBuilder;
import mod.chiselsandbits.client.model.meshing.GreedyMeshFace;
import mod.chiselsandbits.client.util.QuadGenerationUtils;
import mod.chiselsandbits.profiling.ProfilingManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ChiseledBlockBakedModel extends BaseBakedBlockModel {

    public static final ChiseledBlockBakedModel EMPTY = new ChiseledBlockBakedModel(
            BlockInformation.AIR,
            ChiselRenderType.SOLID,
            null,
            null,
            0);

    private final ChiselRenderType chiselRenderType;

    // keep memory requirements low by using arrays.
    private BakedQuad[] up;
    private BakedQuad[] down;
    private BakedQuad[] north;
    private BakedQuad[] south;
    private BakedQuad[] east;
    private BakedQuad[] west;
    private BakedQuad[] generic;

    private List<BakedQuad> getList(
            final Direction side) {
        if (side != null) {
            switch (side) {
                case DOWN:
                    return asList(down);
                case EAST:
                    return asList(east);
                case NORTH:
                    return asList(north);
                case SOUTH:
                    return asList(south);
                case UP:
                    return asList(up);
                case WEST:
                    return asList(west);
                default:
            }
        }

        return asList(generic);
    }

    private List<BakedQuad> asList(
            final BakedQuad[] array) {
        if (array == null) {
            return Collections.emptyList();
        }

        return Arrays.asList(array);
    }

    public ChiseledBlockBakedModel(
            final IBlockInformation state,
            final ChiselRenderType layer,
            final IAreaAccessor data,
            final IBlockNeighborhood blockNeighborhood,
            final long primaryStateRenderSeed) {
        chiselRenderType = layer;
        BakedModel originalModel = null;

        if (state != null && !state.isAir()) {
            originalModel = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getBlockModel(state.getBlockState());
            originalModel = IModelManager.getInstance().adaptToPlatform(originalModel);
        }

        if (originalModel != null && data != null) {
            final boolean shouldLayerRender;
            try (final IProfilerSection ignoredLayerCheck = ProfilingManager.getInstance().withSection("check")) {
                shouldLayerRender = layer.isRequiredForRendering(data);
            }

            if (shouldLayerRender) {
                final ChiseledBlockModelBuilder builder = new ChiseledBlockModelBuilder();
                try (final IProfilerSection ignoredFaceGeneration = ProfilingManager.getInstance().withSection("facegeneration")) {
                    generateFaces(builder, data, blockNeighborhood, primaryStateRenderSeed);
                }

                // convert from builder to final storage.
                try (final IProfilerSection ignoredFinalize = ProfilingManager.getInstance().withSection("finalize")) {
                    up = builder.getSide(Direction.UP);
                    down = builder.getSide(Direction.DOWN);
                    east = builder.getSide(Direction.EAST);
                    west = builder.getSide(Direction.WEST);
                    north = builder.getSide(Direction.NORTH);
                    south = builder.getSide(Direction.SOUTH);
                    generic = builder.getSide(null);
                }
            }
        }
    }

    public boolean isEmpty() {
        boolean trulyEmpty = getList(null).isEmpty();

        for (final Direction e : Direction.values()) {
            trulyEmpty = trulyEmpty && getList(e).isEmpty();
        }

        return trulyEmpty;
    }

    @Nullable
    private Direction getDirectionFromPosition(Vec3 pos) {
        if (pos.x >= 0 && pos.x < 1) {
            if (pos.y >= 0 && pos.y < 1) {
                if (pos.z >= 0 && pos.z < 1) {
                    return null;
                }else {
                    return pos.z < 0 ? Direction.NORTH : Direction.SOUTH;
                }
            }else if (pos.z >= 0 && pos.z < 1) {
                return pos.y < 0 ? Direction.DOWN : Direction.UP;
            }
        }else if (pos.y >= 0 && pos.y < 1 && pos.z >= 0 && pos.z < 1) {
            return pos.x < 0 ? Direction.WEST : Direction.EAST;
        }
        return null;
    }

    private boolean isInBlock(Vec3 pos) {
        return pos.x >= 0 && pos.x < 1 && pos.y >= 0 && pos.y < 1 && pos.z >= 0 && pos.z < 1;
    }

    private void generateFaces(
            final ChiseledBlockModelBuilder builder,
            final IAreaAccessor accessor,
            final IBlockNeighborhood blockNeighborhood,
            final long primaryStateRenderSeed) {
        final GreedyMeshFace[] faces;
        try (final IProfilerSection ignoredFaceProcessing = ProfilingManager.getInstance().withSection("processing")) {
            faces =
                    GreedyMeshBuilder.buildMesh((x, y, z) -> {
                        Vec3 pos = new Vec3(
                                x * StateEntrySize.current().getSizePerBit(),
                                y * StateEntrySize.current().getSizePerBit(),
                                z * StateEntrySize.current().getSizePerBit()
                        );
                        if (isInBlock(pos)) {
                            return accessor.getInAreaTarget(pos)
                                    .map(iStateEntryInfo -> chiselRenderType.isRequiredForRendering(iStateEntryInfo) ? iStateEntryInfo.getBlockInformation() : null)
                                    .orElse(IBlockInformation.AIR);
                        }
                        Direction direction = getDirectionFromPosition(pos);
                        if (direction == null || blockNeighborhood == null)
                            return IBlockInformation.AIR;
                        pos = pos.subtract(Vec3.atLowerCornerOf(direction.getNormal()));
                        IBlockInformation blockInformation = blockNeighborhood.getAreaAccessor(direction) == null ?
                                blockNeighborhood.getBlockInformation(direction) :
                                blockNeighborhood.getAreaAccessor(direction).getInAreaTarget(pos)
                                        .map(IStateEntryInfo::getBlockInformation)
                                        .orElse(IBlockInformation.AIR);
                        return blockInformation.getBlockState().skipRendering(blockInformation.getBlockState(), direction) ? blockInformation : BlockInformation.AIR;
                    }, chiselRenderType);
        }

        try (final IProfilerSection ignoredQuadGeneration = ProfilingManager.getInstance().withSection("quadGeneration")) {
            for (final GreedyMeshFace region : faces) {
                final Direction cullDirection = region.normalDirection();

                List<BakedQuad> target = builder.getList(null);
                if (region.isOnOuterFace()) {
                    target = builder.getList(cullDirection);
                }

                QuadGenerationUtils.generateQuads(target, primaryStateRenderSeed, chiselRenderType.layer, region.faceValue(), cullDirection, region.lowerLeft(), region.upperRight());
            }
        }
    }

    @NotNull
    @Override
    public List<BakedQuad> getQuads(@Nullable final BlockState state, @Nullable final Direction side, @NotNull final RandomSource rand) {
        return getList(side);
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @NotNull
    @Override
    public TextureAtlasSprite getParticleIcon() {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(MissingTextureAtlasSprite.getLocation());
    }
}

package mod.chiselsandbits.client.model.baked.chiseled;

import com.communi.suggestu.scena.core.client.rendering.type.IRenderTypeManager;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.multistate.accessor.IAreaAccessor;
import mod.chiselsandbits.api.multistate.accessor.IStateEntryInfo;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;

import java.security.InvalidParameterException;
import java.util.Collection;

public enum ChiselRenderType
{
    SOLID(RenderTypes.solidMovingBlock(), RenderTypes.entitySolid(TextureAtlas.LOCATION_BLOCKS), ChunkSectionLayer.SOLID, VoxelType.SOLID),
    SOLID_FLUID(RenderTypes.solidMovingBlock(), RenderTypes.entitySolid(TextureAtlas.LOCATION_BLOCKS), ChunkSectionLayer.SOLID, VoxelType.FLUID),
    CUTOUT(RenderTypes.cutoutMovingBlock(), RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS), ChunkSectionLayer.CUTOUT, VoxelType.UNKNOWN),
    TRANSLUCENT(RenderTypes.translucentMovingBlock(), RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS), ChunkSectionLayer.TRANSLUCENT, VoxelType.UNKNOWN),
    TRANSLUCENT_FLUID(RenderTypes.translucentMovingBlock(), RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS), ChunkSectionLayer.TRANSLUCENT, VoxelType.FLUID),
    TRIPWIRE(RenderTypes.tripwireMovingBlock(), RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS), ChunkSectionLayer.TRIPWIRE, VoxelType.UNKNOWN);

    private final RenderType layer;
    private final RenderType entityLayer;
    private final ChunkSectionLayer chunkSectionLayer;
    private final VoxelType  type;

    private static final Multimap<VoxelType, ChiselRenderType> TYPED_RENDER_TYPES = HashMultimap.create();
    static
    {
        for (final ChiselRenderType value : values())
        {
            TYPED_RENDER_TYPES.put(value.type(), value);
        }
    }

    ChiselRenderType(
        final RenderType layer,
        final RenderType entityLayer, final ChunkSectionLayer chunkSectionLayer,
        final VoxelType type)
    {
        this.layer = layer;
        this.entityLayer = entityLayer;
        this.chunkSectionLayer = chunkSectionLayer;
        this.type = type;
    }

    public boolean has(RenderType type)
    {
        return layer().equals(type) || entityLayer().equals(type);
    }

    public boolean isRequiredForRendering(
        final IAreaAccessor accessor)
    {
        if (accessor == null)
        {
            return false;
        }

        return accessor.stream()
            .anyMatch(this::isRequiredForRendering);
    }

    public boolean isRequiredForRendering(
        final IStateEntryInfo stateEntryInfo)
    {
        return isRequiredForRendering(stateEntryInfo.getBlockInformation());
    }

    public boolean isRequiredForRendering(
        final BlockInformation state)
    {
        if (state.isAir() || !this.type().isValidBlockState(state))
        {
            return false;
        }

        if (this.type().isFluid())
        {
            return IRenderTypeManager.getInstance().canRenderInType(state.blockState().getFluidState(), this.chunkSectionLayer());
        }

        return IRenderTypeManager.getInstance().canRenderInType(state.blockState(), this.chunkSectionLayer());
    }

    public static ChiselRenderType fromLayer(
        RenderType layerInfo,
        final boolean isFluid)
    {
        if (layerInfo == null)
        {
            layerInfo = RenderTypes.solidMovingBlock();
        }

        if (ChiselRenderType.CUTOUT.has(layerInfo))
        {
            return CUTOUT;
        }
        else if (ChiselRenderType.SOLID.has(layerInfo))
        {
            return isFluid ? SOLID_FLUID : SOLID;
        }
        else if (ChiselRenderType.TRANSLUCENT.has(layerInfo))
        {
            return isFluid ? TRANSLUCENT_FLUID : TRANSLUCENT;
        }
        else if (ChiselRenderType.TRIPWIRE.has(layerInfo))
        {
            return TRIPWIRE;
        }

        throw new InvalidParameterException();
    }

    public static Collection<ChiselRenderType> getRenderTypes(final VoxelType voxelType)
    {
        return TYPED_RENDER_TYPES.get(voxelType);
    }

    public RenderType layer()
    {
        return layer;
    }

    public RenderType entityLayer()
    {
        return entityLayer;
    }

    public VoxelType type()
    {
        return type;
    }

    public ChunkSectionLayer chunkSectionLayer()
    {
        return chunkSectionLayer;
    }
}

package mod.chiselsandbits.measures;

import mod.chiselsandbits.api.measuring.IMeasuringType;
import mod.chiselsandbits.api.multistate.StateEntrySize;
import mod.chiselsandbits.api.util.BlockHitResultUtils;
import mod.chiselsandbits.api.util.LocalStrings;
import mod.chiselsandbits.api.util.VectorUtils;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.client.icon.IconManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public enum MeasuringType implements IMeasuringType
{
    BIT(LocalStrings.TapeMeasureBit.getText(),
        ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "bit"),
        blockHitResult -> BlockHitResultUtils.getCenterOfHitObject(blockHitResult, StateEntrySize.current().getSizePerBitScalingVector()), (from, to, hitFace) ->
        new Vec3(
            Math.min(from.x(), to.x()) - StateEntrySize.current().getSizePerHalfBit(),
            Math.min(from.y(), to.y()) - StateEntrySize.current().getSizePerHalfBit(),
            Math.min(from.z(), to.z()) - StateEntrySize.current().getSizePerHalfBit()
        )
        ,
        (from, to, hitFace) ->
            new Vec3(
                Math.max(from.x(), to.x()) + StateEntrySize.current().getSizePerHalfBit(),
                Math.max(from.y(), to.y()) + StateEntrySize.current().getSizePerHalfBit(),
                Math.max(from.z(), to.z()) + StateEntrySize.current().getSizePerHalfBit()
            ),
        true),
    BLOCK(LocalStrings.TapeMeasureBlock.getText(), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "block"),
        blockHitResult -> BlockHitResultUtils.getCenterOfHitObject(blockHitResult, VectorUtils.ONE), (from, to, hitFace) ->
        new Vec3(
            Math.min(from.x(), to.x()) - 0.499,
            Math.min(from.y(), to.y()) - 0.499,
            Math.min(from.z(), to.z()) - 0.499
        )
        ,
        (from, to, hitFace) ->
            new Vec3(
                Math.max(from.x(), to.x()) + 0.499,
                Math.max(from.y(), to.y()) + 0.499,
                Math.max(from.z(), to.z()) + 0.499
            ),
        true),
    DISTANCE(LocalStrings.TapeMeasureDistance.getText(), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "line"),
        IClickedPositionAdapter.identity(), (from, to, hitFace) -> from,
        (from, to, hitFace) -> to, false);

    private final Component               displayName;
    private final ResourceLocation        icon;
    private final IClickedPositionAdapter clickedPositionAdapter;
    private final IPositionAdapter        finalStartPositionAdapter;
    private final IPositionAdapter        finalEndPositionAdapter;
    private final boolean                 needsNormalization;

    MeasuringType(
        final Component displayName,
        final ResourceLocation icon,
        final IClickedPositionAdapter startPositionAdapter,
        IPositionAdapter finalStartPositionAdapter,
        IPositionAdapter finalEndPositionAdapter, final boolean needsNormalization)
    {
        this.displayName = displayName;
        this.icon = icon;
        this.clickedPositionAdapter = startPositionAdapter;
        this.finalStartPositionAdapter = finalStartPositionAdapter;
        this.finalEndPositionAdapter = finalEndPositionAdapter;
        this.needsNormalization = needsNormalization;
    }

    @Override
    public TextureAtlasSprite getIcon()
    {
        return IconManager.getInstance().getIcon(icon);
    }

    @Override
    public Component getDisplayName()
    {
        return displayName;
    }

    @Override
    public @NotNull Vec3 adaptClickedPosition(@NotNull final BlockHitResult blockHitResult)
    {
        return this.clickedPositionAdapter.adapt(blockHitResult);
    }

    @Override
    public @NotNull Vec3 adaptStartCorner(@NotNull final Vec3 startPosition, @NotNull final Vec3 endPosition, @NotNull final Direction hitFace)
    {
        return this.finalStartPositionAdapter.adapt(startPosition, endPosition, hitFace);
    }

    @Override
    public @NotNull Vec3 adaptEndCorner(@NotNull final Vec3 startPosition, @NotNull final Vec3 endPosition, @NotNull final Direction hitFace)
    {
        return this.finalEndPositionAdapter.adapt(startPosition, endPosition, hitFace);
    }

    @Override
    public Vec3 getResolution()
    {
        return new Vec3(1, 1, 1);
    }

    @Override
    public boolean isNeedsNormalization()
    {
        return needsNormalization;
    }
}

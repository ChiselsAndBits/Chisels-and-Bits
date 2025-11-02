package mod.chiselsandbits.api.measuring;

import mod.chiselsandbits.api.item.withmode.group.IToolModeGroup;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public interface IMeasuringType extends IToolModeGroup
{
    @NotNull Vec3 adaptClickedPosition(@NotNull BlockHitResult blockHitResult);

    @NotNull Vec3 adaptStartCorner(@NotNull Vec3 startPosition, @NotNull Vec3 endPosition, @NotNull Direction hitFace);

    @NotNull Vec3 adaptEndCorner(@NotNull Vec3 startPosition, @NotNull Vec3 endPosition, @NotNull Direction hitFace);

    Vec3 getResolution();

    boolean isNeedsNormalization();

    @FunctionalInterface
    public interface IPositionAdapter
    {
        @NotNull
        Vec3 adapt(@NotNull final Vec3 startPosition, @NotNull final Vec3 endPosition, @NotNull final Direction hitFace);
    }

    @FunctionalInterface
    public interface IClickedPositionAdapter
    {
        static IClickedPositionAdapter identity()
        {
            return HitResult::getLocation;
        }

        @NotNull
        Vec3 adapt(@NotNull final BlockHitResult startPosition);
    }
}

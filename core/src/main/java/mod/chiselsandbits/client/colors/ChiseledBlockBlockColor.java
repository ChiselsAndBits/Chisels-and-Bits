package mod.chiselsandbits.client.colors;

import mod.chiselsandbits.api.block.state.id.IBlockStateIdManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChiseledBlockBlockColor implements BlockColor
{
    private static final int TINT_MASK = 0xff;
    private static final int TINT_BITS = 8;

    @Override
    public int getColor(
      @NotNull final BlockState state, @Nullable final BlockAndTintGetter displayReader, @Nullable final BlockPos pos, final int color)
    {
        final BlockStateAndTintIndex decompressed = decompress(color);
        return Minecraft.getInstance().getBlockColors().getColor(decompressed.blockState(), displayReader, pos, decompressed.tintIndex());
    }

    public record BlockStateAndTintIndex(BlockState blockState, int tintIndex) {}

    public static BlockStateAndTintIndex decompress(final int partIndex) {
        final BlockState containedState = IBlockStateIdManager.getInstance().getBlockStateFrom(partIndex >> TINT_BITS);
        final int tintValue = partIndex & TINT_MASK;

        return new BlockStateAndTintIndex(
            containedState, tintValue
        );
    }

    public static int compress(final BlockState blockState, final int tintIndex) {
        final int blockStateId = IBlockStateIdManager.getInstance().getIdFrom(blockState);
        return (blockStateId << TINT_BITS) | (tintIndex & TINT_MASK);
    }
}

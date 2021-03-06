package mod.chiselsandbits.client.colors;

import mod.chiselsandbits.api.block.state.id.IBlockStateIdManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ChiseledBlockItemItemColor implements IItemColor
{
    private static final int TINT_MASK = 0xff;
    private static final int TINT_BITS = 8;

    @Override
    public int getColor(
      @NotNull final ItemStack stack,
      final int tint )
    {
        final BlockState state = IBlockStateIdManager.getInstance().getBlockStateFrom( tint >> TINT_BITS );
        final Block block = state.getBlock();
        final Item itemFromBlock = block.asItem();
        int tintValue = tint & TINT_MASK;

        return Minecraft.getInstance().getItemColors().getColor( new ItemStack(itemFromBlock, 1), tintValue );
    }
}

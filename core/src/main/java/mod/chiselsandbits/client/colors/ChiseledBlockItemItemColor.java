package mod.chiselsandbits.client.colors;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChiseledBlockItemItemColor implements ItemTintSource
{
    public static final MapCodec<ChiseledBlockItemItemColor> CODEC = MapCodec.unit(new ChiseledBlockItemItemColor());

    private static final int TINT_MASK = 0xff;
    private static final int TINT_BITS = 8;

    @Override
    public int calculate(final ItemStack itemStack, @Nullable final ClientLevel clientLevel, @Nullable final LivingEntity livingEntity)
    {
        return 0;
    }

    @Override
    public @NotNull MapCodec<? extends ItemTintSource> type()
    {
        return CODEC;
    }

    /*
    @Override
    public int getColor(
      @NotNull final ItemStack stack,
      final int tint )
    {
        final BlockState state = IBlockStateIdManager.getInstance().getBlockStateFrom( tint >> TINT_BITS );
        final BlockInformation blockInformation = new BlockInformation(state, Optional.empty());
        if(state.getBlock() instanceof LiquidBlock) {
            return IClientFluidManager.getInstance().getFluidColor(new FluidInformation(
                    state.getFluidState().getType()
            ));
        }

        if ((!Minecraft.getInstance().options.keyShift.isUnbound() && Minecraft.getInstance().options.keyShift.isDown()) || (Minecraft.getInstance().getWindow() != null && Screen.hasShiftDown()))
        {
            final Block block = state.getBlock();
            final Item item = block.asItem();
            int tintValue = tint & TINT_MASK;

            if (item != Items.AIR)
            {
                return Minecraft.getInstance().itemColors.getColor(new ItemStack(item, 1), tintValue);
            }

            return 0xffffff;
        }

        if (!IEligibilityManager.getInstance().canBeChiseled(blockInformation))
        {
            return 0xffffff;
        }

        final ItemStack workingStack = new ItemStack(state.getBlock(), 1);
        if (workingStack.getItem() instanceof AirItem)
            return 0xffffff;

        final Block block = state.getBlock();
        final Item itemFromBlock = block.asItem();
        int tintValue = tint & TINT_MASK;
        return Minecraft.getInstance().itemColors.getColor( new ItemStack(itemFromBlock, 1), tintValue );
    }
    */
}

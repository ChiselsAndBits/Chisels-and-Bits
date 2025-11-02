package mod.chiselsandbits.client.colors;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BitItemItemColor implements ItemTintSource
{
    public static final MapCodec<BitItemItemColor> CODEC = MapCodec.unit(new BitItemItemColor());

    private static final int TINT_MASK = 0xff;

    /*

    @Override
    public int getColor(
      final ItemStack stack,
      final int tint)
    {
        if (!(stack.getItem() instanceof BitItem))
            return 0xffffff;

        final BlockInformation blockInformation = ((BitItem) stack.getItem()).getBlockInformation(stack);
        if(blockInformation.blockState().getBlock() instanceof LiquidBlock) {
            if ((!Minecraft.getInstance().options.keyShift.isUnbound() && Minecraft.getInstance().options.keyShift.isDown()) || (Minecraft.getInstance().getWindow() != null && Screen.hasShiftDown())) {
                return -1; //No coloring on liquids when pressing shifts -> Buckets
            }

            return IClientFluidManager.getInstance().getFluidColor(new FluidInformation(
                    blockInformation.blockState().getFluidState().getType(),
                    1
            ));
        }

        if ((!Minecraft.getInstance().options.keyShift.isUnbound() && Minecraft.getInstance().options.keyShift.isDown()) || (Minecraft.getInstance().getWindow() != null && Screen.hasShiftDown()))
        {
            final Block block = blockInformation.blockState().getBlock();
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

        final ItemStack workingStack = new ItemStack(blockInformation.blockState().getBlock(), 1);
        if (workingStack.getItem() instanceof AirItem)
            return 0xffffff;

        return Minecraft.getInstance().itemColors.getColor(workingStack, tint);
    }

     */
    @Override
    public int calculate(final @NotNull ItemStack stack, @Nullable final ClientLevel level, @Nullable final LivingEntity entity)
    {
        return -1;
    }

    @Override
    public @NotNull MapCodec<? extends ItemTintSource> type()
    {
        return CODEC;
    }
}

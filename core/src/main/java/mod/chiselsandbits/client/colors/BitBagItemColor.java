package mod.chiselsandbits.client.colors;

import com.mojang.serialization.MapCodec;
import mod.chiselsandbits.item.BitBagItem;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BitBagItemColor implements ItemTintSource
{
    public static MapCodec<BitBagItemColor> CODEC = MapCodec.unit(new BitBagItemColor());

    @Override
    public int calculate(final @NotNull ItemStack stack, @Nullable final ClientLevel level, @Nullable final LivingEntity entity)
    {
        DyeColor color = BitBagItem.getDyedColor( stack );
        if ( color != null )
            return color.getTextureDiffuseColor();

        return -1;
    }

    @Override
    public @NotNull MapCodec<? extends ItemTintSource> type()
    {
        return CODEC;
    }
}

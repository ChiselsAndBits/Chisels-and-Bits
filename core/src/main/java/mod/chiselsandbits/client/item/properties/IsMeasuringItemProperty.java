package mod.chiselsandbits.client.item.properties;

import com.mojang.serialization.MapCodec;
import mod.chiselsandbits.registrars.ModItems;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public record IsMeasuringItemProperty() implements ConditionalItemModelProperty
{
    public static final MapCodec<IsMeasuringItemProperty> CODEC = MapCodec.unit(IsMeasuringItemProperty::new);

    @Override
    public MapCodec<? extends ConditionalItemModelProperty> type()
    {
        return CODEC;
    }

    @Override
    public boolean get(final ItemStack stack, @Nullable final ClientLevel level, @Nullable final LivingEntity entity, final int seed, final ItemDisplayContext displayContext)
    {
        if (stack.getItem() != ModItems.MEASURING_TAPE.get())
        {
            return false;
        }

        return ModItems.MEASURING_TAPE.get().getStart(stack).isPresent();
    }
}

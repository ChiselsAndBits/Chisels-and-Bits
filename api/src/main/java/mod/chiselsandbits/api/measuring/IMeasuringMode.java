package mod.chiselsandbits.api.measuring;

import mod.chiselsandbits.api.item.withmode.IToolMode;
import net.minecraft.world.item.DyeColor;

public interface IMeasuringMode extends IToolMode<IMeasuringType>
{
    DyeColor getColor();

    IMeasuringType getType();
}

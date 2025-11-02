package mod.chiselsandbits.item;

import mod.chiselsandbits.api.util.HelpTextUtils;
import mod.chiselsandbits.api.util.LocalStrings;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class ChiseledPrinterBlockItem extends BlockItem
{
    public ChiseledPrinterBlockItem(final Block block, final Properties properties)
    {
        super(block, properties);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void appendHoverText(
        @NotNull ItemStack stack,
        Item.@NotNull TooltipContext context,
        @NotNull TooltipDisplay tooltipDisplay,
        @NotNull Consumer<Component> tooltipAdder,
        @NotNull TooltipFlag flag)
    {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, flag);
        HelpTextUtils.build(LocalStrings.ChiselStationHelp, tooltipAdder);
    }

}

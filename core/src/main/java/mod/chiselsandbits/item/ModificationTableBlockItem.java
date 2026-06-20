package mod.chiselsandbits.item;

import java.util.function.Consumer;

import mod.chiselsandbits.api.util.HelpTextUtils;
import mod.chiselsandbits.api.util.LocalStrings;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

public class ModificationTableBlockItem extends BlockItem {
	public ModificationTableBlockItem(final Block block, final Properties properties) {
		super(block, properties);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void appendHoverText(
			@NotNull ItemStack stack,
			@NotNull TooltipContext context,
			@NotNull TooltipDisplay tooltipDisplay,
			@NotNull Consumer<Component> tooltipAdder,
			@NotNull TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, flag);
		HelpTextUtils.build(LocalStrings.ModificationTableHelp, tooltipAdder);
	}
}

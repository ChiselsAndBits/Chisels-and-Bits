package mod.chiselsandbits.item;

import java.util.function.Consumer;

import com.communi.suggestu.scena.core.dist.DistExecutor;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.util.HelpTextUtils;
import mod.chiselsandbits.api.util.LocalStrings;
import mod.chiselsandbits.block.BitStorageBlock;
import mod.chiselsandbits.block.entities.BitStorageBlockEntity;
import mod.chiselsandbits.registrars.ModDataComponentTypes;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BitStorageBlockItem extends BlockItem {
	public BitStorageBlockItem(
			final Block block, Properties builder) {
		super(block, builder);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void appendHoverText(
			final ItemStack stack,
			final TooltipContext context,
			final TooltipDisplay tooltipDisplay,
			final Consumer<Component> tooltipAdder,
			final TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, flag);

		if (stack.has(ModDataComponentTypes.BLOCK_INFORMATION.get()) && stack.has(ModDataComponentTypes.COUNT.get())) {
			final BlockInformation containedState = stack.get(ModDataComponentTypes.BLOCK_INFORMATION.get());
			final int count = stack.get(ModDataComponentTypes.COUNT.get());

			HelpTextUtils.build(LocalStrings.HelpBitStorageFilled, tooltipAdder, containedState.blockState().getBlock().getName(), count);
		} else {
			HelpTextUtils.build(LocalStrings.HelpBitStorageEmpty, tooltipAdder);
		}

		tooltipAdder.accept(Component.literal(""));

		if (
				DistExecutor.unsafeRunForDist(
						() -> () -> Minecraft.getInstance().player != null && Minecraft.getInstance().player.isShiftKeyDown(),
						() -> () -> Boolean.TRUE
				)
		) {
			HelpTextUtils.build(LocalStrings.LongHelpBitStorage, tooltipAdder);
		}
	}

	@Override
	protected boolean updateCustomBlockEntityTag(
			final BlockPos pos, final Level worldIn, @Nullable final Player player, final ItemStack stack, final BlockState state) {
		super.updateCustomBlockEntityTag(pos, worldIn, player, stack, state);

		if (worldIn.isClientSide()) {
			return false;
		}

		final BlockEntity blockEntity = worldIn.getBlockEntity(pos);

		if (!(blockEntity instanceof final BitStorageBlockEntity bitStorage)) {
			return false;
		}

		BitStorageBlock.updateEntityFromStack(stack, bitStorage);
		return true;
	}
}

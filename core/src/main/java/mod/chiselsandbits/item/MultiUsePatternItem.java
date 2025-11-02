package mod.chiselsandbits.item;

import com.communi.suggestu.scena.core.dist.Dist;
import com.communi.suggestu.scena.core.dist.DistExecutor;
import mod.chiselsandbits.api.exceptions.SealingNotSupportedException;
import mod.chiselsandbits.api.item.multistate.IMultiStateItemStack;
import mod.chiselsandbits.api.item.pattern.IMultiUsePatternItem;
import mod.chiselsandbits.api.pattern.placement.IPatternPlacementType;
import mod.chiselsandbits.api.sealing.ISupportsUnsealing;
import mod.chiselsandbits.api.util.HelpTextUtils;
import mod.chiselsandbits.api.util.LocalStrings;
import mod.chiselsandbits.registrars.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class MultiUsePatternItem extends SingleUsePatternItem implements IMultiUsePatternItem
{
    public MultiUsePatternItem(final Properties builder)
    {
        super(builder);
    }

    @Override
    protected InteractionResult determineSuccessResult(final BlockPlaceContext context, final ItemStack resultingStack)
    {
        return InteractionResult.SUCCESS;
    }

    @Override
    public @NotNull ItemStack seal(final @NotNull ItemStack source) throws SealingNotSupportedException
    {
        throw new SealingNotSupportedException();
    }

    @Override
    public void appendHoverText(
        final ItemStack stack,
        final TooltipContext context,
        final TooltipDisplay tooltipDisplay,
        final Consumer<Component> tooltipAdder,
        final TooltipFlag flag)
    {
        final IPatternPlacementType mode = getMode(stack);
        if (mode.getGroup().isPresent())
        {
            tooltipAdder.accept(LocalStrings.PatternItemTooltipModeGrouped.getText(mode.getGroup().get().getDisplayName(), mode.getDisplayName()));
        }
        else
        {
            tooltipAdder.accept(LocalStrings.PatternItemTooltipModeSimple.getText(mode.getDisplayName()));
        }

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if ((Minecraft.getInstance().getWindow() != null && Minecraft.getInstance().hasShiftDown())) {
                tooltipAdder.accept(Component.literal("        "));
                tooltipAdder.accept(Component.literal("        "));

                HelpTextUtils.build(
                    LocalStrings.HelpSealedPattern, tooltipAdder
                );
            }
        });
    }

    @Override
    public @NotNull ItemStack unseal(@NotNull final ItemStack source) throws SealingNotSupportedException
    {
        if (source.getItem() instanceof ISupportsUnsealing)
        {
            final ItemStack seal = new ItemStack(ModItems.SINGLE_USE_PATTERN_ITEM.get());
            final IMultiStateItemStack stack = createItemStack(source);
            stack.writeDataTo(seal);
            return seal;
        }

        return source;
    }
}

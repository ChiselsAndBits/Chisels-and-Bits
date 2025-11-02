package mod.chiselsandbits.item;

import com.communi.suggestu.scena.core.dist.Dist;
import com.communi.suggestu.scena.core.dist.DistExecutor;
import mod.chiselsandbits.api.exceptions.SealingNotSupportedException;
import mod.chiselsandbits.api.item.multistate.IMultiStateItemStack;
import mod.chiselsandbits.api.item.pattern.IMultiUsePatternItem;
import mod.chiselsandbits.api.item.pattern.IPatternItem;
import mod.chiselsandbits.api.multistate.accessor.IAreaAccessor;
import mod.chiselsandbits.api.multistate.mutator.IMutatorFactory;
import mod.chiselsandbits.api.multistate.mutator.world.IWorldAreaMutator;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshot;
import mod.chiselsandbits.api.notifications.INotificationManager;
import mod.chiselsandbits.api.pattern.placement.IPatternPlacementType;
import mod.chiselsandbits.api.placement.PlacementResult;
import mod.chiselsandbits.api.util.HelpTextUtils;
import mod.chiselsandbits.api.util.LocalStrings;
import mod.chiselsandbits.item.multistate.SingleBlockMultiStateItemStack;
import mod.chiselsandbits.multistate.snapshot.EmptySnapshot;
import mod.chiselsandbits.registrars.ModDataComponentTypes;
import mod.chiselsandbits.registrars.ModItems;
import mod.chiselsandbits.registrars.ModPatternPlacementTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class SingleUsePatternItem extends Item implements IPatternItem
{

    public SingleUsePatternItem(final Properties builder)
    {
        super(builder);
    }

    @Override
    public @NotNull Component getName(final ItemStack item)
    {
        if (item.has(ModDataComponentTypes.HIGHLIGHT.get()))
        {
            final Component highlight = item.get(ModDataComponentTypes.HIGHLIGHT.get());
            return Objects.requireNonNull(highlight).copy().withStyle(ChatFormatting.RED);
        }

        return super.getName(item);
    }

    /**
     * Creates an itemstack aware context wrapper that gives access to the multistate information contained within the given itemstack.
     *
     * @param stack The stack to get an {@link IMultiStateItemStack} for.
     * @return The {@link IMultiStateItemStack} that represents the data in the given itemstack.
     */
    @NotNull
    @Override
    public IMultiStateItemStack createItemStack(final ItemStack stack)
    {
        //Take care of an empty pattern.
        //Generally the case when this is a stack from the creative menu.
        if (!SingleBlockMultiStateItemStack.hasData(stack)) {
            return EmptySnapshot.Stack.INSTANCE;
        }

        return new SingleBlockMultiStateItemStack(stack);
    }

    @Override
    @NotNull
    public InteractionResult useOn(@NotNull UseOnContext context) {
        final IMultiStateItemStack contents = createItemStack(context.getItemInHand());
        if (contents.getStatistics().isEmpty()) {
            if (context.getPlayer() == null)
                return InteractionResult.FAIL;

            if (!context.getPlayer().isCreative())
                return InteractionResult.FAIL;

            if (!context.getPlayer().isShiftKeyDown())
                return InteractionResult.FAIL;

            final IWorldAreaMutator areaMutator = IMutatorFactory.getInstance().in(context.getLevel(), context.getClickedPos());
            areaMutator.createSnapshot().toItemStack().writeDataTo(context.getItemInHand());
            return InteractionResult.SUCCESS;
        }


        return this.tryPlace(new BlockPlaceContext(context));
    }

    @NotNull
    public InteractionResult tryPlace(@NotNull final BlockPlaceContext context)
    {
        if (context.getPlayer() == null)
            return InteractionResult.FAIL;

        final IAreaAccessor source = this.createItemStack(context.getItemInHand());
        final IMultiStateSnapshot sourceSnapshot = source.createSnapshot();
        final IPatternPlacementType mode = getMode(context.getItemInHand());

        final PlacementResult resultType = mode
          .performPlacement(
            sourceSnapshot,
            context,
            false);

        if (!resultType.isSuccess() && context.getLevel().isClientSide()) {
            INotificationManager.getInstance().notify(
              mode.getIcon(),
              new Vec3(1, 0, 0),
              resultType.getFailureMessage()
            );
        }

        return resultType.isSuccess() ?
                 determineSuccessResult(context, context.getItemInHand()) :
                 InteractionResult.FAIL;
    }

    protected InteractionResult determineSuccessResult(final BlockPlaceContext context, final ItemStack stack) {
        if (context.getPlayer() != null && context.getPlayer().isCreative())
        {
            return InteractionResult.SUCCESS;
        }

        stack.shrink(1);
        return InteractionResult.CONSUME;
    }

    @Override
    public @NotNull ItemStack seal(@NotNull final ItemStack source) throws SealingNotSupportedException
    {
        if (source.getItem() == this)
        {
            if (!(source.getItem() instanceof IMultiUsePatternItem))
            {
                final ItemStack seal = new ItemStack(ModItems.MULTI_USE_PATTERN_ITEM.get());
                final IMultiStateItemStack stack = createItemStack(source);
                stack.writeDataTo(seal);
                return seal;
            }

            throw new SealingNotSupportedException();
        }

        return source;
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

                HelpTextUtils.build(LocalStrings.HelpSimplePattern, tooltipAdder);
            }
        });

    }

    @Override
    public @NotNull IPatternPlacementType getMode(final ItemStack stack)
    {
        return stack.getOrDefault(ModDataComponentTypes.PATTERN_PLACEMENT_TYPE.get(), ModPatternPlacementTypes.PLACEMENT.get());
    }

    @Override
    public void setMode(final ItemStack stack, final IPatternPlacementType mode)
    {
        stack.set(ModDataComponentTypes.PATTERN_PLACEMENT_TYPE.get(), mode);
    }

    @Override
    public @NotNull Collection<IPatternPlacementType> getPossibleModes()
    {
        return ModPatternPlacementTypes.REGISTRY_SUPPLIER.get().getValues();
    }
}

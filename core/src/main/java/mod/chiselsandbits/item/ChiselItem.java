package mod.chiselsandbits.item;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.chiselsandbits.api.chiseling.ChiselingOperation;
import mod.chiselsandbits.api.chiseling.IChiselingContext;
import mod.chiselsandbits.api.chiseling.IChiselingManager;
import mod.chiselsandbits.api.chiseling.ILocalChiselingContextCache;
import mod.chiselsandbits.api.chiseling.mode.IChiselMode;
import mod.chiselsandbits.api.client.render.preview.chiseling.IChiselContextPreviewRendererRegistry;
import mod.chiselsandbits.api.config.IClientConfiguration;
import mod.chiselsandbits.api.item.chisel.IChiselItem;
import mod.chiselsandbits.api.item.chisel.IChiselingItem;
import mod.chiselsandbits.api.item.click.ClickProcessingState;
import mod.chiselsandbits.api.item.named.IDynamicallyHighlightedNameItem;
import mod.chiselsandbits.api.multistate.StateEntrySize;
import mod.chiselsandbits.api.notifications.INotificationManager;
import mod.chiselsandbits.api.util.LocalStrings;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.chiseling.ChiselingManager;
import mod.chiselsandbits.chiseling.LocalChiselingContextCache;
import mod.chiselsandbits.registrars.ModDataComponentTypes;
import mod.chiselsandbits.utils.ItemStackUtils;
import mod.chiselsandbits.utils.TranslationUtils;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ChiselItem extends Item implements IChiselItem, IDynamicallyHighlightedNameItem
{

    private static final Logger LOGGER = LogManager.getLogger();
    private final ToolMaterial material;

    public ChiselItem(
      final ToolMaterial material,
      final Properties builderIn)
    {
        super(builderIn.shovel(material, 0F, -3.0F));
        this.material = material;
    }

    @Override
    public @NotNull Component getName(final ItemStack stack)
    {
        if (stack.has(ModDataComponentTypes.CHISEL_ERROR.get())) {
            return Objects.requireNonNull(stack.get(ModDataComponentTypes.CHISEL_ERROR.get()));
        }

        return super.getName(stack);
    }

    @Override
    public void appendHoverText(
        final ItemStack stack,
        final TooltipContext context,
        final TooltipDisplay tooltipDisplay,
        final Consumer<Component> tooltipAdder,
        final TooltipFlag flag)
    {
        final IChiselMode mode = getMode(stack);
        if (mode.getGroup().isPresent())
        {
            tooltipAdder.accept(TranslationUtils.build("chiselmode.mode_grouped", mode.getGroup().get().getDisplayName(), mode.getDisplayName()));
        }
        else
        {
            tooltipAdder.accept(TranslationUtils.build("chiselmode.mode", mode.getDisplayName()));
        }


        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, flag);
    }

    @NotNull
    @Override
    public IChiselMode getMode(final ItemStack stack)
    {
        return stack.getOrDefault(ModDataComponentTypes.CHISEL_MODE.get(), IChiselMode.getDefaultMode());
    }

    @Override
    public void setMode(final ItemStack stack, final IChiselMode mode)
    {
       stack.set(ModDataComponentTypes.CHISEL_MODE.get(), mode);
    }

    @NotNull
    @Override
    public Collection<IChiselMode> getPossibleModes()
    {
        return IChiselMode.getRegistry()
                 .getValues()
                 .stream()
                 .filter(mode -> !mode.requiresPlaceableEditStack())
                 .sorted(Comparator.comparing(IChiselMode::getRegistryName))
                 .collect(Collectors.toList());
    }

    @Override
    public ClickProcessingState handleLeftClickProcessing(
      final Player playerEntity,
      final InteractionHand hand,
      final BlockPos position,
      final Direction face,
      final ClickProcessingState currentState
    )
    {
        final ItemStack itemStack = playerEntity.getItemInHand(hand);
        if (itemStack.isEmpty() || itemStack.getItem() != this)
        {
            return currentState;
        }

        final IChiselingItem chiselingItem = (IChiselingItem) itemStack.getItem();
        final IChiselMode chiselMode = chiselingItem.getMode(itemStack);

        final IChiselingContext context = IChiselingManager.getInstance().getOrCreateContext(
          playerEntity,
          chiselMode,
          ChiselingOperation.CHISELING,
          false,
          itemStack);

        final ClickProcessingState resultState = chiselMode.onLeftClickBy(
          playerEntity,
          context
        );

        if (context.isComplete())
        {
            playerEntity.getCooldowns().addCooldown(itemStack, Constants.TICKS_BETWEEN_CHISEL_USAGE);
            ILocalChiselingContextCache.getInstance().clear(ChiselingOperation.CHISELING);
        }

        if (context.getError().isPresent() && context.getWorld().isClientSide()) {
            INotificationManager.getInstance().notify(
              context.getMode().getIcon(),
              new Vec3(1, 0, 0),
              context.getError().get()
            );
        }

        return resultState;
    }

    @Override
    public void onLeftClickProcessingEnd(final Player player, final ItemStack stack)
    {
        final IChiselMode chiselMode = getMode(stack);
        Optional<IChiselingContext> context = IChiselingManager.getInstance().get(
          player,
          chiselMode,
          ChiselingOperation.CHISELING);

        if (context.isEmpty()) {
            context = LocalChiselingContextCache.getInstance().get(ChiselingOperation.CHISELING);
        }

        context.ifPresent(c -> {
            chiselMode.onStoppedLeftClicking(player, c);
            if (c.isComplete()) {
                player.getCooldowns().addCooldown(stack, Constants.TICKS_BETWEEN_CHISEL_USAGE);
                LocalChiselingContextCache.getInstance().clear(ChiselingOperation.CHISELING);
            }
        });
    }

    @Override
    public boolean canUse(final Player playerEntity, @NotNull final ItemStack stack)
    {
        final boolean isAllowedToUse = ChiselingManager.getInstance().canChisel(playerEntity) && !playerEntity.getCooldowns().isOnCooldown(stack);
        if (getMode(stack).isSingleClickUse() && !isAllowedToUse && playerEntity.level().isClientSide() && IClientConfiguration.getInstance().getShowCoolDownError().get()) {
            INotificationManager.getInstance().notify(
                    getMode(stack).getIcon(),
                    new Vec3(1, 0, 0),
                    LocalStrings.ChiselAttemptFailedWaitForCoolDown.getText()
            );
        }

        return isAllowedToUse;
    }

    @Override
    public boolean shouldDrawDefaultHighlight(@NotNull final Player playerEntity)
    {
        final ItemStack itemStack = ItemStackUtils.getHighlightItemStackFromPlayer(playerEntity);
        if (itemStack.isEmpty() || itemStack.getItem() != this)
        {
            return true;
        }

        final IChiselingItem chiselingItem = (IChiselingItem) itemStack.getItem();
        final IChiselMode chiselMode = chiselingItem.getMode(itemStack);

        final Optional<IChiselingContext> potentiallyExistingContext =
          IChiselingManager.getInstance().get(playerEntity, chiselMode, ChiselingOperation.CHISELING);
        if (potentiallyExistingContext.isPresent())
        {
            final IChiselingContext context = potentiallyExistingContext.get();

            if (context.getMutator().isPresent())
            {
                return false;
            }

            final IChiselingContext currentContextSnapshot = context.createSnapshot();
            chiselMode.onLeftClickBy(
              playerEntity,
              currentContextSnapshot
            );

            return currentContextSnapshot.getMutator().isEmpty();
        }

        final Optional<IChiselingContext> localCachedContext = ILocalChiselingContextCache
                                                                 .getInstance()
                                                                 .get(ChiselingOperation.CHISELING);

        if (localCachedContext.isPresent()
          &&
              localCachedContext.get().getMode().isStillValid(playerEntity, localCachedContext.get(), ChiselingOperation.CHISELING)
        )
        {
            final IChiselingContext context = localCachedContext.get();

            if (
              context.getMode() == chiselMode
            )

            if (context.getMutator().isPresent())
            {
                return false;
            }

            return context.getMutator().isEmpty();
        }

        final IChiselingContext context = IChiselingManager.getInstance().create(
          playerEntity,
          chiselMode,
          ChiselingOperation.CHISELING,
          true,
          itemStack);

        //We try a left click render primary.
        chiselMode.onLeftClickBy(
          playerEntity,
          context
        );

        //Store it in the local cache.
        if (!context.isComplete())
        ILocalChiselingContextCache.getInstance().set(ChiselingOperation.CHISELING, context);

        return context.getMutator().isEmpty();
    }

    @Override
    public void renderHighlight(
        final Player playerEntity,
        final LevelRenderer levelRenderer,
        final PoseStack poseStack,
        final MultiBufferSource.BufferSource bufferSource,
        final LevelRenderState levelRenderState,
        final float partialTicks)
    {
        final ItemStack itemStack = ItemStackUtils.getHighlightItemStackFromPlayer(playerEntity);
        if (itemStack.isEmpty() || itemStack.getItem() != this)
        {
            return;
        }

        final IChiselingItem chiselingItem = (IChiselingItem) itemStack.getItem();
        final IChiselMode chiselMode = chiselingItem.getMode(itemStack);

        final Optional<IChiselingContext> potentiallyExistingContext =
          IChiselingManager.getInstance().get(playerEntity, chiselMode, ChiselingOperation.CHISELING);

        final Optional<IChiselingContext> potentiallyCachedContext = ILocalChiselingContextCache.getInstance()
                                                                       .get(ChiselingOperation.CHISELING);
        IChiselingContext context;
        if (potentiallyExistingContext.isPresent()) {
            context = potentiallyExistingContext.get();

            chiselMode.onLeftClickBy(
              playerEntity,
              context
            );
        }
        else if (potentiallyCachedContext.isPresent()
                   && potentiallyCachedContext.get().getMode() == chiselMode
                   && potentiallyCachedContext.get().getModeOfOperandus() == ChiselingOperation.CHISELING
                   && chiselMode.isStillValid(playerEntity, potentiallyCachedContext.get(), ChiselingOperation.CHISELING)) {
            context = potentiallyCachedContext.get();
        }
        else
        {
            context =  IChiselingManager.getInstance().create(
              playerEntity,
              chiselMode,
              ChiselingOperation.CHISELING,
              true,
              itemStack
            );

            chiselMode.onLeftClickBy(
              playerEntity,
              context
            );
        }

        if (context.getMutator().isEmpty())
        {
            ILocalChiselingContextCache.getInstance().clear(ChiselingOperation.CHISELING);
            //No bit was included in the chiseling action
            //We interacted with something we could not chisel
            return;
        }

        if (context.getMutator().isPresent() && context.getError().isEmpty()) {
            IChiselContextPreviewRendererRegistry.getInstance().getCurrent()
              .renderExistingContextsBoundingBox(levelRenderer, poseStack, bufferSource, levelRenderState, partialTicks, context);
            ILocalChiselingContextCache.getInstance().set(ChiselingOperation.CHISELING, context);
        }
    }

    @Override
    public boolean isDamageableDuringChiseling()
    {
        return true;
    }

    @Override
    public DataComponentMap components() {
        final DataComponentMap map = super.components();
        final DataComponentMap.Builder builder = DataComponentMap.builder();

        builder.addAll(map);
        builder.set(DataComponents.MAX_DAMAGE, getMaxBrokenBlocksAsDurability() * StateEntrySize.ONE_SIXTEENTH.getBitsPerBlock());

        return builder.build();
    }

    public int getMaxBrokenBlocksAsDurability()
    {
        return material.durability();
    }

    @Override
    public ItemStack adaptItemStack(final ItemStack currentToolStack)
    {
        final Optional<IChiselingContext> chiselingContext =
          ILocalChiselingContextCache.getInstance().get(ChiselingOperation.CHISELING);
        final Optional<IChiselingContext> placingContext =
          ILocalChiselingContextCache.getInstance().get(ChiselingOperation.PLACING);

        if (chiselingContext.isPresent() && chiselingContext.get().getError().isPresent()) {
            final ItemStack errorStack = currentToolStack.copy();
            errorStack.set(ModDataComponentTypes.CHISEL_ERROR.get(), chiselingContext.get().getError().get());
            return errorStack;
        }

        if (placingContext.isPresent() && placingContext.get().getError().isPresent()) {
            final ItemStack errorStack = currentToolStack.copy();
            errorStack.set(ModDataComponentTypes.CHISEL_ERROR.get(), placingContext.get().getError().get());
            return errorStack;
        }

        return currentToolStack;
    }
}

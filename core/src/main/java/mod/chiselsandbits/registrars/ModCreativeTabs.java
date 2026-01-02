package mod.chiselsandbits.registrars;

import com.communi.suggestu.scena.core.creativetab.ICreativeTabManager;
import com.communi.suggestu.scena.core.registries.IPlatformRegistryManager;
import com.communi.suggestu.scena.core.registries.deferred.IRegistrar;
import com.communi.suggestu.scena.core.registries.deferred.IRegistryObject;
import mod.chiselsandbits.api.blockinformation.IBlockInformation;
import mod.chiselsandbits.api.chiseling.eligibility.IEligibilityManager;
import mod.chiselsandbits.api.client.clipboard.ICreativeClipboardManager;
import mod.chiselsandbits.api.item.bit.IBitItem;
import mod.chiselsandbits.api.item.bit.IBitItemManager;
import mod.chiselsandbits.api.util.LocalStrings;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.api.variant.state.IStateVariantManager;
import mod.chiselsandbits.block.ChiseledBlock;
import mod.chiselsandbits.blockinformation.BlockInformation;
import mod.chiselsandbits.api.item.multistate.IMultiStateItemStack;
import mod.chiselsandbits.item.BitBagItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public final class ModCreativeTabs
{
    private static final Logger                         LOGGER    = LogManager.getLogger();
    private static final IRegistrar<CreativeModeTab> REGISTRAR = IRegistrar.create(Registries.CREATIVE_MODE_TAB, Constants.MOD_ID);

    public static IRegistryObject<CreativeModeTab> MAIN = REGISTRAR.register("main", () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 1)
            .icon(() -> new ItemStack(ModItems.ITEM_CHISEL_NETHERITE.get()))
            .title(LocalStrings.ChiselsAndBitsName.getText())
            .displayItems((parameters, output) -> {
                output.accept(new ItemStack(ModItems.ITEM_CHISEL_STONE.get()));
                output.accept(new ItemStack(ModItems.ITEM_CHISEL_IRON.get()));
                output.accept(new ItemStack(ModItems.ITEM_CHISEL_GOLD.get()));
                output.accept(new ItemStack(ModItems.ITEM_CHISEL_DIAMOND.get()));
                output.accept(new ItemStack(ModItems.ITEM_CHISEL_NETHERITE.get()));
                output.accept(new ItemStack(ModItems.ITEM_BIT_BAG_DEFAULT.get()));
                for (DyeColor color : DyeColor.values())
                {
                    output.accept(BitBagItem.dyeBag(new ItemStack(ModItems.ITEM_BIT_BAG_DYED.get()), color));
                }
                output.accept(new ItemStack(ModItems.MAGNIFYING_GLASS.get()));
                output.accept(new ItemStack(ModItems.ITEM_BIT_STORAGE.get()));
                output.accept(new ItemStack(ModItems.ITEM_MODIFICATION_TABLE.get()));
                output.accept(new ItemStack(ModItems.MEASURING_TAPE.get()));
                output.accept(new ItemStack(ModItems.SINGLE_USE_PATTERN_ITEM.get()));
                output.accept(new ItemStack(ModItems.MULTI_USE_PATTERN_ITEM.get()));
                output.accept(new ItemStack(ModItems.QUILL.get()));
                output.accept(new ItemStack(ModItems.SEALANT_ITEM.get()));
                output.accept(new ItemStack(ModItems.CHISELED_PRINTER.get()));
                output.accept(new ItemStack(ModItems.MONOCLE_ITEM.get()));
            })
            .build());

    public static IRegistryObject<CreativeModeTab> BITS = REGISTRAR.register("bits", () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 1)
            .icon(() -> new ItemStack(ModItems.ITEM_BLOCK_BIT.get()))
            .title(LocalStrings.CreativeTabBits.getText())
            .displayItems((parameters, output) -> {
                final Collection<ItemStack> contentGuard = ItemStackLinkedSet.createTypeAndTagSet();

                IPlatformRegistryManager.getInstance().getBlockRegistry().getValues()
                        .forEach(block -> {
                            if (block instanceof ChiseledBlock)
                                return;

                            BlockState blockState = block.defaultBlockState();
                            final Collection<IBlockInformation> defaultStateVariants = IStateVariantManager.getInstance().getAllDefaultVariants(blockState);

                            if (!defaultStateVariants.isEmpty()) {
                                defaultStateVariants.forEach(blockInformation -> {
                                    final ItemStack resultStack = IBitItemManager.getInstance().create(blockInformation);

                                    if (!resultStack.isEmpty() && resultStack.getItem() instanceof IBitItem)
                                    {
                                        if (!contentGuard.contains(resultStack) && contentGuard.add(resultStack)) {
                                            output.accept(resultStack);
                                        }
                                    }
                                });
                                return;
                            }
                            
                            //We need to make sure that we only have still fluids in the interface!
                            if (blockState.getBlock() instanceof LiquidBlock liquidBlock) {
                                final Fluid fluid = liquidBlock.getFluidState(blockState).getType();
                                if (fluid instanceof FlowingFluid flowingFluid) {
                                    blockState = flowingFluid.getSource().defaultFluidState().createLegacyBlock();
                                }
                            }

                            final BlockInformation information = new BlockInformation(blockState, Optional.empty());

                            if (IEligibilityManager.getInstance().canBeChiseled(information)) {
                                final ItemStack resultStack = IBitItemManager.getInstance().create(information);

                                if (!resultStack.isEmpty() && resultStack.getItem() instanceof IBitItem) {
                                    if (!contentGuard.contains(resultStack) && contentGuard.add(resultStack)) {
                                        output.accept(resultStack);
                                    }
                                }
                            }
                        });
            })
            .build());

    public static IRegistryObject<CreativeModeTab> CLIPBOARD = REGISTRAR.register("clipboard", () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 1)
            .icon(() -> new ItemStack(ModItems.PATTERN_SCANNER.get()))
            .title(LocalStrings.CreativeTabClipboard.getText())
            .displayItems((parameters, output) -> {
                output.acceptAll(ICreativeClipboardManager.getInstance().getClipboard()
                        .stream()
                        .map(IMultiStateItemStack::toBlockStack)
                        .toList());
            })
            .build());

    private ModCreativeTabs()
    {
        throw new IllegalStateException("Tried to initialize: ModItemGroups but this is a Utility class.");
    }

    public static void onModConstruction()
    {
        LOGGER.info("Loaded item group configuration.");
    }
}

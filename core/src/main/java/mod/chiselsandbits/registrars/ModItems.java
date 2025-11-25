package mod.chiselsandbits.registrars;

import com.communi.suggestu.scena.core.registries.deferred.IRegistrar;
import com.communi.suggestu.scena.core.registries.deferred.IRegistryObject;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.item.*;
import mod.chiselsandbits.item.bit.BitItem;
import mod.chiselsandbits.legacy.LegacyDyedBitBagItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ModItems {
    private static final Logger LOGGER = LogManager.getLogger();
    private final static IRegistrar<Item> ITEM_REGISTRAR = IRegistrar.create(Registries.ITEM, Constants.MOD_ID);
    public static final IRegistryObject<ChiselItem> ITEM_CHISEL_STONE =
            ITEM_REGISTRAR.register("chisel_stone", () -> new ChiselItem(ToolMaterial.STONE, new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "chisel_stone")))
                .stacksTo(1)));
    public static final IRegistryObject<ChiselItem> ITEM_CHISEL_COPPER =
        ITEM_REGISTRAR.register("chisel_copper", () -> new ChiselItem(ToolMaterial.STONE, new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "chisel_copper")))
            .stacksTo(1)));
    public static final IRegistryObject<ChiselItem> ITEM_CHISEL_IRON =
            ITEM_REGISTRAR.register("chisel_iron", () -> new ChiselItem(ToolMaterial.IRON, new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "chisel_iron"))).stacksTo(1)));
    public static final IRegistryObject<ChiselItem> ITEM_CHISEL_GOLD =
            ITEM_REGISTRAR.register("chisel_gold", () -> new ChiselItem(ToolMaterial.GOLD, new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "chisel_gold"))).stacksTo(1)));
    public static final IRegistryObject<ChiselItem> ITEM_CHISEL_DIAMOND =
            ITEM_REGISTRAR.register("chisel_diamond", () -> new ChiselItem(ToolMaterial.DIAMOND, new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "chisel_diamond"))).stacksTo(1)));
    public static final IRegistryObject<ChiselItem> ITEM_CHISEL_NETHERITE =
            ITEM_REGISTRAR.register("chisel_netherite", () -> new ChiselItem(ToolMaterial.NETHERITE, new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "chisel_netherite"))).stacksTo(1)));
    public static final IRegistryObject<BitItem> ITEM_BLOCK_BIT =
            ITEM_REGISTRAR.register("block_bit", () -> new BitItem(new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "block_bit")))));
    public static final IRegistryObject<MagnifyingGlassItem> MAGNIFYING_GLASS  =
            ITEM_REGISTRAR.register("magnifying_glass", () -> new MagnifyingGlassItem(new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "magnifying_glass")))));
    public static final IRegistryObject<BitBagItem> ITEM_BIT_BAG            =
            ITEM_REGISTRAR.register("bit_bag", () -> new BitBagItem(new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "bit_bag")))));
    public static final IRegistryObject<BitStorageBlockItem>
                                                    ITEM_BIT_STORAGE        =
            ITEM_REGISTRAR.register("bit_storage", () -> new BitStorageBlockItem(ModBlocks.BIT_STORAGE.get(), new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "bit_storage")))
            ));
    public static final IRegistryObject<BlockItem>
            ITEM_MODIFICATION_TABLE =
            ITEM_REGISTRAR.register("modification_table", () -> new ModificationTableBlockItem(ModBlocks.MODIFICATION_TABLE.get(), new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "modification_table")))
            ));
    public static final IRegistryObject<MeasuringTapeItem> MEASURING_TAPE =
            ITEM_REGISTRAR.register("measuring_tape", () -> new MeasuringTapeItem(new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "measuring_tape")))));
    public static final IRegistryObject<SingleUsePatternItem> SINGLE_USE_PATTERN_ITEM =
            ITEM_REGISTRAR.register("pattern_single_use", () -> new SingleUsePatternItem(new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "pattern_single_use")))));
    public static final IRegistryObject<MultiUsePatternItem> MULTI_USE_PATTERN_ITEM =
            ITEM_REGISTRAR.register("pattern_multi_use", () -> new MultiUsePatternItem(new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "pattern_multi_use")))));
    public static final IRegistryObject<QuillItem> QUILL =
            ITEM_REGISTRAR.register("quill", () -> new QuillItem(new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "quill")))));
    public static final IRegistryObject<SealantItem>              SEALANT_ITEM     =
            ITEM_REGISTRAR.register("sealant", () -> new SealantItem(new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "sealant")))));
    public static final IRegistryObject<ChiseledPrinterBlockItem> CHISELED_PRINTER =
            ITEM_REGISTRAR.register("chiseled_printer", () -> new ChiseledPrinterBlockItem(ModBlocks.CHISELED_PRINTER.get(), new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "chiseled_printer")))));
    public static final IRegistryObject<BlockItem>                PATTERN_SCANNER  =
            ITEM_REGISTRAR.register("pattern_scanner", () -> new BlockItem(ModBlocks.PATTERN_SCANNER.get(), new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "pattern_scanner")))));
    public static final IRegistryObject<WrenchItem> WRENCH =
            ITEM_REGISTRAR.register("wrench", () -> new WrenchItem(new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "wrench")))));
    public static final IRegistryObject<UnsealItem> UNSEAL_ITEM =
            ITEM_REGISTRAR.register("unseal", () -> new UnsealItem(new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "unseal")))));
    public static final IRegistryObject<MonocleItem> MONOCLE_ITEM =
            ITEM_REGISTRAR.register("monocle", () -> new MonocleItem(new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "monocle")))));

    public static final IRegistryObject<ChiseledBlockItem> CHISELED_BLOCK =
            ITEM_REGISTRAR.register("chiseled_block", () -> new ChiseledBlockItem(ModBlocks.CHISELED_BLOCK.get(), new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "chiseled_block")))));






    @SuppressWarnings("deprecation")
    public static final IRegistryObject<LegacyDyedBitBagItem> LEGACY_ITEM_BIT_BAG_DYED =
        ITEM_REGISTRAR.register("bit_bag_dyed", () -> new LegacyDyedBitBagItem(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "bit_bag_dyed")))));


    private ModItems() {
        throw new IllegalStateException("Tried to initialize: ModItems but this is a Utility class.");
    }

    public static void onModConstruction() {
        LOGGER.info("Loaded item configuration.");
    }
}

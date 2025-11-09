package mod.chiselsandbits.forge.data.advancement;

import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.registrars.ModBlocks;
import mod.chiselsandbits.registrars.ModItems;
import mod.chiselsandbits.registrars.ModTags;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.advancements.AdvancementProvider;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@EventBusSubscriber(modid = Constants.MOD_ID)
public class ChiselsAndBitsAdvancementGenerator extends AdvancementProvider
{
    @SubscribeEvent
    public static void dataGeneratorSetup(final GatherDataEvent.Client event)
    {
        event.getGenerator().addProvider(true, new ChiselsAndBitsAdvancementGenerator(event.getGenerator().getPackOutput(), event.getLookupProvider()));
    }

    public ChiselsAndBitsAdvancementGenerator(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> holderProvider) {
        super(packOutput, holderProvider, List.of(new Provider()));
    }

    private static final class Provider implements AdvancementSubProvider {

        @Override
        public void generate(HolderLookup.Provider provider, Consumer<AdvancementHolder> consumer) {
            HolderLookup<Item> itemRegistry = provider.lookupOrThrow(Registries.ITEM);
            AdvancementHolder root = Advancement.Builder.advancement()
                    .display(ModItems.ITEM_CHISEL_DIAMOND.get(),
                            Component.translatable("mod.chiselsandbits.advancements.root.title"),
                            Component.translatable("mod.chiselsandbits.advancements.root.description"),
                            ResourceLocation.withDefaultNamespace("textures/gui/advancements/backgrounds/stone.png"),
                            AdvancementType.CHALLENGE,
                            true,
                            true,
                            true)
                    .addCriterion("chisel", InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item()
                            .of(itemRegistry, ModTags.Items.CHISEL)
                            .withCount(MinMaxBounds.Ints.ANY)
                            .build()))
                    .save(consumer, Constants.MOD_ID + ":chiselsandbits/root");

            AdvancementHolder findChiselables = Advancement.Builder.advancement()
                    .parent(root)
                    .display(ModItems.MAGNIFYING_GLASS.get(),
                            Component.translatable("mod.chiselsandbits.advancements.find-chiselables.title"),
                            Component.translatable("mod.chiselsandbits.advancements.find-chiselables.description"),
                            ResourceLocation.withDefaultNamespace("textures/gui/advancements/backgrounds/stone.png"),
                            AdvancementType.TASK,
                            true,
                            true,
                            true)
                    .addCriterion("magnifier_glass", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.MAGNIFYING_GLASS.get()))
                    .save(consumer, Constants.MOD_ID + ":chiselsandbits/find_chiselables");

            AdvancementHolder collectBits = Advancement.Builder.advancement()
                    .parent(root)
                    .display(ModItems.ITEM_BIT_BAG.get(),
                            Component.translatable("mod.chiselsandbits.advancements.collect-bits.title"),
                            Component.translatable("mod.chiselsandbits.advancements.collect-bits.description"),
                            ResourceLocation.withDefaultNamespace("textures/gui/advancements/backgrounds/stone.png"),
                            AdvancementType.TASK,
                            true,
                            true,
                            true)
                    .addCriterion("bit_bag", InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item()
                            .of(itemRegistry, ModTags.Items.BIT_BAG)
                            .withCount(MinMaxBounds.Ints.ANY)
                            .build()))
                    .save(consumer, Constants.MOD_ID + ":chiselsandbits/collect_bits");

            AdvancementHolder makeTank = Advancement.Builder.advancement()
                    .parent(root)
                    .display(ModBlocks.BIT_STORAGE.get(),
                            Component.translatable("mod.chiselsandbits.advancements.make-tank.title"),
                            Component.translatable("mod.chiselsandbits.advancements.make-tank.description"),
                            ResourceLocation.withDefaultNamespace("textures/gui/advancements/backgrounds/stone.png"),
                            AdvancementType.TASK,
                            true,
                            true,
                            true)
                    .addCriterion("bit_tank", InventoryChangeTrigger.TriggerInstance.hasItems(ModBlocks.BIT_STORAGE.get()))
                    .save(consumer, Constants.MOD_ID + ":chiselsandbits/make_tank");
        }
    }
}
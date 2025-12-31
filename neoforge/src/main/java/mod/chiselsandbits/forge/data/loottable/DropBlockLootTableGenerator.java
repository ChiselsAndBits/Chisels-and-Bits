package mod.chiselsandbits.forge.data.loottable;

import com.google.common.collect.ImmutableList;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.registrars.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.WritableRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.ValidationContextSource;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = Constants.MOD_ID)
public class DropBlockLootTableGenerator extends LootTableProvider
{

    @SubscribeEvent
    public static void dataGeneratorSetup(final GatherDataEvent.Client event)
    {
        event.getGenerator().addProvider(true, new DropBlockLootTableGenerator(event.getGenerator().getPackOutput(), FeatureFlags.REGISTRY.allFlags(), event.getLookupProvider()));
    }

    private DropBlockLootTableGenerator(PackOutput packOutput, FeatureFlagSet featureFlagSet, CompletableFuture<HolderLookup.Provider> registries)
    {
        super(packOutput, Set.of(), createTables(featureFlagSet), registries);
    }

    @Override
    protected void validate(final WritableRegistry<LootTable> tables, final ValidationContextSource validationContext, final ProblemReporter.Collector problems)
    {
        //Noop
    }

    private static @NotNull List<LootTableProvider.SubProviderEntry> createTables(final FeatureFlagSet featureFlagSet)
    {
        return ImmutableList.of(
          new SubProviderEntry(
            (provider) -> new SelfDroppingBlocks(featureFlagSet, provider),
            LootContextParamSets.BLOCK
          )
        );
    }

    private static final class SelfDroppingBlocks extends BlockLootSubProvider {

        private SelfDroppingBlocks(FeatureFlagSet featureFlagSet, HolderLookup.Provider registries) {
            super(Set.of(), featureFlagSet, registries);
        }

        @Override
        protected void generate() {
            this.dropSelf(ModBlocks.CHISELED_PRINTER.get());
            this.dropSelf(ModBlocks.MODIFICATION_TABLE.get());
        }

        @Override
        protected @NotNull Iterable<Block> getKnownBlocks()
        {
            return ImmutableList.of(
              ModBlocks.CHISELED_PRINTER.get(),
              ModBlocks.MODIFICATION_TABLE.get()
            );
        }
    }
}

package mod.chiselsandbits.forge.data.tag;

import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.registrars.ModItems;
import mod.chiselsandbits.registrars.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unchecked")
public class ModItemTagGenerator extends net.neoforged.neoforge.common.data.ItemTagsProvider
{

    public ModItemTagGenerator(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> holderProvider) {
        super(packOutput, holderProvider, Constants.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        this.tag(ModTags.Items.BIT_BAG).add(ModItems.ITEM_BIT_BAG_DEFAULT.get(), ModItems.ITEM_BIT_BAG_DYED.get());
        this.tag(ModTags.Items.CHISEL).add(
                ModItems.ITEM_CHISEL_STONE.get(),
                ModItems.ITEM_CHISEL_IRON.get(),
                ModItems.ITEM_CHISEL_GOLD.get(),
                ModItems.ITEM_CHISEL_DIAMOND.get(),
                ModItems.ITEM_CHISEL_NETHERITE.get()
        );
        this.tag(ModTags.Items.FORGE_PAPER).add(Items.PAPER);
        this.tag(ItemTags.PIGLIN_LOVED).add(ModItems.ITEM_CHISEL_GOLD.get());

        this.tag(ItemTags.DURABILITY_ENCHANTABLE).addTags(ModTags.Items.CHISEL);
    }
}

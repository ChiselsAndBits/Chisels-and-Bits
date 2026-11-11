package mod.chiselsandbits.forge.data.tag;

import java.util.concurrent.CompletableFuture;

import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.registrars.ModItems;
import mod.chiselsandbits.registrars.ModTags;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.references.ItemIds;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;

@SuppressWarnings("unchecked")
public class ModItemTagGenerator extends net.neoforged.neoforge.common.data.ItemTagsProvider {
	public ModItemTagGenerator(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> holderProvider) {
		super(packOutput, holderProvider, Constants.MOD_ID);
	}

	@Override
	protected void addTags(HolderLookup.@NonNull Provider provider) {
		this.tag(ModTags.Items.BIT_BAG).add(ModItems.ITEM_BIT_BAG.key(), ModItems.LEGACY_ITEM_BIT_BAG_DYED.key());
		this.tag(ModTags.Items.CHISEL).add(
						ModItems.ITEM_CHISEL_STONE.key(),
						ModItems.ITEM_CHISEL_COPPER.key(),
						ModItems.ITEM_CHISEL_IRON.key(),
						ModItems.ITEM_CHISEL_GOLD.key(),
						ModItems.ITEM_CHISEL_DIAMOND.key(),
						ModItems.ITEM_CHISEL_NETHERITE.key()
		);
		this.tag(ModTags.Items.FORGE_PAPER).add(ItemIds.PAPER);
		this.tag(ItemTags.PIGLIN_LOVED).add(ModItems.ITEM_CHISEL_GOLD.key());
		this.tag(ItemTags.DURABILITY_ENCHANTABLE).addTags(ModTags.Items.CHISEL);
	}
}

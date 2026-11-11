package mod.chiselsandbits.forge.data.tag;

import java.util.concurrent.CompletableFuture;

import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.registrars.ModBlocks;
import mod.chiselsandbits.registrars.ModTags;
import net.minecraft.references.BlockItemIds;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;

public class ModBlockTagGenerator extends BlockTagsProvider {
	public ModBlockTagGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
		super(output, lookupProvider, Constants.MOD_ID);
	}

	@Override
	protected void addTags(HolderLookup.Provider provider) {
		this.tag(ModTags.Blocks.BLOCKED_CHISELABLE);
		this.tag(ModTags.Blocks.FORCED_CHISELABLE)
				.addTag(BlockTags.LEAVES)
				.add(BlockItemIds.GRASS_BLOCK.block())
				.add(BlockItemIds.ICE.block())
				.add(BlockItemIds.PACKED_ICE.block())
				.add(BlockItemIds.BLUE_ICE.block());
		this.tag(ModTags.Blocks.CHISELED_BLOCK).add(
						ModBlocks.CHISELED_BLOCK.key()
		);

		this.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.CHISELED_PRINTER.key());
		this.tag(BlockTags.MINEABLE_WITH_AXE).add(ModBlocks.MODIFICATION_TABLE.key())
				.add(ModBlocks.BIT_STORAGE.key())
				.add(ModBlocks.PATTERN_SCANNER.key());
	}

	@Override
	public String getName() {
		return "Chisels & Bits Block Tags";
	}
}

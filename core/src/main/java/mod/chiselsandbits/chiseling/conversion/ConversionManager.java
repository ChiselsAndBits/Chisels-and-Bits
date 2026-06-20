package mod.chiselsandbits.chiseling.conversion;

import java.util.Optional;

import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.chiseling.conversion.IConversionManager;
import mod.chiselsandbits.api.chiseling.eligibility.IEligibilityManager;
import mod.chiselsandbits.registrars.ModBlocks;

import net.minecraft.world.level.block.Block;

public class ConversionManager implements IConversionManager {
	private static final ConversionManager INSTANCE = new ConversionManager();

	public static ConversionManager getInstance()

	{
		return INSTANCE;
	}

	@Override
	public Optional<Block> getChiseledVariantOf(final Block block)

	{
		if (!IEligibilityManager.getInstance().canBeChiseled(new BlockInformation(block.defaultBlockState(), Optional.empty()))) {
			return Optional.empty();
		}

		return Optional.of(ModBlocks.CHISELED_BLOCK.get());
	}

	private ConversionManager()

	{
	}
}

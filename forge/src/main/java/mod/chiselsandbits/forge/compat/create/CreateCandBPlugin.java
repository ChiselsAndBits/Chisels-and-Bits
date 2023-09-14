package mod.chiselsandbits.forge.compat.create;

import com.simibubi.create.AllMovementBehaviours;
import com.simibubi.create.content.contraptions.BlockMovementChecks;
import mod.chiselsandbits.api.plugin.ChiselsAndBitsPlugin;
import mod.chiselsandbits.api.plugin.IChiselsAndBitsPlugin;
import mod.chiselsandbits.block.ChiseledBlock;
import mod.chiselsandbits.registrars.ModBlocks;

@ChiselsAndBitsPlugin(requiredMods = "create", isExperimental = true)
public class CreateCandBPlugin implements IChiselsAndBitsPlugin {
    @Override
    public String getId() {
        return "create";
    }

    @Override
    public void onInitialize() {
        ModBlocks.MATERIAL_TO_BLOCK_CONVERSIONS.values().forEach(blockRegistration -> {
            AllMovementBehaviours.registerBehaviour(blockRegistration.get(), new ChiseledBlockMovementBehaviour());
        });
        AllMovementBehaviours.registerBehaviour(ModBlocks.CHISELED_BLOCK.get(), new ChiseledBlockMovementBehaviour());
        BlockMovementChecks.registerMovementAllowedCheck((state, world, pos) -> {
            if (state.getBlock() instanceof ChiseledBlock)
                return BlockMovementChecks.CheckResult.SUCCESS;

            return BlockMovementChecks.CheckResult.PASS;
        });
        BlockMovementChecks.registerMovementNecessaryCheck((state, world, pos) -> {
            if (state.getBlock() instanceof ChiseledBlock)
                return BlockMovementChecks.CheckResult.SUCCESS;

            return BlockMovementChecks.CheckResult.PASS;
        });
    }
}

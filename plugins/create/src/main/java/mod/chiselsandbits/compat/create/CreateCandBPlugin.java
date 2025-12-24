package mod.chiselsandbits.compat.create;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import mod.chiselsandbits.api.plugin.ChiselsAndBitsPlugin;
import mod.chiselsandbits.api.plugin.IChiselsAndBitsPlugin;
import mod.chiselsandbits.block.ChiseledBlock;
import mod.chiselsandbits.registrars.ModBlocks;

@ChiselsAndBitsPlugin(requiredMods = "create", isExperimental = false)
public class CreateCandBPlugin implements IChiselsAndBitsPlugin {
    @Override
    public String getId() {
        return "create";
    }

    @Override
    public void onInitialize() {
        ModBlocks.MATERIAL_TO_BLOCK_CONVERSIONS.values().forEach(blockRegistration -> {
            MovementBehaviour.REGISTRY.register(blockRegistration.get(), new ChiseledBlockMovementBehaviour());
        });
        MovementBehaviour.REGISTRY.register(ModBlocks.CHISELED_BLOCK.get(), new ChiseledBlockMovementBehaviour());
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

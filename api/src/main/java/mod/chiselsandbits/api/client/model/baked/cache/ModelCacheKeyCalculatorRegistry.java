package mod.chiselsandbits.api.client.model.baked.cache;

import mod.chiselsandbits.api.IChiselsAndBitsAPI;
import net.minecraft.client.resources.model.UnbakedModel;

public interface ModelCacheKeyCalculatorRegistry
{

    static ModelCacheKeyCalculatorRegistry getInstance() {
        return IChiselsAndBitsAPI.getInstance().getBakedModelCacheKeyCalculatorRegistry();
    }

    /**
     * Register a calculator for the given key types.
     *
     * @param calculator the calculator to register.
     * @param keyType   the key types to register for.
     */
    <T extends UnbakedModel> void unbakedModel(final UnbakedModelCacheKeyCalculator<T> calculator, final Class<? extends T>... keyType);
}
